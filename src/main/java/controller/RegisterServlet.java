package controller;

import model.*;
import dao.UserDAO;
import dao.UserDAOImpl;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.BufferedReader;
import java.sql.Connection;
import java.util.Map;
import java.util.HashMap;
import util.GsonUtil;

// Handle requests to /api/register
@WebServlet("/api/register")
public class RegisterServlet extends HttpServlet {

    private UserDAO userDAO;
    private Gson gson = GsonUtil.getGson();

    @Override
    public void init() throws ServletException {
        Connection connection = DatabaseConnection.getConnection();
        userDAO = new UserDAOImpl(connection);
    }

    // POST /api/register
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String body = readRequestBody(request);
        Map<String, Object> data = gson.fromJson(body, HashMap.class);

        String role = (String) data.get("role");
        String fullName = (String) data.get("fullName");
        String username = (String) data.get("username");
        String password = (String) data.get("password");
        String contactNumber = (String) data.get("contactNumber");

        // Contact number is compulsory for every role
        if (contactNumber == null || contactNumber.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Contact number is required\"}");
            return;
        }

        if (userDAO.findByUsername(username) != null) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.getWriter().write("{\"success\":false,\"message\":\"That username is already taken\"}");
            return;
        }

        User newUser = buildUserFromRole(role, fullName, username, password, data);
        newUser.setContactNumber(contactNumber);

        
        // who need an existing admin to approve them first
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        newUser.setApproved(!isAdmin);

        userDAO.save(newUser, password);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("pendingApproval", isAdmin);
        result.put("message", isAdmin
                ? "Account created. An existing administrator must approve it before you can sign in."
                : "Account created successfully. You can now sign in.");

        response.getWriter().write(gson.toJson(result));
    }

    private User buildUserFromRole(String role, String fullName, String username, String password, Map<String, Object> data) {
        switch (role.toUpperCase()) {
            case "ADMIN":
                String department = (String) data.get("department");
                return new Admin(0, fullName, username, password, department);
            case "RESCUE_TEAM":
                String unitName = (String) data.get("unitName");
                String assignedZone = (String) data.get("assignedZone");
                return new RescueTeam(0, fullName, username, password, unitName, assignedZone);
            case "POLICE_STATION":
                String stationName = (String) data.get("stationName");
                String stationCode = (String) data.get("stationCode");
                String jurisdictionArea = (String) data.get("jurisdictionArea");
                return new PoliceStation(0, fullName, username, password, stationName, stationCode, jurisdictionArea);
            case "VOLUNTEER":
                return new Volunteer(0, fullName, username, password, true);
            case "REPORTER":
                String relationship = (String) data.get("relationshipToMissingPerson");
                return new Reporter(0, fullName, username, password, relationship);
            default:
                throw new IllegalArgumentException("Unknown role: " + role);
        }
    }

    private String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        return buffer.toString();
    }
}