package dao;

import model.MatchRecord;
import model.Person;
import enums.MatchStatus;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class MatchRecordDAOImpl implements MatchRecordDAO {
    private Connection connection;
    private PersonDAO personDAO;

    public MatchRecordDAOImpl(Connection connection) {
        this.connection = connection;
        this.personDAO = new PersonDAOImpl(connection);
    }

    @Override
    public void save(MatchRecord match) {
        String sql = "INSERT INTO match_records (missing_person_id, found_person_id, confidence_score, status) "
                + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, match.getMissingPerson().getId());
            stmt.setInt(2, match.getFoundPerson().getId());
            stmt.setDouble(3, match.getConfidenceScore());
            stmt.setString(4, match.getStatus().name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to save match record: " + e.getMessage());
        }
    }

    @Override
    public MatchRecord findById(int id) {
        String sql = "SELECT * FROM match_records WHERE match_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToMatchRecord(rs);
            }
        } catch (SQLException e) {
            System.out.println("Failed to find match record: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<MatchRecord> findAll() {
        List<MatchRecord> matches = new ArrayList<MatchRecord>();
        String sql = "SELECT * FROM match_records";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                matches.add(mapRowToMatchRecord(rs));
            }
        } catch (SQLException e) {
            System.out.println("Failed to load match records: " + e.getMessage());
        }
        return matches;
    }

    @Override
    public List<MatchRecord> findByStatus(MatchStatus status) {
        List<MatchRecord> matches = new ArrayList<MatchRecord>();
        String sql = "SELECT * FROM match_records WHERE status = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                matches.add(mapRowToMatchRecord(rs));
            }
        } catch (SQLException e) {
            System.out.println("Failed to load match records by status: " + e.getMessage());
        }
        return matches;
    }

    @Override
    public void updateStatus(int id, MatchStatus newStatus) {
        String sql = "UPDATE match_records SET status = ?, verified_at = NOW() WHERE match_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newStatus.name());
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to update match record status: " + e.getMessage());
        }
    }

    private MatchRecord mapRowToMatchRecord(ResultSet rs) throws SQLException {
        int id = rs.getInt("match_id");
        Person missing = personDAO.findById(rs.getInt("missing_person_id"));
        Person found = personDAO.findById(rs.getInt("found_person_id"));
        double confidenceScore = rs.getDouble("confidence_score");

        MatchRecord match = new MatchRecord(id, missing, found, confidenceScore);
        return match;
    }
}
