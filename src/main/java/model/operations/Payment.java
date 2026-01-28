package model.operations;

import model.Account;
import model.Operation;
import model.User;

/**
 * Created by Krzysztof Podlaski on 07.03.2018.
 */
public abstract class Payment extends Operation{

    private double ammount;
    private Account account;

    public Payment(User user, double ammount, String description, Account account, OperationType operationType) {
        super(user, description, operationType);
        this.ammount = ammount;
        this.account=account;
    }

    public double getAmmount() {
        return ammount;
    }

    public Account getAccount() {
        return account;
    }
}
