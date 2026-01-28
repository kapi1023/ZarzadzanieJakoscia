package model;

/**
 * Created by Krzysztof Podlaski on 04.03.2018.
 */
public class User {
    private int id;
    private String name;
    private Role role;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
