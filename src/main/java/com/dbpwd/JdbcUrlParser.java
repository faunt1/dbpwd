package com.dbpwd;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JDBC URL解析器
 * 支持解析各种数据库的连接URL
 */
public class JdbcUrlParser {

    private static final Map<String, Integer> DEFAULT_PORT_MAP = new HashMap<String, Integer>();

    static {
        DEFAULT_PORT_MAP.put("MySQL", 3306);
        DEFAULT_PORT_MAP.put("PostgreSQL", 5432);
        DEFAULT_PORT_MAP.put("Oracle", 1521);
        DEFAULT_PORT_MAP.put("SQLServer", 1433);
        DEFAULT_PORT_MAP.put("SQLServerJtds", 1433);
        DEFAULT_PORT_MAP.put("H2", 9092);
        DEFAULT_PORT_MAP.put("DB2", 50000);
        DEFAULT_PORT_MAP.put("MySQLReplication", 3306);
        DEFAULT_PORT_MAP.put("HSQLDB", 9001);
        DEFAULT_PORT_MAP.put("SQLite", 0);
    }

    // 数据库名标识符模式
    private static final Pattern DATABASE_PATTERN_MYSQL = Pattern.compile("/([^/?]+)");
    private static final Pattern DATABASE_PATTERN_ORACLE = Pattern.compile(":([^:?]+)");
    private static final Pattern DATABASE_PATTERN_SQLSERVER = Pattern.compile(";databaseName=([^;]+)|/([^/?]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATABASE_PATTERN_POSTGRESQL = Pattern.compile("/([^/?]+)");

    /**
     * 解析JDBC URL
     */
    public static DatabaseInfo parse(String jdbcUrl) {
        return parse(jdbcUrl, null);
    }

    /**
     * 解析JDBC URL（指定驱动类名）
     */
    public static DatabaseInfo parse(String jdbcUrl, String driverClassName) {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            return null;
        }

        DatabaseInfo dbInfo = new DatabaseInfo();
        dbInfo.setOriginalUrl(jdbcUrl);

        // 检测数据库类型
        String dbType = detectDatabaseType(jdbcUrl, driverClassName);
        dbInfo.setDbType(dbType);

        // 根据数据库类型解析
        if (dbType != null) {
            switch (dbType) {
                case "MySQL":
                    parseMySQlUrl(jdbcUrl, dbInfo);
                    break;
                case "PostgreSQL":
                    parsePostgreSQLUrl(jdbcUrl, dbInfo);
                    break;
                case "Oracle":
                    parseOracleUrl(jdbcUrl, dbInfo);
                    break;
                case "SQLServer":
                case "SQLServerJtds":
                    parseSQLServerUrl(jdbcUrl, dbInfo);
                    break;
                case "H2":
                case "HSQLDB":
                case "SQLite":
                    parseEmbeddedUrl(jdbcUrl, dbInfo);
                    break;
                default:
                    parseGenericUrl(jdbcUrl, dbInfo);
            }
        } else {
            parseGenericUrl(jdbcUrl, dbInfo);
        }

        return dbInfo;
    }

    /**
     * 检测数据库类型
     */
    private static String detectDatabaseType(String jdbcUrl, String driverClassName) {
        String url = jdbcUrl.toLowerCase();

        // 根据URL前缀检测
        if (url.startsWith("jdbc:mysql:")) {
            return "MySQL";
        } else if (url.startsWith("jdbc:mysql:replication:")) {
            return "MySQLReplication";
        } else if (url.startsWith("jdbc:postgresql:")) {
            return "PostgreSQL";
        } else if (url.startsWith("jdbc:oracle:")) {
            return "Oracle";
        } else if (url.startsWith("jdbc:sqlserver:")) {
            return "SQLServer";
        } else if (url.startsWith("jdbc:jtds:sqlserver:")) {
            return "SQLServerJtds";
        } else if (url.startsWith("jdbc:h2:")) {
            return "H2";
        } else if (url.startsWith("jdbc:hsqldb:")) {
            return "HSQLDB";
        } else if (url.startsWith("jdbc:sqlite:")) {
            return "SQLite";
        } else if (url.startsWith("jdbc:db2:")) {
            return "DB2";
        }

        // 根据驱动类名检测
        if (driverClassName != null) {
            String driver = driverClassName.toLowerCase();
            if (driver.contains("mysql")) {
                return "MySQL";
            } else if (driver.contains("postgresql") || driver.contains("postgres")) {
                return "PostgreSQL";
            } else if (driver.contains("oracle")) {
                return "Oracle";
            } else if (driver.contains("sqlserver") || driver.contains("mssql")) {
                return "SQLServer";
            } else if (driver.contains("jtds")) {
                return "SQLServerJtds";
            } else if (driver.contains("h2")) {
                return "H2";
            } else if (driver.contains("hsqldb")) {
                return "HSQLDB";
            } else if (driver.contains("sqlite")) {
                return "SQLite";
            } else if (driver.contains("db2")) {
                return "DB2";
            }
        }

        return null;
    }

