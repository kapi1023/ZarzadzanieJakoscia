package biz;

import db.dao.DAO;
import model.Account;
import model.Operation;
import model.Role;
import model.User;
import model.exceptions.OperationIsNotAllowedException;
import model.exceptions.UserUnnkownOrBadPasswordException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


class AccountManagerTest {

    private AccountManager accountManager;

    @Mock
    private DAO mockDao;

    @Mock
    private BankHistory mockHistory;

    @Mock
    private AuthenticationManager mockAuth;

    @Mock
    private User mockUser;

    @Mock
    private InterestOperator mockInterestOperator;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        accountManager = new AccountManager();
        
        setField(accountManager, "dao", mockDao);
        setField(accountManager, "history", mockHistory);
        setField(accountManager, "auth", mockAuth);
        setField(accountManager, "interestOperator", mockInterestOperator);
    }
    
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
    @Test
    @DisplayName("paymentIn - powodzenie wpłaty")
    void paymentIn_success() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Account account = createAccount(1, 1000.0, user);

        when(mockDao.findAccountById(1)).thenReturn(account);
        when(mockDao.updateAccountState(account)).thenReturn(true);

        // When
        boolean result = accountManager.paymentIn(user, 500.0, "Wpłata", 1);

        // Then
        assertTrue(result);
        assertEquals(1500.0, account.getAmmount());
        verify(mockHistory).logOperation(any(), eq(true));
        verify(mockDao).updateAccountState(account);
    }

    @Test
    @DisplayName("paymentIn - niepowodzenie wpłaty (ujemna kwota)")
    void paymentIn_failureNegativeAmount() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Account account = createAccount(1, 1000.0, user);

        when(mockDao.findAccountById(1)).thenReturn(account);

        // When
        boolean result = accountManager.paymentIn(user, -100.0, "Ujemna wpłata", 1);

        // Then
        assertFalse(result);
        assertEquals(1000.0, account.getAmmount());
        verify(mockHistory).logOperation(any(), eq(false));
        verify(mockDao, never()).updateAccountState(any());
    }

    @Test
    @DisplayName("paymentIn - niepowodzenie aktualizacji w bazie")
    void paymentIn_databaseUpdateFails() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Account account = createAccount(1, 1000.0, user);

        when(mockDao.findAccountById(1)).thenReturn(account);
        when(mockDao.updateAccountState(account)).thenReturn(false);

        // When
        boolean result = accountManager.paymentIn(user, 500.0, "Wpłata", 1);

        // Then
        assertFalse(result);
        verify(mockHistory).logOperation(any(), eq(false));
    }

    @Test
    @DisplayName("paymentOut - powodzenie wypłaty")
    void paymentOut_success() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Account account = createAccount(1, 1000.0, user);

        when(mockDao.findAccountById(1)).thenReturn(account);
        when(mockAuth.canInvokeOperation(any(), eq(user))).thenReturn(true);
        when(mockDao.updateAccountState(account)).thenReturn(true);

        // When
        boolean result = accountManager.paymentOut(user, 300.0, "Wypłata", 1);

        // Then
        assertTrue(result);
        assertEquals(700.0, account.getAmmount());
        verify(mockHistory).logOperation(any(), eq(true));
    }

    @Test
    @DisplayName("paymentOut - nieautoryzowana operacja")
    void paymentOut_unauthorized() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Account account = createAccount(1, 1000.0, user);

        when(mockDao.findAccountById(1)).thenReturn(account);
        when(mockAuth.canInvokeOperation(any(), eq(user))).thenReturn(false);

        // When/Then
        assertThrows(OperationIsNotAllowedException.class, () -> {
            accountManager.paymentOut(user, 300.0, "Wypłata", 1);
        });

        verify(mockHistory).logUnauthorizedOperation(any(), eq(false));
        assertEquals(1000.0, account.getAmmount());
    }

    @Test
    @DisplayName("paymentOut - niewystarczające środki")
    void paymentOut_insufficientFunds() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Account account = createAccount(1, 100.0, user);

        when(mockDao.findAccountById(1)).thenReturn(account);
        when(mockAuth.canInvokeOperation(any(), eq(user))).thenReturn(true);
        when(mockDao.updateAccountState(account)).thenReturn(true);

        // When
        boolean result = accountManager.paymentOut(user, 500.0, "Wypłata", 1);

        // Then - Bug w kodzie
        assertTrue(result); 
        assertEquals(100.0, account.getAmmount());
    }

    @Test
    @DisplayName("internalPayment - powodzenie transferu")
    void internalPayment_success() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Account sourceAccount = createAccount(1, 1000.0, user);
        Account destAccount = createAccount(2, 500.0, user);

        when(mockDao.findAccountById(1)).thenReturn(sourceAccount);
        when(mockDao.findAccountById(2)).thenReturn(destAccount);
        when(mockAuth.canInvokeOperation(any(), eq(user))).thenReturn(true);
        when(mockDao.updateAccountState(any())).thenReturn(true);

        // When
        boolean result = accountManager.internalPayment(user, 300.0, "Transfer", 1, 2);

        // Then
        assertTrue(result);
        assertEquals(700.0, sourceAccount.getAmmount());
        assertEquals(800.0, destAccount.getAmmount());
        verify(mockHistory, times(2)).logOperation(any(), eq(true));
    }

    @Test
    @DisplayName("internalPayment - nieautoryzowana operacja")
    void internalPayment_unauthorized() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Account sourceAccount = createAccount(1, 1000.0, user);
        Account destAccount = createAccount(2, 500.0, user);

        when(mockDao.findAccountById(1)).thenReturn(sourceAccount);
        when(mockDao.findAccountById(2)).thenReturn(destAccount);
        when(mockAuth.canInvokeOperation(any(), eq(user))).thenReturn(false);

        // When/Then
        assertThrows(OperationIsNotAllowedException.class, () -> {
            accountManager.internalPayment(user, 300.0, "Transfer", 1, 2);
        });

        verify(mockHistory).logUnauthorizedOperation(any(), eq(false));
    }

    @Test
    @DisplayName("internalPayment - niewystarczające środki na koncie źródłowym")
    void internalPayment_insufficientFunds() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Account sourceAccount = createAccount(1, 100.0, user);
        Account destAccount = createAccount(2, 500.0, user);

        when(mockDao.findAccountById(1)).thenReturn(sourceAccount);
        when(mockDao.findAccountById(2)).thenReturn(destAccount);
        when(mockAuth.canInvokeOperation(any(), eq(user))).thenReturn(true);

        // When
        boolean result = accountManager.internalPayment(user, 300.0, "Transfer", 1, 2);

        // Then
        assertFalse(result);
        assertEquals(100.0, sourceAccount.getAmmount());
        assertEquals(500.0, destAccount.getAmmount());
        verify(mockHistory, times(2)).logOperation(any(), eq(false));
    }

    @Test
    @DisplayName("internalPayment - niepowodzenie aktualizacji pierwszego konta")
    void internalPayment_firstUpdateFails() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        Account sourceAccount = createAccount(1, 1000.0, user);
        Account destAccount = createAccount(2, 500.0, user);

        when(mockDao.findAccountById(1)).thenReturn(sourceAccount);
        when(mockDao.findAccountById(2)).thenReturn(destAccount);
        when(mockAuth.canInvokeOperation(any(), eq(user))).thenReturn(true);
        when(mockDao.updateAccountState(sourceAccount)).thenReturn(false);

        // When
        boolean result = accountManager.internalPayment(user, 300.0, "Transfer", 1, 2);

        // Then
        assertFalse(result);
        verify(mockDao, never()).updateAccountState(destAccount);
    }

    @Test
    @DisplayName("logIn - powodzenie logowania")
    void logIn_success() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        when(mockAuth.logIn("jan", "pass123".toCharArray())).thenReturn(user);

        // When
        boolean result = accountManager.logIn("jan", "pass123".toCharArray());

        // Then
        assertTrue(result);
        assertEquals(user, accountManager.getLoggedUser());
    }

    @Test
    @DisplayName("logIn - niepowodzenie logowania")
    void logIn_failure() throws Exception {
        // Given
        when(mockAuth.logIn("jan", "wrongpass".toCharArray()))
            .thenThrow(new UserUnnkownOrBadPasswordException("Bad password"));

        // When/Then
        assertThrows(UserUnnkownOrBadPasswordException.class, () -> {
            accountManager.logIn("jan", "wrongpass".toCharArray());
        });

        assertNull(accountManager.getLoggedUser());
    }

    @Test
    @DisplayName("logOut - powodzenie wylogowania")
    void logOut_success() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        when(mockAuth.logIn("jan", "pass123".toCharArray())).thenReturn(user);
        accountManager.logIn("jan", "pass123".toCharArray());
        
        when(mockAuth.logOut(user)).thenReturn(true);

        // When
        boolean result = accountManager.logOut(user);

        // Then
        assertTrue(result);
        assertNull(accountManager.getLoggedUser());
    }

    @Test
    @DisplayName("logOut - niepowodzenie wylogowania")
    void logOut_failure() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        when(mockAuth.logOut(user)).thenReturn(false);

        // When
        boolean result = accountManager.logOut(user);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("getLoggedUser - zwraca zalogowanego użytkownika")
    void getLoggedUser_returnsLoggedUser() throws Exception {
        // Given
        User user = createUser(1, "Jan Kowalski");
        when(mockAuth.logIn("jan", "pass123".toCharArray())).thenReturn(user);
        accountManager.logIn("jan", "pass123".toCharArray());

        // When
        User loggedUser = accountManager.getLoggedUser();

        // Then
        assertEquals(user, loggedUser);
    }

    @Test
    @DisplayName("getLoggedUser - zwraca null gdy nikt nie jest zalogowany")
    void getLoggedUser_returnsNullWhenNoUserLoggedIn() {
        // When
        User loggedUser = accountManager.getLoggedUser();

        // Then
        assertNull(loggedUser);
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

    private Account createAccount(int id, double amount, User owner) {
        Account account = new Account();
        account.setId(id);
        account.setAmmount(amount);
        account.setOwner(owner);
        return account;
    }

  @Test
@DisplayName("paymentIn - brak konta powinien być błędem (obecnie NPE)")
void paymentIn_accountNotFound_isError() throws SQLException {
    when(mockDao.findAccountById(anyInt())).thenReturn(null);

    assertThrows(NullPointerException.class,
            () -> accountManager.paymentIn(mockUser, 100.0, "Test", 999),
            "Brak obsługi sytuacji gdy konto nie istnieje");
}

@Test
@DisplayName("paymentOut - brak konta powinien być błędem (obecnie NPE)")
void paymentOut_accountNotFound_isError() throws SQLException {
    when(mockDao.findAccountById(anyInt())).thenReturn(null);
    when(mockAuth.canInvokeOperation(any(), any())).thenReturn(true);

    assertThrows(NullPointerException.class,
            () -> accountManager.paymentOut(mockUser, 100.0, "Test", 999),
            "Brak obsługi sytuacji gdy konto nie istnieje");
}

@Test
@DisplayName("paymentOut - wynik operacji nie powinien być nadpisywany przez updateAccountState")
void paymentOut_shouldNotReturnTrueWhenOutcomeFails() throws Exception {
    Account account = new Account();
    account.setId(1);
    account.setAmmount(10.0);

    when(mockDao.findAccountById(1)).thenReturn(account);
    when(mockAuth.canInvokeOperation(any(), any())).thenReturn(true);
    when(mockDao.updateAccountState(any())).thenReturn(true);

    boolean result = accountManager.paymentOut(mockUser, 100.0, "Test", 1);

    assertFalse(result,
            "Metoda zwraca true mimo że wypłata powinna się nie powieść (outcome==false)");
    assertEquals(10.0, account.getAmmount(), 0.0001);
}

@Test
@DisplayName("paymentIn - ujemna kwota powinna być odrzucona")
void paymentIn_negativeAmount_isError() throws SQLException {
    Account account = new Account();
    account.setId(1);
    account.setAmmount(1000.0);

    when(mockDao.findAccountById(1)).thenReturn(account);
    when(mockDao.updateAccountState(any())).thenReturn(true);

    boolean result = accountManager.paymentIn(mockUser, -500.0, "Test", 1);

    assertFalse(result, "Ujemna kwota nie powinna przechodzić");
    assertEquals(1000.0, account.getAmmount(), 0.0001);
    verify(mockHistory).logOperation(any(Operation.class), eq(false));
}

@Test
@DisplayName("internalPayment - brak atomowości powinien skutkować błędem")
void internalPayment_notAtomic_isError() throws Exception {
    Account sourceAccount = new Account();
    sourceAccount.setId(1);
    sourceAccount.setAmmount(1000.0);

    Account destAccount = new Account();
    destAccount.setId(2);
    destAccount.setAmmount(500.0);

    when(mockDao.findAccountById(1)).thenReturn(sourceAccount);
    when(mockDao.findAccountById(2)).thenReturn(destAccount);
    when(mockAuth.canInvokeOperation(any(), any())).thenReturn(true);

    when(mockDao.updateAccountState(sourceAccount)).thenReturn(true);
    when(mockDao.updateAccountState(destAccount)).thenReturn(false);

    boolean result = accountManager.internalPayment(mockUser, 300.0, "Transfer", 1, 2);

    assertFalse(result);

    assertEquals(1000.0, sourceAccount.getAmmount(), 0.0001,
            "Przy niepowodzeniu operacji stan w pamięci powinien zostać wycofany (rollback)");
    assertEquals(500.0, destAccount.getAmmount(), 0.0001,
            "Przy niepowodzeniu operacji stan w pamięci powinien zostać wycofany (rollback)");
}

@Test
@DisplayName("internalPayment - status logowania powinien być niezależny dla obu operacji")
void internalPayment_loggingStatus_shouldBeIndependent() throws Exception {
    Account sourceAccount = new Account();
    sourceAccount.setId(1);
    sourceAccount.setAmmount(1000.0);

    Account destAccount = new Account();
    destAccount.setId(2);
    destAccount.setAmmount(500.0);

    when(mockDao.findAccountById(1)).thenReturn(sourceAccount);
    when(mockDao.findAccountById(2)).thenReturn(destAccount);
    when(mockAuth.canInvokeOperation(any(), any())).thenReturn(true);

    when(mockDao.updateAccountState(sourceAccount)).thenReturn(true);
    when(mockDao.updateAccountState(destAccount)).thenReturn(false);

    accountManager.internalPayment(mockUser, 300.0, "Transfer", 1, 2);

    verify(mockHistory, times(2)).logOperation(any(Operation.class), eq(false));
}

@Test
@DisplayName("operacje - kwota 0 powinna być odrzucona")
void operations_zeroAmount_isError() throws SQLException {
    Account account = new Account();
    account.setId(1);
    account.setAmmount(1000.0);

    when(mockDao.findAccountById(1)).thenReturn(account);
    when(mockDao.updateAccountState(any())).thenReturn(true);

    boolean result = accountManager.paymentIn(mockUser, 0.0, "Zero payment", 1);

    assertFalse(result, "Kwota 0 nie powinna przechodzić");
    assertEquals(1000.0, account.getAmmount(), 0.0001);
    verify(mockHistory).logOperation(any(Operation.class), eq(false));
}

@Test
@DisplayName("buildBank - nie powinno zwracać null (powinno rzucać wyjątek)")
void buildBank_shouldNotReturnNull_isError() {
    AccountManager manager = AccountManager.buildBank();
    assertNotNull(manager, "buildBank nie powinno zwracać null");
}

@Test
@DisplayName("loggedUser - globalny stan jest błędem w kontekście wielu sesji")
void loggedUser_singleGlobalUser_isError() throws Exception {
    User user1 = new User();
    user1.setId(1);
    user1.setName("User1");

    User user2 = new User();
    user2.setId(2);
    user2.setName("User2");

    when(mockAuth.logIn("user1", "pass1".toCharArray())).thenReturn(user1);
    when(mockAuth.logIn("user2", "pass2".toCharArray())).thenReturn(user2);

    accountManager.logIn("user1", "pass1".toCharArray());
    assertEquals(user1, accountManager.getLoggedUser());

    accountManager.logIn("user2", "pass2".toCharArray());

    assertNotEquals(user1, accountManager.getLoggedUser(),
            "Stan loggedUser jest nadpisywany; to uniemożliwia równoległe sesje");
}
}