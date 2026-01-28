package model;

/**
 * Created by Krzysztof Podlaski on 04.03.2018.
 */
public class Password {
    private int userId;
    private String passwd;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }
}
