package com.dbpwd.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库条目模型
 *
 * 对应JSON中的单个数据库配置
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class DatabaseEntry {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private String id;
    private String name;
    private String type;
    private String host;
    private Integer port;
    private String database;
    private String url;
    private String username;
    private String password;
    private Boolean encrypted;
    private String createdAt;
    private String updatedAt;
    private String description;
    private List<String> tags = new ArrayList<>();
    private String driver;
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * 默认构造函数
     */
    public DatabaseEntry() {
        Instant now = Instant.now();
        this.createdAt = ISO_FORMATTER.format(now);
        this.updatedAt = this.createdAt;
    }

    /**
     * 最小构造函数（必填字段）
     */
    public DatabaseEntry(String id, String name, String type, String url,
                         String username, String password) {
        this();
        this.id = id;
        this.name = name;
        this.type = type;
        this.url = url;
        this.username = username;
        this.password = password;
        this.encrypted = false; // 默认为明文，需要加密时设为true
        parseUrl(url);
    }

    /**
     * 从Map构建
     */
    @SuppressWarnings("unchecked")
    public static DatabaseEntry fromMap(Map<String, Object> map) {
        DatabaseEntry entry = new DatabaseEntry();

        entry.setId((String) map.get("id"));
        entry.setName((String) map.get("name"));
        entry.setType((String) map.get("type"));
        entry.setHost((String) map.get("host"));
        entry.setPort(getInteger(map, "port"));
        entry.setDatabase((String) map.get("database"));
        entry.setUrl((String) map.get("url"));
        entry.setUsername((String) map.get("username"));
        entry.setPassword((String) map.get("password"));
        entry.setEncrypted(getBoolean(map, "encrypted"));
        entry.setCreatedAt((String) map.get("createdAt"));
        entry.setUpdatedAt((String) map.get("updatedAt"));
        entry.setDescription((String) map.get("description"));
        entry.setDriver((String) map.get("driver"));

        // 处理tags
        Object tagsObj = map.get("tags");
        if (tagsObj instanceof List) {
            entry.setTags((List<String>) tagsObj);
        }

        return entry;
    }

    private static Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private static Boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    /**
     * 转换为Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        if (id != null) map.put("id", id);
        if (name != null) map.put("name", name);
        if (type != null) map.put("type", type);
        if (host != null) map.put("host", host);
        if (port != null) map.put("port", port);
        if (database != null) map.put("database", database);
        if (url != null) map.put("url", url);
        if (username != null) map.put("username", username);
        if (password != null) map.put("password", password);
        if (encrypted != null) map.put("encrypted", encrypted);
        if (createdAt != null) map.put("createdAt", createdAt);
        if (updatedAt != null) map.put("updatedAt", updatedAt);
        if (description != null) map.put("description", description);
        if (driver != null) map.put("driver", driver);
        if (tags != null && !tags.isEmpty()) map.put("tags", tags);

        return map;
    }

    /**
     * 从JDBC URL解析连接信息
     */
    private void parseUrl(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:")) {
            return;
        }

        try {
            // 格式：jdbc:mysql://host:port/database
            int colonPos = jdbcUrl.indexOf(':', 5); // 跳过jdbc:
            int slashPos = jdbcUrl.indexOf("://", colonPos + 1);

            if (slashPos > 0) {
                String dbType = jdbcUrl.substring(colonPos + 1, slashPos);
                setType(dbType);

                int nextSlashPos = jdbcUrl.indexOf('/', slashPos + 3);

                if (nextSlashPos > 0) {
                    String hostPort = jdbcUrl.substring(slashPos + 3, nextSlashPos);
                    int portColonPos = hostPort.indexOf(':');

                    if (portColonPos > 0) {
                        setHost(hostPort.substring(0, portColonPos));
                        setPort(Integer.parseInt(hostPort.substring(portColonPos + 1)));
                    } else {
                        setHost(hostPort);
                    }

                    int queryPos = jdbcUrl.indexOf('?', nextSlashPos + 1);
                    String dbName = queryPos > 0
                        ? jdbcUrl.substring(nextSlashPos + 1, queryPos)
                        : jdbcUrl.substring(nextSlashPos + 1);

                    setDatabase(dbName);
                }
            }
        } catch (Exception e) {
            // 解析失败，不影响其他功能
        }
    }

    /**
     * 更新更新时间
     */
    public void touchUpdateTime() {
        this.updatedAt = ISO_FORMATTER.format(Instant.now());
    }

    /**
     * 获取JDBC URL（如果未设置，从其他字段生成）
     */
    @JsonIgnore
    public String getJdbcUrl() {
        if (url != null && !url.isEmpty()) {
            return url;
        }

        if (type != null && host != null && database != null) {
            int defaultPort = getDefaultPort(type);
            int portValue = (port != null && port > 0) ? port : defaultPort;

            return String.format("jdbc:%s://%s:%d/%s",
                type, host, portValue, database);
        }

        return null;
    }

    /**
     * 获取数据库类型的默认端口
     */
    private int getDefaultPort(String type) {
        if (type == null) {
            return 3306;
        }

        switch (type.toLowerCase()) {
            case "mysql":
                return 3306;
            case "postgresql":
                return 5432;
            case "oracle":
                return 1521;
            case "sqlserver":
                return 1433;
            default:
                return 3306;
        }
    }

    // ==================== Getters and Setters ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    @Override
    public String toString() {
        return "DatabaseEntry{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", url='" + getJdbcUrl() + '\'' +
                ", username='" + username + '\'' +
                ", encrypted=" + encrypted +
                ", tags=" + tags +
                '}';
    }
}
