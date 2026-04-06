// Author: Yunqi Dong, Andrew ID: yunqid
package ds.web;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Persists mobile→web-service access logs and supports dashboard aggregates.
 * Dashboard traffic must not be logged (callers only use this from {@link JobSearchServlet}).
 */
public class AccessLogRepository {

    public static final String DB_NAME = "project4_task2";
    public static final String COLLECTION = "access_logs";

    private final MongoCollection<Document> collection;

    public AccessLogRepository(MongoClient client) {
        MongoDatabase db = client.getDatabase(DB_NAME);
        this.collection = db.getCollection(COLLECTION);
    }

    public void insertAccessLog(Document doc) {
        collection.insertOne(doc);
    }

    public long countAll() {
        return collection.countDocuments();
    }

    public double averageThirdPartyLatencyMs() {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(Filters.gt("thirdPartyLatencyMs", 0)),
                Aggregates.group(null, Accumulators.avg("avgMs", "$thirdPartyLatencyMs"))
        );
        AggregateIterable<Document> it = collection.aggregate(pipeline);
        Document first = it.first();
        if (first == null) {
            return 0;
        }
        Number avg = first.get("avgMs", Number.class);
        return avg != null ? avg.doubleValue() : 0;
    }

    public List<KeywordCount> topKeywords(int limit) {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(Filters.exists("keyword", true)),
                Aggregates.group("$keyword", Accumulators.sum("count", 1)),
                Aggregates.sort(Sorts.descending("count")),
                Aggregates.limit(limit)
        );
        List<KeywordCount> rows = new ArrayList<>();
        for (Document d : collection.aggregate(pipeline)) {
            String kw = d.getString("_id");
            Number c = d.get("count", Number.class);
            rows.add(new KeywordCount(kw != null ? kw : "", c != null ? c.intValue() : 0));
        }
        return rows;
    }

    public int distinctDeviceCountEstimate() {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.group("$deviceModel"),
                Aggregates.count("n")
        );
        Document doc = collection.aggregate(pipeline).first();
        if (doc == null) {
            return 0;
        }
        Number n = doc.get("n", Number.class);
        return n != null ? n.intValue() : 0;
    }

    public List<AccessLogRow> recentLogs(int limit) {
        List<AccessLogRow> rows = new ArrayList<>();
        for (Document d : collection.find().sort(Sorts.descending("receivedAt")).limit(limit)) {
            rows.add(AccessLogRow.fromDocument(d));
        }
        return rows;
    }

    public static Document newAccessDocument(
            Date receivedAt,
            String clientIp,
            String userAgent,
            String deviceModel,
            String keyword,
            String thirdPartyUrl,
            int thirdPartyStatus,
            long thirdPartyLatencyMs,
            int jobsReturned,
            int replyHttpStatus) {
        return new Document()
                .append("receivedAt", receivedAt)
                .append("clientIp", clientIp)
                .append("userAgent", userAgent != null ? userAgent : "")
                .append("deviceModel", deviceModel != null ? deviceModel : "")
                .append("keyword", keyword)
                .append("thirdPartyUrl", thirdPartyUrl)
                .append("thirdPartyStatus", thirdPartyStatus)
                .append("thirdPartyLatencyMs", thirdPartyLatencyMs)
                .append("jobsReturned", jobsReturned)
                .append("replyHttpStatus", replyHttpStatus);
    }

    public static final class KeywordCount {
        private final String keyword;
        private final int count;

        public KeywordCount(String keyword, int count) {
            this.keyword = keyword;
            this.count = count;
        }

        public String getKeyword() {
            return keyword;
        }

        public int getCount() {
            return count;
        }
    }

    public static final class AccessLogRow {
        private final String receivedAt;
        private final String clientIp;
        private final String deviceModel;
        private final String keyword;
        private final int thirdPartyStatus;
        private final long thirdPartyLatencyMs;
        private final int jobsReturned;
        private final int replyHttpStatus;

        private AccessLogRow(String receivedAt, String clientIp, String deviceModel, String keyword,
                             int thirdPartyStatus, long thirdPartyLatencyMs, int jobsReturned, int replyHttpStatus) {
            this.receivedAt = receivedAt;
            this.clientIp = clientIp;
            this.deviceModel = deviceModel;
            this.keyword = keyword;
            this.thirdPartyStatus = thirdPartyStatus;
            this.thirdPartyLatencyMs = thirdPartyLatencyMs;
            this.jobsReturned = jobsReturned;
            this.replyHttpStatus = replyHttpStatus;
        }

        static AccessLogRow fromDocument(Document d) {
            Date when = d.getDate("receivedAt");
            String ts = when != null ? when.toString() : "";
            return new AccessLogRow(
                    ts,
                    str(d, "clientIp"),
                    str(d, "deviceModel"),
                    str(d, "keyword"),
                    num(d, "thirdPartyStatus"),
                    numLong(d, "thirdPartyLatencyMs"),
                    num(d, "jobsReturned"),
                    num(d, "replyHttpStatus")
            );
        }

        private static String str(Document d, String k) {
            Object v = d.get(k);
            return v != null ? v.toString() : "";
        }

        private static int num(Document d, String k) {
            Object v = d.get(k);
            if (v instanceof Number) {
                return ((Number) v).intValue();
            }
            return 0;
        }

        private static long numLong(Document d, String k) {
            Object v = d.get(k);
            if (v instanceof Number) {
                return ((Number) v).longValue();
            }
            return 0L;
        }

        public String getReceivedAt() {
            return receivedAt;
        }

        public String getClientIp() {
            return clientIp;
        }

        public String getDeviceModel() {
            return deviceModel;
        }

        public String getKeyword() {
            return keyword;
        }

        public int getThirdPartyStatus() {
            return thirdPartyStatus;
        }

        public long getThirdPartyLatencyMs() {
            return thirdPartyLatencyMs;
        }

        public int getJobsReturned() {
            return jobsReturned;
        }

        public int getReplyHttpStatus() {
            return replyHttpStatus;
        }
    }
}
