package biz;

import db.dao.DAO;
import model.Account;
import model.Role;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


class InterestOperatorTest {

    private InterestOperator interestOperator;

    @Mock
    private DAO mockDao;

    @Mock
    private AccountManager mockAccountManager;

    @Mock
    private User mockInterestUser;

    @Mock
    private BankHistory mockBankHistory;


    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        interestOperator = new InterestOperator(mockDao, mockAccountManager);
        setField(interestOperator, "bankHistory", mockBankHistory);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

     @Test
    @DisplayName("countInterestForAccount - powodzenie naliczenia odsetek")
    void countInterestForAccount_success() throws Exception {
        // Given
        User interestUser = createUser(99, "InterestOperator");
        Account account = createAccount(1, 10000.0);
        
        when(mockDao.findUserByName("InterestOperator")).thenReturn(interestUser);
        when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(true);

        // When
        interestOperator.countInterestForAccount(account);

        // Then
        // Odsetki = 10000 * 0.2 = 2000
        verify(mockAccountManager).paymentIn(
            eq(interestUser), 
            eq(2000.0), 
            eq("Interest ..."), 
            eq(1)
        );
        verify(mockBankHistory).logOperation(any(), eq(true));
    }

    @Test
    @DisplayName("countInterestForAccount - niepowodzenie paymentIn")
    void countInterestForAccount_paymentInFails() throws Exception {
        // Given
        User interestUser = createUser(99, "InterestOperator");
        Account account = createAccount(1, 5000.0);
        
        when(mockDao.findUserByName("InterestOperator")).thenReturn(interestUser);
        when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(false);

        // When
        interestOperator.countInterestForAccount(account);

        // Then
        verify(mockBankHistory).logOperation(any(), eq(false));
    }

    @Test
    @DisplayName("countInterestForAccount - dla konta z zerowym saldem")
    void countInterestForAccount_zeroBalance() throws Exception {
        // Given
        User interestUser = createUser(99, "InterestOperator");
        Account account = createAccount(1, 0.0);
        
        when(mockDao.findUserByName("InterestOperator")).thenReturn(interestUser);
        when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(true);

        // When
        interestOperator.countInterestForAccount(account);

        // Then
        // Odsetki = 0 * 0.2 = 0
        verify(mockAccountManager).paymentIn(
            eq(interestUser), 
            eq(0.0), 
            anyString(), 
            eq(1)
        );
    }

    @Test
    @DisplayName("countInterestForAccount - dla konta z dużym saldem")
    void countInterestForAccount_largeBalance() throws Exception {
        // Given
        User interestUser = createUser(99, "InterestOperator");
        Account account = createAccount(1, 100000.0);
        
        when(mockDao.findUserByName("InterestOperator")).thenReturn(interestUser);
        when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(true);

        // When
        interestOperator.countInterestForAccount(account);

        // Then
        // Odsetki = 100000 * 0.2 = 20000
        verify(mockAccountManager).paymentIn(
            eq(interestUser), 
            eq(20000.0), 
            anyString(), 
            eq(1)
        );
    }

    @Test
    @DisplayName("countInterestForAccount - dla konta z małym saldem")
    void countInterestForAccount_smallBalance() throws Exception {
        // Given
        User interestUser = createUser(99, "InterestOperator");
        Account account = createAccount(1, 100.0);
        
        when(mockDao.findUserByName("InterestOperator")).thenReturn(interestUser);
        when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(true);

        // When
        interestOperator.countInterestForAccount(account);

        // Then
        // Odsetki = 100 * 0.2 = 20
        verify(mockAccountManager).paymentIn(
            eq(interestUser), 
            eq(20.0), 
            anyString(), 
            eq(1)
        );
    }

