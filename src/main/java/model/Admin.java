package model;

import enums.UserRole;

public class Admin extends User {
    //attributes
    private String department;
    //constructor
        public Admin(int id, String fullName, String username, String password, String department) {
        super(id, fullName, username, password, UserRole.ADMIN);
        this.department = department;
    }
    //methods
    public String getDepartment() {
        return department;
    }

    @Override
    public String[] getPermissions() {
        return new String[] { "MANAGE_USERS", "MANAGE_DISASTERS", "VERIFY_MATCH", "VIEW_ALL" };
    }
}
