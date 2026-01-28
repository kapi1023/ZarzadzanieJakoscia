package gui;

import biz.AccountManager;
import model.exceptions.UserUnnkownOrBadPasswordException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerAdapter;
import java.sql.SQLException;

/**
 * Created by Krzysztof Podlaski on 07.03.2018.
 */
public class Application {
    private JPanel mainPanel;
    private JButton signInButton;
    private AccountManager accountManager;
    private JLabel userInfo;
    private JFrame additionalWindow;
    LoginForm loginForm;
    public Application() {
        accountManager = AccountManager.buildBank();


        signInButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (accountManager.getLoggedUser()==null) {
                    additionalWindow = new JFrame("Log in");
                    loginForm = new LoginForm(Application.this);
                    additionalWindow.setContentPane(loginForm.loginPanel);
                    additionalWindow.setSize(400, 180);
                    additionalWindow.setLocation(200, 100);
                    additionalWindow.setVisible(true);
                    additionalWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                }
                else{
                    try {
                        boolean r = accountManager.logOut(accountManager.getLoggedUser());
                        if (r)  {
                            signInButton.setText("LogIn");
                            userInfo.setText("Not Logged Yet");
                        }
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }


                }
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Application");
        frame.setContentPane(new Application().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600,600);
        frame.setLocation(200,100);
        frame.setVisible(true);
    }

    void logInOperation(){
        String userName = loginForm.userNameTextField.getText();
        char[] password = loginForm.passwordField.getPassword();
        boolean success = false;
        try {
            success = accountManager.logIn(userName, password);

        } catch (UserUnnkownOrBadPasswordException e1) {
            e1.printStackTrace();
            //Show Dialog for user
        } catch (SQLException e1) {
            e1.printStackTrace();
            //Show Dialog for user
        }
        if(success){
            additionalWindow.setVisible(false);
            additionalWindow.dispose();
            additionalWindow = null;
            userInfo.setText(accountManager.getLoggedUser().getName());
            signInButton.setText("Logout");
        }

    }



}
