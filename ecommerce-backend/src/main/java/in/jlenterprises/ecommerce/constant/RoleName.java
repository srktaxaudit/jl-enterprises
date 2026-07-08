package in.jlenterprises.ecommerce.constant;

/** Roles / authorities. New roles are auto-seeded by DataInitializer on startup. */
public enum RoleName {
    ROLE_SUPER_ADMIN,
    ROLE_ADMIN,
    ROLE_MANAGER,
    // ── Department-scoped staff roles ──
    ROLE_INVENTORY_MANAGER,
    ROLE_ORDER_MANAGER,
    ROLE_PRODUCT_MANAGER,
    ROLE_MARKETING_MANAGER,
    ROLE_CUSTOMER_SUPPORT,
    ROLE_ACCOUNTANT,
    ROLE_HR,
    ROLE_CUSTOMER
}
