package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.dto.customer.AddressDto;
import in.jlenterprises.ecommerce.entity.Address;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.mapper.AddressMapper;
import in.jlenterprises.ecommerce.repository.AddressRepository;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.request.customer.AddressRequest;
import in.jlenterprises.ecommerce.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    public AddressServiceImpl(AddressRepository addressRepository, UserRepository userRepository,
                              AddressMapper addressMapper) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.addressMapper = addressMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDto> list(UUID userId) {
        return addressMapper.toDtoList(addressRepository.findByUserId(userId));
    }

    @Override
    @Transactional
    public AddressDto create(UUID userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        Address address = addressMapper.toEntity(request);
        address.setUser(user);
        if (request.defaultAddress()) {
            clearExistingDefault(userId);
            address.setDefaultAddress(true);
        }
        return addressMapper.toDto(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressDto update(UUID userId, UUID addressId, AddressRequest request) {
        Address address = getOwned(userId, addressId);
        addressMapper.updateEntity(address, request);
        if (request.defaultAddress()) {
            clearExistingDefault(userId);
            address.setDefaultAddress(true);
        }
        return addressMapper.toDto(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID addressId) {
        Address address = getOwned(userId, addressId);
        address.setDeleted(true);
        addressRepository.save(address);
    }

    @Override
    @Transactional
    public AddressDto setDefault(UUID userId, UUID addressId) {
        Address address = getOwned(userId, addressId);
        clearExistingDefault(userId);
        address.setDefaultAddress(true);
        return addressMapper.toDto(addressRepository.save(address));
    }

    private void clearExistingDefault(UUID userId) {
        addressRepository.findByUserIdAndDefaultAddressTrue(userId).ifPresent(existing -> {
            existing.setDefaultAddress(false);
            addressRepository.save(existing);
        });
    }

    private Address getOwned(UUID userId, UUID addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Address", addressId));
    }
}