    /**
     * 解析MySQL连接URL
     */
    private static void parseMySQlUrl(String jdbcUrl, DatabaseInfo dbInfo) {
        String url = jdbcUrl;
        String prefix = "jdbc:mysql://";

        // 处理复制模式
        if (jdbcUrl.toLowerCase().startsWith("jdbc:mysql:replication://")) {
            prefix = "jdbc:mysql:replication://";
        }

        if (url.startsWith(prefix)) {
            url = url.substring(prefix.length());

            // 分离查询参数
            String[] parts = url.split("\\?", 2);
            String hostPart = parts[0];

            // 解析主机端口
            String[] hostPortPath = hostPart.split("/", 2);
            String hostPort = hostPortPath[0];

            String[] hostPortArray = hostPort.split(":");
            if (hostPortArray.length >= 1) {
                dbInfo.setHost(hostPortArray[0]);
            }
            if (hostPortArray.length >= 2) {
                try {
                    dbInfo.setPort(Integer.parseInt(hostPortArray[1]));
                } catch (NumberFormatException e) {
                    dbInfo.setPort(DEFAULT_PORT_MAP.get("MySQL"));
                }
            } else {
                dbInfo.setPort(DEFAULT_PORT_MAP.get("MySQL"));
            }

            // 解析数据库名
            if (hostPortPath.length >= 2) {
                dbInfo.setDatabase(hostPortPath[1]);
            }
        }
    }

    /**
     * 解析PostgreSQL连接URL
     */
    private static void parsePostgreSQLUrl(String jdbcUrl, DatabaseInfo dbInfo) {
        String url = jdbcUrl.substring("jdbc:postgresql://".length());

        // 分离查询参数
        String[] parts = url.split("\\?", 2);
        String hostPart = parts[0];

        // 解析主机端口
        String[] hostPortPath = hostPart.split("/", 2);
        String hostPort = hostPortPath[0];

        String[] hostPortArray = hostPort.split(":");
        if (hostPortArray.length >= 1) {
            dbInfo.setHost(hostPortArray[0]);
        }
        if (hostPortArray.length >= 2) {
            try {
                dbInfo.setPort(Integer.parseInt(hostPortArray[1]));
            } catch (NumberFormatException e) {
                dbInfo.setPort(DEFAULT_PORT_MAP.get("PostgreSQL"));
            }
        } else {
            dbInfo.setPort(DEFAULT_PORT_MAP.get("PostgreSQL"));
        }

        // 解析数据库名
        if (hostPortPath.length >= 2) {
            dbInfo.setDatabase(hostPortPath[1]);
        }
    }

    /**
     * 解析Oracle连接URL
     */
    private static void parseOracleUrl(String jdbcUrl, DatabaseInfo dbInfo) {
        String url = jdbcUrl.substring("jdbc:oracle:".length());

        // Oracle Thin驱动格式: jdbc:oracle:thin:@//host:port/sid 或 jdbc:oracle:thin:@host:port:sid
        if (url.startsWith("thin:@//")) {
            // 格式: jdbc:oracle:thin:@//host:port/sid
            url = url.substring("thin:@//".length());

            String[] hostPortDb = url.split("/", 2);
            String hostPort = hostPortDb[0];

            String[] hostPortArray = hostPort.split(":");
            if (hostPortArray.length >= 1) {
                dbInfo.setHost(hostPortArray[0]);
            }
            if (hostPortArray.length >= 2) {
                try {
                    dbInfo.setPort(Integer.parseInt(hostPortArray[1]));
                } catch (NumberFormatException e) {
                    dbInfo.setPort(DEFAULT_PORT_MAP.get("Oracle"));
                }
            } else {
                dbInfo.setPort(DEFAULT_PORT_MAP.get("Oracle"));
            }

            if (hostPortDb.length >= 2) {
                dbInfo.setDatabase(hostPortDb[1]);
            }
        } else {
            // 格式: jdbc:oracle:thin:@host:port:sid
            if (url.startsWith("thin:@")) {
                url = url.substring("thin:@".length());

                String[] parts = url.split(":");
                if (parts.length >= 1) {
                    dbInfo.setHost(parts[0]);
                }
                if (parts.length >= 2) {
                    try {
                        dbInfo.setPort(Integer.parseInt(parts[1]));
                    } catch (NumberFormatException e) {
                        dbInfo.setPort(DEFAULT_PORT_MAP.get("Oracle"));
                    }
                } else {
                    dbInfo.setPort(DEFAULT_PORT_MAP.get("Oracle"));
                }
                if (parts.length >= 3) {
                    dbInfo.setDatabase(parts[2]);
                }
            }
        }
    }

