import java.io.File;
import java.sql.*;

/**
 * SQLCipher demo database setup utility
 *
 * Responsible for creating and initializing SQLCipher demo database (encrypted SQLite)
 *
 * SQLCipher provides SQLite encryption:
 * - Database file encryption
 * - JDBC connection with password support
 * - Transparent encryption, SQL queries remain unchanged
 *
 * @author Database Password Manager
 * @version 4.3
 */
public class SqlCipherDatabaseSetup {

    private static final String DB_PATH = "target/dbpwd-demo.db";
    private static final String DB_PASSWORD = "test123"; // SQLCipher database password

    /**
     * Initialize SQLCipher demo database
     */
    public static void setupDemoDatabase() {
        try {
            // Ensure directory exists
            File dbFile = new File(DB_PATH);
            dbFile.getParentFile().mkdirs();

            // Delete old database file (if exists)
            if (dbFile.exists()) {
                if (!dbFile.delete()) {
                    System.err.println("Cannot delete old database file: " + DB_PATH);
                }
            }

            // Connect to SQLCipher database
            // Note: SQLCipher requires specifying password in URL: jdbc:sqlite:path?cipher=sqlcipher&key=password
            String url = "jdbc:sqlite:" + DB_PATH + "?cipher=sqlcipher&key=" + DB_PASSWORD;
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(url);

            System.out.println("SQLCipher demo database connected successfully: " + DB_PATH);

            // Execute initialization script
            initDatabase(conn);

            conn.close();

            System.out.println("SQLCipher demo database initialized successfully");

        } catch (Exception e) {
            System.err.println("Failed to initialize SQLCipher demo database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Execute initialization script
     */
    private static void initDatabase(Connection conn) throws Exception {
        Statement stmt = conn.createStatement();

        // Create users table
        stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                      "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                      "name TEXT NOT NULL, " +
                      "email TEXT NOT NULL UNIQUE, " +
                      "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                      "updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");

        // Create orders table
        stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                      "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                      "user_id INTEGER NOT NULL, " +
                      "product_name TEXT NOT NULL, " +
                      "amount REAL NOT NULL, " +
                      "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                      "FOREIGN KEY (user_id) REFERENCES users(id))");

        // Insert demo data
        stmt.execute("INSERT OR IGNORE INTO users (name, email) VALUES " +
                      "('Alice', 'alice@example.com'), " +
                      "('Bob', 'bob@example.com'), " +
                      "('Charlie', 'charlie@example.com')");

        stmt.execute("INSERT OR IGNORE INTO orders (user_id, product_name, amount) VALUES " +
                      "(1, 'Product A', 100.00), " +
                      "(2, 'Product B', 200.00), " +
                      "(1, 'Product C', 150.00)");

        stmt.close();
    }

    /**
     * Get SQLCipher demo database connection
     *
     * @return Database connection
     */
    public static Connection getDemoConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + DB_PATH + "?cipher=sqlcipher&key=" + DB_PASSWORD;
            return DriverManager.getConnection(url);
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLCipher JDBC driver not found", e);
        }
    }

    /**
     * Get demo database path
     *
     * @return Database file path
     */
    public static String getDatabasePath() {
        return DB_PATH;
    }

    /**
     * Get database password
     *
     * @return Database password
     */
    public static String getDatabasePassword() {
        return DB_PASSWORD;
    }

    /**
     * Cleanup demo database
     */
    public static void cleanupDatabase() {
        File dbFile = new File(DB_PATH);
        if (dbFile.exists()) {
            if (dbFile.delete()) {
                System.out.println("Demo database deleted: " + DB_PATH);
            } else {
                System.err.println("Failed to delete demo database: " + DB_PATH);
            }
        }
    }

    /**
     * Main method for manual database initialization
     */
    public static void main(String[] args) {
        System.out.println("=== SQLCipher Demo Database Initialization ===");
        setupDemoDatabase();
        System.out.println("\nDemo database path: " + DB_PATH);
        System.out.println("Database password: " + DB_PASSWORD);
        System.out.println("JDBC URL: jdbc:sqlite:" + DB_PATH + "?cipher=sqlcipher&key=" + DB_PASSWORD);

        // Test connection and query data
        try {
            Connection conn = getDemoConnection();
            System.out.println("\nTest connection successful!");

            // Query data
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users");
            if (rs.next()) {
                System.out.println("User count: " + rs.getInt("count"));
            }
            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            System.err.println("Test connection failed: " + e.getMessage());
        }
    }
}
