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

    // Settings are business config, not a secret store (real secrets live in env
    // vars). As defense-in-depth, never echo back a value whose key looks like a
    // credential — so an accidentally-stored secret can't leak via the admin API.
    private static final java.util.regex.Pattern SENSITIVE_KEY =
            java.util.regex.Pattern.compile("(?i)(secret|token|password|passwd|api[_-]?key|private[_-]?key|access[_-]?key)");

    private static boolean isSensitive(String key) {
        return key != null && SENSITIVE_KEY.matcher(key).find();
    }

    private final AppSettingRepository repository;

    public SettingServiceImpl(AppSettingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettingDto> list() {
        return repository.findAll().stream()
                .map(s -> new SettingDto(s.getKey(),
                        isSensitive(s.getKey()) ? "••••••••" : s.getValue(),
                        s.getUpdatedAt()))
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
