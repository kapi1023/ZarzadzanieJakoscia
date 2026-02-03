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


class InterestTest {

    @Mock
    private User mockUser;

    @Mock
    private Account mockAccount;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockUser.getName()).thenReturn("Piotr Wiśniewski");
        when(mockAccount.getId()).thenReturn(3);
    }

    @Test
    @DisplayName("Powinien utworzyć operację odsetek z poprawnymi parametrami")
    void shouldCreateInterestWithCorrectParameters() {
        double amount = 50.0;
        String description = "Odsetki miesięczne";
        
        Interest interest = new Interest(mockUser, amount, description, mockAccount);
        
        assertNotNull(interest);
        assertEquals(amount, interest.getAmmount());
        assertEquals(description, interest.getDescription());
        assertEquals(mockUser, interest.getUser());
        assertEquals(mockAccount, interest.getAccount());
        assertEquals(OperationType.INTEREST, interest.getType());
    }

    @Test
    @DisplayName("Powinien ustawić datę utworzenia operacji")
    void shouldSetCreationDate() {
        Date before = new Date();
        
        Interest interest = new Interest(mockUser, 25.0, "Test", mockAccount);
        
        Date after = new Date();
        
        assertNotNull(interest.getDate());
        assertTrue(interest.getDate().getTime() >= before.getTime());
        assertTrue(interest.getDate().getTime() <= after.getTime());
    }

    @Test
    @DisplayName("Powinien utworzyć operację odsetek z małą kwotą")
    void shouldCreateInterestWithSmallAmount() {
        Interest interest = new Interest(mockUser, 0.01, "Minimalne odsetki", mockAccount);
        
        assertEquals(0.01, interest.getAmmount(), 0.001);
    }

    @Test
    @DisplayName("Powinien utworzyć operację odsetek z kwotą zero")
    void shouldCreateInterestWithZeroAmount() {
        Interest interest = new Interest(mockUser, 0.0, "Brak odsetek", mockAccount);
        
        assertEquals(0.0, interest.getAmmount());
    }

    @Test
    @DisplayName("Powinien utworzyć operację odsetek z dużą kwotą")
    void shouldCreateInterestWithLargeAmount() {
        double largeAmount = 5_000.0;
        
        Interest interest = new Interest(mockUser, largeAmount, "Roczne odsetki", mockAccount);
        
        assertEquals(largeAmount, interest.getAmmount());
    }

    @Test
    @DisplayName("Powinien mieć poprawny typ operacji")
    void shouldHaveCorrectOperationType() {
        Interest interest = new Interest(mockUser, 100.0, "Odsetki", mockAccount);
        
        assertEquals(OperationType.INTEREST, interest.getType());
        assertEquals(4, interest.getType().getId());
    }

    @Test
    @DisplayName("Powinien zachować referencję do konta")
    void shouldMaintainAccountReference() {
        Interest interest = new Interest(mockUser, 75.0, "Test", mockAccount);
        
        assertSame(mockAccount, interest.getAccount());
    }

    @Test
    @DisplayName("Powinien zachować referencję do użytkownika")
    void shouldMaintainUserReference() {
        Interest interest = new Interest(mockUser, 75.0, "Test", mockAccount);
        
        assertSame(mockUser, interest.getUser());
    }

    @Test
    @DisplayName("Powinien utworzyć operację z opisem zawierającym znaki specjalne")
    void shouldCreateInterestWithSpecialCharactersInDescription() {
        String specialDescription = "Odsetki: 5% p.a. (2024-2025)";
        
        Interest interest = new Interest(mockUser, 125.0, specialDescription, mockAccount);
        
        assertEquals(specialDescription, interest.getDescription());
    }
}
