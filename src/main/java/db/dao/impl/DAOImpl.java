package db.dao.impl;

import db.dao.DAO;
import model.*;
import model.operations.Interest;
import model.operations.LogOperation;
import model.operations.Payment;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by Krzysztof Podlaski on 04.03.2018.
 */
public class DAOImpl implements DAO{
    Connection db;


    public User findUserByName(String userName) throws SQLException {
        String sql = "SELECT " +
                "user.id as user_id, " +
                "user.name as user_name, " +
                "role.id as role_id, " +
                "role.name as role_name " +
                "FROM User , Role  " +
                "WHERE role.id=user.role_id " +
                "AND user_name = '"+userName+"'";
        User user = null;
        Statement st = null;
        try{
            st = db.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()){
                user = getUserFromRS(rs);
            }
            st.close();
        }
        catch(SQLException e){
            if (st!= null) {
                st.close();
            }
            throw e;
        }
        return user;
    }




    public Password findPasswordForUser(User user) throws SQLException {
        String sql = "SELECT passwd FROM Password  WHERE user_id="+user.getId();
        Statement st =null;
        Password passwd =null;
        try {
            st= db.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) passwd = new Password();
            passwd.setPasswd(rs.getString("passwd"));
            passwd.setUserId(user.getId());
            st.close();
        }
        catch (SQLException e){
            if (st!=null) st.close();
            throw e;
        }

        return passwd;
    }


    public Account findAccountById(int accountId) throws SQLException {
        String sql= "SELECT " +
                "user.id as user_id, " +
                "user.name as user_name, " +
                "role.id as role_id, " +
                "role.name as role_name, " +
                "account.id as account_id, " +
                "ammount " +
                "FROM User , Role, Account " +
                "WHERE role.id=user.role_id AND " +
                "owner_id = user.id AND " +
                "user_id = "+accountId;
        Statement st = null;
        Account account = null;
        try {
            st = db.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) account = getAccountFromRs(rs);
            st.close();
        }
        catch(SQLException e){
            if (st != null) st.close();
            throw e;
        }
        return account;
    }


    public boolean updateAccountState(Account account) throws SQLException {
        String sql = "UPDATE ACCOUNT SET ammount = "+account.getAmmount() +
                " WHERE id="+account.getId();
        Statement st = null;
        int infected_rows =0;
        try {
            st = db.createStatement();
            infected_rows = st.executeUpdate(sql);
            st.close();
        }
        catch(SQLException e){
            if (st!=null) st.close();
            throw e;
        }
        if (infected_rows>=1) return true;
        return false;
    }

    public boolean setUserPassword(User user, String passwd, String oldPass) throws SQLException {
        String sql = "UPDATE PASSWORD SET passwd = '"+passwd +
                "' WHERE user_id="+user.getId()+" AND passwd = '"+oldPass+"'" ;
        Statement st = null;
        int infected_rows =0;
        try {
            st = db.createStatement();
            infected_rows = st.executeUpdate(sql);
            st.close();
        }
        catch(SQLException e){
            if (st!=null) st.close();
            throw e;
        }
        if (infected_rows>=1) return true;
        return false;
    }

    private User getUserFromRS(ResultSet rs) throws SQLException {
        User user = new User();
        Role role = new Role();
        user.setId(rs.getInt("user_id"));
        user.setName(rs.getString("user_name"));
        role.setId(rs.getInt("role_id"));
        role.setName(rs.getString("role_name"));
        user.setRole(role);
        return user;
    }

    private Account getAccountFromRs(ResultSet rs) throws SQLException {
        User user = getUserFromRS(rs);
        Account account = new Account();
        account.setOwner(user);
        account.setId(rs.getInt("account_id"));
        account.setAmmount(rs.getDouble("ammount"));
        return account;
    }

    public void close() throws SQLException {
        db.close();
    }

    public void logOperation(Operation operation, boolean success) throws SQLException {
        Statement st = db.createStatement();
        String sqlStart = "INSERT INTO OPERATION ";
        String sqlFields = "(`type`, `description`, `date`, `user_id` ";
        String sqlValues = "VALUES ('"+operation.getType()+"'," +
                "'"+operation.getDescription()+"'," +
                "'"+operation.getDate()+"'," +
                operation.getUser().getId();
        switch(operation.getType()){
            case INTEREST: buildQueryInterest((Interest) operation,sqlFields,sqlValues);break;
            case LOG_IN: buildQueryLogOperation((LogOperation) operation,sqlFields,sqlValues);break;
            case LOG_OUT: buildQueryLogOperation((LogOperation) operation,sqlFields,sqlValues);break;
            case WITHDRAW: buildQueryPayment((Payment) operation,sqlFields,sqlValues);break;
            case PAYMENT_IN: buildQueryPayment((Payment) operation,sqlFields,sqlValues);break;
        }
        sqlFields+=") ";
        sqlValues+=") ";
        st.executeUpdate(sqlStart+sqlFields+sqlValues);
        ResultSet rs = st.getGeneratedKeys();
        if (rs.next()) {
            int generatedkey=rs.getInt(1);
            Statement st2 = db.createStatement();
            String sql ="INSERT INTO History (`operation_id`,`succesfull`) " +
                    "VALUES ("+rs.getInt(1)+", '"+success+"')";
            st2.execute(sql);
            st2.close();
        }
        st.close();

    }

    private void buildQueryPayment(Payment operation, String sqlFields, String sqlValues) {
        sqlFields+= ", `ammount`,`account_id`) ";
        sqlValues+= ", "+operation.getAmmount()+","+
                    operation.getAccount().getId()+")";
    }

    private void buildQueryInterest(Interest operation, String sqlFields, String sqlValues) {
        sqlFields+= ", `ammount`,`account_id`) ";
        sqlValues+= ", "+operation.getAmmount()+","+
                operation.getAccount().getId()+")";
    }

    private void buildQueryLogOperation(LogOperation operation, String sqlFields, String sqlValues) {
        sqlFields+= " )";
        sqlValues+= " )";
    }


}
