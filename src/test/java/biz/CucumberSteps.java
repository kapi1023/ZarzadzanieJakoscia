package biz;

import db.dao.DAO;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import model.Account;
import model.User;
import model.exceptions.OperationIsNotAllowedException;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CucumberSteps {

    int higherst_user_id = 0;
    AccountManager target = null;
    List<User> users;
    DAO daoMock;
    AuthenticationManager authMock;
    InterestOperator interestOperator;
    BankHistory history;
    boolean testResult = false;

    @Given("Setup environments with mocks")
    public void setupEnvironmentsWithMocks() throws Exception {
        users = new ArrayList<User>();
        daoMock = mock(DAO.class);
        authMock = mock(AuthenticationManager.class);
        interestOperator = mock(InterestOperator.class);
        history = mock(BankHistory.class);
        testResult = false;
        createTarget();
    }

    @Given("We have user {string}")
    public void we_have_user(String name) throws SQLException {
        higherst_user_id++;
        we_have_user_with_id(name, higherst_user_id);
    }

    @Given("We have user {string} with id: {int}")
    public void we_have_user_with_id(String name, Integer uID) throws SQLException {
        User user = new User();
        user.setName(name);
        user.setId(uID);
        users.add(user);
        higherst_user_id = Math.max(uID,higherst_user_id);
        when(daoMock.findUserByName(name)).thenReturn(user);
    }
    @Given("{string} have account: {int} with: {double} pln")
    public void have_account_with_pln(String userName, Integer accId, Double amount) throws SQLException {
        User u = daoMock.findUserByName(userName);
        Account acc = new Account();
        acc.setOwner(u);
        acc.setId(accId);
        acc.setAmmount(amount);
        when(daoMock.findAccountById(accId)).thenReturn(acc);
    }
    @Given("There is an account:{int} with {double} pln")
    public void there_is_an_account_with_pln(Integer accId, Double amount) throws SQLException {
        Account acc = new Account();
        acc.setId(accId);
        acc.setAmmount(amount);
        when(daoMock.findAccountById(accId)).thenReturn(acc);
    }

    @Given("All database operations are sucessfull")
    public void allDBOperationsAreSucessfull() throws SQLException {
        when(daoMock.updateAccountState(any())).thenReturn(true);
    }

    @Given("Everything is authorised")
    public void everything_is_authorised() throws SQLException {
        // Write code here that turns the phrase above into concrete actions
        when(authMock.canInvokeOperation(any(),any())).thenReturn(true);
    }

    @Given("All {string} operations are allowed")
    public void all_user_operations_are_authorised(String userName) throws SQLException {
        // Write code here that turns the phrase above into concrete actions
        User user = daoMock.findUserByName(userName);
        when(authMock.canInvokeOperation(any(), eq(user) )).thenReturn(true);
    }

    @When("{string} make transfer from acc: {int} to acc: {int} with amount: {double}")
    public void make_transfer_from_acc_to_acc_with_amount(String userName, Integer srcId,
                                                          Integer dstId, Double amount) throws Exception {
        if (target == null) createTarget();
        User u = daoMock.findUserByName(userName);
        testResult = target.internalPayment(u,amount," ", srcId, dstId);
    }
    @Then("account:{int} value:{double} pln")
    public void account_value_pln(Integer accId, Double amount) throws SQLException {
        Account acc = daoMock.findAccountById(accId);
        assertEquals(amount,acc.getAmmount(),0.01);
    }
    @Then("All operations were successful")
    public void all_operations_were_successful() {
        assertTrue(testResult);
    }

    @Then("Operation is unsuccessful")
    public void all_operations_were_unsuccessful() {
        assertFalse(testResult);
    }

    @Then("No updates on accounts")
    public void noUpdatesOnDB() throws SQLException {
        verify(daoMock, never()).updateAccountState(any());
    }


    private void createTarget() throws Exception{
        target = new AccountManager();
        target.interestOperator = interestOperator;
        target.history = history;
        target.auth = authMock;
        //Co zrobić jak pole jest prywatne
        Field f = AccountManager.class.getDeclaredField("dao");
        f.setAccessible(true);
        f.set(target, daoMock);
    }
}
