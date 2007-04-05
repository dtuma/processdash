package teamdash.process;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import teamdash.XMLUtils;

public class CustomProcessServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain");
        response.getWriter().print("ACK");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ServletInputStream in = request.getInputStream();

        try {
            Document settings = XMLUtils.parse(in);
            CustomProcess process = new CustomProcess(settings);
            CustomProcessPublisher.publish(process, response.getOutputStream(),
                    new ClasspathContentProvider());
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }
}
