package com.dbpwd.demo.servlet.annotation;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/info")
public class InfoApiServlet extends HttpServlet {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");

        Map<String, Object> info = new HashMap<>();
        info.put("demo", "Servlet Annotation Demo");
        info.put("integrationMode", "Servlet 3.0+ Annotations");
        info.put("configuration", "Java Annotations (@WebListener, @WebServlet)");
        info.put("description", "No web.xml required, uses annotations for configuration");

        Map<String, String> components = new HashMap<>();
        components.put("initializer", "@WebListener - AnnotationDemoInitializer");
        components.put("productApi", "@WebServlet - ProductApiServlet");
        components.put("infoApi", "@WebServlet - InfoApiServlet");
        info.put("components", components);

        objectMapper.writeValue(resp.getWriter(), info);
    }
}
