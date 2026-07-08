package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.ExchangeStatus;
import in.jlenterprises.ecommerce.constant.NotificationType;
import in.jlenterprises.ecommerce.dto.exchange.ExchangeRequestDto;
import in.jlenterprises.ecommerce.entity.ExchangeRequest;
import in.jlenterprises.ecommerce.entity.Product;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.repository.ExchangeRequestRepository;
import in.jlenterprises.ecommerce.repository.ProductRepository;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.request.exchange.ExchangeCreateRequest;
import in.jlenterprises.ecommerce.service.ExchangeService;
import in.jlenterprises.ecommerce.service.NotificationService;
import in.jlenterprises.ecommerce.storage.SupabaseStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ExchangeServiceImpl implements ExchangeService {

    private static final Set<String> IMG_TYPES =
            Set.of("image/png", "image/jpeg", "image/jpg", "image/webp");
    private static final long MAX_IMG_BYTES = 5L * 1024 * 1024;

    private final ExchangeRequestRepository repository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final SupabaseStorageService storage;

    public ExchangeServiceImpl(ExchangeRequestRepository repository, UserRepository userRepository,
                               ProductRepository productRepository, NotificationService notificationService,
                               SupabaseStorageService storage) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.notificationService = notificationService;
        this.storage = storage;
    }

    // ── Customer ──
    @Override
    @Transactional
    public ExchangeRequestDto create(UUID userId, ExchangeCreateRequest r) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        ExchangeRequest e = new ExchangeRequest();
        e.setUser(user);
        e.setApplianceCategory(r.applianceCategory().trim());
        e.setBrand(trimOrNull(r.brand()));
        e.setModelNumber(trimOrNull(r.modelNumber()));
        e.setPurchaseYear(r.purchaseYear());
        e.setConditionGrade(r.conditionGrade() == null ? null : r.conditionGrade().trim().toUpperCase());
        e.setWorking(r.working() == null || r.working());
        e.setReason(trimOrNull(r.reason()));
        e.setExpectedValue(r.expectedValue());
        if (r.imageUrls() != null) e.getImageUrls().addAll(r.imageUrls());

        if (r.desiredProductId() != null) {
            Product p = productRepository.findById(r.desiredProductId()).orElse(null);
            e.setDesiredProductId(r.desiredProductId());
            e.setDesiredProductName(p != null ? p.getName() : trimOrNull(r.desiredProductName()));
        } else {
            e.setDesiredProductName(trimOrNull(r.desiredProductName()));
        }

        e.setEstimatedValue(estimate(e));
        e.setExchangeStatus(ExchangeStatus.PENDING);
        ExchangeRequest saved = repository.save(e);

        notificationService.notifyUser(userId, NotificationType.ORDER, "Exchange request received",
                "We've estimated your old " + saved.getApplianceCategory() + " at around ₹" + saved.getEstimatedValue()
                        + ". Our team will review it and confirm the final value.", "/my-exchanges.html");
        notificationService.notifyAdmins(NotificationType.ORDER, "New exchange request",
                "Exchange request for a " + saved.getApplianceCategory() + " from " + user.getEmail()
                        + " (est. ₹" + saved.getEstimatedValue() + ").", "/admin-exchanges.html");
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRequestDto> listMine(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRequestDto> checkoutOptions(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(e -> e.getAppliedOrderId() == null)
                .filter(e -> e.getExchangeStatus() == ExchangeStatus.APPROVED
                        || e.getExchangeStatus() == ExchangeStatus.OFFER_SENT)
                .filter(e -> e.getFinalValue() != null && e.getFinalValue().signum() > 0)
                .map(this::toDto)
                .toList();
    }

    @Override
    public String uploadImage(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("Choose an image to upload.");
        String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!IMG_TYPES.contains(ct)) throw new BusinessException("Unsupported format. Use PNG, JPG or WebP.");
        if (file.getSize() > MAX_IMG_BYTES) {
            throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "Each image must be under 5 MB.");
        }
        String ext = ct.contains("png") ? "png" : ct.contains("webp") ? "webp" : "jpg";
        String objectPath = "exchanges/" + userId + "/" + UUID.randomUUID() + "." + ext;
        try {
            return storage.upload(objectPath, file.getBytes(), ct);
        } catch (java.io.IOException ex) {
            throw new BusinessException("Could not read the uploaded image.");
        }
    }

    // ── Admin ──
    @Override
    @Transactional(readOnly = true)
    public Page<ExchangeRequestDto> list(ExchangeStatus status, Pageable pageable) {
        Page<ExchangeRequest> page = status == null
                ? repository.findAll(pageable)
                : repository.findByExchangeStatus(status, pageable);
        return page.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ExchangeRequestDto get(UUID id) {
        return toDto(entity(id));
    }

    @Override
    @Transactional
    public ExchangeRequestDto updateStatus(UUID id, ExchangeStatus status, String internalNotes) {
        ExchangeRequest e = entity(id);
        ExchangeStatus from = e.getExchangeStatus();
        // Guard illegal jumps (e.g. moving out of a terminal state). Re-setting the
        // same status is allowed so an admin can update notes without a status change.
        if (from != status && !TRANSITIONS.getOrDefault(from, Set.of()).contains(status)) {
            throw new BusinessException("Cannot change an exchange request from " + from + " to " + status + ".");
        }
        e.setExchangeStatus(status);
        if (internalNotes != null) e.setInternalNotes(internalNotes);
        // Approving with no explicit value falls back to the auto-estimate.
        if (status == ExchangeStatus.APPROVED && e.getFinalValue() == null) {
            e.setFinalValue(e.getEstimatedValue());
        }
        ExchangeRequest saved = repository.save(e);
        notifyStatus(saved);
        return toDto(saved);
    }

    @Override
    @Transactional
    public ExchangeRequestDto setFinalValue(UUID id, BigDecimal finalValue) {
        if (finalValue == null || finalValue.signum() < 0) throw new BusinessException("Enter a valid exchange value.");
        ExchangeRequest e = entity(id);
        e.setFinalValue(finalValue.setScale(2, java.math.RoundingMode.HALF_UP));
        return toDto(repository.save(e));
    }

    // ── Checkout integration ──
    @Override
    @Transactional(readOnly = true)
    public BigDecimal valueForCheckout(UUID userId, UUID exchangeId) {
        ExchangeRequest e = entity(exchangeId);
        if (!e.getUser().getId().equals(userId)) {
            throw new BusinessException("This exchange request is not yours.");
        }
        if (e.getAppliedOrderId() != null) {
            throw new BusinessException("This exchange has already been used on another order.");
        }
        if (e.getExchangeStatus() != ExchangeStatus.APPROVED && e.getExchangeStatus() != ExchangeStatus.OFFER_SENT) {
            throw new BusinessException("This exchange request is not approved yet.");
        }
        if (e.getFinalValue() == null || e.getFinalValue().signum() <= 0) {
            throw new BusinessException("This exchange does not have an approved value yet.");
        }
        return e.getFinalValue();
    }

    @Override
    @Transactional
    public void applyToOrder(UUID exchangeId, UUID orderId) {
        ExchangeRequest e = entity(exchangeId);
        e.setExchangeStatus(ExchangeStatus.COMPLETED);
        e.setAppliedOrderId(orderId);
        repository.save(e);
        notificationService.notifyUser(e.getUser().getId(), NotificationType.ORDER, "Exchange applied",
                "Your exchange value of ₹" + e.getFinalValue() + " was applied to your order.", "/my-exchanges.html");
    }

    // ── helpers ──

    /** Allowed status transitions (admin). Terminal states (REJECTED, COMPLETED,
        CANCELLED) have no outgoing edges, so a used/closed exchange cannot be reopened. */
    private static final Map<ExchangeStatus, Set<ExchangeStatus>> TRANSITIONS = Map.of(
            ExchangeStatus.PENDING, Set.of(ExchangeStatus.UNDER_REVIEW, ExchangeStatus.INSPECTION_SCHEDULED,
                    ExchangeStatus.OFFER_SENT, ExchangeStatus.APPROVED, ExchangeStatus.REJECTED, ExchangeStatus.CANCELLED),
            ExchangeStatus.UNDER_REVIEW, Set.of(ExchangeStatus.INSPECTION_SCHEDULED, ExchangeStatus.OFFER_SENT,
                    ExchangeStatus.APPROVED, ExchangeStatus.REJECTED, ExchangeStatus.CANCELLED),
            ExchangeStatus.INSPECTION_SCHEDULED, Set.of(ExchangeStatus.OFFER_SENT, ExchangeStatus.APPROVED,
                    ExchangeStatus.REJECTED, ExchangeStatus.CANCELLED),
            ExchangeStatus.OFFER_SENT, Set.of(ExchangeStatus.APPROVED, ExchangeStatus.REJECTED, ExchangeStatus.CANCELLED),
            ExchangeStatus.APPROVED, Set.of(ExchangeStatus.COMPLETED, ExchangeStatus.CANCELLED));

    private ExchangeRequest entity(UUID id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("ExchangeRequest", id));
    }

    private void notifyStatus(ExchangeRequest e) {
        String extra = e.getExchangeStatus() == ExchangeStatus.APPROVED && e.getFinalValue() != null
                ? " Approved value: ₹" + e.getFinalValue() + ". Apply it at checkout on your next purchase."
                : "";
        notificationService.notifyUser(e.getUser().getId(), NotificationType.ORDER,
                "Exchange request " + e.getExchangeStatus(),
                "Your exchange request for the " + e.getApplianceCategory() + " is now "
                        + e.getExchangeStatus() + "." + extra, "/my-exchanges.html");
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // Rough auto-valuation: category base × age depreciation × condition factor.
    private static final Map<String, BigDecimal> BASE = Map.of(
            "air conditioner", new BigDecimal("8000"),
            "refrigerator", new BigDecimal("6000"),
            "television", new BigDecimal("5000"),
            "washing machine", new BigDecimal("4000"),
            "kitchen", new BigDecimal("2500"),
            "furniture", new BigDecimal("1500"));

    private static BigDecimal baseFor(String category) {
        String c = category == null ? "" : category.toLowerCase();
        if (c.contains("air") || c.equals("ac")) return BASE.get("air conditioner");
        if (c.contains("fridge") || c.contains("refriger")) return BASE.get("refrigerator");
        if (c.contains("tv") || c.contains("tele")) return BASE.get("television");
        if (c.contains("wash")) return BASE.get("washing machine");
        if (c.contains("kitchen") || c.contains("stove") || c.contains("micro")) return BASE.get("kitchen");
        if (c.contains("furnitur") || c.contains("sofa") || c.contains("bed")) return BASE.get("furniture");
        return new BigDecimal("2000");
    }

    private static double conditionFactor(String grade) {
        if (grade == null) return 0.5;
        return switch (grade.trim().toUpperCase()) {
            case "EXCELLENT" -> 1.0;
            case "GOOD" -> 0.75;
            case "FAIR" -> 0.5;
            case "POOR" -> 0.3;
            case "NOT_WORKING" -> 0.2;
            default -> 0.5;
        };
    }

    private static BigDecimal estimate(ExchangeRequest e) {
        BigDecimal base = baseFor(e.getApplianceCategory());
        int years = 0;
        if (e.getPurchaseYear() != null && e.getPurchaseYear() > 1990) {
            years = Math.max(0, Year.now().getValue() - e.getPurchaseYear());
        }
        double ageFactor = Math.max(0.2, 1.0 - years * 0.08);
        double condFactor = conditionFactor(e.getConditionGrade());
        if (!e.isWorking()) condFactor *= 0.4;
        double val = base.doubleValue() * ageFactor * condFactor;
        long rounded = Math.round(val / 10.0) * 10L; // nearest ₹10
        return BigDecimal.valueOf(Math.max(0, rounded));
    }

    private ExchangeRequestDto toDto(ExchangeRequest e) {
        User u = e.getUser();
        String name = u == null ? "" :
                (((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                        + (u.getLastName() == null ? "" : u.getLastName())).trim());
        if (name.isEmpty() && u != null) name = u.getEmail();
        return new ExchangeRequestDto(
                e.getId(), name, u == null ? null : u.getEmail(),
                e.getApplianceCategory(), e.getBrand(), e.getModelNumber(), e.getPurchaseYear(),
                e.getConditionGrade(), e.isWorking(), e.getReason(),
                List.copyOf(e.getImageUrls()),
                e.getExpectedValue(), e.getEstimatedValue(), e.getFinalValue(),
                e.getDesiredProductId(), e.getDesiredProductName(),
                e.getExchangeStatus(), e.getInternalNotes(), e.getAppliedOrderId(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
