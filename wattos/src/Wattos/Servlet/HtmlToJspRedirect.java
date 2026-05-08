package Wattos.Servlet;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Permanent (301) redirect from the legacy static .html docs pages to
 * their .jsp replacements. Mapped in web.xml to the specific old URLs;
 * the request URI is rewritten by stripping its trailing ".html" and
 * appending ".jsp" so external bookmarks / references continue to work.
 */
public class HtmlToJspRedirect extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String uri = req.getRequestURI();
        String target = uri.endsWith(".html")
            ? uri.substring(0, uri.length() - ".html".length()) + ".jsp"
            : uri;
        String qs = req.getQueryString();
        if (qs != null && !qs.isEmpty()) {
            target = target + "?" + qs;
        }
        resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        resp.setHeader("Location", target);
    }
}
