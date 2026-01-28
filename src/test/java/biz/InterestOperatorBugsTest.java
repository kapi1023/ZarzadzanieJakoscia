package biz;

import db.dao.DAO;
import model.Account;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testy pokazujące błędy w InterestOperator
 * ⚠️ Te testy WYKRYWAJĄ PROBLEMY w kodzie!
 */
class InterestOperatorBugsTest {

    private InterestOperator interestOperator;

    @Mock
    private DAO mockDao;

    @Mock
    private AccountManager mockAccountManager;

    @Mock
    private User mockInterestUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        interestOperator = new InterestOperator(mockDao, mockAccountManager);
    }

    @Test
    @DisplayName("🐛 BUG: NullPointerException - bankHistory nigdy nie jest inicjalizowane")
    void bug_bankHistory_isNeverInitialized() throws SQLException {
        // Given: Normalny scenariusz
        Account account = new Account();
        account.setId(1);
        account.setAmmount(1000.0);

        when(mockDao.findUserByName("InterestOperator")).thenReturn(mockInterestUser);
        when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(true);

        // When/Then: Próba naliczenia odsetek
        assertThrows(NullPointerException.class, () -> {
            interestOperator.countInterestForAccount(account);
        }, "❌ KRYTYCZNY BUG: bankHistory jest null! " +
           "W konstruktorze InterestOperator nigdy nie jest ustawiane!");

        System.out.println("⚠️ WYKRYTO: InterestOperator.bankHistory = null");
        System.out.println("   Konstruktor nie inicjalizuje tego pola!");
    }

    @Test
    @DisplayName("🐛 BUG: Brak walidacji czy użytkownik InterestOperator istnieje")
    void bug_interestOperatorUser_mayNotExist() throws SQLException {
        // Given: Użytkownik "InterestOperator" nie istnieje w bazie
        Account account = new Account();
        account.setId(1);
        account.setAmmount(1000.0);

        when(mockDao.findUserByName("InterestOperator")).thenReturn(null); // ⚠️ null!

        // When/Then: 
        // Nawet jeśli bankHistory było zainicjalizowane, dostaniemy NPE
        // przy tworzeniu Interest(user, ...) bo user = null
        assertThrows(Exception.class, () -> {
            // Musimy obejść pierwszy bug (bankHistory=null) żeby pokazać ten bug
            interestOperator.bankHistory = mock(BankHistory.class);
            interestOperator.countInterestForAccount(account);
        });

        System.out.println("⚠️ WYKRYTO: Brak walidacji czy użytkownik 'InterestOperator' istnieje");
        System.out.println("   Jeśli nie ma go w bazie, dostaniemy NPE lub błędne dane");
    }

    @Test
    @DisplayName("🐛 BUG: Współczynnik odsetek 0.2 = 20% - czy to nie pomyłka?")
    void bug_interestFactor_isSuspiciouslyHigh() throws SQLException {
        // Given
        Account account = new Account();
        account.setId(1);
        account.setAmmount(10000.0); // 10,000 zł

        when(mockDao.findUserByName("InterestOperator")).thenReturn(mockInterestUser);
        
        // Obejście bugów żeby dotrzeć do testu współczynnika
        interestOperator.bankHistory = mock(BankHistory.class);

        // Capture wpłaty
        when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenAnswer(invocation -> {
                double amount = invocation.getArgument(1);
                
                // Then: Odsetki = 10,000 * 0.2 = 2,000 zł
                assertEquals(2000.0, amount, 0.01, 
                    "❌ PODEJRZANE: Odsetki = 20% salda! " +
                    "Dla 10,000 zł to 2,000 zł odsetek!");
                
                System.out.println("⚠️ WYKRYTO: interestFactor = 0.2 (20%)");
                System.out.println("   To wydaje się za dużo! Może powinno być 0.02 (2%) lub 0.002 (0.2%)?");
                System.out.println("   Brak dokumentacji: to rocznie? miesięcznie? dziennie?");
                
                return true;
            });

        // When
        interestOperator.countInterestForAccount(account);

        // Verify
        verify(mockAccountManager).paymentIn(any(), eq(2000.0), anyString(), eq(1));
    }

    @Test
    @DisplayName("🐛 BUG: Odsetki mogą być naliczane wielokrotnie bez kontroli")
    void bug_noControlOverMultipleInterestCalculations() throws SQLException {
        // Given
        Account account = new Account();
        account.setId(1);
        account.setAmmount(1000.0);

        when(mockDao.findUserByName("InterestOperator")).thenReturn(mockInterestUser);
        when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(true);
        interestOperator.bankHistory = mock(BankHistory.class);

        // When: Naliczamy odsetki 3 razy
        interestOperator.countInterestForAccount(account);
        interestOperator.countInterestForAccount(account);
        interestOperator.countInterestForAccount(account);

        // Then: Nic nie blokuje wielokrotnego naliczenia odsetek
        verify(mockAccountManager, times(3))
            .paymentIn(any(), anyDouble(), anyString(), anyInt());

        System.out.println("⚠️ WYKRYTO: Brak mechanizmu blokującego wielokrotne naliczenie odsetek");
        System.out.println("   Metoda może być wywołana wiele razy dla tego samego konta");
        System.out.println("   Brak sprawdzenia czy odsetki już zostały naliczone w tym okresie");
    }

    @Test
    @DisplayName("🐛 BUG: Brak walidacji czy account nie jest null")
    void bug_noValidationForNullAccount() throws SQLException {
        // Given: null account
        when(mockDao.findUserByName("InterestOperator")).thenReturn(mockInterestUser);
        interestOperator.bankHistory = mock(BankHistory.class);

        // When/Then: NPE przy account.getAmmount()
        assertThrows(NullPointerException.class, () -> {
            interestOperator.countInterestForAccount(null);
        }, "❌ Brak walidacji parametru account");

        System.out.println("⚠️ WYKRYTO: Brak walidacji parametru wejściowego (account)");
    }

    @Test
    @DisplayName("🐛 BUG: Odsetki dla konta z saldem 0 lub ujemnym")
    void bug_interestForZeroOrNegativeBalance() throws SQLException {
        // Given: Konto z saldem 0
        Account account = new Account();
        account.setId(1);
        account.setAmmount(0.0);

        when(mockDao.findUserByName("InterestOperator")).thenReturn(mockInterestUser);
        when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(true);
        interestOperator.bankHistory = mock(BankHistory.class);

        // When: Naliczenie odsetek
        interestOperator.countInterestForAccount(account);

        // Then: Wpłata 0 zł - czy to sensowne?
        verify(mockAccountManager).paymentIn(any(), eq(0.0), anyString(), anyInt());

        System.out.println("⚠️ WYKRYTO: System nalicza odsetki (0.0) nawet dla konta z saldem 0");
        System.out.println("   To generuje niepotrzebne operacje w systemie");
    }

    @Test
    @DisplayName("🐛 BUG: Logowanie operacji mimo że paymentIn się nie powiodła")
    void bug_logsOperationEvenWhenPaymentFails() throws SQLException {
        // Given
        Account account = new Account();
        account.setId(1);
        account.setAmmount(1000.0);

        when(mockDao.findUserByName("InterestOperator")).thenReturn(mockInterestUser);
        when(mockAccountManager.paymentIn(any(), anyDouble(), anyString(), anyInt()))
            .thenReturn(false); // ⚠️ Wpłata się nie powiodła!
        
        BankHistory spyHistory = mock(BankHistory.class);
        interestOperator.bankHistory = spyHistory;

        // When
        interestOperator.countInterestForAccount(account);

        // Then: Operacja jest logowana mimo niepowodzenia
        verify(spyHistory).logOperation(any(), eq(false));

        // To jest OK, ale pokazuje że success przekazywany z paymentIn
        // jest poprawnie propagowany
        System.out.println("✓ Operacja jest logowana z poprawnym statusem (false)");
        System.out.println("  Ale czy nie powinniśmy rzucić wyjątku gdy naliczenie się nie powiedzie?");
    }
}
