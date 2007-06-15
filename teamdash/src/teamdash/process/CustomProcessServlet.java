package teamdash.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

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

        String prefix = getInitParameter("prefix");
        String resName = prefix + "/team/lib/script-v1.xml";
        try {
            URL u = CustomProcessServlet.class.getResource(resName);
            BufferedReader in = new BufferedReader(new InputStreamReader(u
                    .openConnection().getInputStream(), "UTF-8"));

            response.getWriter().println("Prefix is " + prefix);
            response.getWriter().println();

            String line = in.readLine();
            while (line != null && line.indexOf("generation-script") == -1)
                line = in.readLine();
            do {
                response.getWriter().println(line);
                line = in.readLine();
            } while (line != null && line.indexOf("startingJar") == -1);

        } catch (Exception e) {
            response.getWriter().print("Can't find " + resName);
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ServletInputStream in = request.getInputStream();

        try {
            Document settings = XMLUtils.parse(in);
            CustomProcess process = new CustomProcess(settings);
            CustomProcessPublisher.publish(process, response.getOutputStream(),
                    new ClasspathContentProvider(getInitParameter("prefix")));
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }
}