    @Test
    @DisplayName("countInterestForAccount - wielokrotne wywołanie dla tego samego konta")
    void countInterestForAccount_multipleCalls() throws Exception {
        // Given
        User interestUser = createUser(99, "InterestOperator");
        Account account = createAccount(1, 1000.0);
        
        when(mockDao.findUserByName("InterestOperator")).thenReturn(interestUser);
        when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(true);

        // When
        interestOperator.countInterestForAccount(account);
        interestOperator.countInterestForAccount(account);
        interestOperator.countInterestForAccount(account);

        // Then
        verify(mockAccountManager, times(3)).paymentIn(any(), eq(200.0), anyString(), anyInt());
        verify(mockBankHistory, times(3)).logOperation(any(), eq(true));
    }

    @Test
    @DisplayName("countInterestForAccount - SQLException podczas paymentIn")
    void countInterestForAccount_sqlExceptionDuringPayment() throws Exception {
        // Given
        User interestUser = createUser(99, "InterestOperator");
        Account account = createAccount(1, 1000.0);
        
        when(mockDao.findUserByName("InterestOperator")).thenReturn(interestUser);
        when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenThrow(new SQLException("Database error"));

        // When/Then
        assertThrows(SQLException.class, () -> {
            interestOperator.countInterestForAccount(account);
        });
    }

    @Test
    @DisplayName("constructor - inicjalizuje dao i accountManager")
    void constructor_initializesFields() {
        // When
        InterestOperator operator = new InterestOperator(mockDao, mockAccountManager);

        // Then
        assertNotNull(operator);
    }

    @Test
    @DisplayName("countInterestForAccount - sprawdza czy używa poprawnego współczynnika")
    void countInterestForAccount_usesCorrectInterestFactor() throws Exception {
        // Given
        User interestUser = createUser(99, "InterestOperator");
        Account account = createAccount(1, 1000.0);
        
        when(mockDao.findUserByName("InterestOperator")).thenReturn(interestUser);
        when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenAnswer(invocation -> {
                double amount = invocation.getArgument(1);
                // Sprawdzamy czy odsetki = 1000 * 0.2 = 200
                assertEquals(200.0, amount, 0.01);
                return true;
            });

        // When
        interestOperator.countInterestForAccount(account);

        // Then
        verify(mockAccountManager).paymentIn(any(), eq(200.0), anyString(), eq(1));
    }

    @Test
    @DisplayName("countInterestForAccount - używa właściwego opisu")
    void countInterestForAccount_usesCorrectDescription() throws Exception {
        // Given
        User interestUser = createUser(99, "InterestOperator");
        Account account = createAccount(1, 1000.0);
        
        when(mockDao.findUserByName("InterestOperator")).thenReturn(interestUser);
        when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(true);

        // When
        interestOperator.countInterestForAccount(account);

        // Then
        verify(mockAccountManager).paymentIn(
            any(), 
            anyDouble(), 
            eq("Interest ..."), 
            anyInt()
        );
    }

    @Test
    @DisplayName("countInterestForAccount - używa ID konta")
    void countInterestForAccount_usesAccountId() throws Exception {
        // Given
        User interestUser = createUser(99, "InterestOperator");
        Account account = createAccount(42, 1000.0);
        
        when(mockDao.findUserByName("InterestOperator")).thenReturn(interestUser);
        when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(true);

        // When
        interestOperator.countInterestForAccount(account);

        // Then
        verify(mockAccountManager).paymentIn(
            any(), 
            anyDouble(), 
            anyString(), 
            eq(42)
        );
    }

    private User createUser(int id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        Role role = new Role();
        role.setId(1);
        role.setName("System");
        user.setRole(role);
        return user;
    }

    private Account createAccount(int id, double amount) {
        Account account = new Account();
        account.setId(id);
        account.setAmmount(amount);
        User owner = createUser(1, "Owner");
        account.setOwner(owner);
        return account;
    }

