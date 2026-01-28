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
 * Testy jednostkowe dla klasy Withdraw
 */
class WithdrawTest {

    @Mock
    private User mockUser;

    @Mock
    private Account mockAccount;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockUser.getName()).thenReturn("Anna Nowak");
        when(mockAccount.getId()).thenReturn(2);
    }

    @Test
    @DisplayName("Powinien utworzyć operację wypłaty z poprawnymi parametrami")
    void shouldCreateWithdrawWithCorrectParameters() {
        double amount = 300.0;
        String description = "Wypłata z bankomatu";
        
        Withdraw withdraw = new Withdraw(mockUser, amount, description, mockAccount);
        
        assertNotNull(withdraw);
        assertEquals(amount, withdraw.getAmmount());
        assertEquals(description, withdraw.getDescription());
        assertEquals(mockUser, withdraw.getUser());
        assertEquals(mockAccount, withdraw.getAccount());
        assertEquals(OperationType.WITHDRAW, withdraw.getType());
    }

    @Test
    @DisplayName("Powinien ustawić datę utworzenia operacji")
    void shouldSetCreationDate() {
        Date before = new Date();
        
        Withdraw withdraw = new Withdraw(mockUser, 100.0, "Test", mockAccount);
        
        Date after = new Date();
        
        assertNotNull(withdraw.getDate());
        assertTrue(withdraw.getDate().getTime() >= before.getTime());
        assertTrue(withdraw.getDate().getTime() <= after.getTime());
    }

    @Test
    @DisplayName("Powinien utworzyć operację wypłaty z małą kwotą")
    void shouldCreateWithdrawWithSmallAmount() {
        Withdraw withdraw = new Withdraw(mockUser, 0.01, "Minimalna wypłata", mockAccount);
        
        assertEquals(0.01, withdraw.getAmmount(), 0.001);
    }

    @Test
    @DisplayName("Powinien utworzyć operację wypłaty z dużą kwotą")
    void shouldCreateWithdrawWithLargeAmount() {
        double largeAmount = 50_000.0;
        
        Withdraw withdraw = new Withdraw(mockUser, largeAmount, "Duża wypłata", mockAccount);
        
        assertEquals(largeAmount, withdraw.getAmmount());
    }

    @Test
    @DisplayName("Powinien utworzyć operację wypłaty z pustym opisem")
    void shouldCreateWithdrawWithEmptyDescription() {
        Withdraw withdraw = new Withdraw(mockUser, 200.0, "", mockAccount);
        
        assertEquals("", withdraw.getDescription());
    }

    @Test
    @DisplayName("Powinien mieć poprawny typ operacji")
    void shouldHaveCorrectOperationType() {
        Withdraw withdraw = new Withdraw(mockUser, 150.0, "Wypłata", mockAccount);
        
        assertEquals(OperationType.WITHDRAW, withdraw.getType());
        assertEquals(1, withdraw.getType().getId());
    }

    @Test
    @DisplayName("Powinien zachować referencję do konta")
    void shouldMaintainAccountReference() {
        Withdraw withdraw = new Withdraw(mockUser, 250.0, "Test", mockAccount);
        
        assertSame(mockAccount, withdraw.getAccount());
    }

    @Test
    @DisplayName("Powinien zachować referencję do użytkownika")
    void shouldMaintainUserReference() {
        Withdraw withdraw = new Withdraw(mockUser, 250.0, "Test", mockAccount);
        
        assertSame(mockUser, withdraw.getUser());
    }
}
