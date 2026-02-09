import com.dbpwd.crypto.SM4Cipher;
import com.dbpwd.crypto.ENCFormat;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Struts 1.x test application
 *
 * Test Database Password Manager in Struts 1.x web application
 * Using SQLCipher as test database
 *
 * Features:
 * 1. Struts 1.x action pattern for database access
 * 2. SM4 encryption for database password
 * 3. SQLCipher database connection
 * 4. Data retrieval and display
 *
 * @author Database Password Manager
 * @version 4.3
 */
public class StrutsTestApp {

    private static final String DB_PATH = "target/dbpwd-test.db";
    private static final String DB_PASSWORD = "test123"; // SQLCipher database password
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH + "?cipher=sqlcipher&key=" + DB_PASSWORD;
    private static final String TEST_SM4_KEY = "0123456789abcdef0123456789abcdef";

    /**
     * Database configuration utility
     */
    public static class DbConfig {
        public static final String DB_PATH = DB_PATH;
        public static final String DB_PASSWORD = DB_PASSWORD;
        public static final String DB_URL = DB_URL;
        public static final String SM4_KEY = TEST_SM4_KEY;

        /**
         * Get encrypted database password
         */
        public static String getEncryptedPassword() {
            try {
                String cipherText = SM4Cipher.encrypt(DB_PASSWORD, SM4_KEY);
                return ENCFormat.formatSM4(cipherText);
            } catch (Exception e) {
                throw new RuntimeException("Encrypt database password failed: " + e.getMessage(), e);
            }
        }

        /**
         * Get database connection
         */
        public Connection getConnection() throws Exception {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(DB_URL);
        }
    }

    /**
     * Data access utility
     */
    public static class DataAccessUtil {

        /**
         * Query all users
         */
        public static List<Map<String, Object>> queryUsers() throws Exception {
            List<Map<String, Object>> users = new ArrayList<>();
            try (Connection conn = DriverManager.getConnection(DbConfig.DB_URL);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name, email FROM users ORDER BY id")) {
                while (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getInt("id"));
                    user.put("name", rs.getString("name"));
                    user.put("email", rs.getString("email"));
                    users.add(user);
                }
            }
            return users;
        }

