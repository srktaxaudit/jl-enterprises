package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.admin.SettingDto;

import java.util.List;

public interface SettingService {

    List<SettingDto> list();

    SettingDto upsert(String key, String value);

    void delete(String key);
}
