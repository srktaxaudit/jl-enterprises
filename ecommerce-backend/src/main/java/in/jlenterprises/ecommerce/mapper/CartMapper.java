package in.jlenterprises.ecommerce.mapper;

import in.jlenterprises.ecommerce.dto.cart.CartDto;
import in.jlenterprises.ecommerce.dto.cart.CartItemDto;
import in.jlenterprises.ecommerce.entity.Cart;
import in.jlenterprises.ecommerce.entity.CartItem;
import in.jlenterprises.ecommerce.entity.Product;
import in.jlenterprises.ecommerce.entity.ProductImage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Hand-written mapper for the cart — line totals and the subtotal are computed,
 * which is clearer done explicitly than via MapStruct expressions. Must be
 * called within an open transaction (touches lazy product/image associations).
 */
@Component
public class CartMapper {

    public CartDto toDto(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream().map(this::toItemDto).toList();
        BigDecimal subtotal = items.stream()
                .map(CartItemDto::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int count = items.stream().mapToInt(CartItemDto::quantity).sum();
        return new CartDto(cart.getId(), items, count, subtotal);
    }

    private CartItemDto toItemDto(CartItem item) {
        Product product = item.getProduct();
        BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemDto(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                primaryImage(product),
                item.getVariant() == null ? null : item.getVariant().getId(),
                item.getUnitPrice(),
                item.getQuantity(),
                lineTotal
        );
    }

    private String primaryImage(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) return null;
        return product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .map(ProductImage::getUrl)
                .findFirst()
                .orElseGet(() -> product.getImages().get(0).getUrl());
    }
}
