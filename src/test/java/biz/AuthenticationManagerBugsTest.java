package biz;

import db.dao.DAO;
import model.Operation;
import model.Password;
import model.Role;
import model.User;
import model.exceptions.UserUnnkownOrBadPasswordException;
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

/**
 * Testy pokazujące błędy w AuthenticationManager
 * ⚠️ Te testy WYKRYWAJĄ PROBLEMY w kodzie!
 */
class AuthenticationManagerBugsTest {

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
    @DisplayName("🐛 BUG: NullPointerException gdy user.getRole() jest null")
    void bug_canInvokeOperation_npeWhenRoleIsNull() {
        // Given: User bez roli
        User user = new User();
        user.setId(1);
        user.setName("Test User");
        user.setRole(null); // ⚠️ Rola nie ustawiona

        Operation operation = mock(Withdraw.class);
        when(operation.getType()).thenReturn(OperationType.WITHDRAW);

        // When/Then: NPE przy user.getRole().getName()
        assertThrows(NullPointerException.class, () -> {
            authManager.canInvokeOperation(operation, user);
        }, "❌ BUG: Brak sprawdzenia czy user.getRole() != null");

        System.out.println("⚠️ WYKRYTO: canInvokeOperation nie sprawdza czy role jest null");
    }

    @Test
    @DisplayName("🐛 BUG: Każdy może wykonać wpłatę na dowolne konto")
    void bug_anyoneCanPayIn() {
        // Given: Zwykły użytkownik (nie admin)
        User regularUser = new User();
        regularUser.setId(1);
        Role userRole = new Role();
        userRole.setName("User");
        regularUser.setRole(userRole);

        // Operacja wpłaty na cudze konto
        Operation paymentIn = mock(PaymentIn.class);
        when(paymentIn.getType()).thenReturn(OperationType.PAYMENT_IN);

        // When
        boolean canInvoke = authManager.canInvokeOperation(paymentIn, regularUser);

        // Then: Każdy może wpłacać!
        assertTrue(canInvoke, 
            "❌ PROBLEM BEZPIECZEŃSTWA: Każdy może wpłacać na dowolne konto!");

        System.out.println("⚠️ WYKRYTO: PAYMENT_IN zawsze zwraca true");
        System.out.println("   Każdy użytkownik może wpłacić na dowolne konto");
        System.out.println("   To może być exploit - wpłata skradzionych pieniędzy?");
    }

    @Test
    @DisplayName("🐛 BUG: Słaba walidacja właściciela konta przy wypłacie")
    void bug_weakAccountOwnerValidation() {
        // Given: Dwóch użytkowników
        User accountOwner = new User();
        accountOwner.setId(1);
        Role userRole = new Role();
        userRole.setName("User");
        accountOwner.setRole(userRole);

        User attacker = new User();
        attacker.setId(2);
        attacker.setRole(userRole);

        // Operacja wypłaty utworzona przez właściciela
        Withdraw withdraw = mock(Withdraw.class);
        when(withdraw.getType()).thenReturn(OperationType.WITHDRAW);
        when(withdraw.getUser()).thenReturn(accountOwner); // ⚠️ User w operacji

        // When: Atakujący próbuje wykonać operację
        boolean canInvoke = authManager.canInvokeOperation(withdraw, attacker);

        // Then: Sprawdza tylko czy ID użytkownika = ID w operacji
        // ❌ Nie sprawdza czy attacker jest właścicielem KONTA!
        assertFalse(canInvoke, "Attacker ma inne ID");
        
        // Ale jeśli attacker utworzy operację ze swoim ID:
        when(withdraw.getUser()).thenReturn(attacker);
        boolean canInvoke2 = authManager.canInvokeOperation(withdraw, attacker);
        
        assertTrue(canInvoke2, 
            "❌ PROBLEM: Sprawdza tylko czy user = operation.getUser(), " +
            "ale nie sprawdza czy user jest właścicielem KONTA!");

        System.out.println("⚠️ WYKRYTO: Walidacja sprawdza tylko ID użytkownika w operacji");
        System.out.println("   Nie sprawdza czy użytkownik jest właścicielem konta!");
        System.out.println("   Potencjalna luka bezpieczeństwa");
    }

    @Test
    @DisplayName("🐛 BUG: Brak obsługi operacji INTEREST")
    void bug_noHandlingForInterestOperation() {
        // Given: Admin user
        User admin = new User();
        admin.setId(1);
        Role adminRole = new Role();
        adminRole.setName("Admin");
        admin.setRole(adminRole);

        // Operacja INTEREST
        Operation interest = mock(Operation.class);
        when(interest.getType()).thenReturn(OperationType.INTEREST);

        // When: Admin ma dostęp
        boolean adminCan = authManager.canInvokeOperation(interest, admin);
        assertTrue(adminCan, "Admin może wszystko");

        // Ale dla zwykłego użytkownika:
        User regularUser = new User();
        regularUser.setId(2);
        Role userRole = new Role();
        userRole.setName("User");
        regularUser.setRole(userRole);

        boolean userCan = authManager.canInvokeOperation(interest, regularUser);
        
        // Then: Zwraca false (z default)
        assertFalse(userCan, 
            "❌ Operacja INTEREST nie jest jawnie obsługiwana - co powinno być regułą?");

        System.out.println("⚠️ WYKRYTO: Brak jawnej obsługi dla OperationType.INTEREST");
        System.out.println("   Tylko admin może naliczać odsetki (przez default false)");
        System.out.println("   Czy to właściwe zachowanie?");
    }

