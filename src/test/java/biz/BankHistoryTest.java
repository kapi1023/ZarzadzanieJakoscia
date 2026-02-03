package biz;

import db.dao.DAO;
import model.Account;
import model.Operation;
import model.Role;
import model.User;
import model.operations.Interest;
import model.operations.LogIn;
import model.operations.LogOut;
import model.operations.PaymentIn;
import model.operations.Withdraw;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;

class BankHistoryTest {

    private BankHistory bankHistory;

    @Mock
    private DAO mockDao;

    @Mock
    private Account mockAccount;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bankHistory = new BankHistory(mockDao);
    }

     @Test
    @DisplayName("logLoginSuccess - loguje pomyślne logowanie")
    void logLoginSuccess() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");

        // When
        bankHistory.logLoginSuccess(user);

        // Then
        verify(mockDao).logOperation(any(LogIn.class), eq(true));
    }

    @Test
    @DisplayName("logLoginFailure - loguje nieudane logowanie z użytkownikiem")
    void logLoginFailure_withUser() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        String info = "Złe hasło";

        // When
        bankHistory.logLoginFailure(user, info);

        // Then
        verify(mockDao).logOperation(any(LogIn.class), eq(false));
    }

    @Test
    @DisplayName("logLoginFailure - loguje nieudane logowanie bez użytkownika")
    void logLoginFailure_withoutUser() throws Exception {
        // Given
        String info = "Zła nazwa użytkownika test";

        // When
        bankHistory.logLoginFailure(null, info);

        // Then
        verify(mockDao).logOperation(any(LogIn.class), eq(false));
    }

    @Test
    @DisplayName("logLogOut - loguje wylogowanie")
    void logLogOut() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");

        // When
        bankHistory.logLogOut(user);

        // Then
        verify(mockDao).logOperation(any(LogOut.class), eq(true));
    }

    @Test
    @DisplayName("logOperation - deleguje do DAO z powodzeniem")
    void logOperation_success() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Operation operation = new LogIn(user, "Test login");

        // When
        bankHistory.logOperation(operation, true);

        // Then
        verify(mockDao).logOperation(operation, true);
    }

    @Test
    @DisplayName("logOperation - deleguje do DAO z niepowodzeniem")
    void logOperation_failure() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Operation operation = new LogOut(user, "Test logout");

        // When
        bankHistory.logOperation(operation, false);

        // Then
        verify(mockDao).logOperation(operation, false);
    }

    @Test
    @DisplayName("logOperation - PaymentIn")
    void logOperation_paymentIn() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Account account = createAccount(1, 1000.0);
        Operation operation = new PaymentIn(user, 500.0, "Wpłata", account);

        // When
        bankHistory.logOperation(operation, true);

        // Then
        verify(mockDao).logOperation(operation, true);
    }

    @Test
    @DisplayName("logOperation - Withdraw")
    void logOperation_withdraw() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Account account = createAccount(1, 1000.0);
        Operation operation = new Withdraw(user, 300.0, "Wypłata", account);

        // When
        bankHistory.logOperation(operation, true);

        // Then
        verify(mockDao).logOperation(operation, true);
    }

    @Test
    @DisplayName("logOperation - Interest")
    void logOperation_interest() throws Exception {
        // Given
        User user = createUser(1, "System");
        Account account = createAccount(1, 10000.0);
        Operation operation = new Interest(user, 2000.0, "Odsetki", account);

        // When
        bankHistory.logOperation(operation, true);

        // Then
        verify(mockDao).logOperation(operation, true);
    }

    @Test
    @DisplayName("logPaymentIn - rzuca RuntimeException")
    void logPaymentIn_throwsException() {
        // Given
        Account account = createAccount(1, 1000.0);

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            bankHistory.logPaymentIn(account, 100.0, true);
        });
    }

    @Test
    @DisplayName("logPaymentOut - rzuca RuntimeException")
    void logPaymentOut_throwsException() {
        // Given
        Account account = createAccount(1, 1000.0);

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            bankHistory.logPaymentOut(account, 100.0, true);
        });
    }

    @Test
    @DisplayName("logUnauthorizedOperation - rzuca RuntimeException")
    void logUnauthorizedOperation_throwsException() {
        // Given
        User user = createUser(1, "Attacker");
        Account account = createAccount(1, 1000.0);
        Operation operation = new Withdraw(user, 300.0, "Unauthorized", account);

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            bankHistory.logUnauthorizedOperation(operation, false);
        });
    }

    @Test
    @DisplayName("logOperation - SQLException jest propagowany")
    void logOperation_propagatesSQLException() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Operation operation = new LogIn(user, "Test");
        
        doThrow(new SQLException("Database error"))
            .when(mockDao).logOperation(any(), anyBoolean());

        // When/Then
        assertThrows(SQLException.class, () -> {
            bankHistory.logOperation(operation, true);
        });
    }

    @Test
    @DisplayName("logLoginSuccess - wiele wywołań")
    void logLoginSuccess_multipleCalls() throws Exception {
        // Given
        User user1 = createUser(1, "User1");
        User user2 = createUser(2, "User2");
        User user3 = createUser(3, "User3");

        // When
        bankHistory.logLoginSuccess(user1);
        bankHistory.logLoginSuccess(user2);
        bankHistory.logLoginSuccess(user3);

        // Then
        verify(mockDao, times(3)).logOperation(any(LogIn.class), eq(true));
    }

    @Test
    @DisplayName("logLogOut - wiele wywołań")
    void logLogOut_multipleCalls() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");

        // When
        bankHistory.logLogOut(user);
        bankHistory.logLogOut(user);

        // Then
        verify(mockDao, times(2)).logOperation(any(LogOut.class), eq(true));
    }

    @Test
    @DisplayName("constructor - inicjalizuje dao")
    void constructor_initializesDao() {
        // When
        BankHistory history = new BankHistory(mockDao);

        // Then
        assertNotNull(history);
    }

    @Test
    @DisplayName("logLoginFailure - różne komunikaty błędów")
    void logLoginFailure_differentMessages() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");

        // When
        bankHistory.logLoginFailure(user, "Złe hasło");
        bankHistory.logLoginFailure(user, "Konto zablokowane");
        bankHistory.logLoginFailure(null, "Nieznany użytkownik");

        // Then
        verify(mockDao, times(3)).logOperation(any(LogIn.class), eq(false));
    }

    @Test
    @DisplayName("logOperation - wszystkie typy operacji")
    void logOperation_allOperationTypes() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Account account = createAccount(1, 1000.0);

        Operation login = new LogIn(user, "Login");
        Operation logout = new LogOut(user, "Logout");
        Operation paymentIn = new PaymentIn(user, 100.0, "Payment", account);
        Operation withdraw = new Withdraw(user, 50.0, "Withdraw", account);
        Operation interest = new Interest(user, 20.0, "Interest", account);

        // When
        bankHistory.logOperation(login, true);
        bankHistory.logOperation(logout, true);
        bankHistory.logOperation(paymentIn, true);
        bankHistory.logOperation(withdraw, false);
        bankHistory.logOperation(interest, true);

        // Then
        verify(mockDao, times(5)).logOperation(any(Operation.class), anyBoolean());
    }

    @Test
    @DisplayName("logOperation - operacje z różnymi statusami")
    void logOperation_differentStatuses() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Operation operation1 = new LogIn(user, "Test1");
        Operation operation2 = new LogIn(user, "Test2");
        Operation operation3 = new LogIn(user, "Test3");

        // When
        bankHistory.logOperation(operation1, true);
        bankHistory.logOperation(operation2, false);
        bankHistory.logOperation(operation3, true);

        // Then
        verify(mockDao).logOperation(operation1, true);
        verify(mockDao).logOperation(operation2, false);
        verify(mockDao).logOperation(operation3, true);
    }

    // Helper methods
    private User createUser(int id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        Role role = new Role();
        role.setId(1);
        role.setName("User");
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
@DisplayName("logPaymentIn - rzuca RuntimeException (metoda niezaimplementowana)")
void logPaymentIn_throwsRuntimeException() {
    double amount = 100.0;
    boolean success = true;

    assertThrows(RuntimeException.class,
            () -> bankHistory.logPaymentIn(mockAccount, amount, success),
            "logPaymentIn nie jest zaimplementowane i rzuca RuntimeException");
}

@Test
@DisplayName("logPaymentOut - rzuca RuntimeException (metoda niezaimplementowana)")
void logPaymentOut_throwsRuntimeException() {
    double amount = 100.0;
    boolean success = true;

    assertThrows(RuntimeException.class,
            () -> bankHistory.logPaymentOut(mockAccount, amount, success),
            "logPaymentOut nie jest zaimplementowane i rzuca RuntimeException");
}

@Test
@DisplayName("logUnauthorizedOperation - rzuca RuntimeException mimo że jest wywoływane")
void logUnauthorizedOperation_throwsButIsCalled() {
    User user = new User();
    user.setId(1);

    model.operations.Withdraw operation =
            new model.operations.Withdraw(user, 100.0, "Test", mockAccount);

    assertThrows(RuntimeException.class,
            () -> bankHistory.logUnauthorizedOperation(operation, false),
            "logUnauthorizedOperation rzuca RuntimeException; jeśli metoda jest wywoływana w kodzie, spowoduje awarię");
}

@Test
@DisplayName("logLoginFailure - akceptuje null user")
void logLoginFailure_withNullUser() {
    User nullUser = null;
    String info = "Zła nazwa użytkownika test";

    assertDoesNotThrow(() -> bankHistory.logLoginFailure(nullUser, info),
            "Metoda nie rzuca wyjątku dla user=null; warto doprecyzować czy Operation z user=null jest poprawne");
}

@Test
@DisplayName("walidacja parametrów - brak obsługi null account w logPaymentIn")
void noParameterValidation_nullAccount() {
    assertThrows(NullPointerException.class,
            () -> bankHistory.logPaymentIn(null, 100.0, true),
            "Brak walidacji account=null");
}

@Test
@DisplayName("logOperation - deleguje do DAO")
void logOperation_delegatesToDao() {
    User user = new User();
    user.setId(1);

    model.operations.LogIn logIn =
            new model.operations.LogIn(user, "Test login");

    assertDoesNotThrow(() -> bankHistory.logOperation(logIn, true));
}

}   