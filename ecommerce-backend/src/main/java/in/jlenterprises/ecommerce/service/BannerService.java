package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.catalog.BannerDto;
import in.jlenterprises.ecommerce.request.admin.BannerRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface BannerService {

    /** Public: active banners for a position (or all positions if null), within their schedule. */
    List<BannerDto> listActive(String position);

    /** Admin: every banner (incl. inactive), ordered by sort order. */
    List<BannerDto> listAll();

    BannerDto create(BannerRequest request);

    BannerDto update(UUID id, BannerRequest request);

    void delete(UUID id);

    /** Upload a banner image to storage; returns its public URL. */
    String uploadImage(MultipartFile file);
}