    @Test
    @DisplayName("🐛 BUG: Porównanie String przez equals() - case sensitive")
    void bug_roleName_caseSensitive() {
        // Given: User z rolą "admin" (małymi literami)
        User user = new User();
        user.setId(1);
        Role role = new Role();
        role.setName("admin"); // ⚠️ Małe litery
        user.setRole(role);

        Operation operation = mock(Operation.class);
        when(operation.getType()).thenReturn(OperationType.WITHDRAW);

        // When
        boolean canInvoke = authManager.canInvokeOperation(operation, user);

        // Then: Nie rozpoznaje jako admina!
        assertFalse(canInvoke, 
            "❌ BUG: Porównanie 'Admin' vs 'admin' - case sensitive!");

        System.out.println("⚠️ WYKRYTO: equals('Admin') jest case-sensitive");
        System.out.println("   Jeśli w bazie jest 'admin' lub 'ADMIN', nie zadziała");
        System.out.println("   Powinno być: equalsIgnoreCase('Admin')");
    }

    @Test
    @DisplayName("🐛 BUG: Brak walidacji parametrów w logIn")
    void bug_login_noParameterValidation() throws SQLException, UserUnnkownOrBadPasswordException {
        // Given: null userName
        when(mockDao.findUserByName(null)).thenReturn(null);

        // When/Then: NPE lub SQLException
        assertThrows(Exception.class, () -> {
            authManager.logIn(null, "password".toCharArray());
        });

        System.out.println("⚠️ WYKRYTO: Brak walidacji parametrów wejściowych w logIn");
        System.out.println("   userName=null powoduje błąd");
        
        // Również password może być null
        when(mockDao.findUserByName("user")).thenReturn(mock(User.class));
        when(mockDao.findPasswordForUser(any())).thenReturn(mock(Password.class));
        
        assertThrows(NullPointerException.class, () -> {
            authManager.logIn("user", null);
        }, "password=null również nie jest walidowane");
    }

    @Test
    @DisplayName("🐛 BUG: Logowanie ujawnia czy użytkownik istnieje")
    void bug_login_revealsUserExistence() throws SQLException {
        // Given: Nieistniejący użytkownik
        when(mockDao.findUserByName("ghost")).thenReturn(null);

        // When
        assertThrows(UserUnnkownOrBadPasswordException.class, () -> {
            authManager.logIn("ghost", "password".toCharArray());
        });

        // Then: Różne komunikaty dla złej nazwy i złego hasła
        verify(mockHistory).logLoginFailure(null, "Zła nazwa użytkownika ghost");

        // To ujawnia czy użytkownik istnieje!
        System.out.println("⚠️ PROBLEM BEZPIECZEŃSTWA: Różne komunikaty dla:");
        System.out.println("   - nieistniejącego użytkownika: 'Zła nazwa użytkownika'");
        System.out.println("   - złego hasła: 'Bad Password'");
        System.out.println("   Atakujący może sprawdzić które konta istnieją!");
        System.out.println("   Powinien być jeden komunikat: 'Nieprawidłowe dane logowania'");
    }

    @Test
    @DisplayName("🐛 BUG: Hasło w char[] jest czyszczone za wcześnie")
    void bug_passwordArrayCleared() {
        // Given: hasło
        char[] password = "secretPass".toCharArray();
        char[] originalCopy = password.clone();

        // When: hashPassword
        String hashed = AuthenticationManager.hashPassword(password);

        // Then: Tablica została wyczyszczona
        assertNotNull(hashed);
        assertNotEquals(new String(originalCopy), new String(password), 
            "Hasło zostało wyczyszczone w tablicy");

        // ⚠️ Problem: jeśli wywołujący kod potrzebuje hasła ponownie, nie ma go!
        System.out.println("⚠️ INFORMACJA: hashPassword() czyści tablicę char[]");
        System.out.println("   To jest dobre dla bezpieczeństwa, ale może zaskoczyć!");
        System.out.println("   Jest w finally{} więc działa zawsze");
    }

    @Test
    @DisplayName("✓ Pozytywny test: Admin ma dostęp do wszystkiego")
    void adminHasAccessToEverything() {
        // Given: Admin
        User admin = new User();
        admin.setId(1);
        Role adminRole = new Role();
        adminRole.setName("Admin");
        admin.setRole(adminRole);

        // When/Then: Wszystkie operacje dozwolone
        for (OperationType type : OperationType.values()) {
            Operation operation = mock(Operation.class);
            when(operation.getType()).thenReturn(type);
            
            assertTrue(authManager.canInvokeOperation(operation, admin),
                "Admin powinien mieć dostęp do " + type);
        }

        System.out.println("✓ Admin ma dostęp do wszystkich operacji - to jest OK");
    }
}
