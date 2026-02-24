package com.dbpwd.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 密码文件数据模型
 *
 * 对应完整的JSON文件结构：
 * {
 *   "version": "1.0",
 *   "lastModified": "2026-02-08T10:30:45Z",
 *   "databases": [...]
 * }
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasswordFile {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final String DEFAULT_VERSION = "1.0";

    private String version;
    private String lastModified;
    private List<DatabaseEntry> databases = new ArrayList<>();

    /**
     * 默认构造函数
     */
    public PasswordFile() {
        this.version = DEFAULT_VERSION;
        this.lastModified = ISO_FORMATTER.format(Instant.now());
    }

    /**
     * 从Map构建
     */
    @SuppressWarnings("unchecked")
    public static PasswordFile fromMap(Map<String, Object> map) {
        PasswordFile file = new PasswordFile();

        file.setVersion((String) map.get("version"));
        file.setLastModified((String) map.get("lastModified"));

        Object databasesObj = map.get("databases");
        if (databasesObj instanceof List) {
            List<Map<String, Object>> dbList = (List<Map<String, Object>>) databasesObj;
            for (Map<String, Object> dbMap : dbList) {
                file.getDatabases().add(DatabaseEntry.fromMap(dbMap));
            }
        }

        return file;
    }

    /**
     * 转换为Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        if (version != null) map.put("version", version);
        if (lastModified != null) map.put("lastModified", lastModified);

        List<Map<String, Object>> dbList = new ArrayList<>();
        for (DatabaseEntry entry : databases) {
            dbList.add(entry.toMap());
        }
        map.put("databases", dbList);

        return map;
    }

    /**
     * 根据ID查找数据库条目
     * 支持 placeholder 格式：@db_name@ 会被转换为 db_name
     */
    public DatabaseEntry getById(String id) {
        if (id == null) {
            return null;
        }

        String lookupId = stripPlaceholder(id);

        for (DatabaseEntry entry : databases) {
            if (lookupId.equals(entry.getId())) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 去除 placeholder 格式的 @ 符号
     * @db_name@ -> db_name
     */
    private String stripPlaceholder(String id) {
        if (id == null) {
            return null;
        }
        if (id.startsWith("@") && id.endsWith("@") && id.length() > 2) {
            return id.substring(1, id.length() - 1);
        }
        return id;
    }

    /**
     * 根据URL查找数据库条目
     */
    public DatabaseEntry getByUrl(String url) {
        if (url == null) {
            return null;
        }

        for (DatabaseEntry entry : databases) {
            String entryUrl = entry.getUrl();
            if (url.equals(entryUrl)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 根据用户名查找数据库条目
     */
    public List<DatabaseEntry> getByUsername(String username) {
        List<DatabaseEntry> result = new ArrayList<>();

        if (username != null) {
            for (DatabaseEntry entry : databases) {
                if (username.equals(entry.getUsername())) {
                    result.add(entry);
                }
            }
        }

        return result;
    }

    /**
     * 添加数据库条目
     */
    public void addDatabase(DatabaseEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("DatabaseEntry不能为null");
        }

        // 移除同ID的条目
        if (entry.getId() != null) {
            databases.removeIf(e -> entry.getId().equals(e.getId()));
        }

        databases.add(entry);
        touchLastModified();
    }

    /**
     * 更新数据库条目
     */
    public void updateDatabase(DatabaseEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("DatabaseEntry不能为null");
        }

        if (entry.getId() == null) {
            throw new IllegalArgumentException("DatabaseEntry的ID不能为null");
        }

        DatabaseEntry existing = getById(entry.getId());
        if (existing != null) {
            // 更新现有条目的字段
            if (entry.getName() != null) existing.setName(entry.getName());
            if (entry.getType() != null) existing.setType(entry.getType());
            if (entry.getHost() != null) existing.setHost(entry.getHost());
            if (entry.getPort() != null) existing.setPort(entry.getPort());
            if (entry.getDatabase() != null) existing.setDatabase(entry.getDatabase());
            if (entry.getUrl() != null) existing.setUrl(entry.getUrl());
            if (entry.getUsername() != null) existing.setUsername(entry.getUsername());
            if (entry.getPassword() != null) existing.setPassword(entry.getPassword());
            if (entry.getEncrypted() != null) existing.setEncrypted(entry.getEncrypted());
            if (entry.getDescription() != null) existing.setDescription(entry.getDescription());
            if (entry.getDriver() != null) existing.setDriver(entry.getDriver());
            if (entry.getTags() != null && !entry.getTags().isEmpty()) {
                existing.setTags(entry.getTags());
            }

            entry.touchUpdateTime();
        } else {
            addDatabase(entry);
        }

        touchLastModified();
    }

    /**
     * 删除数据库条目
     */
    public boolean removeDatabase(String id) {
        if (id == null) {
            return false;
        }

        boolean removed = databases.removeIf(entry -> id.equals(entry.getId()));
        if (removed) {
            touchLastModified();
        }
        return removed;
    }

    /**
     * 获取所有数据库条目
     */
    public List<DatabaseEntry> getAllDatabases() {
        return new ArrayList<>(databases);
    }

    /**
     * 根据标签查找数据库条目
     */
    public List<DatabaseEntry> getByTag(String tag) {
        List<DatabaseEntry> result = new ArrayList<>();

        if (tag != null) {
            for (DatabaseEntry entry : databases) {
                if (entry.getTags() != null && entry.getTags().contains(tag)) {
                    result.add(entry);
                }
            }
        }

        return result;
    }

    /**
     * 获取数据库条目数量
     */
    public int size() {
        return databases.size();
    }

    /**
     * 检查是否为空
     */
    @JsonIgnore
    public boolean isEmpty() {
        return databases.isEmpty();
    }

    /**
     * 更新文件的最后修改时间
     */
    public void touchLastModified() {
        this.lastModified = ISO_FORMATTER.format(Instant.now());
    }

    /**
     * 清空所有数据库条目
     */
    public void clear() {
        databases.clear();
        touchLastModified();
    }

    // ==================== Getters and Setters ====================

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public List<DatabaseEntry> getDatabases() {
        return databases;
    }

    public void setDatabases(List<DatabaseEntry> databases) {
        this.databases = databases != null ? databases : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "PasswordFile{" +
                "version='" + version + '\'' +
                ", lastModified='" + lastModified + '\'' +
                ", databaseCount=" + databases.size() +
                '}';
    }
}
