package model;

import model.operations.OperationType;

import java.util.Date;

/**
 * Created by Krzysztof Podlaski on 04.03.2018.
 */
public abstract class Operation {
    private int id;
    private OperationType type;
    private String description;
    private User user;
    private Date date;

    protected Operation(User user, String description, OperationType operationType) {
        this.user=user;
        this.description=description;
        this.date=new Date();
        this.type=operationType;
    }

    public OperationType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public User getUser() {
        return user;
    }

    public Date getDate() {
        return date;
    }
}
