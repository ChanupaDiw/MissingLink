package model;

import interfaces.Locatable;
import enums.DisasterType;
import java.time.LocalDate;

public class DisasterEvent implements Locatable {
    //attributes
    private int disasterId;
    private DisasterType type;
    private String description;
    private LocalDate dateOccurred;
    private Location location;
    private double radiusKm;
    private boolean active;
    //constructor
    public DisasterEvent(int disasterId, DisasterType type, String description,
                          LocalDate dateOccurred, Location location, double radiusKm) {
        this.disasterId = disasterId;
        this.type = type;
        this.description = description;
        this.dateOccurred = dateOccurred;
        this.location = location;
        this.radiusKm = radiusKm;
        this.active = true;
    }

    //checking a given points applicable for disaster zone
    public boolean isWithinZone(Location point) {
        return location.distanceTo(point) <= radiusKm;
    }

    public boolean isActive() {
        return active;
    }

    public void resolve() {
        this.active = false;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    public int getId() {
        return disasterId;
    }

    public DisasterType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDateOccurred() {
        return dateOccurred;
    }

    public double getRadiusKm() {
        return radiusKm;
    }

    @Override
    public String toString() {
        return type + " at " + location.getAddress() + " on " + dateOccurred
                + (active ? " [ACTIVE]" : " [RESOLVED]");
    }
}
