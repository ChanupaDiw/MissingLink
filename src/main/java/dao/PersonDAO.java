package dao;

import model.Person;
import enums.PersonStatus;
import java.util.List;

public interface PersonDAO {
    void save(Person person);
    void update(Person person);
    Person findById(int id);
    List<Person> findAll();
    List<Person> findByStatus(PersonStatus status);
    void updateStatus(int id, PersonStatus newStatus);
    void delete(int id);
}
