package com.zheqiushui.httpservice.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Qiushui.Zhe
 * @date 2021/12/12 21:35
 */
public class GuardServlet extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    private void logRequestDetail(HttpServletRequest req) {
        String path = req.getRequestURI();
        String addr = req.getRemoteAddr();
        String host = req.getRemoteHost();
        int port = req.getRemotePort();
        String user = req.getRemoteUser();

        StringBuilder builder = new StringBuilder();
        builder.append("Path:").append(path).append("\n");
        builder.append("remote address:").append(addr).append("\n");
        builder.append("remote host:").append(host).append("\n");
        builder.append("remote port:").append(port).append("\n");
        builder.append("remote user:").append(user).append("\n");

        logger.warn("Invalid request come in - {}", builder.toString());
    }
}
