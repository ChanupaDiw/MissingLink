package dao;

import model.*;
import enums.*;
import java.sql.*;
import java.util.*;

public class PersonDAOImpl implements PersonDAO {
    private Connection connection;
    private UserDAO userDAO;

    public PersonDAOImpl(Connection connection) {
        this.connection = connection;
        this.userDAO = new UserDAOImpl(connection);
    }

    @Override
    public void save(Person person) {
        String sql = "INSERT INTO persons (person_type, full_name, age, gender, physical_description, "
                + "photo_url, nic_number, status, latitude, longitude, address, circumstances, condition_at_discovery, "
                + "disaster_id, reported_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            Location location = person.getLocation();

            if (person instanceof LostPerson) {
                LostPerson lost = (LostPerson) person;
                stmt.setString(1, "LOST");
                setCommonFields(stmt, person, location);
                stmt.setString(12, lost.getCircumstances());
                stmt.setNull(13, Types.VARCHAR);
            } else {
                FoundPerson found = (FoundPerson) person;
                stmt.setString(1, "FOUND");
                setCommonFields(stmt, person, location);
                stmt.setNull(12, Types.VARCHAR);
                stmt.setString(13, found.getConditionAtDiscovery());
            }

            // Properly set the disaster_id so admin dashboard counts work
            if (person.getDisasterEvent() != null && person.getDisasterEvent().getId() != 0) {
                stmt.setInt(14, person.getDisasterEvent().getId());
            } else {
                stmt.setNull(14, Types.INTEGER);
            }

            if (person.getReportedBy() != null && person.getReportedBy().getId() != 0) {
                stmt.setInt(15, person.getReportedBy().getId());
            } else {
                stmt.setNull(15, Types.INTEGER);
            }

            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to save person: " + e.getMessage());
        }
    }

    // Handle saving the edited report data back to the database
    @Override
    public void update(Person person) {
        String sql = "UPDATE persons SET full_name = ?, age = ?, gender = ?, physical_description = ?, "
                + "nic_number = ?, latitude = ?, longitude = ?, address = ?, circumstances = ?, condition_at_discovery = ?, "
                + "date_last_updated = CURRENT_DATE WHERE person_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, person.getName());
            stmt.setInt(2, person.getAge());
            stmt.setString(3, person.getGender());
            stmt.setString(4, person.getPhysicalDescription());
            stmt.setString(5, person.getNicNumber());
            stmt.setDouble(6, person.getLocation().getLatitude());
            stmt.setDouble(7, person.getLocation().getLongitude());
            stmt.setString(8, person.getLocation().getAddress());

            if (person instanceof LostPerson) {
                LostPerson lost = (LostPerson) person;
                stmt.setString(9, lost.getCircumstances());
                stmt.setNull(10, Types.VARCHAR);
            } else {
                FoundPerson found = (FoundPerson) person;
                stmt.setNull(9, Types.VARCHAR);
                stmt.setString(10, found.getConditionAtDiscovery());
            }

            stmt.setInt(11, person.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to update person: " + e.getMessage());
        }
    }

    // Sets the fields shared by both LostPerson and FoundPerson 
    private void setCommonFields(PreparedStatement stmt, Person person, Location location) throws SQLException {
        stmt.setString(2, person.getName());
        stmt.setInt(3, person.getAge());
        stmt.setString(4, person.getGender());
        stmt.setString(5, person.getPhysicalDescription());
        stmt.setString(6, person.getPhotoUrl());
        stmt.setString(7, person.getNicNumber());
        stmt.setString(8, person.getStatus().name());
        stmt.setDouble(9, location.getLatitude());
        stmt.setDouble(10, location.getLongitude());
        stmt.setString(11, location.getAddress());
    }

    @Override
    public Person findById(int id) {
        String sql = "SELECT * FROM persons WHERE person_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToPerson(rs);
            }
        } catch (SQLException e) {
            System.out.println("Failed to find person: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Person> findAll() {
        List<Person> persons = new ArrayList<Person>();
        String sql = "SELECT * FROM persons";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                persons.add(mapRowToPerson(rs));
            }
        } catch (SQLException e) {
            System.out.println("Failed to load persons: " + e.getMessage());
        }
        return persons;
    }

    @Override
    public List<Person> findByStatus(PersonStatus status) {
        List<Person> persons = new ArrayList<Person>();
        String sql = "SELECT * FROM persons WHERE status = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                persons.add(mapRowToPerson(rs));
            }
        } catch (SQLException e) {
            System.out.println("Failed to load persons by status: " + e.getMessage());
        }
        return persons;
    }

    @Override
    public void updateStatus(int id, PersonStatus newStatus) {
        String sql = "UPDATE persons SET status = ?, date_last_updated = CURRENT_DATE WHERE person_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newStatus.name());
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to update status: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM persons WHERE person_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to delete person: " + e.getMessage());
        }
    }

    // Reconstructs the correct subclass from a result row,
    
    private Person mapRowToPerson(ResultSet rs) throws SQLException {
        int id = rs.getInt("person_id");
        String type = rs.getString("person_type");
        String name = rs.getString("full_name");
        int age = rs.getInt("age");
        String gender = rs.getString("gender");
        String description = rs.getString("physical_description");
        String photoUrl = rs.getString("photo_url");
        String nicNumber = rs.getString("nic_number");

        Location location = new Location(rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getString("address"));

        DisasterEvent disasterEvent = null;
        int disasterIdColumn = rs.getInt("disaster_id");
        if (!rs.wasNull()) {
            disasterEvent = new DisasterEvent(disasterIdColumn, null, null, null, null, 0);
        }

        // Load the reporting user, if one is recorded - needed so "my submissions" filtering works
        User reportedBy = null;
        int reportedByColumn = rs.getInt("reported_by");
        if (!rs.wasNull()) {
            reportedBy = userDAO.findById(reportedByColumn);
        }

        if ("LOST".equals(type)) {
            String circumstances = rs.getString("circumstances");
            LostPerson lost = new LostPerson(id, name, age, gender, description, photoUrl, location, circumstances, disasterEvent, reportedBy);
            lost.setNicNumber(nicNumber);
            return lost;
        } else {
            String condition = rs.getString("condition_at_discovery");
            FoundPerson found = new FoundPerson(id, name, age, gender, description, photoUrl, location, condition, disasterEvent, reportedBy);
            found.setNicNumber(nicNumber);
            return found;
        }
    }
}