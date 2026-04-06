// Author: Yunqi Dong, Andrew ID: yunqid
package ds.web;

import com.mongodb.client.MongoClient;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Operations dashboard (browser). Does not write access logs.
 */
@WebServlet(name = "DashboardServlet", urlPatterns = "/dashboard")
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        MongoClient mongo = MongoHolder.getClient();
        if (mongo == null) {
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "MongoDB is not configured (set MONGODB_URI for the container / server).");
            return;
        }

        AccessLogRepository repo = new AccessLogRepository(mongo);
        req.setAttribute("totalRequests", repo.countAll());
        req.setAttribute("avgThirdPartyLatencyMs", repo.averageThirdPartyLatencyMs());
        req.setAttribute("topKeywords", repo.topKeywords(10));
        req.setAttribute("distinctDeviceModels", repo.distinctDeviceCountEstimate());
        req.setAttribute("recentLogs", repo.recentLogs(50));

        req.getRequestDispatcher("/dashboard.jsp").forward(req, resp);
    }
}
