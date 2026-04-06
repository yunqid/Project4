// Author: Yunqi Dong, Andrew ID: yunqid
package ds.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses Arbeitnow JSON and returns a slim list of jobs matching a keyword.
 */
public final class JobSearchService {

    private JobSearchService() {
    }

    public static List<JsonObject> filterByKeyword(String rawJson, String keyword) {
        List<JsonObject> out = new ArrayList<>();
        if (rawJson == null || rawJson.isBlank()) {
            return out;
        }
        JsonElement root = JsonParser.parseString(rawJson);
        if (!root.isJsonObject() || !root.getAsJsonObject().has("data")) {
            return out;
        }
        JsonElement dataEl = root.getAsJsonObject().get("data");
        if (!dataEl.isJsonArray()) {
            return out;
        }
        JsonArray data = dataEl.getAsJsonArray();
        String needle = keyword.toLowerCase(Locale.ROOT);
        for (JsonElement el : data) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject job = el.getAsJsonObject();
            if (matches(job, needle)) {
                out.add(toSlimJob(job));
            }
        }
        return out;
    }

    private static boolean matches(JsonObject job, String needle) {
        return contains(job, "title", needle)
                || contains(job, "company_name", needle)
                || contains(job, "location", needle)
                || tagsContain(job, needle);
    }

    private static boolean contains(JsonObject job, String field, String needle) {
        if (!job.has(field) || job.get(field).isJsonNull()) {
            return false;
        }
        return job.get(field).getAsString().toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean tagsContain(JsonObject job, String needle) {
        if (!job.has("tags") || !job.get("tags").isJsonArray()) {
            return false;
        }
        for (JsonElement t : job.getAsJsonArray("tags")) {
            if (t.isJsonPrimitive() && t.getAsString().toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    /** Only fields needed by the mobile client. */
    private static JsonObject toSlimJob(JsonObject full) {
        JsonObject slim = new JsonObject();
        copyString(slim, full, "title");
        copyString(slim, full, "company_name");
        copyString(slim, full, "location");
        copyString(slim, full, "url");
        if (full.has("remote") && !full.get("remote").isJsonNull()) {
            slim.add("remote", full.get("remote"));
        }
        return slim;
    }

    private static void copyString(JsonObject to, JsonObject from, String key) {
        if (from.has(key) && !from.get(key).isJsonNull()) {
            to.addProperty(key, from.get(key).getAsString());
        } else {
            to.addProperty(key, "");
        }
    }
}
