package controller;

import model.Person;
import model.LostPerson;
import model.FoundPerson;
import model.Location;
import model.User;
import model.DisasterEvent;
import dao.PersonDAO;
import dao.PersonDAOImpl;
import enums.PersonStatus;
import com.google.gson.Gson;
import util.GsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.BufferedReader;
import java.sql.Connection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

// Handle request to /api/persons
@WebServlet("/api/persons")
public class PersonServlet extends HttpServlet {

    private PersonDAO personDAO;
    private Gson gson = GsonUtil.getGson();
    @Override
    public void init() throws ServletException {
        Connection connection = DatabaseConnection.getConnection();
        personDAO = new PersonDAOImpl(connection);
    }

    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String statusParam = request.getParameter("status");
        String mineParam = request.getParameter("mine");

        List<Person> results;

        if ("true".equalsIgnoreCase(mineParam)) {
            HttpSession session = request.getSession(false);
            User currentUser = session != null ? (User) session.getAttribute("user") : null;

            if (currentUser == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("[]");
                return;
            }

            results = new ArrayList<Person>();
            for (Person person : personDAO.findAll()) {
                if (person.getReportedBy() != null && person.getReportedBy().getId() == currentUser.getId()) {
                    results.add(person);
                }
            }
        } else if (statusParam != null) {
            PersonStatus status = PersonStatus.valueOf(statusParam.toUpperCase());
            results = personDAO.findByStatus(status);
        } else {
            results = personDAO.findAll();
        }

        response.getWriter().write(gson.toJson(results));
    }

    // POST /api/persons -> create a new missing or found person report
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Associate the report with  logged in
        HttpSession session = request.getSession(false);
        User currentUser = session != null ? (User) session.getAttribute("user") : null;

        String body = readRequestBody(request);
        Map<String, Object> data = gson.fromJson(body, HashMap.class);

        String type = (String) data.get("type"); // "LOST" or "FOUND"
        String name = (String) data.get("name");
        int age = ((Double) data.get("age")).intValue();
        String gender = (String) data.get("gender");
        String description = (String) data.get("description");
        double lat = (Double) data.get("latitude");
        double lng = (Double) data.get("longitude");
        String address = (String) data.get("address");
        String photoUrl = (String) data.get("photoUrl");
        String nicNumber = (String) data.get("nicNumber");

        DisasterEvent disasterEvent = null;
        if (data.containsKey("disasterId") && data.get("disasterId") != null) {
            int disasterId = ((Double) data.get("disasterId")).intValue();
            // Passing the disasterId to the object 
            disasterEvent = new DisasterEvent(disasterId, null, null, null, null, 0);
        }

        Location location = new Location(lat, lng, address);

        if ("LOST".equalsIgnoreCase(type)) {
            String circumstances = (String) data.get("circumstances");
            LostPerson person = new LostPerson(0, name, age, gender, description, photoUrl,
                    location, circumstances, disasterEvent, currentUser);
            try {
                person.setNicNumber(nicNumber);
            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
                return;
            }
            personDAO.save(person);
        } else {
            String condition = (String) data.get("condition");
            FoundPerson person = new FoundPerson(0, name, age, gender, description, photoUrl,
                    location, condition, disasterEvent, currentUser);
            try {
                person.setNicNumber(nicNumber);
            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
                return;
            }
            personDAO.save(person);
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
        response.getWriter().write("{\"message\":\"Report saved successfully\"}");
    }

    // PUT /api/persons -> edit an existing submission
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String body = readRequestBody(request);
        Map<String, Object> data = gson.fromJson(body, HashMap.class);

        int personId = ((Double) data.get("personId")).intValue();
        String type = (String) data.get("type");
        String name = (String) data.get("name");
        int age = ((Double) data.get("age")).intValue();
        String gender = (String) data.get("gender");
        String description = (String) data.get("description");
        double lat = (Double) data.get("latitude");
        double lng = (Double) data.get("longitude");
        String address = (String) data.get("address");
        String extraInfo = (String) data.get("extraInfo"); // Condition or Circumstances
        String nicNumber = (String) data.get("nicNumber");

        Location location = new Location(lat, lng, address);

        if ("LOST".equalsIgnoreCase(type)) {
            LostPerson person = new LostPerson(personId, name, age, gender, description, null,
                    location, extraInfo, null, null);
            try {
                person.setNicNumber(nicNumber);
            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
                return;
            }
            personDAO.update(person);
        } else {
            FoundPerson person = new FoundPerson(personId, name, age, gender, description, null,
                    location, extraInfo, null, null);
            try {
                person.setNicNumber(nicNumber);
            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
                return;
            }
            personDAO.update(person);
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"message\":\"Report updated successfully\"}");
    }

    // remove a submission entirely
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String personIdParam = request.getParameter("personId");
        if (personIdParam == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"personId is required\"}");
            return;
        }

        int personId;
        try {
            personId = Integer.parseInt(personIdParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"personId must be a number\"}");
            return;
        }

        personDAO.delete(personId);

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"success\":true,\"message\":\"Submission deleted\"}");
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