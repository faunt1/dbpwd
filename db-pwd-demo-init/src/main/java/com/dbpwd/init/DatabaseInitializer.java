package com.dbpwd.init;

import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.core.PasswordService;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.model.DatabaseEntry;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Demo数据库初始化工具
 *
 * 统一管理所有Demo应用的数据库初始化：
 * 1. 创建/更新密钥文件 (sm4.key)
 * 2. 创建/更新密码文件 (db.pwd)
 * 3. 创建H2 FILE格式数据库
 * 4. 验证密码解密和数据库连接
 *
 * 用法：
 *   java -jar db-pwd-demo-init.jar           # 初始化所有数据库
 *   java -jar db-pwd-demo-init.jar --single  # 仅创建单个测试库
 *   java -jar db-pwd-demo-init.jar --verify  # 仅验证现有配置
 *   java -jar db-pwd-demo-init.jar --db test_db  # 初始化指定数据库
 *
 * @author Database Password Manager
 * @version 2.0
 */
public class DatabaseInitializer {

    private static final Logger logger = Logger.getLogger(DatabaseInitializer.class.getName());

    private static final String USER_HOME = System.getProperty("user.home");
    private static final String DBPWD_DIR = USER_HOME + "/.dbpwd";
    private static final String KEY_FILE = DBPWD_DIR + "/sm4.key";
    private static final String PWD_FILE = DBPWD_DIR + "/db.pwd";
    private static final String H2_DATA_DIR = DBPWD_DIR + "/h2_data";

    private static final String DEFAULT_USERNAME = "sa";
    private static final String DEFAULT_PASSWORD = "sa";

    private static final DemoDatabase[] DEMO_DATABASES = {
        new DemoDatabase("simple_jdbc_db", "dbpwd_simple_demo", "Simple JDBC Demo Database"),
        new DemoDatabase("druid_demo_db", "dbpwd_druid_demo", "Druid Demo Database"),
        new DemoDatabase("druid_demo_spring_db", "dbpwd_druid_demo_spring", "Druid Spring XML Demo Database"),
        new DemoDatabase("spring_boot_db", "dbpwd_spring_boot_demo", "Spring Boot Demo Database"),
        new DemoDatabase("spring_boot_api_db", "dbpwd_spring_boot_api_demo", "Spring Boot API Demo Database"),
        new DemoDatabase("spring_framework_db", "dbpwd_spring_demo", "Spring Framework Demo Database"),
        new DemoDatabase("spring_demo_xml_db", "dbpwd_spring_xml_demo", "Spring XML Demo Database"),
        new DemoDatabase("mybatis_demo_db", "dbpwd_mybatis_demo", "MyBatis Demo Database"),
        new DemoDatabase("servlet_demo_db", "dbpwd_servlet_demo", "Servlet Demo Database"),
        new DemoDatabase("servlet_demo_annotation_db", "dbpwd_servlet_annotation_demo", "Servlet Annotation Demo Database"),
        new DemoDatabase("struts_demo_db", "dbpwd_struts_demo", "Struts Demo Database")
    };

    private String sm4Key;
    private PasswordFileManager pwdManager;
    private String initSql;

    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            logger.warning("H2 driver not found: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   Database Password Manager - Demo Init");
        System.out.println("=========================================");
        System.out.println();

        DatabaseInitializer initializer = new DatabaseInitializer();

        String mode = args.length > 0 ? args[0] : "--all";
        String targetDb = args.length > 1 ? args[1] : null;

        try {
            switch (mode) {
                case "--single":
                    initializer.initSingle();
                    break;
                case "--verify":
                    initializer.verify();
                    break;
                case "--db":
                    if (targetDb != null) {
                        initializer.initDatabaseById(targetDb);
                    } else {
                        System.err.println("Error: --db requires database id");
                        printUsage();
                        System.exit(1);
                    }
                    break;
                case "--all":
                default:
                    initializer.initAll();
                    break;
            }
        } catch (Exception e) {
            System.err.println("Initialization failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar db-pwd-demo-init.jar           # Initialize all databases");
        System.out.println("  java -jar db-pwd-demo-init.jar --single  # Create single test database");
        System.out.println("  java -jar db-pwd-demo-init.jar --verify  # Verify existing config");
        System.out.println("  java -jar db-pwd-demo-init.jar --db <id> # Initialize specific database");
    }

