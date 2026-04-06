// Author: Yunqi Dong, Andrew ID: yunqid
// Source note: Written for Project 4 Task 1 — API feasibility (Arbeitnow Job Board API)
package ds;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Fetches job listings from the Arbeitnow public API and prints fields from the first listing.
 * API docs: https://documenter.getpostman.com/view/18545278/UVJbJdKh
 */
public class ApiTest {

    private static final String API_URL = "https://www.arbeitnow.com/api/job-board-api";

    public static void main(String[] args) {
        try {
            String response = sendGet(API_URL);

            JsonObject root = JsonParser.parseString(response).getAsJsonObject();
            if (!root.has("data") || !root.get("data").isJsonArray()) {
                System.out.println("Unexpected response: missing \"data\" array.");
                return;
            }

            JsonArray jobs = root.getAsJsonArray("data");
            if (jobs.size() == 0) {
                System.out.println("No job listings returned.");
                return;
            }

            JsonObject first = jobs.get(0).getAsJsonObject();

            String title = getString(first, "title");
            String company = getString(first, "company_name");
            String location = getString(first, "location");
            String remote = first.has("remote") && !first.get("remote").isJsonNull()
                    ? String.valueOf(first.get("remote"))
                    : "N/A";
            String url = getString(first, "url");

            System.out.println("API call successful.");
            System.out.println("First job listing (sample fields):");
            System.out.println("  Title: " + title);
            System.out.println("  Company: " + company);
            System.out.println("  Location: " + location);
            System.out.println("  Remote: " + remote);
            System.out.println("  URL: " + url);

        } catch (Exception e) {
            System.out.println("API test failed.");
            e.printStackTrace();
        }
    }

    private static String getString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return "N/A";
        }
        return obj.get(key).getAsString();
    }

    private static String sendGet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("HTTP error code: " + status);
        }

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } finally {
            conn.disconnect();
        }
    }
}
