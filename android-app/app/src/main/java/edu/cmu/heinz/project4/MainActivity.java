// Author: Yunqi Dong, Andrew ID: yunqid
package edu.cmu.heinz.project4;

import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Native client for the Project 4 job search servlet (repeatable searches).
 */
public class MainActivity extends AppCompatActivity {

    private EditText keywordInput;
    private TextView resultsText;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        keywordInput = findViewById(R.id.keyword_input);
        resultsText = findViewById(R.id.results_text);
        Button searchButton = findViewById(R.id.search_button);

        searchButton.setOnClickListener(v -> runSearch());
    }

    private void runSearch() {
        String keyword = keywordInput.getText().toString().trim();
        if (keyword.isEmpty()) {
            Toast.makeText(this, R.string.need_keyword, Toast.LENGTH_SHORT).show();
            return;
        }

        resultsText.setText(R.string.loading);

        io.execute(() -> {
            try {
                String base = BuildConfig.BASE_URL.trim();
                if (!base.endsWith("/")) {
                    base = base + "/";
                }
                String q = java.net.URLEncoder.encode(keyword, StandardCharsets.UTF_8);
                String dev = java.net.URLEncoder.encode(Build.MODEL != null ? Build.MODEL : "unknown",
                        StandardCharsets.UTF_8);
                String url = base + "api/jobs?keyword=" + q + "&device=" + dev;

                String body = httpGet(url);
                JSONObject root = new JSONObject(body);

                if (root.has("error")) {
                    String msg = root.optString("error", "Unknown error");
                    runOnUiThread(() -> resultsText.setText(getString(R.string.server_error, msg)));
                    return;
                }

                JSONArray jobs = root.optJSONArray("jobs");
                if (jobs == null) {
                    runOnUiThread(() -> resultsText.setText(R.string.bad_response));
                    return;
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < jobs.length(); i++) {
                    JSONObject j = jobs.getJSONObject(i);
                    sb.append(j.optString("title")).append('\n')
                            .append(j.optString("company_name")).append(" — ")
                            .append(j.optString("location")).append('\n')
                            .append(j.optString("url")).append("\n\n");
                }
                String out = sb.length() == 0 ? getString(R.string.no_matches) : sb.toString();
                runOnUiThread(() -> resultsText.setText(out));
            } catch (Exception e) {
                runOnUiThread(() ->
                        resultsText.setText(getString(R.string.network_failure, e.getMessage())));
            }
        });
    }

    private static String httpGet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(20_000);
        conn.setReadTimeout(30_000);

        int code = conn.getResponseCode();
        String raw;
        InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
            stream = conn.getInputStream();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            raw = sb.toString();
        } finally {
            conn.disconnect();
        }

        if (code != 200) {
            throw new IllegalStateException("HTTP " + code + ": " + raw);
        }
        return raw;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
}
