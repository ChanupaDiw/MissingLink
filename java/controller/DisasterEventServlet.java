package controller;

import model.DisasterEvent;
import model.Location;
import dao.DisasterEventDAO;
import dao.DisasterEventDAOImpl;
import enums.DisasterType;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.BufferedReader;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import util.GsonUtil;

//  requests to /api/disasters
@WebServlet("/api/disasters")
public class DisasterEventServlet extends HttpServlet {

    private DisasterEventDAO disasterEventDAO;
    private Gson gson = GsonUtil.getGson();

    @Override
    public void init() throws ServletException {
        Connection connection = DatabaseConnection.getConnection();
        disasterEventDAO = new DisasterEventDAOImpl(connection);
    }

    // GET /api/disasters -> all disaster zones
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        List<DisasterEvent> events = disasterEventDAO.findAll();
        response.getWriter().write(gson.toJson(events));
    }

    // POST /api/disasters  to create new disaster
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String body = readRequestBody(request);
        Map<String, Object> data = gson.fromJson(body, HashMap.class);

        DisasterType type = DisasterType.valueOf(((String) data.get("type")).toUpperCase());
        double lat = (Double) data.get("latitude");
        double lng = (Double) data.get("longitude");
        String address = (String) data.get("address");
        double radiusKm = (Double) data.get("radiusKm");
        String description = (String) data.get("description");

        Location location = new Location(lat, lng, address);
        DisasterEvent event = new DisasterEvent(0, type, description, LocalDate.now(), location, radiusKm);

        disasterEventDAO.save(event);

        response.setStatus(HttpServletResponse.SC_CREATED);
        response.getWriter().write("{\"message\":\"Disaster zone registered\"}");
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
