package model.operations;

/**
 * Created by Krzysztof Podlaski on 07.03.2018.
 */
public enum OperationType {
    PAYMENT_IN(0),
    WITHDRAW(1),
    LOG_IN(2),
    LOG_OUT(3),
    INTEREST(4);

    private int id;
    private OperationType(int v){
        id=v;
    }

    public int getId(){return id;}

}
