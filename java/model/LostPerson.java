package model;

import enums.PersonStatus;
import interfaces.Locatable;

public class LostPerson extends Person {
    private Location lastKnownLocation;
    private String circumstances; // note about missing details

    public LostPerson(int personId, String fullName, int age, String gender, String physicalDescription,
                      String photoUrl, Location lastKnownLocation, String circumstances,
                      DisasterEvent disasterEvent, User reportedBy) {
        super(personId, fullName, age, gender, physicalDescription, photoUrl,
                PersonStatus.MISSING, disasterEvent, reportedBy);
        this.lastKnownLocation = lastKnownLocation;
        this.circumstances = circumstances;
    }

    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    public String getCircumstances() {
        return circumstances;
    }

    @Override
    public Location getLocation() {
        return lastKnownLocation;
    }

    @Override
    public String getSummary() {
        return getName() + " went missing near " + lastKnownLocation.getAddress()
                + " on " + getDateReported() + ".";
    }
}
