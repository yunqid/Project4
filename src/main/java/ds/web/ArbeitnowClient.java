// Author: Yunqi Dong, Andrew ID: yunqid
package ds.web;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Fetches JSON from the public Arbeitnow Job Board API.
 * Docs: https://documenter.getpostman.com/view/18545278/UVJbJdKh
 */
public final class ArbeitnowClient {

    public static final String JOB_BOARD_URL = "https://www.arbeitnow.com/api/job-board-api";

    private ArbeitnowClient() {
    }

    public static ArbeitnowFetchResult fetchJobs() {
        long start = System.currentTimeMillis();
        HttpURLConnection conn = null;
        try {
            URL url = new URL(JOB_BOARD_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);

            int status = conn.getResponseCode();
            String body;
            InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (stream == null) {
                stream = conn.getInputStream();
            }
            try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                body = sb.toString();
            }
            long latency = System.currentTimeMillis() - start;
            return new ArbeitnowFetchResult(status, latency, body, null);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return new ArbeitnowFetchResult(0, latency, null, e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
