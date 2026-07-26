package dao;

import model.DisasterEvent;
import model.Location;
import enums.DisasterType;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

public class DisasterEventDAOImpl implements DisasterEventDAO {
    private Connection connection;

    public DisasterEventDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(DisasterEvent event) {
        String sql = "INSERT INTO disaster_events (type, description, date_occurred, latitude, longitude, "
                + "address, radius_km) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            Location location = event.getLocation();
            stmt.setString(1, event.getType().name());
            stmt.setString(2, event.getDescription());
            stmt.setDate(3, Date.valueOf(event.getDateOccurred()));
            stmt.setDouble(4, location.getLatitude());
            stmt.setDouble(5, location.getLongitude());
            stmt.setString(6, location.getAddress());
            stmt.setDouble(7, event.getRadiusKm());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to save disaster event: " + e.getMessage());
        }
    }

    @Override
    public DisasterEvent findById(int id) {
        String sql = "SELECT * FROM disaster_events WHERE disaster_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToDisasterEvent(rs);
            }
        } catch (SQLException e) {
            System.out.println("Failed to find disaster event: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<DisasterEvent> findAll() {
        List<DisasterEvent> events = new ArrayList<DisasterEvent>();
        String sql = "SELECT * FROM disaster_events";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                events.add(mapRowToDisasterEvent(rs));
            }
        } catch (SQLException e) {
            System.out.println("Failed to load disaster events: " + e.getMessage());
        }
        return events;
    }

    @Override
    public void update(DisasterEvent event) {
        String sql = "UPDATE disaster_events SET type = ?, description = ?, latitude = ?, "
                + "longitude = ?, address = ?, radius_km = ? WHERE disaster_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            Location location = event.getLocation();
            stmt.setString(1, event.getType().name());
            stmt.setString(2, event.getDescription());
            stmt.setDouble(3, location.getLatitude());
            stmt.setDouble(4, location.getLongitude());
            stmt.setString(5, location.getAddress());
            stmt.setDouble(6, event.getRadiusKm());
            stmt.setInt(7, event.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to update disaster event: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM disaster_events WHERE disaster_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to delete disaster event: " + e.getMessage());
        }
    }

    private DisasterEvent mapRowToDisasterEvent(ResultSet rs) throws SQLException {
        int id = rs.getInt("disaster_id");
        DisasterType type = DisasterType.valueOf(rs.getString("type"));
        String description = rs.getString("description");
        LocalDate dateOccurred = rs.getDate("date_occurred").toLocalDate();
        Location location = new Location(rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getString("address"));
        double radiusKm = rs.getDouble("radius_km");

        return new DisasterEvent(id, type, description, dateOccurred, location, radiusKm);
    }
}