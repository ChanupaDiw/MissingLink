package model;

import interfaces.Locatable;
import enums.PersonStatus;
import java.time.LocalDate;

public abstract class Person implements Locatable, Comparable<Person> {
    private int personId;
    private String fullName;
    private int age;
    private String gender;
    private String physicalDescription;
    private String photoUrl;
    private String nicNumber;
    private PersonStatus status;
    private LocalDate dateReported;
    private LocalDate dateLastUpdated;
    private DisasterEvent disasterEvent;
    private User reportedBy;
    private User lastUpdatedBy;

    public Person(int personId, String fullName, int age, String gender, String physicalDescription,
                  String photoUrl, PersonStatus status, DisasterEvent disasterEvent, User reportedBy) {
        this.personId = personId;
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.physicalDescription = physicalDescription;
        this.photoUrl = photoUrl;
        this.status = status;
        this.disasterEvent = disasterEvent;
        this.reportedBy = reportedBy;
        this.dateReported = LocalDate.now();
        this.dateLastUpdated = LocalDate.now();
    }

    public abstract String getSummary();

    public void updateStatus(PersonStatus newStatus, User updatedBy) {
        this.status = newStatus;
        this.lastUpdatedBy = updatedBy;
        this.dateLastUpdated = LocalDate.now();
    }

    
    @Override
    public int compareTo(Person other) {
        return this.dateReported.compareTo(other.dateReported);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Person)) {
            return false;
        }
        Person other = (Person) obj;
        return this.personId == other.personId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(personId);
    }

    // getters setters

    public int getId() {
        return personId;
    }

    public String getName() {
        return fullName;
    }

    public void setName(String fullName) {
        this.fullName = fullName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public String getNicNumber() {
        return nicNumber;
    }

    public String getPhysicalDescription() {
        return physicalDescription;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public PersonStatus getStatus() {
        return status;
    }

    public LocalDate getDateReported() {
        return dateReported;
    }

    public LocalDate getDateLastUpdated() {
        return dateLastUpdated;
    }

    public DisasterEvent getDisasterEvent() {
        return disasterEvent;
    }

    public User getReportedBy() {
        return reportedBy;
    }

    public User getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setNicNumber(String nicNumber) {
        if (nicNumber != null && !nicNumber.isEmpty() && !isValidNic(nicNumber)) {
            throw new IllegalArgumentException(
                    "Invalid NIC number: must be exactly 12 digits, or 9 digits followed by 'V'.");
        }
        this.nicNumber = nicNumber;
    }

    public static boolean isValidNic(String nic) {
        return nic.matches("\\d{12}") || nic.matches("\\d{9}[Vv]");
    }


    @Override
    public String toString() {
        return fullName + " (" + age + ", " + gender + ") - " + status;
    }
}
