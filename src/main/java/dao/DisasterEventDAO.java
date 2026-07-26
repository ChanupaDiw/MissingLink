package dao;

import model.DisasterEvent;
import java.util.List;

public interface DisasterEventDAO {
    void save(DisasterEvent event);
    DisasterEvent findById(int id);
    List<DisasterEvent> findAll();
    void update(DisasterEvent event);
    void delete(int id);
}