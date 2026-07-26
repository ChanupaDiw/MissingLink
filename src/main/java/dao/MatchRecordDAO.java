package dao;

import model.MatchRecord;
import enums.MatchStatus;
import java.util.List;

public interface MatchRecordDAO {
    void save(MatchRecord match);
    MatchRecord findById(int id);
    List<MatchRecord> findAll();
    List<MatchRecord> findByStatus(MatchStatus status);
    void updateStatus(int id, MatchStatus newStatus);
}
