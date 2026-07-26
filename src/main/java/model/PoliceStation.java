package model;

import enums.UserRole;

public class PoliceStation extends User {
    private String stationName;
    private String stationCode;
    private String jurisdictionArea;

    public PoliceStation(int id, String fullName, String username, String password,
                          String stationName, String stationCode, String jurisdictionArea) {
        super(id, fullName, username, password, UserRole.POLICE_STATION);
        this.stationName = stationName;
        this.stationCode = stationCode;
        this.jurisdictionArea = jurisdictionArea;
    }

    public String getStationName() {
        return stationName;
    }

    public String getStationCode() {
        return stationCode;
    }

    public String getJurisdictionArea() {
        return jurisdictionArea;
    }

    @Override
    public String[] getPermissions() {
        // Police stations can observe all records and help verify matches,
        // but don't manage users or create/edit disaster events
        return new String[] { "VIEW_ALL", "VERIFY_MATCH" };
    }
}
