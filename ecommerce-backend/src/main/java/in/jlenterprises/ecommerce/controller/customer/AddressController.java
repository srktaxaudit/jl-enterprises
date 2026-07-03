package in.jlenterprises.ecommerce.controller.customer;

import in.jlenterprises.ecommerce.dto.customer.AddressDto;
import in.jlenterprises.ecommerce.request.customer.AddressRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.security.SecurityUtils;
import in.jlenterprises.ecommerce.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/addresses")
@Tag(name = "Addresses", description = "Customer address book")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    @Operation(summary = "List my addresses")
    public ApiResponse<List<AddressDto>> list() {
        return ApiResponse.success(addressService.list(SecurityUtils.currentUserId()));
    }

    @PostMapping
    @Operation(summary = "Add a new address")
    public ResponseEntity<ApiResponse<AddressDto>> create(@Valid @RequestBody AddressRequest request) {
        AddressDto dto = addressService.create(SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Address added", dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an address")
    public ApiResponse<AddressDto> update(@PathVariable UUID id, @Valid @RequestBody AddressRequest request) {
        return ApiResponse.success("Address updated", addressService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an address")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        addressService.delete(SecurityUtils.currentUserId(), id);
        return ApiResponse.message("Address deleted");
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "Mark an address as default")
    public ApiResponse<AddressDto> setDefault(@PathVariable UUID id) {
        return ApiResponse.success("Default address set", addressService.setDefault(SecurityUtils.currentUserId(), id));
    }
}
