package controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.UUID;


@WebServlet("/api/upload")
@MultipartConfig(
        maxFileSize = 2 * 1024 * 1024,      
        maxRequestSize = 3 * 1024 * 1024
)
public class UploadServlet extends HttpServlet {

    private static final String UPLOAD_SUBDIR = "uploads";
    private String uploadDirPath;

    @Override
    public void init() throws ServletException {
        // Resolve to a real path inside the deployed webapp
        String realPath = getServletContext().getRealPath("/" + UPLOAD_SUBDIR);
        File dir = new File(realPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.uploadDirPath = dir.getAbsolutePath();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Part filePart = request.getPart("photo");

        if (filePart == null || filePart.getSize() == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"No file uploaded\"}");
            return;
        }

        String contentType = filePart.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Only image files are allowed\"}");
            return;
        }

        String originalName = getFileName(filePart);
        String extension = "";
        int dotIndex = originalName != null ? originalName.lastIndexOf('.') : -1;
        if (dotIndex > 0) {
            extension = originalName.substring(dotIndex); // includes the dot
        }

        // Unique filename 
        String storedFileName = UUID.randomUUID().toString() + extension;
        File targetFile = new File(uploadDirPath, storedFileName);

        try (InputStream in = filePart.getInputStream();
             OutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        
        String photoUrl = request.getContextPath() + "/" + UPLOAD_SUBDIR + "/" + storedFileName;

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"photoUrl\":\"" + photoUrl + "\"}");
    }

    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition == null) return null;
        for (String token : contentDisposition.split(";")) {
            token = token.trim();
            if (token.startsWith("filename")) {
                return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }
}
