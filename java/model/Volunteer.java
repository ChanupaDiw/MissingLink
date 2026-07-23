package model;

import enums.UserRole;

public class Volunteer extends User {
    private boolean available;

    public Volunteer(int id, String fullName, String username, String password, boolean available) {
        super(id, fullName, username, password, UserRole.VOLUNTEER);
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String[] getPermissions() {
        return new String[] { "REPORT_FOUND_PERSON", "UPDATE_STATUS", "VIEW_ALL" };
    }
}