    /**
     * 解析SQL Server连接URL
     */
    private static void parseSQLServerUrl(String jdbcUrl, DatabaseInfo dbInfo) {
        // 格式: jdbc:sqlserver://host:port;databaseName=xxx
        String url = jdbcUrl.substring(jdbcUrl.indexOf("://") + 3);

        // 分离主机和参数
        String[] parts = url.split(";", 2);
        String hostPort = parts[0];

        String[] hostPortArray = hostPort.split(":");
        if (hostPortArray.length >= 1) {
            dbInfo.setHost(hostPortArray[0]);
        }
        if (hostPortArray.length >= 2) {
            try {
                dbInfo.setPort(Integer.parseInt(hostPortArray[1]));
            } catch (NumberFormatException e) {
                dbInfo.setPort(DEFAULT_PORT_MAP.get("SQLServer"));
            }
        } else {
            dbInfo.setPort(DEFAULT_PORT_MAP.get("SQLServer"));
        }

        // 解析数据库名
        if (parts.length >= 2) {
            Matcher m = Pattern.compile("databaseName=([^;]+)", Pattern.CASE_INSENSITIVE).matcher(parts[1]);
            if (m.find()) {
                dbInfo.setDatabase(m.group(1));
            }
        }
    }

    /**
     * 解析嵌入式数据库连接URL (H2, HSQLDB, SQLite)
     */
    private static void parseEmbeddedUrl(String jdbcUrl, DatabaseInfo dbInfo) {
        // 嵌入式数据库通常只有文件路径，没有主机和端口
        String url = jdbcUrl.substring(jdbcUrl.indexOf(":") + 1);
        // 跳过协议部分
        int colonIdx = url.indexOf(":");
        if (colonIdx > 0) {
            url = url.substring(colonIdx + 1);
        }

        String[] parts = url.split(";", 2);
        if (parts.length >= 1) {
            dbInfo.setDatabase(parts[0]);
            // 对于文件路径，可以作为host信息存储
            dbInfo.setHost("localhost");
        }
        dbInfo.setPort(0);
    }

    /**
     * 通用URL解析方法
     */
    private static void parseGenericUrl(String jdbcUrl, DatabaseInfo dbInfo) {
        int protocolEnd = jdbcUrl.indexOf("://");
        if (protocolEnd > 0) {
            String url = jdbcUrl.substring(protocolEnd + 3);

            // 分离查询参数
            String[] parts = url.split("\\?", 2);
            String mainPart = parts[0];

            // 尝试找出主机部分
            int firstSlash = mainPart.indexOf('/');
            if (firstSlash > 0) {
                String hostPort = mainPart.substring(0, firstSlash);
                String path = mainPart.substring(firstSlash + 1);

                String[] hostPortArray = hostPort.split(":");
                if (hostPortArray.length >= 1) {
                    dbInfo.setHost(hostPortArray[0]);
                }
                if (hostPortArray.length >= 2) {
                    try {
                        dbInfo.setPort(Integer.parseInt(hostPortArray[1]));
                    } catch (NumberFormatException e) {
                        // 使用默认值
                    }
                }
                dbInfo.setDatabase(path);
            }
        }
    }

    /**
     * 数据库信息类
     */
    public static class DatabaseInfo {
        private String dbType;
        private String host;
        private int port;
        private String database;
        private String originalUrl;

        public DatabaseInfo() {
        }

        public DatabaseInfo(String dbType, String host, int port, String database) {
            this.dbType = dbType;
            this.host = host;
            this.port = port;
            this.database = database;
        }

        public String getDbType() {
            return dbType;
        }

        public void setDbType(String dbType) {
            this.dbType = dbType;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getOriginalUrl() {
            return originalUrl;
        }

        public void setOriginalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
        }

        @Override
        public String toString() {
            return "DatabaseInfo{" +
                    "dbType='" + dbType + '\'' +
                    ", host='" + host + '\'' +
                    ", port=" + port +
                    ", database='" + database + '\'' +
                    '}';
        }
    }
}