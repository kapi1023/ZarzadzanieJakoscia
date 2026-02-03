package model.operations;

import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


class LogInTest {

    @Mock
    private User mockUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockUser.getName()).thenReturn("Jan Kowalski");
        when(mockUser.getId()).thenReturn(1);
    }

    @Test
    @DisplayName("Powinien utworzyć operację logowania z poprawnymi parametrami")
    void shouldCreateLogInWithCorrectParameters() {
        String description = "Logowanie użytkownika";
        
        LogIn logIn = new LogIn(mockUser, description);
        
        assertNotNull(logIn);
        assertEquals(description, logIn.getDescription());
        assertEquals(mockUser, logIn.getUser());
        assertEquals(OperationType.LOG_IN, logIn.getType());
    }

    @Test
    @DisplayName("Powinien ustawić datę utworzenia operacji")
    void shouldSetCreationDate() {
        Date before = new Date();
        
        LogIn logIn = new LogIn(mockUser, "Test logowania");
        
        Date after = new Date();
        
        assertNotNull(logIn.getDate());
        assertTrue(logIn.getDate().getTime() >= before.getTime());
        assertTrue(logIn.getDate().getTime() <= after.getTime());
    }

    @Test
    @DisplayName("Powinien mieć poprawny typ operacji")
    void shouldHaveCorrectOperationType() {
        LogIn logIn = new LogIn(mockUser, "Logowanie");
        
        assertEquals(OperationType.LOG_IN, logIn.getType());
        assertEquals(2, logIn.getType().getId());
    }

    @Test
    @DisplayName("Powinien zachować referencję do użytkownika")
    void shouldMaintainUserReference() {
        LogIn logIn = new LogIn(mockUser, "Test");
        
        assertSame(mockUser, logIn.getUser());
    }

    @Test
    @DisplayName("Powinien utworzyć operację z pustym opisem")
    void shouldCreateLogInWithEmptyDescription() {
        LogIn logIn = new LogIn(mockUser, "");
        
        assertEquals("", logIn.getDescription());
    }

    @Test
    @DisplayName("Powinien utworzyć operację z długim opisem")
    void shouldCreateLogInWithLongDescription() {
        String longDescription = "Użytkownik " + mockUser.getName() + " zalogował się do systemu bankowego z adresu IP 192.168.1.100";
        
        LogIn logIn = new LogIn(mockUser, longDescription);
        
        assertEquals(longDescription, logIn.getDescription());
    }

    @Test
    @DisplayName("Powinien utworzyć operację z opisem zawierającym znaki specjalne")
    void shouldCreateLogInWithSpecialCharactersInDescription() {
        String specialDescription = "Logowanie: user@example.com (2026-01-28 10:30:00)";
        
        LogIn logIn = new LogIn(mockUser, specialDescription);
        
        assertEquals(specialDescription, logIn.getDescription());
    }

    @Test
    @DisplayName("Powinien utworzyć wiele operacji logowania dla tego samego użytkownika")
    void shouldCreateMultipleLogInsForSameUser() {
        LogIn logIn1 = new LogIn(mockUser, "Pierwsze logowanie");
        LogIn logIn2 = new LogIn(mockUser, "Drugie logowanie");
        
        assertNotNull(logIn1);
        assertNotNull(logIn2);
        assertNotSame(logIn1, logIn2);
        assertSame(mockUser, logIn1.getUser());
        assertSame(mockUser, logIn2.getUser());
    }
}
