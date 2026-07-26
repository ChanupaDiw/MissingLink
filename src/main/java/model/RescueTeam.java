package model;

import enums.UserRole;

public class RescueTeam extends User {
    private String unitName;
    private String assignedZone;

    public RescueTeam(int id, String fullName, String username, String password, String unitName, String assignedZone) {
        super(id, fullName, username, password, UserRole.RESCUE_TEAM);
        this.unitName = unitName;
        this.assignedZone = assignedZone;
    }

    public String getUnitName() {
        return unitName;
    }

    public String getAssignedZone() {
        return assignedZone;
    }

    @Override
    public String[] getPermissions() {
        return new String[] {
            "VIEW_ALL", "REPORT_MISSING_PERSON", "REPORT_FOUND_PERSON",
            "UPDATE_STATUS", "VERIFY_MATCH"
        };
    }
}
