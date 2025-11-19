package HospitalManagementSystem;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * DBConfig centralizes loading of DB credentials.
 * It checks environment variables first (DB_URL, DB_USER, DB_PASS),
 * then tries to load config.properties from the project root.
 * Finally it falls back to sensible defaults used by the app.
 */
public class DBConfig {
    private static final String DEFAULT_URL = "jdbc:mysql://127.0.0.1:3306/hospital?user=root";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "Rvddraje42151@";

    private static String url;
    private static String user;
    private static String pass;

    static {
        // Load from environment
        url = System.getenv("DB_URL");
        user = System.getenv("DB_USER");
        pass = System.getenv("DB_PASS");

        // If any is missing, try properties file
        if (url == null || user == null || pass == null) {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream("config.properties")) {
                props.load(in);
                if (url == null) url = props.getProperty("db.url");
                if (user == null) user = props.getProperty("db.user");
                if (pass == null) pass = props.getProperty("db.pass");
            } catch (IOException ignored) {
                // ignore — we'll use defaults
            }
        }

        if (url == null) url = DEFAULT_URL;
        if (user == null) user = DEFAULT_USER;
        if (pass == null) pass = DEFAULT_PASS;
    }

    public static String getUrl() { return url; }
    public static String getUser() { return user; }
    public static String getPass() { return pass; }
}
