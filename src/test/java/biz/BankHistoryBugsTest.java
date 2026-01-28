package biz;

import db.dao.DAO;
import model.Account;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pokazujące błędy w BankHistory
 * ⚠️ Te testy WYKRYWAJĄ PROBLEMY w kodzie!
 */
class BankHistoryBugsTest {

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
    @DisplayName("🐛 BUG: logPaymentIn rzuca RuntimeException - niezaimplementowane!")
    void bug_logPaymentIn_throwsRuntimeException() {
        // Given: Parametry metody
        double amount = 100.0;
        boolean success = true;

        // When/Then: Metoda rzuca RuntimeException
        assertThrows(RuntimeException.class, () -> {
            bankHistory.logPaymentIn(mockAccount, amount, success);
        }, "❌ KRYTYCZNY BUG: Metoda celowo rzuca RuntimeException!");

        System.out.println("⚠️ WYKRYTO: logPaymentIn() rzuca RuntimeException");
        System.out.println("   Metoda wydaje się być niezaimplementowana (stub)");
        System.out.println("   Ale nigdzie w kodzie nie jest używana - martwy kod?");
    }

    @Test
    @DisplayName("🐛 BUG: logPaymentOut rzuca RuntimeException - niezaimplementowane!")
    void bug_logPaymentOut_throwsRuntimeException() {
        // Given: Parametry metody
        double amount = 100.0;
        boolean success = true;

        // When/Then: Metoda rzuca RuntimeException
        assertThrows(RuntimeException.class, () -> {
            bankHistory.logPaymentOut(mockAccount, amount, success);
        }, "❌ KRYTYCZNY BUG: Metoda celowo rzuca RuntimeException!");

        System.out.println("⚠️ WYKRYTO: logPaymentOut() rzuca RuntimeException");
        System.out.println("   Metoda wydaje się być niezaimplementowana (stub)");
        System.out.println("   Ale nigdzie w kodzie nie jest używana - martwy kod?");
    }

    @Test
    @DisplayName("🐛 BUG: logUnauthorizedOperation rzuca RuntimeException ale jest UŻYWANA!")
    void bug_logUnauthorizedOperation_throwsButIsCalled() {
        // Given: Parametry
        User user = new User();
        user.setId(1);
        
        // Tworzymy mock operacji
        model.operations.Withdraw mockOperation = 
            new model.operations.Withdraw(user, 100.0, "Test", mockAccount);

        // When/Then: Ta metoda jest WYWOŁYWANA w AccountManager.paymentOut!
        assertThrows(RuntimeException.class, () -> {
            bankHistory.logUnauthorizedOperation(mockOperation, false);
        }, "❌ KRYTYCZNY BUG: Metoda rzuca RuntimeException ale JEŚ UŻYWANA W KODZIE!");

        System.out.println("⚠️ WYKRYTO: logUnauthorizedOperation() rzuca RuntimeException");
        System.out.println("   ❌❌❌ ALE TA METODA JEST WYWOŁYWANA W:");
        System.out.println("   - AccountManager.paymentOut() linia 42");
        System.out.println("   - AccountManager.internalPayment() linia 57");
        System.out.println("   To spowoduje CRASH aplikacji przy nieautoryzowanej operacji!");
    }

    @Test
    @DisplayName("🐛 BUG: logLoginFailure z null user może być problematyczne")
    void bug_logLoginFailure_withNullUser() throws Exception {
        // Given: Nieudane logowanie dla nieistniejącego użytkownika
        User nullUser = null;
        String info = "Zła nazwa użytkownika test";

        // When: Logowanie z null user - czy DAO to obsłuży?
        // To zależy od implementacji DAO.logOperation
        
        // Metoda tworzy LogIn(null, info) - czy to legalne?
        assertDoesNotThrow(() -> {
            bankHistory.logLoginFailure(nullUser, info);
        }, "Metoda nie rzuca wyjątku, ale czy LogIn(null, ...) jest poprawne?");

        System.out.println("⚠️ WYKRYTO: logLoginFailure akceptuje null jako user");
        System.out.println("   Tworzy Operation z user=null");
        System.out.println("   Czy DAO.logOperation() to obsłuży? Może być NPE w bazie");
    }

    @Test
    @DisplayName("🐛 BUG: Brak walidacji parametrów w metodach")
    void bug_noParameterValidation() {
        // Test 1: null account
        assertThrows(NullPointerException.class, () -> {
            bankHistory.logPaymentIn(null, 100.0, true);
        }, "Brak walidacji account=null");

        System.out.println("⚠️ WYKRYTO: Brak walidacji parametrów wejściowych");
        System.out.println("   null account, null user - brak sprawdzeń");
    }

    @Test
    @DisplayName("🐛 Analiza: Dlaczego są 2 metody logPaymentIn i logPaymentOut?")
    void analysis_duplicateLoggingMethods() {
        System.out.println("🤔 ANALIZA PROJEKTOWA:");
        System.out.println("   BankHistory ma:");
        System.out.println("   - logPaymentIn(Account, double, boolean)  [niezaimplementowana]");
        System.out.println("   - logPaymentOut(Account, double, boolean) [niezaimplementowana]");
        System.out.println("   - logOperation(Operation, boolean)        [używana wszędzie]");
        System.out.println();
        System.out.println("   Pytanie: Po co pierwsze dwie metody?");
        System.out.println("   - Są niezaimplementowane (RuntimeException)");
        System.out.println("   - Nigdzie nie są wywoływane");
        System.out.println("   - logOperation() wystarczy do wszystkiego");
        System.out.println();
        System.out.println("   Możliwe wytłumaczenia:");
        System.out.println("   1. Stary kod, który miał być usunięty");
        System.out.println("   2. Planowana funkcjonalność nigdy niezaimplementowana");
        System.out.println("   3. Pozostałość po refactoringu");
        System.out.println();
        System.out.println("   Rekomendacja: Usunąć martwy kod lub zaimplementować");
    }

    @Test
    @DisplayName("✓ Pozytywny test: logOperation deleguje do DAO")
    void logOperation_delegatesToDao() throws Exception {
        // Given
        User user = new User();
        user.setId(1);
        model.operations.LogIn logIn = 
            new model.operations.LogIn(user, "Test login");

        // When
        assertDoesNotThrow(() -> {
            bankHistory.logOperation(logIn, true);
        });

        System.out.println("✓ logOperation() poprawnie deleguje do DAO - to jest OK");
    }

    @Test
    @DisplayName("🐛 BUG: logLogOut ma literówkę w opisie")
    void bug_logLogOut_typoInDescription() throws Exception {
        // Given
        User user = new User();
        user.setId(1);
        user.setName("Test User");

        // When: logLogOut
        assertDoesNotThrow(() -> {
            bankHistory.logLogOut(user);
        });

        // Then: Sprawdzamy opis
        // Tworzy: new LogOut(user, "Logowanie ")  ⚠️ powinno być "Wylogowanie"
        System.out.println("⚠️ WYKRYTO: logLogOut() tworzy LogOut z opisem 'Logowanie'");
        System.out.println("   Powinno być 'Wylogowanie' (copy-paste error)");
        System.out.println("   Zobacz: BankHistory.java linia 30");
    }
}
