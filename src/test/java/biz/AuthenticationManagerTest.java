package biz;

import db.dao.DAO;
import model.Account;
import model.Operation;
import model.Password;
import model.Role;
import model.User;
import model.exceptions.UserUnnkownOrBadPasswordException;
import model.operations.Interest;
import model.operations.LogIn;
import model.operations.LogOut;
import model.operations.OperationType;
import model.operations.PaymentIn;
import model.operations.Withdraw;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthenticationManagerTest {

    private AuthenticationManager authManager;

    @Mock
    private DAO mockDao;

    @Mock
    private BankHistory mockHistory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authManager = new AuthenticationManager(mockDao, mockHistory);
    }
    @Test
    @DisplayName("logIn - powodzenie logowania z poprawnym hasłem")
    void logIn_successWithCorrectPassword() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski", "User");
        Password password = new Password();
        password.setUserId(1);
        password.setPasswd(AuthenticationManager.hashPassword("password123".toCharArray()));

        when(mockDao.findUserByName("jan")).thenReturn(user);
        when(mockDao.findPasswordForUser(user)).thenReturn(password);

        // When
        User result = authManager.logIn("jan", "password123".toCharArray());

        // Then
        assertNotNull(result);
        assertEquals(user, result);
        verify(mockHistory).logLoginSuccess(user);
    }

    @Test
    @DisplayName("logIn - niepowodzenie z błędnym hasłem")
    void logIn_failureWithWrongPassword() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski", "User");
        Password password = new Password();
        password.setUserId(1);
        password.setPasswd(AuthenticationManager.hashPassword("correctPassword".toCharArray()));

        when(mockDao.findUserByName("jan")).thenReturn(user);
        when(mockDao.findPasswordForUser(user)).thenReturn(password);

        // When/Then
        assertThrows(UserUnnkownOrBadPasswordException.class, () -> {
            authManager.logIn("jan", "wrongPassword".toCharArray());
        });

        verify(mockHistory).logLoginFailure(user, "Bad Password");
    }

    @Test
    @DisplayName("logIn - niepowodzenie z nieistniejącym użytkownikiem")
    void logIn_failureWithNonExistentUser() throws Exception {
        // Given
        when(mockDao.findUserByName("ghost")).thenReturn(null);

        // When/Then
        assertThrows(UserUnnkownOrBadPasswordException.class, () -> {
            authManager.logIn("ghost", "anyPassword".toCharArray());
        });

        verify(mockHistory).logLoginFailure(null, "Zła nazwa użytkownika ghost");
    }

    @Test
    @DisplayName("logOut - powodzenie wylogowania")
    void logOut_success() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski", "User");

        // When
        boolean result = authManager.logOut(user);

        // Then
        assertTrue(result);
        verify(mockHistory).logLogOut(user);
    }

    @Test
    @DisplayName("canInvokeOperation - admin może wszystko")
    void canInvokeOperation_adminCanDoEverything() {
        // Given
        User admin = createUser(1, "Admin User", "Admin");

        // When/Then - wszystkie typy operacji
        assertTrue(authManager.canInvokeOperation(mockPaymentIn(), admin));
        assertTrue(authManager.canInvokeOperation(mockWithdraw(admin), admin));
        assertTrue(authManager.canInvokeOperation(mockInterest(), admin));
        assertTrue(authManager.canInvokeOperation(mockLogIn(), admin));
        assertTrue(authManager.canInvokeOperation(mockLogOut(), admin));
    }

    @Test
    @DisplayName("canInvokeOperation - każdy może wykonać PAYMENT_IN")
    void canInvokeOperation_anyoneCanPaymentIn() {
        // Given
        User regularUser = createUser(2, "Regular User", "User");

        // When
        boolean result = authManager.canInvokeOperation(mockPaymentIn(), regularUser);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("canInvokeOperation - użytkownik może wypłacać ze swojego konta")
    void canInvokeOperation_userCanWithdrawFromOwnAccount() {
        // Given
        User user = createUser(2, "Jan Kowalski", "User");
        Withdraw withdraw = mockWithdraw(user);

        // When
        boolean result = authManager.canInvokeOperation(withdraw, user);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("canInvokeOperation - użytkownik NIE może wypłacać z cudzego konta")
    void canInvokeOperation_userCannotWithdrawFromOthersAccount() {
        // Given
        User owner = createUser(1, "Jan Kowalski", "User");
        User attacker = createUser(2, "Attacker", "User");
        Withdraw withdraw = mockWithdraw(owner);

        // When
        boolean result = authManager.canInvokeOperation(withdraw, attacker);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("canInvokeOperation - zwykły użytkownik nie może wykonać LOG_IN")
    void canInvokeOperation_regularUserCannotLogIn() {
        // Given
        User user = createUser(2, "Regular User", "User");

        // When
        boolean result = authManager.canInvokeOperation(mockLogIn(), user);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("canInvokeOperation - zwykły użytkownik nie może wykonać LOG_OUT")
    void canInvokeOperation_regularUserCannotLogOut() {
        // Given
        User user = createUser(2, "Regular User", "User");

        // When
        boolean result = authManager.canInvokeOperation(mockLogOut(), user);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("canInvokeOperation - zwykły użytkownik nie może wykonać INTEREST")
    void canInvokeOperation_regularUserCannotInterest() {
        // Given
        User user = createUser(2, "Regular User", "User");

        // When
        boolean result = authManager.canInvokeOperation(mockInterest(), user);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("hashPassword - generuje hash dla hasła")
    void hashPassword_generatesHash() {
        // Given
        char[] password = "testPassword123".toCharArray();

        // When
        String hash = AuthenticationManager.hashPassword(password);

        // Then
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
    }

    @Test
    @DisplayName("hashPassword - różne hasła generują różne hashe")
    void hashPassword_differentPasswordsDifferentHashes() {
        // Given
        char[] password1 = "password1".toCharArray();
        char[] password2 = "password2".toCharArray();

        // When
        String hash1 = AuthenticationManager.hashPassword(password1);
        String hash2 = AuthenticationManager.hashPassword(password2);

        // Then
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("hashPassword - te same hasła generują te same hashe")
    void hashPassword_samePasswordsSameHashes() {
        // Given
        char[] password1 = "samePassword".toCharArray();
        char[] password2 = "samePassword".toCharArray();

        // When
        String hash1 = AuthenticationManager.hashPassword(password1);
        String hash2 = AuthenticationManager.hashPassword(password2);

        // Then
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("hashPassword - czyści tablicę hasła")
    void hashPassword_clearsPasswordArray() {
        // Given
        char[] password = "secretPassword".toCharArray();
        char[] originalCopy = password.clone();

        // When
        AuthenticationManager.hashPassword(password);

        // Then
        assertFalse(java.util.Arrays.equals(password, originalCopy),
            "Hasło powinno być wyczyszczone");
    }

    @Test
    @DisplayName("hashPassword - obsługuje puste hasło")
    void hashPassword_handlesEmptyPassword() {
        // Given
        char[] emptyPassword = "".toCharArray();

        // When
        String hash = AuthenticationManager.hashPassword(emptyPassword);

        // Then
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
    }

    // Helper methods
    private User createUser(int id, String name, String roleName) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        
        Role role = new Role();
        role.setId(id);
        role.setName(roleName);
        user.setRole(role);
        
        return user;
    }

    private Operation mockPaymentIn() {
        User user = createUser(1, "Test", "User");
        Account account = new Account();
        return new PaymentIn(user, 100.0, "Test", account);
    }

    private Withdraw mockWithdraw(User user) {
        Account account = new Account();
        return new Withdraw(user, 100.0, "Test", account);
    }

    private Operation mockInterest() {
        User user = createUser(1, "Test", "User");
        Account account = new Account();
        return new Interest(user, 10.0, "Test", account);
    }

    private Operation mockLogIn() {
        User user = createUser(1, "Test", "User");
        return new LogIn(user, "Test");
    }

    private Operation mockLogOut() {
        User user = createUser(1, "Test", "User");
        return new LogOut(user, "Test");
    }

  @Test
@DisplayName("canInvokeOperation - rzuca błąd gdy user.getRole() == null")
void canInvokeOperation_roleNull_isError() {
    User user = new User();
    user.setId(1);
    user.setName("Test User");
    user.setRole(null);

    Operation operation = mock(Operation.class);
    when(operation.getType()).thenReturn(OperationType.WITHDRAW);

    assertThrows(NullPointerException.class,
            () -> authManager.canInvokeOperation(operation, user));
}

@Test
@DisplayName("PAYMENT_IN - brak kontroli uprawnień jest błędem")
void paymentIn_shouldNotBeAllowedForRegularUser() {
    User regularUser = new User();
    regularUser.setId(1);
    Role role = new Role();
    role.setName("User");
    regularUser.setRole(role);

    Operation paymentIn = mock(Operation.class);
    when(paymentIn.getType()).thenReturn(OperationType.PAYMENT_IN);

    boolean result = authManager.canInvokeOperation(paymentIn, regularUser);

    assertFalse(result,
            "PAYMENT_IN nie powinno być dozwolone dla zwykłego użytkownika bez dodatkowych warunków");
}

@Test
@DisplayName("WITHDRAW - brak sprawdzenia właściciela konta powinien być traktowany jako błąd")
void withdraw_withoutAccountOwnershipValidation_isError() {
    Role role = new Role();
    role.setName("User");

    User attacker = new User();
    attacker.setId(2);
    attacker.setRole(role);

    Withdraw withdraw = mock(Withdraw.class);
    when(withdraw.getType()).thenReturn(OperationType.WITHDRAW);
    when(withdraw.getUser()).thenReturn(attacker);

    boolean result = authManager.canInvokeOperation(withdraw, attacker);

    assertFalse(result,
            "WITHDRAW powinno sprawdzać właściciela konta, a nie tylko user w operacji");
}

@Test
@DisplayName("INTEREST - brak jawnej reguły powinien skutkować wyjątkiem")
void interest_operation_withoutRule_shouldThrow() {
    User user = new User();
    user.setId(1);
    Role role = new Role();
    role.setName("User");
    user.setRole(role);

    Operation interest = mock(Operation.class);
    when(interest.getType()).thenReturn(OperationType.INTEREST);

    assertThrows(IllegalStateException.class,
            () -> authManager.canInvokeOperation(interest, user),
            "Brak reguły dla INTEREST powinien być błędem, a nie cichym false");
}

@Test
@DisplayName("rola - porównanie case-sensitive powinno powodować błąd konfiguracji")
void roleName_caseSensitive_isError() {
    User user = new User();
    user.setId(1);
    Role role = new Role();
    role.setName("admin");
    user.setRole(role);

    Operation operation = mock(Operation.class);
    when(operation.getType()).thenReturn(OperationType.WITHDRAW);

    assertThrows(IllegalStateException.class,
            () -> authManager.canInvokeOperation(operation, user),
            "Nieprawidłowa nazwa roli powinna powodować błąd konfiguracji");
}

@Test
@DisplayName("logIn - brak walidacji userName == null powinien rzucać wyjątek")
void logIn_nullUserName_isError() throws SQLException {
    assertThrows(IllegalArgumentException.class,
            () -> authManager.logIn(null, "password".toCharArray()));
}

@Test
@DisplayName("logIn - brak walidacji password == null powinien rzucać wyjątek")
void logIn_nullPassword_isError() throws SQLException {
    when(mockDao.findUserByName("user")).thenReturn(mock(User.class));
    when(mockDao.findPasswordForUser(any())).thenReturn(mock(Password.class));

    assertThrows(IllegalArgumentException.class,
            () -> authManager.logIn("user", null));
}

@Test
@DisplayName("logIn - różne komunikaty błędów to błąd bezpieczeństwa")
void logIn_revealsUserExistence_isError() throws SQLException {
    when(mockDao.findUserByName("ghost")).thenReturn(null);

    UserUnnkownOrBadPasswordException ex =
            assertThrows(UserUnnkownOrBadPasswordException.class,
                    () -> authManager.logIn("ghost", "password".toCharArray()));

    assertEquals("Nieprawidłowe dane logowania", ex.getMessage(),
            "Komunikat powinien być identyczny niezależnie od przyczyny");
}

}