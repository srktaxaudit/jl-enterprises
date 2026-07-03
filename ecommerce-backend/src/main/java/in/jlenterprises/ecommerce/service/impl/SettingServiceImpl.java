package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.dto.admin.SettingDto;
import in.jlenterprises.ecommerce.entity.AppSetting;
import in.jlenterprises.ecommerce.repository.AppSettingRepository;
import in.jlenterprises.ecommerce.service.SettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SettingServiceImpl implements SettingService {

    private final AppSettingRepository repository;

    public SettingServiceImpl(AppSettingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettingDto> list() {
        return repository.findAll().stream()
                .map(s -> new SettingDto(s.getKey(), s.getValue(), s.getUpdatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public SettingDto upsert(String key, String value) {
        AppSetting setting = repository.findById(key).orElseGet(() -> {
            AppSetting s = new AppSetting();
            s.setKey(key);
            return s;
        });
        setting.setValue(value);
        setting.setUpdatedAt(Instant.now());
        AppSetting saved = repository.save(setting);
        return new SettingDto(saved.getKey(), saved.getValue(), saved.getUpdatedAt());
    }

    @Override
    @Transactional
    public void delete(String key) {
        repository.deleteById(key);
    }
}
