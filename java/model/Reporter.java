package model;

import enums.UserRole;

public class Reporter extends User {
    private String relationshipToMissingPerson;

    public Reporter(int id, String fullName, String username, String password, String relationshipToMissingPerson) {
        super(id, fullName, username, password, UserRole.REPORTER);
        this.relationshipToMissingPerson = relationshipToMissingPerson;
    }

    public String getRelationshipToMissingPerson() {
        return relationshipToMissingPerson;
    }

    @Override
    public String[] getPermissions() {
        return new String[] { "REPORT_MISSING_PERSON", "VIEW_OWN_REPORTS" };
    }
}
