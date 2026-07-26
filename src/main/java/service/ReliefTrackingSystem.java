package service;

import model.Person;
import model.DisasterEvent;
import model.MatchRecord;
import dao.PersonDAO;
import dao.DisasterEventDAO;
import java.util.List;
import java.util.ArrayList;

public class ReliefTrackingSystem {
    private PersonDAO personDAO;
    private DisasterEventDAO disasterEventDAO;
    private MatchingService matchingService;

    private List<Person> persons;
    private List<DisasterEvent> disasterEvents;

    public ReliefTrackingSystem(PersonDAO personDAO, DisasterEventDAO disasterEventDAO) {
        this.personDAO = personDAO;
        this.disasterEventDAO = disasterEventDAO;
        this.matchingService = new MatchingService();
        this.persons = new ArrayList<Person>();
        this.disasterEvents = new ArrayList<DisasterEvent>();
    }

    public void reportPerson(Person person) {
        persons.add(person);
        personDAO.save(person);
    }

    public void registerDisasterEvent(DisasterEvent event) {
        disasterEvents.add(event);
        disasterEventDAO.save(event);
    }

    public List<MatchRecord> runMatching() {
        return matchingService.findMatches(persons);
    }

    public List<Person> getPersons() {
        return persons;
    }

    public List<DisasterEvent> getDisasterEvents() {
        return disasterEvents;
    }
}
