package model.operations;

import model.Operation;
import model.User;

/**
 * Created by Krzysztof Podlaski on 07.03.2018.
 */
public class LogOperation extends Operation{
    protected LogOperation(User user, String description, OperationType operationType) {
        super(user, description, operationType);
    }
}
