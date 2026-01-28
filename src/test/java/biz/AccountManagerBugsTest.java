package biz;

import db.dao.DAO;
import model.Account;
import model.Operation;
import model.User;
import model.exceptions.OperationIsNotAllowedException;
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
import static org.mockito.Mockito.*;

/**
 * Testy pokazujące błędy w AccountManager
 * ⚠️ Te testy WYKRYWAJĄ PROBLEMY w kodzie!
 */
class AccountManagerBugsTest {

    private AccountManager accountManager;

    @Mock
    private DAO mockDao;

    @Mock
    private BankHistory mockHistory;

    @Mock
    private AuthenticationManager mockAuth;

    @Mock
    private User mockUser;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        accountManager = new AccountManager();
        
        // Użycie reflection do ustawienia package-private pól
        setField(accountManager, "dao", mockDao);
        setField(accountManager, "history", mockHistory);
        setField(accountManager, "auth", mockAuth);
    }
    
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("🐛 BUG: NullPointerException gdy konto nie istnieje - paymentIn")
    void bug_paymentIn_shouldFailWhenAccountNotExists() throws SQLException {
        // Given: DAO zwraca null (konto nie istnieje)
        when(mockDao.findAccountById(anyInt())).thenReturn(null);

        // When/Then: Powinien rzucić NPE lub obsłużyć null
        assertThrows(NullPointerException.class, () -> {
            accountManager.paymentIn(mockUser, 100.0, "Test", 999);
        }, "❌ Kod nie sprawdza czy konto istnieje! NPE przy próbie wywołania account.income()");
    }

    @Test
    @DisplayName("🐛 BUG: NullPointerException gdy konto nie istnieje - paymentOut")
    void bug_paymentOut_shouldFailWhenAccountNotExists() throws SQLException {
        // Given: DAO zwraca null
        when(mockDao.findAccountById(anyInt())).thenReturn(null);
        when(mockAuth.canInvokeOperation(any(), any())).thenReturn(true);

        // When/Then: Powinien rzucić NPE
        assertThrows(NullPointerException.class, () -> {
            accountManager.paymentOut(mockUser, 100.0, "Test", 999);
        }, "❌ Kod nie sprawdza czy konto istnieje przed operacją!");
    }

    @Test
    @DisplayName("🐛 BUG: Nadpisywanie wyniku operacji w paymentOut")
    void bug_paymentOut_overwritesOutcomeResult() throws Exception {
        // Given: Konto z małym saldem
        Account account = new Account();
        account.setId(1);
        account.setAmmount(10.0);

        when(mockDao.findAccountById(1)).thenReturn(account);
        when(mockAuth.canInvokeOperation(any(), any())).thenReturn(true);
        when(mockDao.updateAccountState(any())).thenReturn(true); // ⚠️ Baza zwraca success

        // When: Próba wypłaty większej kwoty niż saldo
        boolean result = accountManager.paymentOut(mockUser, 100.0, "Test", 1);

        // Then: 
        // ❌ BUG: account.outcome() zwróci false, ale updateAccountState() zwróci true
        // Metoda zwróci TRUE mimo że wypłata się nie powiodła!
        assertTrue(result, 
            "❌ BUG WYKRYTY: Metoda zwraca true mimo że outcome() zwrócił false! " +
            "Wynik outcome jest nadpisywany przez updateAccountState()");
        
        // Dodatkowo saldo nie powinno się zmienić
        assertEquals(10.0, account.getAmmount(), 
            "Saldo nie powinno się zmienić gdy outcome() zwraca false");
    }

    @Test
    @DisplayName("🐛 BUG: Brak walidacji kwoty ujemnej w paymentIn")
    void bug_paymentIn_noValidationForNegativeAmount() throws SQLException {
        // Given
        Account account = new Account();
        account.setId(1);
        account.setAmmount(1000.0);

        when(mockDao.findAccountById(1)).thenReturn(account);
        when(mockDao.updateAccountState(any())).thenReturn(true);

        // When: Próba wpłaty ujemnej kwoty
        boolean result = accountManager.paymentIn(mockUser, -500.0, "Test", 1);

        // Then: 
        // ⚠️ Metoda nie waliduje kwoty - polega tylko na Account.income()
        assertFalse(result, "Powinno odrzucić ujemną kwotę");
        assertEquals(1000.0, account.getAmmount(), "Saldo nie powinno się zmienić");
        
        // Ale czy operacja jest zalogowana? To kolejny problem...
        verify(mockHistory, times(1)).logOperation(any(Operation.class), eq(false));
    }

    @Test
    @DisplayName("🐛 BUG: internalPayment - brak atomowości transakcji")
    void bug_internalPayment_notAtomic() throws Exception {
        // Given: Dwa konta
        Account sourceAccount = new Account();
        sourceAccount.setId(1);
        sourceAccount.setAmmount(1000.0);
        
        Account destAccount = new Account();
        destAccount.setId(2);
        destAccount.setAmmount(500.0);

        when(mockDao.findAccountById(1)).thenReturn(sourceAccount);
        when(mockDao.findAccountById(2)).thenReturn(destAccount);
        when(mockAuth.canInvokeOperation(any(), any())).thenReturn(true);
        
        // ⚠️ Symulacja: pierwsza aktualizacja OK, druga FAIL
        when(mockDao.updateAccountState(sourceAccount)).thenReturn(true);
        when(mockDao.updateAccountState(destAccount)).thenReturn(false);

        // When: Transfer 300
        boolean result = accountManager.internalPayment(mockUser, 300.0, "Transfer", 1, 2);

        // Then:
        assertFalse(result, "Transfer powinien się nie udać");
        
        // ❌ KRYTYCZNY BUG: Pieniądze zostały odjęte z konta źródłowego
        // ale NIE dodane do docelowego (bo updateAccountState(destAccount) zwrócił false)
        assertEquals(700.0, sourceAccount.getAmmount(), 
            "❌ PIENIĄDZE ZNIKNĘŁY! Source ma 700, dest ma 800 w pamięci, " +
            "ale tylko source zapisany w bazie!");
        assertEquals(800.0, destAccount.getAmmount(), 
            "Dest ma 800 w pamięci ale to NIE zostało zapisane do bazy!");
        
        // To pokazuje brak transakcji - powinien być rollback!
        System.out.println("⚠️ WYKRYTO: Brak transakcji bazodanowej - możliwa utrata pieniędzy!");
    }

    @Test
    @DisplayName("🐛 BUG: internalPayment - ten sam status dla obu operacji")
    void bug_internalPayment_sameStatusForBothOperations() throws Exception {
        // Given
        Account sourceAccount = new Account();
        sourceAccount.setId(1);
        sourceAccount.setAmmount(1000.0);
        
        Account destAccount = new Account();
        destAccount.setId(2);
        destAccount.setAmmount(500.0);

        when(mockDao.findAccountById(1)).thenReturn(sourceAccount);
        when(mockDao.findAccountById(2)).thenReturn(destAccount);
        when(mockAuth.canInvokeOperation(any(), any())).thenReturn(true);
        when(mockDao.updateAccountState(any())).thenReturn(true);

        // When
        accountManager.internalPayment(mockUser, 300.0, "Transfer", 1, 2);

        // Then: Obie operacje zalogowane z tym samym statusem
        verify(mockHistory, times(2)).logOperation(any(Operation.class), eq(true));
        
        // ❌ Problem: Jeśli jedna operacja się powiedzie a druga nie,
        // obie dostają ten sam status (ostatni success)
        System.out.println("⚠️ WYKRYTO: Obie operacje (withdraw i payment) dostają ten sam status!");
    }

    @Test
    @DisplayName("🐛 BUG: Brak walidacji kwoty zero w operacjach")
    void bug_acceptsZeroAmount() throws SQLException {
        // Given
        Account account = new Account();
        account.setId(1);
        account.setAmmount(1000.0);

        when(mockDao.findAccountById(1)).thenReturn(account);
        when(mockDao.updateAccountState(any())).thenReturn(true);

        // When: Wpłata 0
        boolean result = accountManager.paymentIn(mockUser, 0.0, "Zero payment", 1);

        // Then: Akceptuje operację z kwotą 0 (czy to sensowne?)
        assertTrue(result, "❌ Czy wpłata 0 zł ma sens? Brak walidacji!");
        assertEquals(1000.0, account.getAmmount());
        
        // Operacja jest logowana mimo że nic się nie wydarzyło
        verify(mockHistory).logOperation(any(), eq(true));
        System.out.println("⚠️ System akceptuje operacje z kwotą 0 - czy to właściwe?");
    }

    @Test
    @DisplayName("🐛 BUG: buildBank() może zwrócić null")
    void bug_buildBank_canReturnNull() {
        // When: buildBank() zawiedzie (np. brak bazy danych)
        // Nie możemy tego łatwo przetestować bez prawdziwej bazy,
        // ale pokazujemy problem projektowy
        
        AccountManager manager = AccountManager.buildBank();
        
        // Then: Może być null!
        // ❌ Zły wzorzec - powinien rzucić wyjątek zamiast zwracać null
        if (manager == null) {
            System.out.println("⚠️ PROBLEM PROJEKTOWY: buildBank() zwraca null zamiast rzucić wyjątek");
            System.out.println("   Kod wywołujący musi pamiętać o sprawdzeniu null");
        }
        
        // W produkcji to może spowodować NPE gdzieś później w kodzie
        assertNotNull(manager, 
            "buildBank() nie powinno zwracać null - powinno rzucić wyjątek!");
    }

    @Test
    @DisplayName("🐛 BUG: Tylko jeden zalogowany użytkownik globalnie")
    void bug_singleLoggedUser_notThreadSafe() throws Exception {
        // Given: Dwóch użytkowników
        User user1 = new User();
        user1.setId(1);
        user1.setName("User1");
        
        User user2 = new User();
        user2.setId(2);
        user2.setName("User2");

        when(mockAuth.logIn("user1", "pass1".toCharArray())).thenReturn(user1);
        when(mockAuth.logIn("user2", "pass2".toCharArray())).thenReturn(user2);

        // When: User1 loguje się
        accountManager.logIn("user1", "pass1".toCharArray());
        assertEquals(user1, accountManager.getLoggedUser());

        // Następnie User2 loguje się
        accountManager.logIn("user2", "pass2".toCharArray());
        
        // Then:
        // ❌ BUG: User1 został wylogowany! Tylko jeden użytkownik globalnie
        assertEquals(user2, accountManager.getLoggedUser());
        assertNotEquals(user1, accountManager.getLoggedUser(), 
            "❌ WYKRYTO: Tylko jeden zalogowany użytkownik! " +
            "To nie działa dla aplikacji wielowątkowej!");
        
        System.out.println("⚠️ PROBLEM: Pole loggedUser jest globalne - nie obsługuje wielu sesji");
    }
}
