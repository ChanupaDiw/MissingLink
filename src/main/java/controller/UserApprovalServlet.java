package controller;

import model.User;
import dao.UserDAO;
import dao.UserDAOImpl;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.BufferedReader;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import util.GsonUtil;

// Handles requests to /api/pending-admins
@WebServlet("/api/pending-admins")
public class UserApprovalServlet extends HttpServlet {

    private UserDAO userDAO;
    private Gson gson = GsonUtil.getGson();;

    @Override
    public void init() throws ServletException {
        Connection connection = DatabaseConnection.getConnection();
        userDAO = new UserDAOImpl(connection);
    }

    // list admin accounts awaiting approval
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        User currentUser = getSessionUser(request);
        if (currentUser == null || !currentUser.hasPermission("MANAGE_USERS")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("[]");
            return;
        }

        List<User> pending = userDAO.findPendingAdmins();
        response.getWriter().write(gson.toJson(pending));
    }

    //  approve that admin account
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        User currentUser = getSessionUser(request);
        if (currentUser == null || !currentUser.hasPermission("MANAGE_USERS")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"success\":false,\"message\":\"You do not have permission to approve accounts\"}");
            return;
        }

        String body = readRequestBody(request);
        Map<String, Object> data = gson.fromJson(body, HashMap.class);
        int userId = ((Double) data.get("userId")).intValue();

        userDAO.approve(userId);

        response.getWriter().write("{\"success\":true}");
    }

    private User getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null ? (User) session.getAttribute("user") : null;
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
