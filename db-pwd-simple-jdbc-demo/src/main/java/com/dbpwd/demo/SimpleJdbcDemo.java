import com.dbpwd.crypto.Sm4Cipher;
import com.dbpwd.crypto.ENCFormat;

import java.sql.*;

/**
 * Simple JDBC demo application
 *
 * Demo Database Password Manager in plain JDBC application
 * Using SQLCipher as demo database
 *
 * Features:
 * 1. Use SM4 encryption for database password
 * 2. Store encrypted password in password file
 * 3. Create SQLCipher datasource and decorate
 * 4. Verify password decryption and database connection flow
 *
 * @author Database Password Manager
 * @version 4.3
 */
public class SimpleJdbcDemo {

    // Demo database configuration
    private static final String DB_PATH = "target/dbpwd-demo.db";
    private static final String DB_PASSWORD = "test123"; // SQLCipher database password
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH + "?cipher=sqlcipher&key=" + DB_PASSWORD;

    // SM4 encryption configuration
    private static final String TEST_SM4_KEY = "0123456789abcdef0123456789abcdef";

    public static void main(String[] args) {
        System.out.println("=== Simple JDBC Demo Application ===");
        System.out.println("Database: SQLCipher (encrypted SQLite)");
        System.out.println("Database path: " + DB_PATH);
        System.out.println("Database password: " + DB_PASSWORD);
        System.out.println("SM4 key: " + TEST_SM4_KEY);
        System.out.println();

        try {
            // Step 1: Encrypt database password
            String encryptedDbPassword = encryptDatabasePassword(DB_PASSWORD, TEST_SM4_KEY);
            System.out.println("Step 1: Encrypt database password");
            System.out.println("  Original password: " + DB_PASSWORD);
            System.out.println("  Encrypted password: " + encryptedDbPassword);
            System.out.println();

            // Step 2: Test direct SQLCipher connection
            System.out.println("Step 2: Test direct connection");
            testDirectConnection(DB_PASSWORD);

            System.out.println();
            System.out.println("=== Demo Completed Successfully ===");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Encrypt database password
     */
    private static String encryptDatabasePassword(String plainPassword, String sm4Key) {
        try {
            String cipherText = Sm4Cipher.encrypt(plainPassword, sm4Key);
            return ENCFormat.formatSM4(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Encrypt database password failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test direct connection to SQLCipher database
     */
    private static void testDirectConnection(String dbPassword) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            // SQLCipher connection (requires cipher and key parameters)
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(DB_URL);

            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users");

            if (rs.next()) {
                int count = rs.getInt("count");
                System.out.println("  User count: " + count);
            }

            System.out.println("  Direct connection successful!");

        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (stmt != null) try { stmt.close(); } catch (Exception ignored) {}
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }
    }
}
