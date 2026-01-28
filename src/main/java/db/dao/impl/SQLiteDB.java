package db.dao.impl;

import db.dao.DAO;
import model.operations.OperationType;

import java.sql.*;

/**
 * Created by Krzysztof Podlaski on 04.03.2018.
 */
public class SQLiteDB {

    public static DAO createDAO() throws ClassNotFoundException, SQLException {
        String url = "jdbc:sqlite:./bank.db";
        return createDAO(url);
    }

    public static DAO createDAO(String url) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(url);
        DAOImpl dao = new DAOImpl();
        dao.db = conn;
        return dao;
    }

    public static void initializeDB() throws ClassNotFoundException, SQLException {
        initializeDB("jdbc:sqlite:./bank.db");
    }


    public static void initializeDB(String connectionString) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:./bank.db");
        String sql;
        Statement st = conn.createStatement();

        sql = "CREATE TABLE `Role` (" +
                " `id`	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                " `name`	TEXT NOT NULL UNIQUE )";
        st.executeUpdate(sql);

        sql = "CREATE TABLE \"User\" ( " +
                " `id`  INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " `name` TEXT NOT NULL UNIQUE, " +
                " `role_id` INTEGER NOT NULL, " +
                " FOREIGN KEY(`role_id`) REFERENCES Role(id) " +
                ")";
        st.executeUpdate(sql);

        sql = "CREATE TABLE \"Password\" ( " +
                " `user_id` INTEGER, " +
                " `passwd` TEXT, " +
                " FOREIGN KEY(`user_id`) REFERENCES User(id) " +
                ")";
        st.executeUpdate(sql);

        sql = "CREATE TABLE `Account` (" +
                " `id`\tINTEGER PRIMARY KEY AUTOINCREMENT, " +
                " `owner_id` INTEGER, " +
                " `ammount` REAL, " +
                "FOREIGN KEY(`owner_id`) REFERENCES User(id) " +
                ")";
        st.executeUpdate(sql);

        sql = "CREATE TABLE `Operation` ( " +
                " `id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " `ammount` REAL, " +
                " `description` TEXT NOT NULL, " +
                " `date` TEXT NOT NULL, " +
                " `user_id` INTEGER," +
                " `account_id` INTEGER," +
                " `type` INTEGER," +
                "FOREIGN KEY(`type`) REFERENCES Operation_Types ( id ), "+
                "FOREIGN KEY(`user_id`) REFERENCES User ( id ), "+
                "FOREIGN KEY(`account_id`) REFERENCES Account ( id ) "+
                ")";
        st.executeUpdate(sql);

        sql = "CREATE TABLE `History` ( " +
                " `operation_id` INTEGER, " +
                " `succesfull` INTEGER NOT NULL, " +
                " FOREIGN KEY(`operation_id`) REFERENCES Operation(id) " +
                ")";
        st.executeUpdate(sql);

        sql= "CREATE TABLE `Operation_Types` ( " +
                " `id` INTEGER, " +
                " `name` TEXT, " +
                " PRIMARY KEY(id) " +
                ")";

        st.executeUpdate(sql);

        sql = "INSERT INTO Operation_Types " +
                " (`id`, `name`) VALUES " +
                " (?,?)";
        PreparedStatement pst = conn.prepareStatement(sql);

        for (OperationType type : OperationType.values()) {
            pst.setString(type.getId(),type.name());
            pst.execute();
        }
        pst.close();


        st.close();
        conn.close();
    }
}
