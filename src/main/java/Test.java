import biz.AuthenticationManager;
import db.dao.DAO;
import db.dao.impl.SQLiteDB;
import model.Account;
import model.User;

import java.sql.SQLException;

/**
 * Created by Krzysztof Podlaski on 04.03.2018.
 */
public class Test {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        DAO dao = SQLiteDB.createDAO();
        User user = dao.findUserByName("Adam");
        System.out.println(user.getId()+ " "+user.getName()+ " "+user.getRole().getName());
        Account acc = dao.findAccountById(2);
        user = acc.getOwner();
        System.out.println(user.getId()+ " "+user.getName()+ " "+user.getRole().getName());
        System.out.println(acc.getAmmount());
        acc.setAmmount(11433.85);
        dao.updateAccountState(acc);

        /*String s = AuthenticationManager.hashPassword("Adam");
        System.out.println(s);
        System.out.println(s.equals(AuthenticationManager.hashPassword("Adam")));
        user = dao.findUserByName("Adam");
        dao.setUserPassword(user,s,"Adam");
        user = dao.findUserByName("Ewa");
        s = AuthenticationManager.hashPassword("Ewa");
        dao.setUserPassword(user,s,"Ewa");
        user = dao.findUserByName("Admin");
        s = AuthenticationManager.hashPassword("Admin");
        dao.setUserPassword(user,s,"Admin");
        */

    }
}
