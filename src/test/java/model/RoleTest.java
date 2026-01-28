package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe dla klasy Role
 */
class RoleTest {

    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role();
    }

    @Test
    @DisplayName("Powinien utworzyć rolę i ustawić ID")
    void shouldCreateRoleAndSetId() {
        role.setId(1);
        assertEquals(1, role.getId());
    }

    @Test
    @DisplayName("Powinien poprawnie ustawić i pobrać nazwę roli")
    void shouldSetAndGetName() {
        role.setName("Administrator");
        assertEquals("Administrator", role.getName());
    }

    @Test
    @DisplayName("Powinien obsługiwać null jako nazwę")
    void shouldHandleNullName() {
        role.setName(null);
        assertNull(role.getName());
    }

    @Test
    @DisplayName("Powinien obsługiwać pustą nazwę")
    void shouldHandleEmptyName() {
        role.setName("");
        assertEquals("", role.getName());
    }

    @Test
    @DisplayName("Powinien poprawnie ustawić wszystkie właściwości")
    void shouldSetAllProperties() {
        role.setId(5);
        role.setName("User");

        assertEquals(5, role.getId());
        assertEquals("User", role.getName());
    }

    @Test
    @DisplayName("Powinien obsługiwać ujemne ID")
    void shouldHandleNegativeId() {
        role.setId(-1);
        assertEquals(-1, role.getId());
    }

    @Test
    @DisplayName("Powinien obsługiwać różne typy nazw ról")
    void shouldHandleDifferentRoleNames() {
        String[] roleNames = {"Admin", "User", "Guest", "Moderator", "SuperAdmin"};
        
        for (String name : roleNames) {
            role.setName(name);
            assertEquals(name, role.getName());
        }
    }

    @Test
    @DisplayName("Powinien obsługiwać nazwę ze spacjami")
    void shouldHandleNameWithSpaces() {
        role.setName("Super Admin");
        assertEquals("Super Admin", role.getName());
    }
}
