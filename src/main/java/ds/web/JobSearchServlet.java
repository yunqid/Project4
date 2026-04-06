// Author: Yunqi Dong, Andrew ID: yunqid
package ds.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoClient;
import org.bson.Document;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Mobile client endpoint: searches Arbeitnow listings by keyword (server-side filter).
 * GET /api/jobs?keyword=...&device=... (device optional, for logging)
 */
@WebServlet(name = "JobSearchServlet", urlPatterns = "/api/jobs")
public class JobSearchServlet extends HttpServlet {

    private static final int MAX_KEYWORD_LEN = 80;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");

        String keyword = req.getParameter("keyword");
        String device = req.getParameter("device");
        String userAgent = req.getHeader("User-Agent");

        if (keyword == null || keyword.isBlank()) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "keyword is required");
            return;
        }
        keyword = keyword.trim();
        if (keyword.length() > MAX_KEYWORD_LEN) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "keyword too long");
            return;
        }

        MongoClient mongo = MongoHolder.getClient();
        if (mongo == null) {
            writeJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "database unavailable");
            return;
        }

        ArbeitnowFetchResult fetch = ArbeitnowClient.fetchJobs();
        int thirdPartyStatus = fetch.getHttpStatus();
        long thirdPartyLatency = fetch.getLatencyMs();
        String thirdPartyUrl = ArbeitnowClient.JOB_BOARD_URL;

        if (fetch.getErrorMessage() != null) {
            logAndReplyFailure(req, resp, mongo, keyword, device, userAgent, thirdPartyUrl,
                    thirdPartyStatus, thirdPartyLatency, HttpServletResponse.SC_BAD_GATEWAY,
                    "third party unreachable");
            return;
        }

        if (!fetch.isSuccess()) {
            logAndReplyFailure(req, resp, mongo, keyword, device, userAgent, thirdPartyUrl,
                    thirdPartyStatus, thirdPartyLatency, HttpServletResponse.SC_BAD_GATEWAY,
                    "third party error");
            return;
        }

        List<JsonObject> jobs;
        try {
            jobs = JobSearchService.filterByKeyword(fetch.getResponseBody(), keyword);
        } catch (Exception e) {
            logAndReplyFailure(req, resp, mongo, keyword, device, userAgent, thirdPartyUrl,
                    thirdPartyStatus, thirdPartyLatency, HttpServletResponse.SC_BAD_GATEWAY,
                    "invalid third party data");
            return;
        }

        JsonObject body = new JsonObject();
        JsonArray arr = new JsonArray();
        for (JsonObject j : jobs) {
            arr.add(j);
        }
        body.add("jobs", arr);

        int replyStatus = HttpServletResponse.SC_OK;
        persistLog(req, mongo, keyword, device, userAgent, thirdPartyUrl, thirdPartyStatus,
                thirdPartyLatency, jobs.size(), replyStatus);

        resp.setStatus(replyStatus);
        resp.getWriter().write(body.toString());
    }

    private void logAndReplyFailure(HttpServletRequest req, HttpServletResponse resp, MongoClient mongo,
                                    String keyword, String device, String userAgent, String url,
                                    int tpStatus, long tpLatency, int replyStatus, String message)
            throws IOException {
        persistLog(req, mongo, keyword, device, userAgent, url, tpStatus, tpLatency, 0, replyStatus);
        writeJsonError(resp, replyStatus, message);
    }

    private void persistLog(HttpServletRequest req, MongoClient mongo, String keyword, String device,
                            String userAgent, String thirdPartyUrl, int thirdPartyStatus,
                            long thirdPartyLatencyMs, int jobsReturned, int replyHttpStatus) {
        try {
            String ip = clientIp(req);
            Document doc = AccessLogRepository.newAccessDocument(
                    new Date(),
                    ip,
                    userAgent,
                    device != null ? device : "",
                    keyword,
                    thirdPartyUrl,
                    thirdPartyStatus,
                    thirdPartyLatencyMs,
                    jobsReturned,
                    replyHttpStatus
            );
            new AccessLogRepository(mongo).insertAccessLog(doc);
        } catch (Exception ignored) {
            // logging must not break API response
        }
    }

    private static String clientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return req.getRemoteAddr();
    }

    private static void writeJsonError(HttpServletResponse resp, int code, String message) throws IOException {
        resp.setStatus(code);
        JsonObject err = new JsonObject();
        err.addProperty("error", message);
        resp.getWriter().write(err.toString());
    }
}
