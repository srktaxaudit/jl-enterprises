package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.ExchangeStatus;
import in.jlenterprises.ecommerce.entity.ExchangeRequest;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.repository.ExchangeRequestRepository;
import in.jlenterprises.ecommerce.service.impl.ExchangeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A trade-in credit consumed by an order must come back to the customer when that order is
 * cancelled or refunded — including automatic cancellation by the abandoned-order sweeper.
 * Before this fix the credit was permanently lost (COMPLETED is terminal for admin edits).
 */
@ExtendWith(MockitoExtension.class)
class ExchangeReleaseTest {

    @Mock ExchangeRequestRepository repository;
    @Mock NotificationService notificationService;

    @InjectMocks ExchangeServiceImpl service;

    @Test
    void releaseRestoresCreditAndUnlinksOrder() {
        UUID orderId = UUID.randomUUID();
        User user = new User();
        user.setEmail("c@example.com");
        ExchangeRequest e = new ExchangeRequest();
        e.setUser(user);
        e.setExchangeStatus(ExchangeStatus.COMPLETED);
        e.setAppliedOrderId(orderId);
        e.setFinalValue(new BigDecimal("8000.00"));
        when(repository.findByAppliedOrderId(orderId)).thenReturn(Optional.of(e));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.releaseFromOrder(orderId);

        assertEquals(ExchangeStatus.APPROVED, e.getExchangeStatus(), "credit must be spendable again");
        assertNull(e.getAppliedOrderId(), "the cancelled order must be unlinked");
        verify(repository).save(e);
    }

    @Test
    void releaseIsANoOpWhenTheOrderUsedNoExchange() {
        UUID orderId = UUID.randomUUID();
        when(repository.findByAppliedOrderId(orderId)).thenReturn(Optional.empty());

        service.releaseFromOrder(orderId);

        verify(repository, never()).save(any());
    }
}
