package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.entity.NumberSequence;
import in.jlenterprises.ecommerce.repository.NumberSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Allocates gap-tolerant, collision-free, non-reusable running numbers per key.
 * Used for statutory voucher / document numbering — the old {@code count()+1} scheme
 * both raced (two posts could get the same number) and re-issued a number after a
 * delete. Here the counter row is locked and only advances, so neither can happen.
 */
@Service
public class NumberSequenceService {

    private final NumberSequenceRepository repo;

    public NumberSequenceService(NumberSequenceRepository repo) {
        this.repo = repo;
    }

    /**
     * Next value for {@code seqKey}. Runs in the caller's transaction and locks the row, so
     * concurrent callers serialize and a rolled-back caller releases the value (no reuse across
     * committed rows). The first allocation for a brand-new key creates its row starting at 1.
     */
    @Transactional
    public long next(String seqKey) {
        NumberSequence s = repo.findForUpdate(seqKey)
                .orElseGet(() -> repo.save(new NumberSequence(seqKey, 1)));
        long value = s.getNextValue();
        s.setNextValue(value + 1);
        repo.save(s);
        return value;
    }
}
