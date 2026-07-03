package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BannerRepository extends JpaRepository<Banner, UUID> {

    List<Banner> findByActiveTrueOrderBySortOrderAsc();

    List<Banner> findByPositionAndActiveTrueOrderBySortOrderAsc(String position);
}
