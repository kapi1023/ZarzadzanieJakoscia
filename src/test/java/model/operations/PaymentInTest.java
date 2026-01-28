package model.operations;

import model.Account;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Testy jednostkowe dla klasy PaymentIn
 */
class PaymentInTest {

    @Mock
    private User mockUser;

    @Mock
    private Account mockAccount;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockUser.getName()).thenReturn("Jan Kowalski");
        when(mockAccount.getId()).thenReturn(1);
    }

    @Test
    @DisplayName("Powinien utworzyć operację wpłaty z poprawnymi parametrami")
    void shouldCreatePaymentInWithCorrectParameters() {
        double amount = 500.0;
        String description = "Wpłata gotówkowa";
        
        PaymentIn paymentIn = new PaymentIn(mockUser, amount, description, mockAccount);
        
        assertNotNull(paymentIn);
        assertEquals(amount, paymentIn.getAmmount());
        assertEquals(description, paymentIn.getDescription());
        assertEquals(mockUser, paymentIn.getUser());
        assertEquals(mockAccount, paymentIn.getAccount());
        assertEquals(OperationType.PAYMENT_IN, paymentIn.getType());
    }

    @Test
    @DisplayName("Powinien ustawić datę utworzenia operacji")
    void shouldSetCreationDate() {
        Date before = new Date();
        
        PaymentIn paymentIn = new PaymentIn(mockUser, 100.0, "Test", mockAccount);
        
        Date after = new Date();
        
        assertNotNull(paymentIn.getDate());
        assertTrue(paymentIn.getDate().getTime() >= before.getTime());
        assertTrue(paymentIn.getDate().getTime() <= after.getTime());
    }

    @Test
    @DisplayName("Powinien utworzyć operację wpłaty z zerową kwotą")
    void shouldCreatePaymentInWithZeroAmount() {
        PaymentIn paymentIn = new PaymentIn(mockUser, 0.0, "Testowa wpłata", mockAccount);
        
        assertEquals(0.0, paymentIn.getAmmount());
    }

    @Test
    @DisplayName("Powinien utworzyć operację wpłaty z dużą kwotą")
    void shouldCreatePaymentInWithLargeAmount() {
        double largeAmount = 1_000_000.0;
        
        PaymentIn paymentIn = new PaymentIn(mockUser, largeAmount, "Duża wpłata", mockAccount);
        
        assertEquals(largeAmount, paymentIn.getAmmount());
    }

    @Test
    @DisplayName("Powinien utworzyć operację wpłaty z pustym opisem")
    void shouldCreatePaymentInWithEmptyDescription() {
        PaymentIn paymentIn = new PaymentIn(mockUser, 200.0, "", mockAccount);
        
        assertEquals("", paymentIn.getDescription());
    }

    @Test
    @DisplayName("Powinien utworzyć operację wpłaty z długim opisem")
    void shouldCreatePaymentInWithLongDescription() {
        String longDescription = "a".repeat(500);
        
        PaymentIn paymentIn = new PaymentIn(mockUser, 300.0, longDescription, mockAccount);
        
        assertEquals(longDescription, paymentIn.getDescription());
    }

    @Test
    @DisplayName("Powinien mieć poprawny typ operacji")
    void shouldHaveCorrectOperationType() {
        PaymentIn paymentIn = new PaymentIn(mockUser, 150.0, "Wpłata", mockAccount);
        
        assertEquals(OperationType.PAYMENT_IN, paymentIn.getType());
        assertEquals(0, paymentIn.getType().getId());
    }
}