    /**
     * 初始化所有数据库
     */
    public void initAll() throws Exception {
        System.out.println("Mode: Initialize all demo databases");
        System.out.println();

        ensureDirectories();
        initKeyFile();
        initPwdFile();
        initH2Databases();
        verify();

        System.out.println();
        System.out.println("=========================================");
        System.out.println("   All demo databases initialized!");
        System.out.println("=========================================");
    }

    /**
     * 初始化单个测试数据库
     */
    public void initSingle() throws Exception {
        System.out.println("Mode: Single test database");
        System.out.println();

        ensureDirectories();
        initKeyFile();
        initPwdFile();

        DemoDatabase testDb = new DemoDatabase("test_db", "dbpwd_test", "Test Database");
        initH2Database(testDb);

        System.out.println();
        System.out.println("Test database created: test_db");
        System.out.println("Password: " + DEFAULT_PASSWORD);
    }

    /**
     * 初始化指定数据库
     */
    public void initDatabaseById(String dbId) throws Exception {
        System.out.println("Mode: Initialize specific database: " + dbId);
        System.out.println();

        ensureDirectories();
        initKeyFile();
        initPwdFile();

        DemoDatabase targetDb = null;
        for (DemoDatabase db : DEMO_DATABASES) {
            if (db.placeholder.equals(dbId)) {
                targetDb = db;
                break;
            }
        }

        if (targetDb == null) {
            System.err.println("Database not found: " + dbId);
            System.out.println("Available databases:");
            for (DemoDatabase db : DEMO_DATABASES) {
                System.out.println("  - " + db.placeholder);
            }
            System.exit(1);
        }

        initH2Database(targetDb);
        verifyDatabase(targetDb);

        System.out.println();
        System.out.println("Database initialized: " + dbId);
    }

    /**
     * 验证现有配置
     */
    public void verify() throws Exception {
        System.out.println();
        System.out.println("=========================================");
        System.out.println("   Verification");
        System.out.println("=========================================");
        System.out.println();

        loadKeyAndPwdFile();

        int successCount = 0;
        int failCount = 0;
        List<String> failedDbs = new ArrayList<>();

        for (DemoDatabase db : DEMO_DATABASES) {
            System.out.print("Verifying: " + db.placeholder + " ... ");
            try {
                if (verifyDatabase(db)) {
                    System.out.println("OK (password=" + DEFAULT_PASSWORD + ")");
                    successCount++;
                } else {
                    System.out.println("SKIPPED (database not found)");
                }
            } catch (Exception e) {
                System.out.println("FAILED: " + e.getMessage());
                failCount++;
                failedDbs.add(db.placeholder);
            }
        }

        System.out.println();
        System.out.println("Verification Summary:");
        System.out.println("  Success: " + successCount);
        System.out.println("  Failed: " + failCount);

        if (!failedDbs.isEmpty()) {
            System.out.println("  Failed databases: " + String.join(", ", failedDbs));
        }
    }

    /**
     * 确保目录存在
     */
    private void ensureDirectories() {
        File dbpwdDir = new File(DBPWD_DIR);
        File h2DataDir = new File(H2_DATA_DIR);

        if (!dbpwdDir.exists() && !dbpwdDir.mkdirs()) {
            throw new RuntimeException("Failed to create directory: " + DBPWD_DIR);
        }

        if (!h2DataDir.exists() && !h2DataDir.mkdirs()) {
            throw new RuntimeException("Failed to create directory: " + H2_DATA_DIR);
        }

        loadInitSql();

        System.out.println("Config directory: " + DBPWD_DIR);
        System.out.println("H2 data directory: " + H2_DATA_DIR);
        System.out.println();
    }

