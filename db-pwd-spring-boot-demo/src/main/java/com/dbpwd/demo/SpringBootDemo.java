package com.dbpwd.demo;

import com.dbpwd.framework.DataSourceDecorator;
import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.crypto.ENCFormat;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.bind.annotation.GetMapping;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot demo application
 *
 * Demo Database Password Manager in Spring Boot web application
 * Using MySQL database
 *
 * 集成模式: 模式2（配置文件装饰器模式）
 * 解密实现: 框架实现（DataSourceDecorator自动拦截和替换）
 * 占位符: @spring_boot_db@
 *
 * 工作机制:
 * 1. Spring Boot应用启动时加载配置文件
 * 2. DataSourceDecorator拦截DataSource初始化
 * 3. 根据占位符 @spring_boot_db@ 查找 db.pwd 中的密文密码
 * 4. 使用 sm4.key 解密密码
 * 5. 将明文密码注入到DataSource配置中
 * 6. 建立连接池和数据库连接
 * 7. 应用正常使用数据库连接
 *
 * @author Database Password Manager
 * @version 5.1
 */
@SpringBootApplication
public class SpringBootDemo {

    // 占位符标识（用于标识不同的数据库配置）
    private static final String DB_PLACEHOLDER = "spring_boot_db";

    // MySQL database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/dbpwd_demo?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USERNAME = "dbpwd";
    private static final String SM4_KEY = "0123456789abcdef0123456789abcdef";

