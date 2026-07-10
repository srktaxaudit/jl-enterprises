package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.dto.catalog.BannerDto;
import in.jlenterprises.ecommerce.entity.Banner;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.repository.BannerRepository;
import in.jlenterprises.ecommerce.request.admin.BannerRequest;
import in.jlenterprises.ecommerce.service.BannerService;
import in.jlenterprises.ecommerce.storage.SupabaseStorageService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class BannerServiceImpl implements BannerService {

    private static final Set<String> IMG_TYPES = Set.of("image/png", "image/jpeg", "image/jpg", "image/webp");
    private static final long MAX_IMG_BYTES = 5L * 1024 * 1024;

    private final BannerRepository bannerRepository;
    private final SupabaseStorageService storage;

    public BannerServiceImpl(BannerRepository bannerRepository, SupabaseStorageService storage) {
        this.bannerRepository = bannerRepository;
        this.storage = storage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerDto> listActive(String position) {
        List<Banner> banners = (position == null || position.isBlank())
                ? bannerRepository.findByActiveTrueOrderBySortOrderAsc()
                : bannerRepository.findByPositionAndActiveTrueOrderBySortOrderAsc(position.trim());
        Instant now = Instant.now();
        return banners.stream()
                .filter(b -> (b.getStartsAt() == null || !b.getStartsAt().isAfter(now))
                          && (b.getEndsAt() == null || !b.getEndsAt().isBefore(now)))
                .map(BannerServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerDto> listAll() {
        return bannerRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder"))
                .stream().map(BannerServiceImpl::toDto).toList();
    }

    @Override
    @Transactional
    public BannerDto create(BannerRequest r) {
        Banner b = new Banner();
        apply(b, r);
        return toDto(bannerRepository.save(b));
    }

    @Override
    @Transactional
    public BannerDto update(UUID id, BannerRequest r) {
        Banner b = bannerRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Banner", id));
        apply(b, r);
        return toDto(bannerRepository.save(b));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Banner b = bannerRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Banner", id));
        b.setDeleted(true);
        bannerRepository.save(b);
    }

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("Choose an image to upload.");
        String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!IMG_TYPES.contains(ct)) throw new BusinessException("Unsupported format. Use PNG, JPG or WebP.");
        if (file.getSize() > MAX_IMG_BYTES) throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "Image must be under 5 MB.");
        String ext = ct.contains("png") ? "png" : ct.contains("webp") ? "webp" : "jpg";
        String objectPath = "banners/" + UUID.randomUUID() + "." + ext;
        try {
            return storage.upload(objectPath, file.getBytes(), ct);
        } catch (java.io.IOException ex) {
            throw new BusinessException("Could not read the uploaded image.");
        }
    }

    private static void apply(Banner b, BannerRequest r) {
        b.setTitle(r.title().trim());
        b.setImageUrl(r.imageUrl().trim());
        b.setLinkUrl(r.linkUrl() == null || r.linkUrl().isBlank() ? null : r.linkUrl().trim());
        b.setPosition(r.position() == null || r.position().isBlank() ? "HERO" : r.position().trim().toUpperCase());
        b.setSortOrder(r.sortOrder());
        b.setActive(r.active() == null || r.active());
        b.setStartsAt(r.startsAt());
        b.setEndsAt(r.endsAt());
    }

    private static BannerDto toDto(Banner b) {
        return new BannerDto(b.getId(), b.getTitle(), b.getImageUrl(), b.getLinkUrl(),
                b.getPosition(), b.getSortOrder(), b.isActive(), b.getStartsAt(), b.getEndsAt());
    }
}
