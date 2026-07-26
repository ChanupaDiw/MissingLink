package model;

import enums.PersonStatus;

public class FoundPerson extends Person {
    private Location foundLocation;
    private String conditionAtDiscovery; // e.g. "injured", "stable", "unconscious"

    public FoundPerson(int personId, String fullName, int age, String gender, String physicalDescription,
                        String photoUrl, Location foundLocation, String conditionAtDiscovery,
                        DisasterEvent disasterEvent, User reportedBy) {
        super(personId, fullName, age, gender, physicalDescription, photoUrl,
                PersonStatus.FOUND, disasterEvent, reportedBy);
        this.foundLocation = foundLocation;
        this.conditionAtDiscovery = conditionAtDiscovery;
    }

    public Location getFoundLocation() {
        return foundLocation;
    }

    public String getConditionAtDiscovery() {
        return conditionAtDiscovery;
    }

    @Override
    public Location getLocation() {
        return foundLocation;
    }

    @Override
    public String getSummary() {
        return getName() + " was found at " + foundLocation.getAddress()
                + " on " + getDateReported() + ". Condition: " + conditionAtDiscovery + ".";
    }
}
