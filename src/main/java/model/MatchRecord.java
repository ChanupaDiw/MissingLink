package model;

import enums.MatchStatus;
import java.time.LocalDateTime;

public class MatchRecord {
    //attributes
    private int matchId;
    private Person missingPerson;
    private Person foundPerson;
    private double confidenceScore;
    private User verifiedBy;
    private LocalDateTime verifiedAt;
    private MatchStatus status;
    //constructor
    public MatchRecord(int matchId, Person missingPerson, Person foundPerson, double confidenceScore) {
        this.matchId = matchId;
        this.missingPerson = missingPerson;
        this.foundPerson = foundPerson;
        this.confidenceScore = confidenceScore;
        this.status = MatchStatus.SUGGESTED;
    }

   //admin confirmation for match records
    public void confirm(User verifier) {
        this.status = MatchStatus.CONFIRMED;
        this.verifiedBy = verifier;
        this.verifiedAt = LocalDateTime.now();
        missingPerson.updateStatus(enums.PersonStatus.FOUND, verifier);
    }

    public void reject(User verifier) {
        this.status = MatchStatus.REJECTED;
        this.verifiedBy = verifier;
        this.verifiedAt = LocalDateTime.now();
    }

    public boolean isHighConfidence() {

        return confidenceScore >= 75;
    }

    public int getId() {
        return matchId;
    }

    public Person getMissingPerson() {
        return missingPerson;
    }

    public Person getFoundPerson() {
        return foundPerson;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public User getVerifiedBy() {
        return verifiedBy;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public MatchStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return missingPerson.getName() + " <-> " + foundPerson.getName()
                + " (confidence: " + confidenceScore + "%, " + status + ")";
    }
}
