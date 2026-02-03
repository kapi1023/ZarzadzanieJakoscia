package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;


class UserTest {

    private User user;

    @Mock
    private Role mockRole;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
    }

    @Test
    @DisplayName("Powinien utworzyć użytkownika i ustawić ID")
    void shouldCreateUserAndSetId() {
        user.setId(1);
        assertEquals(1, user.getId());
    }

    @Test
    @DisplayName("Powinien poprawnie ustawić i pobrać nazwę użytkownika")
    void shouldSetAndGetName() {
        user.setName("Jan Kowalski");
        assertEquals("Jan Kowalski", user.getName());
    }

    @Test
    @DisplayName("Powinien poprawnie ustawić i pobrać rolę")
    void shouldSetAndGetRole() {
        user.setRole(mockRole);
        assertEquals(mockRole, user.getRole());
    }

    @Test
    @DisplayName("Powinien obsługiwać null jako nazwę")
    void shouldHandleNullName() {
        user.setName(null);
        assertNull(user.getName());
    }

    @Test
    @DisplayName("Powinien obsługiwać null jako rolę")
    void shouldHandleNullRole() {
        user.setRole(null);
        assertNull(user.getRole());
    }

    @Test
    @DisplayName("Powinien obsługiwać pustą nazwę")
    void shouldHandleEmptyName() {
        user.setName("");
        assertEquals("", user.getName());
    }

    @Test
    @DisplayName("Powinien poprawnie ustawić wszystkie właściwości")
    void shouldSetAllProperties() {
        Role role = new Role();
        role.setId(1);
        role.setName("Admin");

        user.setId(100);
        user.setName("Anna Nowak");
        user.setRole(role);

        assertEquals(100, user.getId());
        assertEquals("Anna Nowak", user.getName());
        assertEquals(role, user.getRole());
    }
}
