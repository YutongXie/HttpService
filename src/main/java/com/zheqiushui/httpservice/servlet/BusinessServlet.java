package com.zheqiushui.httpservice.servlet;

import com.zheqiushui.httpservice.model.Constants;
import com.zheqiushui.httpservice.util.LimitedSizeInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.event.PrintJobAttributeEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Qiushui.Zhe
 * @date 2021/12/12 21:41
 */
public class BusinessServlet extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            InputStream inputStream = new LimitedSizeInputStream(req.getInputStream(), Constants.REQUEST_BODY_SIZE_LIMIT_BUSINESS);
            byte[] input = IOUtils.toByteArray(inputStream);
            process(input);
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e) {
            logger.error("Invalid request ", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    private void process(byte[] input) {
        //
    }
}
