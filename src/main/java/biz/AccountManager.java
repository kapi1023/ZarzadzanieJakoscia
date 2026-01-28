package biz;

import db.dao.DAO;
import db.dao.impl.DAOImpl;
import db.dao.impl.SQLiteDB;
import model.Account;
import model.Operation;
import model.User;
import model.exceptions.OperationIsNotAllowedException;
import model.exceptions.UserUnnkownOrBadPasswordException;
import model.operations.PaymentIn;
import model.operations.Withdraw;

import java.sql.SQLException;

/**
 * Created by Krzysztof Podlaski on 04.03.2018.
 */
public class AccountManager {
    private DAO dao;
    BankHistory history;
    AuthenticationManager auth;
    InterestOperator interestOperator;
    User loggedUser=null;

    public boolean paymentIn(User user, double ammount,
                   String description, int accountId) throws SQLException {
        Account account = dao.findAccountById(accountId);
        Operation operation = new PaymentIn(user, ammount,description, account);
        boolean success = account.income(ammount);
        if (success)
            success = dao.updateAccountState(account);
        history.logOperation(operation, success);
        return success;
    }

    public boolean paymentOut(User user, double ammount, String description, int accountId) throws OperationIsNotAllowedException, SQLException {
        Account account = dao.findAccountById(accountId);
        Operation operation = new Withdraw(user, ammount,description, account);
        boolean success = auth.canInvokeOperation(operation,user );
        if (!success){
            history.logUnauthorizedOperation(operation, success);
            throw new OperationIsNotAllowedException("Unauthorized operation");
        }
        success = account.outcome(ammount);
        success = dao.updateAccountState(account);
        history.logOperation(operation, success);
        return success;
    }

    public boolean internalPayment(User user, double ammount, String description, int sourceAccountId, int destAccountId) throws OperationIsNotAllowedException, SQLException {
        Account sourceAccount = dao.findAccountById(sourceAccountId);
        Account destAccount = dao.findAccountById(destAccountId);
        Operation withdraw = new Withdraw(user, ammount,description, sourceAccount);
        Operation payment = new PaymentIn(user, ammount,description, destAccount);
        boolean success = auth.canInvokeOperation(withdraw,user );
        if (!success){
            history.logUnauthorizedOperation(withdraw, success);
            throw new OperationIsNotAllowedException("Unauthorized operation");
        }
        success = sourceAccount.outcome(ammount);
        success = success && destAccount.income(ammount);
        if (success) {
            success = dao.updateAccountState(sourceAccount);
            if (success) dao.updateAccountState(destAccount);
        }
        history.logOperation(withdraw, success);
        history.logOperation(payment, success);
        return success;
    }

    public static AccountManager buildBank() {
        try {
            DAO dao = SQLiteDB.createDAO();
            BankHistory history = new BankHistory(dao);
            AuthenticationManager am = new AuthenticationManager(dao, history);
            AccountManager aManager = new AccountManager();
            InterestOperator io = new InterestOperator(dao, aManager);
            aManager.dao = dao;
            aManager.auth = am;
            aManager.history = history;
            aManager.interestOperator = io;
            return aManager;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean logIn(String userName, char[] password) throws UserUnnkownOrBadPasswordException, SQLException {
        loggedUser =  auth.logIn(userName, password);
        return loggedUser!=null;
    }

    public boolean logOut(User user) throws SQLException {
        if (auth.logOut(user)) {
            loggedUser = null;
            return true;
        }
        return false;
    }

    public User getLoggedUser() {
        return loggedUser;
    }
}
