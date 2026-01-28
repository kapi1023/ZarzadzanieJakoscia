package biz;

import db.dao.DAO;
import model.Account;
import model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountManagerTest {

    AccountManager target;
    DAO mockDao;
    AuthenticationManager mockAuth;
    BankHistory mockHistory;
    InterestOperator mockInterestOperator;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        target = new AccountManager();
        mockDao = Mockito.mock(DAO.class);
        mockHistory = Mockito.mock(BankHistory.class);
        mockAuth = Mockito.mock(AuthenticationManager.class);
        target.auth = mockAuth;
        target.history = mockHistory;
        //target.dao = mockDao;
        Field daoF = target.getClass().getDeclaredField("dao");
        daoF.setAccessible(true);
        daoF.set(target,mockDao);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void paymentIn() throws SQLException {
        Account account = new Account();
        //GIVEN: There is a user Marianna
        //GIVEN: Marianna has an accounut with id 12 and amount 1562.34 pln
        User user = new User(); user.setName("Marianna");
        account.setId(12);
        account.setOwner(user);
        account.setAmmount(1562.34);
        when(mockDao.findAccountById(12)).thenReturn(account);
        when(mockDao.updateAccountState(account)).thenReturn(true);
        //WHEN: Marainna puts 202.43 pln on her account
        boolean result = target.paymentIn(user, 202.43, "--",12);
        //THEN: There is 1764.78 pl on account 12
        //THEN: operation was sucessfull
        //THEN: database update of account operation was invoked once
        assertTrue(result);
        assertEquals(1764.77,account.getAmmount(),0.01);
        verify(mockDao,times(1)).updateAccountState(any());
    }

    @Test
    void paymentInNegativeAmount() throws SQLException {
        Account account = new Account();
        //GIVEN: There is a user Marianna
        //GIVEN: Marianna has an accounut with id 12 and amount 1562.34 pln
        User user = new User(); user.setName("Marianna");
        account.setId(12);
        account.setOwner(user);
        account.setAmmount(1562.34);
        when(mockDao.findAccountById(12)).thenReturn(account);
        when(mockDao.updateAccountState(account)).thenReturn(true);
        //WHEN: Marainna puts -3 pln on her account
        boolean result = target.paymentIn(user, -3, "--",12);
        //THEN: There is 1562.34 pl on account 12
        //THEN: operation was unsucessfull
        //THEN: database was never updated
        assertFalse(result);
        assertEquals(1562.34,account.getAmmount(),0.01);
        verify(mockDao,never()).updateAccountState(any());


    }
    //when(mockDao.findAccountById(12)).thenThrow(SQLException.class);
}