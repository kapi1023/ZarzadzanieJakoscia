package db.dao;

import model.Account;
import model.Operation;
import model.Password;
import model.User;

import java.sql.SQLException;

/**
 * Created by Krzysztof Podlaski on 04.03.2018.
 */
public interface DAO {
    User findUserByName(String userName) throws SQLException;
    Password findPasswordForUser(User user) throws SQLException;
    Account findAccountById(int accountId) throws SQLException;
    boolean updateAccountState(Account account) throws SQLException;
    boolean setUserPassword(User user, String passwd, String oldPass) throws SQLException;
    void close() throws SQLException;

    void logOperation(Operation operation, boolean success) throws SQLException;
}
