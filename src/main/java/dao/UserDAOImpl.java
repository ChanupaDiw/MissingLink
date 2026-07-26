package dao;

import model.*;
import enums.UserRole;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class UserDAOImpl implements UserDAO {
    private Connection connection;

    public UserDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToUser(rs);
            }
        } catch (SQLException e) {
            System.out.println("Failed to find user: " + e.getMessage());
        }
        return null;
    }

    @Override
    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToUser(rs);
            }
        } catch (SQLException e) {
            System.out.println("Failed to find user: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void save(User user, String plainPassword) {
        String sql = "INSERT INTO users (full_name, username, password_hash, contact_number, role, "
                + "department, unit_name, assigned_zone, station_name, station_code, jurisdiction_area, "
                + "available, relationship_to_missing_person, approved) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getUsername());
           
            stmt.setString(3, plainPassword);
            stmt.setString(4, user.getContactNumber());
            stmt.setString(5, user.getRole().name());

            // Role-specific columns - only the relevant one gets set, rest stay null
            stmt.setNull(6, Types.VARCHAR);
            stmt.setNull(7, Types.VARCHAR);
            stmt.setNull(8, Types.VARCHAR);
            stmt.setNull(9, Types.VARCHAR);
            stmt.setNull(10, Types.VARCHAR);
            stmt.setNull(11, Types.VARCHAR);
            stmt.setNull(12, Types.BOOLEAN);
            stmt.setNull(13, Types.VARCHAR);

            if (user instanceof Admin) {
                stmt.setString(6, ((Admin) user).getDepartment());
            } else if (user instanceof RescueTeam) {
                RescueTeam rt = (RescueTeam) user;
                stmt.setString(7, rt.getUnitName());
                stmt.setString(8, rt.getAssignedZone());
            } else if (user instanceof PoliceStation) {
                PoliceStation ps = (PoliceStation) user;
                stmt.setString(9, ps.getStationName());
                stmt.setString(10, ps.getStationCode());
                stmt.setString(11, ps.getJurisdictionArea());
            } else if (user instanceof Volunteer) {
                stmt.setBoolean(12, ((Volunteer) user).isAvailable());
            } else if (user instanceof Reporter) {
                stmt.setString(13, ((Reporter) user).getRelationshipToMissingPerson());
            }

            stmt.setBoolean(14, user.isApproved());

            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to save user: " + e.getMessage());
        }
    }

    @Override
    public List<User> findPendingAdmins() {
        List<User> pending = new ArrayList<User>();
        String sql = "SELECT * FROM users WHERE role = 'ADMIN' AND approved = FALSE";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                pending.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            System.out.println("Failed to load pending admins: " + e.getMessage());
        }
        return pending;
    }

    @Override
    public void approve(int userId) {
        String sql = "UPDATE users SET approved = TRUE WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to approve user: " + e.getMessage());
        }
    }

    // Reconstructs the correct User subclass based on the role column
    private User mapRowToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String fullName = rs.getString("full_name");
        String username = rs.getString("username");
        String password = rs.getString("password_hash");
        UserRole role = UserRole.valueOf(rs.getString("role"));

        User result;
        switch (role) {
            case ADMIN:
                result = new Admin(id, fullName, username, password, rs.getString("department"));
                break;
            case RESCUE_TEAM:
                result = new RescueTeam(id, fullName, username, password,
                        rs.getString("unit_name"), rs.getString("assigned_zone"));
                break;
            case POLICE_STATION:
                result = new PoliceStation(id, fullName, username, password,
                        rs.getString("station_name"), rs.getString("station_code"), rs.getString("jurisdiction_area"));
                break;
            case VOLUNTEER:
                result = new Volunteer(id, fullName, username, password, rs.getBoolean("available"));
                break;
            case REPORTER:
                result = new Reporter(id, fullName, username, password, rs.getString("relationship_to_missing_person"));
                break;
            default:
                throw new IllegalStateException("Unknown role: " + role);
        }

        result.setContactNumber(rs.getString("contact_number"));
        result.setApproved(rs.getBoolean("approved"));
        return result;
    }
}