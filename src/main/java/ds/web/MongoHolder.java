// Author: Yunqi Dong, Andrew ID: yunqid
package ds.web;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

/**
 * Lazily creates a single MongoClient for MongoDB Atlas.
 * Override with environment variable {@code MONGODB_URI} (recommended for Codespaces / Docker).
 */
public final class MongoHolder {

    private static volatile MongoClient client;
    private static volatile String lastError;

    /** Default Atlas URI; prefer setting MONGODB_URI in the deployment environment. */
    private static final String DEFAULT_MONGODB_URI =
            "mongodb+srv://project4user:123456ab@cluster0.kk4vccf.mongodb.net/?appName=Cluster0";

    private MongoHolder() {
    }

    private static String connectionString() {
        String env = System.getenv("MONGODB_URI");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return DEFAULT_MONGODB_URI;
    }

    public static synchronized MongoClient getClient() {
        if (client != null) {
            return client;
        }
        try {
            client = MongoClients.create(connectionString());
            lastError = null;
            return client;
        } catch (Exception e) {
            lastError = e.getMessage();
            return null;
        }
    }

    public static String getLastError() {
        return lastError;
    }
}
