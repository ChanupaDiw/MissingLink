package controller;

import model.*;
import dao.*;
import service.MatchingService;
import enums.PersonStatus;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.BufferedReader;
import java.sql.Connection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import util.GsonUtil;

//  /api/matches
@WebServlet("/api/matches")
public class MatchRecordServlet extends HttpServlet {

    private PersonDAO personDAO;
    private MatchRecordDAO matchRecordDAO;
    private MatchingService matchingService;
    private Gson gson = GsonUtil.getGson();

    @Override
    public void init() throws ServletException {
        Connection connection = DatabaseConnection.getConnection();
        personDAO = new PersonDAOImpl(connection);
        matchRecordDAO = new MatchRecordDAOImpl(connection);
        matchingService = new MatchingService();
    }

    // GET /api/matches

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if ("true".equalsIgnoreCase(request.getParameter("history"))) {
            List<MatchRecord> allRecords = matchRecordDAO.findAll();
            response.getWriter().write(gson.toJson(allRecords));
            return;
        }

        List<Person> allPersons = personDAO.findAll();
        List<MatchRecord> candidateMatches = matchingService.findMatches(allPersons);
        List<MatchRecord> alreadyDecided = matchRecordDAO.findAll();

        List<MatchRecord> pending = new ArrayList<MatchRecord>();
        for (MatchRecord candidate : candidateMatches) {
            if (!isAlreadyDecided(candidate, alreadyDecided)) {
                pending.add(candidate);
            }
        }

        response.getWriter().write(gson.toJson(pending));
    }

    // Checks matching reocred dunlicated
    private boolean isAlreadyDecided(MatchRecord candidate, List<MatchRecord> decided) {
        for (MatchRecord existing : decided) {
            boolean samePair = existing.getMissingPerson().getId() == candidate.getMissingPerson().getId()
                    && existing.getFoundPerson().getId() == candidate.getFoundPerson().getId();
            if (samePair) {
                return true;
            }
        }
        return false;
    }

    // POST /api/matches
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User currentUser = session != null ? (User) session.getAttribute("user") : null;



        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"message\":\"Your session has expired. Please log in again.\"}");
            return;
        }

        if (!currentUser.hasPermission("VERIFY_MATCH")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"success\":false,\"message\":\"You do not have permission to verify matches\"}");
            return;
        }

        String body = readRequestBody(request);
        Map<String, Object> data = gson.fromJson(body, HashMap.class);

        String action = (String) data.get("action");
        int missingPersonId = ((Double) data.get("missingPersonId")).intValue();
        int foundPersonId = ((Double) data.get("foundPersonId")).intValue();
        double confidenceScore = (Double) data.get("confidenceScore");

        Person missing = personDAO.findById(missingPersonId);
        Person found = personDAO.findById(foundPersonId);

        if (missing == null || found == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Person record not found\"}");
            return;
        }

        MatchRecord match = new MatchRecord(0, missing, found, confidenceScore);

        if ("confirm".equalsIgnoreCase(action)) {
            match.confirm(currentUser);

            personDAO.updateStatus(missing.getId(), PersonStatus.FOUND);
        } else {
            match.reject(currentUser);
        }

        matchRecordDAO.save(match);

        response.getWriter().write("{\"success\":true}");
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