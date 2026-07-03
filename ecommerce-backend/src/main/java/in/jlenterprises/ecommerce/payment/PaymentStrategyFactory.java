package in.jlenterprises.ecommerce.payment;

import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves the {@link PaymentStrategy} for a {@link PaymentMethod} (Factory pattern). */
@Component
public class PaymentStrategyFactory {

    private final Map<PaymentMethod, PaymentStrategy> strategies;

    public PaymentStrategyFactory(List<PaymentStrategy> strategyBeans) {
        this.strategies = strategyBeans.stream()
                .collect(Collectors.toMap(PaymentStrategy::method, Function.identity()));
    }

    public PaymentStrategy forMethod(PaymentMethod method) {
        PaymentStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new BusinessException("Unsupported payment method: " + method);
        }
        return strategy;
    }
}
