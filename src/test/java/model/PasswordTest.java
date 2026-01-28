package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe dla klasy Password
 */
class PasswordTest {

    private Password password;

    @BeforeEach
    void setUp() {
        password = new Password();
    }

    @Test
    @DisplayName("Powinien utworzyć hasło i ustawić userId")
    void shouldCreatePasswordAndSetUserId() {
        password.setUserId(1);
        assertEquals(1, password.getUserId());
    }

    @Test
    @DisplayName("Powinien poprawnie ustawić i pobrać hasło")
    void shouldSetAndGetPassword() {
        password.setPasswd("secretPassword123");
        assertEquals("secretPassword123", password.getPasswd());
    }

    @Test
    @DisplayName("Powinien obsługiwać null jako hasło")
    void shouldHandleNullPassword() {
        password.setPasswd(null);
        assertNull(password.getPasswd());
    }

    @Test
    @DisplayName("Powinien obsługiwać puste hasło")
    void shouldHandleEmptyPassword() {
        password.setPasswd("");
        assertEquals("", password.getPasswd());
    }

    @Test
    @DisplayName("Powinien obsługiwać długie hasło")
    void shouldHandleLongPassword() {
        String longPassword = "a".repeat(1000);
        password.setPasswd(longPassword);
        assertEquals(longPassword, password.getPasswd());
    }

    @Test
    @DisplayName("Powinien poprawnie ustawić wszystkie właściwości")
    void shouldSetAllProperties() {
        password.setUserId(42);
        password.setPasswd("mySecurePassword");

        assertEquals(42, password.getUserId());
        assertEquals("mySecurePassword", password.getPasswd());
    }

    @Test
    @DisplayName("Powinien obsługiwać ujemne userId")
    void shouldHandleNegativeUserId() {
        password.setUserId(-1);
        assertEquals(-1, password.getUserId());
    }

    @Test
    @DisplayName("Powinien obsługiwać hasło ze znakami specjalnymi")
    void shouldHandlePasswordWithSpecialCharacters() {
        String specialPassword = "p@ssw0rd!#$%^&*()";
        password.setPasswd(specialPassword);
        assertEquals(specialPassword, password.getPasswd());
    }
}