    /**
     * 加载初始化 SQL 脚本
     */
    private void loadInitSql() {
        try {
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("h2_init.sql");
            if (is != null) {
                java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
                initSql = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
                System.out.println("Init SQL loaded from h2_init.sql");
            } else {
                throw new RuntimeException("Cannot find h2_init.sql in classpath");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load h2_init.sql: " + e.getMessage(), e);
        }
    }

    /**
     * 初始化密钥文件
     */
    private void initKeyFile() throws Exception {
        File keyFile = new File(KEY_FILE);

        if (keyFile.exists()) {
            System.out.println("Loading existing key file: " + KEY_FILE);
            sm4Key = new Sm4KeyLoader(KEY_FILE).loadKey();
        } else {
            System.out.println("Generating new key file: " + KEY_FILE);
            sm4Key = Sm4KeyLoader.generateKey(KEY_FILE, false);
        }
        System.out.println();
    }

    /**
     * 初始化密码文件
     */
    private void initPwdFile() throws Exception {
        pwdManager = new PasswordFileManager(PWD_FILE);
        pwdManager.load();

        System.out.println("Password file: " + PWD_FILE);
        System.out.println("Existing entries: " + pwdManager.size());
        System.out.println();

        int addedCount = 0;
        int updatedCount = 0;

        for (DemoDatabase db : DEMO_DATABASES) {
            String encryptedPassword = encryptPassword(DEFAULT_PASSWORD);
            String dbUrl = "jdbc:h2:file:" + H2_DATA_DIR + "/" + db.dbName + ";MODE=MySQL";

            DatabaseEntry existing = pwdManager.getById(db.placeholder);
            if (existing == null) {
                DatabaseEntry entry = new DatabaseEntry(
                    db.placeholder,
                    db.name,
                    "h2",
                    dbUrl,
                    DEFAULT_USERNAME,
                    encryptedPassword
                );
                entry.setEncrypted(true);
                pwdManager.addDatabase(entry);
                addedCount++;
                System.out.println("  Added: " + db.placeholder);
            } else {
                existing.setUrl(dbUrl);
                existing.setUsername(DEFAULT_USERNAME);
                existing.setPassword(encryptedPassword);
                existing.setEncrypted(true);
                existing.touchUpdateTime();
                pwdManager.updateDatabase(existing);
                updatedCount++;
                System.out.println("  Updated: " + db.placeholder);
            }
        }

        pwdManager.save();
        System.out.println();
        System.out.println("Password file updated:");
        System.out.println("  Added: " + addedCount);
        System.out.println("  Updated: " + updatedCount);
        System.out.println();
    }

    /**
     * 初始化所有H2数据库
     * 注意：每次初始化会删除现有数据库并重新创建，确保数据不重复
     */
    private void initH2Databases() throws Exception {
        System.out.println("=========================================");
        System.out.println("   Creating H2 Databases");
        System.out.println("   (Existing databases will be deleted)");
        System.out.println("=========================================");
        System.out.println();

        int createdCount = 0;

        for (DemoDatabase db : DEMO_DATABASES) {
            // 删除已存在的数据库文件，确保干净初始化
            deleteDatabaseFiles(db.dbName);
            
            initH2Database(db);
            createdCount++;
            System.out.println("  Created: " + db.placeholder);
        }

        System.out.println();
        System.out.println("H2 Databases initialized: " + createdCount);
    }

    /**
     * 删除H2数据库文件
     */
    private void deleteDatabaseFiles(String dbName) {
        File h2DataDir = new File(H2_DATA_DIR);
        
        // H2 file database creates .mv.db and .trace.db files
        File[] filesToDelete = {
            new File(h2DataDir, dbName + ".mv.db"),
            new File(h2DataDir, dbName + ".trace.db")
        };
        
        for (File file : filesToDelete) {
            if (file.exists()) {
                if (!file.delete()) {
                    logger.warning("Failed to delete: " + file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 确保表存在
     */
    private boolean ensureTables(DemoDatabase db) throws Exception {
        String url = "jdbc:h2:file:" + H2_DATA_DIR + "/" + db.dbName + ";MODE=MySQL";

        try (Connection conn = DriverManager.getConnection(url, DEFAULT_USERNAME, DEFAULT_PASSWORD);
             Statement stmt = conn.createStatement()) {

            return executeInitSql(conn);
        }
    }

    /**
     * 初始化单个H2数据库
     */
    private void initH2Database(DemoDatabase db) throws Exception {
        String url = "jdbc:h2:file:" + H2_DATA_DIR + "/" + db.dbName + ";MODE=MySQL";

        try (Connection conn = DriverManager.getConnection(url, DEFAULT_USERNAME, DEFAULT_PASSWORD);
             Statement stmt = conn.createStatement()) {

            executeInitSql(conn);
        }

        System.out.println("  Created: " + db.placeholder + " -> " + db.dbName);
    }

    /**
     * 执行初始化 SQL 脚本
     */
    private boolean executeInitSql(Connection conn) throws Exception {
        String[] lines = initSql.split("\n");
        StringBuilder currentStatement = new StringBuilder();
        boolean executedAny = false;

        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            
            currentStatement.append(" ").append(trimmed);
            
            if (trimmed.endsWith(";")) {
                String stmt = currentStatement.toString().trim();
                if (stmt.endsWith(";")) {
                    stmt = stmt.substring(0, stmt.length() - 1);
                }
                
                if (!stmt.isEmpty()) {
                    try {
                        conn.createStatement().execute(stmt);
                        executedAny = true;
                    } catch (Exception e) {
                    }
                }
                currentStatement = new StringBuilder();
            }
        }
        
        String stmt = currentStatement.toString().trim();
        if (!stmt.isEmpty()) {
            try {
                conn.createStatement().execute(stmt);
                executedAny = true;
            } catch (Exception e) {
            }
        }
        
        return executedAny;
    }

    /**
     * 验证单个数据库
     */
    private boolean verifyDatabase(DemoDatabase db) throws Exception {
        File dbFile = new File(H2_DATA_DIR, db.dbName + ".mv.db");
        if (!dbFile.exists()) {
            return false;
        }

        String decryptedPassword = PasswordService.getDatabasePassword(db.placeholder);

        if (!DEFAULT_PASSWORD.equals(decryptedPassword)) {
            throw new Exception("Password mismatch! Expected: " + DEFAULT_PASSWORD + ", Got: " + decryptedPassword);
        }

        String url = "jdbc:h2:file:" + H2_DATA_DIR + "/" + db.dbName + ";MODE=MySQL";

        try (Connection conn = DriverManager.getConnection(url, DEFAULT_USERNAME, decryptedPassword);
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next()) {
                rs.getInt(1);
            }
            rs.close();
        }

        return true;
    }

    /**
     * 加载密钥和密码文件
     */
    private void loadKeyAndPwdFile() throws Exception {
        sm4Key = new Sm4KeyLoader(KEY_FILE).loadKey();
        pwdManager = new PasswordFileManager(PWD_FILE);
        pwdManager.load();
    }

    /**
     * 加密密码
     */
    private String encryptPassword(String plainPassword) throws Exception {
        String cipherText = Sm4GCMCipher.encrypt(plainPassword, sm4Key);
        return ENCFormat.formatSM4GCM(cipherText);
    }

    /**
     * Demo数据库配置
     */
    static class DemoDatabase {
        String placeholder;
        String dbName;
        String name;

        DemoDatabase(String placeholder, String dbName, String name) {
            this.placeholder = placeholder;
            this.dbName = dbName;
            this.name = name;
        }
    }
}
