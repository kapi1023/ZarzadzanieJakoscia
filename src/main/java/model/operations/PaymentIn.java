package model.operations;

import model.Account;
import model.User;

/**
 * Created by Krzysztof Podlaski on 07.03.2018.
 */
public class PaymentIn  extends Payment{

    public PaymentIn(User user, double ammount, String description, Account account) {
        super(user, ammount, description, account, OperationType.PAYMENT_IN);
    }
}
