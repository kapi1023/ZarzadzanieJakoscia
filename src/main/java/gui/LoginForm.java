package gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Krzysztof Podlaski on 07.03.2018.
 */
public class LoginForm {
    private JButton loginStartButton;
    JPasswordField passwordField;
    JPanel loginPanel;
    private Application parent;
    public  JTextField userNameTextField;


    public LoginForm(final Application parent) {
        this.parent=parent;

        loginStartButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parent.logInOperation();
            }
        });
    }
}