  @Test
@DisplayName("bankHistory - brak inicjalizacji może powodować NullPointerException")
void bankHistory_notInitialized_canCauseNPE() throws Exception {
    InterestOperator operatorWithoutHistory = new InterestOperator(mockDao, mockAccountManager);
    Account account = new Account();
    account.setId(1);
    account.setAmmount(1000.0);

    when(mockDao.findUserByName("InterestOperator")).thenReturn(mockInterestUser);
    when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(true);

    assertThrows(NullPointerException.class,
            () -> operatorWithoutHistory.countInterestForAccount(account),
            "bankHistory nie jest ustawione w konstruktorze");
}

@Test
@DisplayName("InterestOperator user - brak obsługi gdy użytkownik nie istnieje")
void interestOperatorUser_missing_canCauseNPE() throws SQLException {
    Account account = new Account();
    account.setId(1);
    account.setAmmount(1000.0);

    when(mockDao.findUserByName("InterestOperator")).thenReturn(null);

    assertThrows(NullPointerException.class,
            () -> interestOperator.countInterestForAccount(account),
            "Brak obsługi sytuacji, gdy DAO zwraca null dla użytkownika InterestOperator");
}

@Test
@DisplayName("współczynnik odsetek - wartość 0.2 może być niezamierzona")
void interestFactor_mayBeTooHigh() throws SQLException {
    Account account = new Account();
    account.setId(1);
    account.setAmmount(10000.0);

    when(mockDao.findUserByName("InterestOperator")).thenReturn(mockInterestUser);

    when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenAnswer(invocation -> {
                double amount = invocation.getArgument(1);
                assertEquals(2000.0, amount, 0.01,
                        "Odsetki wychodzą 20% salda; warto doprecyzować stawkę i okres naliczania");
                return true;
            });

    interestOperator.countInterestForAccount(account);

    verify(mockAccountManager).paymentIn(any(), eq(2000.0), anyString(), eq(1));
}

@Test
@DisplayName("naliczanie odsetek - brak kontroli wielokrotnych naliczeń")
void noControlOverMultipleInterestCalculations() throws SQLException {
    Account account = new Account();
    account.setId(1);
    account.setAmmount(1000.0);

    when(mockDao.findUserByName("InterestOperator")).thenReturn(mockInterestUser);
    when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(true);

    interestOperator.countInterestForAccount(account);
    interestOperator.countInterestForAccount(account);
    interestOperator.countInterestForAccount(account);

    verify(mockAccountManager, times(3))
            .paymentIn(any(), anyDouble(), anyString(), anyInt());
}

@Test
@DisplayName("walidacja wejścia - brak obsługi null account")
void noValidationForNullAccount() throws SQLException {
    when(mockDao.findUserByName("InterestOperator")).thenReturn(mockInterestUser);

    assertThrows(NullPointerException.class,
            () -> interestOperator.countInterestForAccount(null),
            "Brak walidacji parametru account (null)");
}

@Test
@DisplayName("saldo 0 lub ujemne - naliczanie odsetek może generować zbędne operacje")
void interestForZeroOrNegativeBalance() throws SQLException {
    Account account = new Account();
    account.setId(1);
    account.setAmmount(0.0);

    when(mockDao.findUserByName("InterestOperator")).thenReturn(mockInterestUser);
    when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(true);

    interestOperator.countInterestForAccount(account);

    verify(mockAccountManager).paymentIn(any(), eq(0.0), anyString(), anyInt());
}

@Test
@DisplayName("logowanie - status powinien odzwierciedlać wynik paymentIn")
void logsOperationReflectsPaymentStatus() throws SQLException {
    Account account = new Account();
    account.setId(1);
    account.setAmmount(1000.0);

    when(mockDao.findUserByName("InterestOperator")).thenReturn(mockInterestUser);
    when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(false);

    BankHistory spyHistory = mock(BankHistory.class);
    interestOperator.bankHistory = spyHistory;

    interestOperator.countInterestForAccount(account);

    verify(spyHistory).logOperation(any(), eq(false));
}

}