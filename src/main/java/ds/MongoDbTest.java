// Author: Yunqi Dong, Andrew ID: yunqid
// Source note: Written for Project 4 Task 1 — MongoDB Atlas read/write demo
// Driver usage follows MongoDB Java Sync quick-start patterns:
// https://www.mongodb.com/docs/drivers/java/sync/current/quick-start/
package ds;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Scanner;

/**
 * Prompts for a string, stores it in MongoDB Atlas, then reads and prints all stored strings.
 * Set {@code MONGODB_URI} to your Atlas connection string before running (optional; default matches web app).
 */
public class MongoDbTest {

    private static final String DB_NAME = "project4_task1";
    private static final String COLLECTION_NAME = "user_strings";

    private static final String DEFAULT_URI =
            "mongodb+srv://project4user:123456ab@cluster0.kk4vccf.mongodb.net/?appName=Cluster0";

    public static void main(String[] args) {
        String uri = System.getenv("MONGODB_URI");
        if (uri == null || uri.isBlank()) {
            uri = DEFAULT_URI;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter a string to store: ");
        String input = scanner.nextLine();

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(DB_NAME);
            MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

            Document doc = new Document("text", input);
            collection.insertOne(doc);

            System.out.println("Inserted one document. All stored \"text\" values:");
            for (Document d : collection.find()) {
                Object text = d.get("text");
                System.out.println(text != null ? text.toString() : "(null)");
            }
        } catch (Exception e) {
            System.err.println("MongoDB operation failed.");
            e.printStackTrace();
        }
    }
}