        /**
         * Query all orders
         */
        public static List<Map<String, Object>> queryOrders() throws Exception {
            List<Map<String, Object>> orders = new ArrayList<>();
            try (Connection conn = DriverManager.getConnection(DbConfig.DB_URL);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT o.id, u.name as user_name, o.product_name as productName, o.amount " +
                     "FROM orders o LEFT JOIN users u ON o.user_id = u.id ORDER BY o.id")) {
                while (rs.next()) {
                    Map<String, Object> order = new HashMap<>();
                    order.put("id", rs.getInt("id"));
                    order.put("userName", rs.getString("user_name"));
                    order.put("productName", rs.getString("productName"));
                    order.put("amount", rs.getDouble("amount"));
                    orders.add(order);
                }
            }
            return orders;
        }

        /**
         * Get user count
         */
        public static int getUserCount() throws Exception {
            try (Connection conn = DriverManager.getConnection(DbConfig.DB_URL);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users")) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
            return 0;
        }
    }

    /**
     * Base action for database operations
     */
    public static abstract class BaseDatabaseAction extends Action {

        protected Map<String, Object> executeDatabase(ActionMapping mapping,
                                                       ActionForm form,
                                                       HttpServletRequest request,
                                                       HttpServletResponse response)
                throws Exception {
            Map<String, Object> result = new HashMap<>();

            try {
                // Test SM4 encryption (simulated)
                String encryptedPassword = DbConfig.getEncryptedPassword();
                result.put("encryptedPassword", "***"); // Don't expose actual encrypted password

                // Add database info
                result.put("dbPath", DbConfig.DB_PATH);
                result.put("dbConnected", "true");

                return result;

            } catch (Exception e) {
                result.put("error", e.getMessage());
                result.put("dbConnected", "false");
                throw e;
            }
        }
    }

    /**
     * Home action - displays application info
     */
    public static class HomeAction extends BaseDatabaseAction {
        @Override
        public ActionForward execute(ActionMapping mapping,
                                     ActionForm form,
                                     HttpServletRequest request,
                                     HttpServletResponse response)
                throws Exception {

            Map<String, Object> result = new HashMap<>();
            result.put("application", "Database Password Manager - Struts Test");
            result.put("status", "running");
            result.put("database", "SQLCipher");
            result.put("description", "Test application with encrypted database password");

            request.setAttribute("result", result);
            return mapping.findForward("success");
        }
    }

    /**
     * Users action - displays user list
     */
    public static class UsersAction extends BaseDatabaseAction {
        @Override
        public ActionForward execute(ActionMapping mapping,
                                     ActionForm form,
                                     HttpServletRequest request,
                                     HttpServletResponse response)
                throws Exception {

            List<Map<String, Object>> users = DataAccessUtil.queryUsers();
            request.setAttribute("users", users);
            request.setAttribute("userCount", users.size());
            return mapping.findForward("success");
        }
    }

    /**
     * Orders action - displays order list
     */
    public static class OrdersAction extends BaseDatabaseAction {
        @Override
        public ActionForward execute(ActionMapping mapping,
                                     ActionForm form,
                                     HttpServletRequest request,
                                     HttpServletResponse response)
                throws Exception {

            List<Map<String, Object>> orders = DataAccessUtil.queryOrders();
            request.setAttribute("orders", orders);
            return mapping.findForward("success");
        }
    }

    /**
     * Health check action
     */
    public static class HealthCheckAction extends BaseDatabaseAction {
        @Override
        public ActionForward execute(ActionMapping mapping,
                                     ActionForm form,
                                     HttpServletRequest request,
                                     HttpServletResponse response)
                throws Exception {

            Map<String, Object> result = new HashMap<>();
            int userCount = DataAccessUtil.getUserCount();
            result.put("status", "healthy");
            result.put("userCount", userCount);
            result.put("database", "connected");

            request.setAttribute("result", result);
            return mapping.findForward("success");
        }
    }

    /**
     * Main method for standalone testing (without servlet container)
     */
    public static void main(String[] args) {
        System.out.println("=== Struts Test Application (Standalone) ===");
        System.out.println("Database: SQLCipher (encrypted SQLite)");
        System.out.println("Database path: " + DB_PATH);
        System.out.println("Database password: " + DB_PASSWORD);
        System.out.println("SM4 key: " + TEST_SM4_KEY);
        System.out.println();

        try {
            // Test SM4 encryption
            String encryptedDbPassword = SM4Cipher.encrypt(DB_PASSWORD, TEST_SM4_KEY);
            String formattedEncrypted = ENCFormat.formatSM4(encryptedDbPassword);
            System.out.println("SM4 Encryption Test:");
            System.out.println("  Original password: " + DB_PASSWORD);
            System.out.println("  Encrypted password: " + formattedEncrypted);
            System.out.println();

            // Test database access
            System.out.println("Database Access Test:");
            System.out.println("  User count: " + DataAccessUtil.getUserCount());

            List<Map<String, Object>> users = DataAccessUtil.queryUsers();
            System.out.println("  Users:");
            for (Map<String, Object> user : users) {
                System.out.println("    - ID: " + user.get("id") + ", Name: " + user.get("name") +
                        ", Email: " + user.get("email"));
            }

            List<Map<String, Object>> orders = DataAccessUtil.queryOrders();
            System.out.println("  Orders:");
            for (Map<String, Object> order : orders) {
                System.out.println("    - Order ID: " + order.get("id") + ", User: " + order.get("userName") +
                        ", Product: " + order.get("productName") + ", Amount: " + order.get("amount"));
            }

            System.out.println();
            System.out.println("=== All Tests Passed ===");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
