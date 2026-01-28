package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe dla klasy Account
 */
class AccountTest {

    private Account account;
    
    @Mock
    private User mockUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        account = new Account();
        account.setId(1);
        account.setAmmount(1000.0);
        account.setOwner(mockUser);
    }

    @Test
    @DisplayName("Powinien utworzyć konto z poprawnymi wartościami")
    void shouldCreateAccountWithCorrectValues() {
        assertEquals(1, account.getId());
        assertEquals(1000.0, account.getAmmount());
        assertEquals(mockUser, account.getOwner());
    }

    @Test
    @DisplayName("Powinien zwiększyć saldo przy poprawnej wpłacie")
    void shouldIncreaseBalanceOnValidIncome() {
        boolean result = account.income(500.0);
        
        assertTrue(result);
        assertEquals(1500.0, account.getAmmount());
    }

    @Test
    @DisplayName("Powinien odrzucić ujemną wpłatę")
    void shouldRejectNegativeIncome() {
        boolean result = account.income(-100.0);
        
        assertFalse(result);
        assertEquals(1000.0, account.getAmmount());
    }

    @Test
    @DisplayName("Powinien zaakceptować wpłatę zero")
    void shouldAcceptZeroIncome() {
        boolean result = account.income(0.0);
        
        assertTrue(result);
        assertEquals(1000.0, account.getAmmount());
    }

    @Test
    @DisplayName("Powinien zmniejszyć saldo przy poprawnej wypłacie")
    void shouldDecreaseBalanceOnValidOutcome() {
        boolean result = account.outcome(300.0);
        
        assertTrue(result);
        assertEquals(700.0, account.getAmmount());
    }

    @Test
    @DisplayName("Powinien odrzucić wypłatę większą niż saldo")
    void shouldRejectOutcomeGreaterThanBalance() {
        boolean result = account.outcome(1500.0);
        
        assertFalse(result);
        assertEquals(1000.0, account.getAmmount());
    }

    @Test
    @DisplayName("Powinien odrzucić wypłatę mniejszą niż 0.01")
    void shouldRejectOutcomeLessThanMinimum() {
        boolean result = account.outcome(0.001);
        
        assertFalse(result);
        assertEquals(1000.0, account.getAmmount());
    }

    @Test
    @DisplayName("Powinien zaakceptować wypłatę równą 0.01")
    void shouldAcceptOutcomeEqualToMinimum() {
        boolean result = account.outcome(0.01);
        
        assertTrue(result);
        assertEquals(999.99, account.getAmmount(), 0.001);
    }

    @Test
    @DisplayName("Powinien odrzucić ujemną wypłatę")
    void shouldRejectNegativeOutcome() {
        boolean result = account.outcome(-50.0);
        
        assertFalse(result);
        assertEquals(1000.0, account.getAmmount());
    }

    @Test
    @DisplayName("Powinien zaakceptować wypłatę całego salda")
    void shouldAcceptOutcomeOfEntireBalance() {
        boolean result = account.outcome(1000.0);
        
        assertTrue(result);
        assertEquals(0.0, account.getAmmount());
    }

    @Test
    @DisplayName("Powinien poprawnie ustawić i pobrać ID")
    void shouldSetAndGetId() {
        account.setId(100);
        assertEquals(100, account.getId());
    }

    @Test
    @DisplayName("Powinien poprawnie ustawić i pobrać saldo")
    void shouldSetAndGetAmmount() {
        account.setAmmount(5000.0);
        assertEquals(5000.0, account.getAmmount());
    }

    @Test
    @DisplayName("Powinien poprawnie ustawić i pobrać właściciela")
    void shouldSetAndGetOwner() {
        User newUser = new User();
        account.setOwner(newUser);
        assertEquals(newUser, account.getOwner());
    }

    @Test
    @DisplayName("Powinien wykonać sekwencję operacji wpłat i wypłat")
    void shouldHandleSequenceOfOperations() {
        account.income(500.0);  // 1500
        account.outcome(300.0); // 1200
        account.income(200.0);  // 1400
        account.outcome(100.0); // 1300
        
        assertEquals(1300.0, account.getAmmount());
    }
}
