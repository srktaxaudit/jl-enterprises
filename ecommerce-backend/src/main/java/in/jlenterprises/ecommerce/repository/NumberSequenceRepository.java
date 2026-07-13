package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.NumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NumberSequenceRepository extends JpaRepository<NumberSequence, String> {

    /** Fetch the counter row with a write lock (SELECT … FOR UPDATE) so concurrent allocations serialize. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from NumberSequence s where s.seqKey = :key")
    Optional<NumberSequence> findForUpdate(@Param("key") String key);
}
