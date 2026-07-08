package in.jlenterprises.ecommerce.constant.hr;

/** How an employee is paid — drives payroll computation. */
public enum EmploymentType {
    FULL_TIME,     // fixed monthly salary
    PART_TIME,     // fixed monthly (lower) — treated like FULL_TIME for pay
    DAILY_WAGE,    // paid per payable day (dailyRate × days)
    SERVICE_BASED  // paid per service/unit (perServiceRate × units)
}
