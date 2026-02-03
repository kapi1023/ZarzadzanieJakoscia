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


class LogOutTest {

    @Mock
    private User mockUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockUser.getName()).thenReturn("Anna Nowak");
        when(mockUser.getId()).thenReturn(2);
    }

    @Test
    @DisplayName("Powinien utworzyć operację wylogowania z poprawnymi parametrami")
    void shouldCreateLogOutWithCorrectParameters() {
        String description = "Wylogowanie użytkownika";
        
        LogOut logOut = new LogOut(mockUser, description);
        
        assertNotNull(logOut);
        assertEquals(description, logOut.getDescription());
        assertEquals(mockUser, logOut.getUser());
        assertEquals(OperationType.LOG_OUT, logOut.getType());
    }

    @Test
    @DisplayName("Powinien ustawić datę utworzenia operacji")
    void shouldSetCreationDate() {
        Date before = new Date();
        
        LogOut logOut = new LogOut(mockUser, "Test wylogowania");
        
        Date after = new Date();
        
        assertNotNull(logOut.getDate());
        assertTrue(logOut.getDate().getTime() >= before.getTime());
        assertTrue(logOut.getDate().getTime() <= after.getTime());
    }

    @Test
    @DisplayName("Powinien mieć poprawny typ operacji")
    void shouldHaveCorrectOperationType() {
        LogOut logOut = new LogOut(mockUser, "Wylogowanie");
        
        assertEquals(OperationType.LOG_OUT, logOut.getType());
        assertEquals(3, logOut.getType().getId());
    }

    @Test
    @DisplayName("Powinien zachować referencję do użytkownika")
    void shouldMaintainUserReference() {
        LogOut logOut = new LogOut(mockUser, "Test");
        
        assertSame(mockUser, logOut.getUser());
    }

    @Test
    @DisplayName("Powinien utworzyć operację z pustym opisem")
    void shouldCreateLogOutWithEmptyDescription() {
        LogOut logOut = new LogOut(mockUser, "");
        
        assertEquals("", logOut.getDescription());
    }

    @Test
    @DisplayName("Powinien utworzyć operację z długim opisem")
    void shouldCreateLogOutWithLongDescription() {
        String longDescription = "Użytkownik " + mockUser.getName() + " wylogował się z systemu bankowego. Sesja trwała 45 minut.";
        
        LogOut logOut = new LogOut(mockUser, longDescription);
        
        assertEquals(longDescription, logOut.getDescription());
    }

    @Test
    @DisplayName("Powinien utworzyć operację z opisem zawierającym znaki specjalne")
    void shouldCreateLogOutWithSpecialCharactersInDescription() {
        String specialDescription = "Wylogowanie: user@example.com (2026-01-28 11:45:00)";
        
        LogOut logOut = new LogOut(mockUser, specialDescription);
        
        assertEquals(specialDescription, logOut.getDescription());
    }

    @Test
    @DisplayName("Powinien utworzyć wiele operacji wylogowania dla tego samego użytkownika")
    void shouldCreateMultipleLogOutsForSameUser() {
        LogOut logOut1 = new LogOut(mockUser, "Pierwsze wylogowanie");
        LogOut logOut2 = new LogOut(mockUser, "Drugie wylogowanie");
        
        assertNotNull(logOut1);
        assertNotNull(logOut2);
        assertNotSame(logOut1, logOut2);
        assertSame(mockUser, logOut1.getUser());
        assertSame(mockUser, logOut2.getUser());
    }

    @Test
    @DisplayName("Powinien utworzyć parę operacji logowania i wylogowania")
    void shouldCreateLogInAndLogOutPair() {
        LogIn logIn = new LogIn(mockUser, "Logowanie");
        LogOut logOut = new LogOut(mockUser, "Wylogowanie");
        
        assertNotNull(logIn);
        assertNotNull(logOut);
        assertEquals(OperationType.LOG_IN, logIn.getType());
        assertEquals(OperationType.LOG_OUT, logOut.getType());
        assertSame(mockUser, logIn.getUser());
        assertSame(mockUser, logOut.getUser());
    }
}
