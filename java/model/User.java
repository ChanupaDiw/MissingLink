package model;

import enums.UserRole;

public abstract class User {
    //attributes
    private int id;
    private String fullName;
    private String username;
    private transient String password;
    private String contactNumber;
    private UserRole role;
    private boolean approved = true;
    //constructor
    public User(int id, String fullName, String username, String password, UserRole role) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isApproved() {
        return approved;
    }
    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }


    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    protected boolean checkPassword(String attempt) {
        return password.equals(attempt);
    }


    public boolean authenticate(String attempt) {
        return checkPassword(attempt);
    }


    public boolean hasPermission(String permission) {
        String[] permissions = getPermissions();
        for (String p : permissions) {
            if (p.equalsIgnoreCase(permission)) {
                return true;
            }
        }
        return false;
    }

    
    public abstract String[] getPermissions();
}