    static {
        // Test SM4-GCM encryption at class load time
        try {
            String encryptedDbPassword = Sm4GCMCipher.encrypt("dbpwd123", SM4_KEY);
            String formattedEncrypted = ENCFormat.formatSM4GCM(encryptedDbPassword);
            System.out.println("=== Spring Boot Demo Application ===");
            System.out.println("集成模式: 模式2（配置文件装饰器模式）");
            System.out.println("解密实现: 框架实现（DataSourceDecorator自动拦截和替换）");
            System.out.println("占位符(Placeholder): @" + DB_PLACEHOLDER + "@");
            System.out.println("实际数据库: dbpwd_demo");
            System.out.println("Database URL: " + DB_URL);
            System.out.println("Database username: " + DB_USERNAME);
            System.out.println("SM4 key: " + SM4_KEY);
            System.out.println("SM4-GCM Encryption Test:");
            System.out.println("  Encrypted password: " + formattedEncrypted);
            System.out.println();
        } catch (Exception e) {
            System.err.println("SM4 encryption test failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("Starting Spring Boot application...");
        SpringApplication.run(SpringBootDemo.class, args);
        System.out.println("Spring Boot application started successfully!");
        System.out.println("Access http://localhost:8080 for demo");
        System.out.println();

        // Test SM4-GCM decryption after application started
        try {
            String formattedEncrypted = ENCFormat.formatSM4GCM(Sm4GCMCipher.encrypt("dbpwd123", SM4_KEY));
            String algorithm = ENCFormat.extractAlgorithm(formattedEncrypted);
            String cipherText = ENCFormat.extractCipherText(formattedEncrypted);

            // 仅支持SM4-GCM格式，ECB格式抛出异常
            if (!"SM4-GCM".equalsIgnoreCase(algorithm)) {
                throw new IllegalArgumentException(
                    "Unsupported encryption algorithm: " + algorithm + ". Only SM4-GCM is supported for new applications");
            }

            String decryptedPassword = Sm4GCMCipher.decrypt(cipherText, SM4_KEY);

            System.out.println("SM4-GCM Decryption Test:");
            System.out.println("  Algorithm: " + algorithm);
            System.out.println("  Decrypted password: " + decryptedPassword);
            boolean verified = "dbpwd123".equals(decryptedPassword);
            System.out.println("  Verification: " + (verified ? "SUCCESS" : "FAILED"));
            if (!verified) {
                throw new RuntimeException("密码解密验证失败");
            }
            System.out.println();
            System.out.println("=== Demo Completed Successfully ===");
        } catch (Exception e) {
            System.err.println("SM4-GCM decryption test failed: " + e.getMessage());
        }
    }

    /**
     * 创建DataSource Bean
     *
     * 配置文件中的密码使用占位符格式: @spring_boot_db@
     * DataSourceDecorator会自动拦截并解密密码
     */
    @Bean
    public DataSource dataSource() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        // 创建原始数据源，密码使用占位符
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(DB_URL);
        dataSource.setUsername(DB_USERNAME);

        // 使用占位符作为密码（ DataSourceDecorator 将会解密）
        String placeholderPassword = "@" + DB_PLACEHOLDER + "@";
        dataSource.setPassword(placeholderPassword);

        System.out.println("DataSource configured with placeholder password: " + placeholderPassword);
        System.out.println("DataSourceDecorator will decrypt the password when connection is requested");
        System.out.println();

        // 使用DataSourceDecorator包装原始数据源
        // 注意：实际使用中，Spring Boot会自动检测并装饰DataSource
        return dataSource;
    }

    /**
     * DataSource装饰器Bean
     *
     * 这个Bean会替换原始的DataSource，在获取连接时自动解密密码
     */
    @Bean
    public DataSource dataSourceDecorator(DataSource dataSource) throws Exception {
        // 如果已经是装饰器，直接返回
        if (dataSource instanceof DataSourceDecorator) {
            return dataSource;
        }

        // 使用装饰器包装原始数据源
        DataSourceDecorator decorator = new DataSourceDecorator(dataSource, DB_PLACEHOLDER, SM4_KEY);

        System.out.println("DataSourceDecorator created for: " + DB_PLACEHOLDER);
        System.out.println("The placeholder password @" + DB_PLACEHOLDER + "@ will be replaced with decrypted password from db.pwd");
        System.out.println();

        return decorator;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public CommandLineRunner initDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            System.out.println("Initializing demo database...");

            // Create users table
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(255) NOT NULL UNIQUE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Create orders table
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "product_name VARCHAR(200) NOT NULL, " +
                    "amount DECIMAL(10,2) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))");

            // Check if data already exists
            Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            if (userCount == 0) {
                // Insert demo data
                jdbcTemplate.update("INSERT INTO users (name, email) VALUES " +
                        "('Alice', 'alice@example.com'), " +
                        "('Bob', 'bob@example.com'), " +
                        "('Charlie', 'charlie@example.com')");

                jdbcTemplate.update("INSERT INTO orders (user_id, product_name, amount) VALUES " +
                        "(1, 'Product A', 100.00), " +
                        "(2, 'Product B', 200.00), " +
                        "(1, 'Product C', 150.00)");

                System.out.println("Database initialized with 3 users and 3 orders");
            } else {
                System.out.println("Database already contains " + userCount + " users");
            }
        };
    }

    @org.springframework.web.bind.annotation.RestController
    public static class DemoController {

        private final JdbcTemplate jdbcTemplate;

        public DemoController(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @GetMapping("/")
        public Map<String, Object> index() {
            Map<String, Object> result = new HashMap<>();
            result.put("application", "Database Password Manager - Spring Boot Demo");
            result.put("status", "running");
            result.put("database", "MySQL");
            result.put("description", "Demo application with SM4-GCM encrypted database password");
            result.put("encryption", "SM4-GCM (Authenticated Encryption)");
            result.put("integrationMode", "模式2（配置文件装饰器模式）");
            result.put("placeholder", "@spring_boot_db@");
            return result;
        }

        @GetMapping("/health")
        public Map<String, Object> health() {
            try {
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) as count FROM users", Integer.class
                );
                Map<String, Object> result = new HashMap<>();
                result.put("status", "healthy");
                result.put("user_count", count);
                result.put("database", "MySQL - connected");
                return result;
            } catch (Exception e) {
                Map<String, Object> result = new HashMap<>();
                result.put("status", "unhealthy");
                result.put("error", e.getMessage());
                return result;
            }
        }

        @GetMapping("/users")
        public List<Map<String, Object>> getUsers() {
            return jdbcTemplate.queryForList(
                "SELECT id, name, email, " +
                "DATE_FORMAT(created_at, '%Y-%m-%dT%H:%i:%s') as createdAt " +
                "FROM users ORDER BY id"
            );
        }

        @GetMapping("/orders")
        public List<Map<String, Object>> getOrders() {
            return jdbcTemplate.queryForList(
                "SELECT o.id, u.name as userName, o.product_name as productName, " +
                "o.amount, DATE_FORMAT(o.created_at, '%Y-%m-%dT%H:%i:%s') as createdAt " +
                "FROM orders o LEFT JOIN users u ON o.user_id = u.id ORDER BY o.id"
            );
        }

        @GetMapping("/stats")
        public Map<String, Object> getStats() {
            Map<String, Object> result = new HashMap<>();

            try {
                Integer userCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users", Integer.class
                );
                Integer orderCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM orders", Integer.class
                );
                Double totalAmount = jdbcTemplate.queryForObject(
                    "SELECT SUM(amount) FROM orders", Double.class
                );

                result.put("userCount", userCount);
                result.put("orderCount", orderCount);
                result.put("totalAmount", totalAmount != null ? totalAmount : 0.0);
                result.put("status", "success");
            } catch (Exception e) {
                result.put("status", "error");
                result.put("message", e.getMessage());
            }

            return result;
        }
    }
}
