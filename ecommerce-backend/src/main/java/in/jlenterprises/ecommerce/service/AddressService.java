package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.customer.AddressDto;
import in.jlenterprises.ecommerce.request.customer.AddressRequest;

import java.util.List;
import java.util.UUID;

/** Address book operations, always scoped to the owning user. */
public interface AddressService {

    List<AddressDto> list(UUID userId);

    AddressDto create(UUID userId, AddressRequest request);

    AddressDto update(UUID userId, UUID addressId, AddressRequest request);

    void delete(UUID userId, UUID addressId);

    AddressDto setDefault(UUID userId, UUID addressId);
}
