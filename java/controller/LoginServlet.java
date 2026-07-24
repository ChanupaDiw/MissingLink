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
import java.util.Map;
import java.util.HashMap;
import util.GsonUtil;

// Handles requests to /api/login
@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    private UserDAO userDAO;
    private Gson gson = GsonUtil.getGson();
    @Override
    public void init() throws ServletException {
        Connection connection = DatabaseConnection.getConnection();
        userDAO = new UserDAOImpl(connection);
    }

    // POST /api/login  
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String body = readRequestBody(request);
        Map<String, Object> data = gson.fromJson(body, HashMap.class);

        String username = (String) data.get("username");
        String password = (String) data.get("password");

        User user = userDAO.findByUsername(username);

        if (user == null || !user.authenticate(password)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid username or password\"}");
            return;
        }

        if (!user.isApproved()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"success\":false,\"message\":\"Your admin account is pending approval from an existing administrator.\"}");
            return;
        }

        
        
        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("userId", user.getId());
        result.put("fullName", user.getFullName());
        result.put("role", user.getRole().name());
        result.put("permissions", user.getPermissions());

        response.getWriter().write(gson.toJson(result));
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
