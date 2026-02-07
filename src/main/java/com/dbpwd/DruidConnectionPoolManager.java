package com.dbpwd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Druid多连接池管理器
 * 支持管理多个数据库连接池，包括读写分离、分库分表等场景
 */
public class DruidConnectionPoolManager {

    private static final Logger logger = LoggerFactory.getLogger(DruidConnectionPoolManager.class);

    private Map<String, DruidConnectionPool> dataSources = new HashMap<String, DruidConnectionPool>();
    private Map<String, DataSourceGroup> groups = new HashMap<String, DataSourceGroup>();
    private AtomicInteger connectionCounter = new AtomicInteger(0);

    /**
     * 默认构造函数
     */
    public DruidConnectionPoolManager() {
    }

    /**
     * 添加数据源
     */
    public void addDataSource(String name, String configFilePath) throws Exception {
        addDataSource(name, configFilePath, null);
    }

    /**
     * 添加数据源并指定组
     */
    public void addDataSource(String name, String configFilePath, String groupName) throws Exception {
        if (dataSources.containsKey(name)) {
            throw new IllegalArgumentException("数据源 '" + name + "' 已存在");
        }

        logger.info("初始化数据源: " + name);
        DruidConnectionPool pool = new DruidConnectionPool(configFilePath);
        dataSources.put(name, pool);

        // 如果指定了组，添加到组中
        if (groupName != null && !groupName.isEmpty()) {
            DataSourceGroup group = groups.computeIfAbsent(groupName, new java.util.function.Function<String, DataSourceGroup>() {
                @Override
                public DataSourceGroup apply(String s) {
                    return new DataSourceGroup(s);
                }
            });
            group.addDataSource(name, pool);
        }

        JdbcUrlParser.DatabaseInfo dbInfo = JdbcUrlParser.parse(pool.getDataSource().getUrl());
        logger.info("数据源 '" + name + "'添加成功: " + dbInfo);
        logger.info("  类型: " + dbInfo.getDbType());
        logger.info("  主机: " + dbInfo.getHost());
        logger.info("  端口: " + dbInfo.getPort());
        logger.info("  数据库: " + dbInfo.getDatabase());
    }

    /**
     * 添加数据源（使用Properties）
     */
    public void addDataSource(String name, java.util.Properties props, String groupName) throws Exception {
        if (dataSources.containsKey(name)) {
            throw new IllegalArgumentException("数据源 '" + name + "' 已存在");
        }

        logger.info("初始化数据源: " + name);
        DruidConnectionPool pool = new DruidConnectionPool(props);
        dataSources.put(name, pool);

        // 如果指定了组，添加到组中
        if (groupName != null && !groupName.isEmpty()) {
            DataSourceGroup group = groups.computeIfAbsent(groupName, new java.util.function.Function<String, DataSourceGroup>() {
                @Override
                public DataSourceGroup apply(String s) {
                    return new DataSourceGroup(s);
                }
            });
            group.addDataSource(name, pool);
        }
    }

    /**
     * 批量添加数据源
     */
    public void addDataSources(Map<String, String> dataSourceConfigs) throws Exception {
        for (Map.Entry<String, String> entry : dataSourceConfigs.entrySet()) {
            addDataSource(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 获取连接
     */
    public Connection getConnection(String name) throws SQLException {
        DruidConnectionPool pool = dataSources.get(name);
        if (pool == null) {
            throw new SQLException("数据源 '" + name + "' 不存在");
        }
        return pool.getConnection();
    }

    /**
     * 创建数据源组
     */
    public DataSourceGroup createGroup(String groupName) {
        if (groups.containsKey(groupName)) {
            return groups.get(groupName);
        }
        DataSourceGroup group = new DataSourceGroup(groupName);
        groups.put(groupName, group);
        return group;
    }

    /**
     * 获取数据源组
     */
    public DataSourceGroup getGroup(String groupName) {
        return groups.get(groupName);
    }

    /**
     * 获取主库连接（用于写操作）
     */
    public Connection getPrimaryConnection(String groupName) throws SQLException {
        DataSourceGroup group = groups.get(groupName);
        if (group == null) {
            throw new SQLException("数据源组 '" + groupName + "' 不存在");
        }
        return group.getPrimaryConnection(connectionCounter);
    }

    /**
     * 获取从库连接（用于读操作，自动负载均衡）
     */
    public Connection getSecondaryConnection(String groupName) throws SQLException {
        DataSourceGroup group = groups.get(groupName);
        if (group == null) {
            throw new SQLException("数据源组 '" + groupName + "' 不存在");
        }
        return group.getSecondaryConnection(connectionCounter);
    }

    /**
     * 移除数据源
     */
    public void removeDataSource(String name) {
        DruidConnectionPool pool = dataSources.remove(name);
        if (pool != null) {
            // 从所有组中移除
            for (DataSourceGroup group : groups.values()) {
                group.removeDataSource(name);
            }
            pool.close();
            logger.info("数据源 '" + name + "' 已移除");
        }
    }

    /**
     * 获取数据源列表
     */
    public List<String> getDataSourceNames() {
        return new ArrayList<String>(dataSources.keySet());
    }

    /**
     * 获取数据源池
     */
    public DruidConnectionPool getDataSourcePool(String name) {
        return dataSources.get(name);
    }

    /**
     * 检查数据源是否存在
     */
    public boolean hasDataSource(String name) {
        return dataSources.containsKey(name);
    }

    /**
     * 获取组列表
     */
    public List<String> getGroupNames() {
        return new ArrayList<String>(groups.keySet());
    }

    /**
     * 获取所有连接池的状态
     */
    public Map<String, String> getAllStatus() {
        Map<String, String> status = new HashMap<String, String>();
        for (Map.Entry<String, DruidConnectionPool> entry : dataSources.entrySet()) {
            status.put(entry.getKey(), entry.getValue().getStatusInfo());
        }
        return status;
    }

    /**
     * 检查所有数据源的连接
     */
    public Map<String, Boolean> testAllConnections() {
        Map<String, Boolean> results = new HashMap<String, Boolean>();
        for (Map.Entry<String, DruidConnectionPool> entry : dataSources.entrySet()) {
            results.put(entry.getKey(), entry.getValue().testConnection());
        }
        return results;
    }

    /**
     * 关闭所有连接池
     */
    public void closeAll() {
        logger.info("关闭所有连接池...");
        for (DruidConnectionPool pool : dataSources.values()) {
            try {
                pool.close();
            } catch (Exception e) {
                logger.warn("关闭连接池时出错: " + e.getMessage());
            }
        }
        dataSources.clear();
        groups.clear();
        logger.info("所有连接池已关闭");
    }

    /**
     * 数据源组 - 用于读写分离场景
     */
    public static class DataSourceGroup {
        private String name;
        private String primaryDataSource;
        private List<String> secondaryDataSources = new ArrayList<String>();
        private Map<String, DruidConnectionPool> dataSourceMap = new HashMap<String, DruidConnectionPool>();

        public DataSourceGroup(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        /**
         * 设置主库
         */
        public void setPrimary(String dataSourceName) {
            this.primaryDataSource = dataSourceName;
        }

        /**
         * 添加从库
         */
        public void addSecondary(String dataSourceName) {
            if (!secondaryDataSources.contains(dataSourceName)) {
                secondaryDataSources.add(dataSourceName);
            }
        }

        /**
         * 移除从库
         */
        public void removeSecondary(String dataSourceName) {
            secondaryDataSources.remove(dataSourceName);
        }

        /**
         * 添加数据源到组
         */
        void addDataSource(String name, DruidConnectionPool pool) {
            dataSourceMap.put(name, pool);
        }

        /**
         * 从组中移除数据源
         */
        void removeDataSource(String name) {
            dataSourceMap.remove(name);
            secondaryDataSources.remove(name);
            if (primaryDataSource != null && primaryDataSource.equals(name)) {
                primaryDataSource = null;
            }
        }

        /**
         * 获取主库连接
         */
        public Connection getPrimaryConnection(AtomicInteger counter) throws SQLException {
            if (primaryDataSource == null) {
                throw new SQLException("组 '" + name + "' 没有配置主库");
            }
            DruidConnectionPool pool = dataSourceMap.get(primaryDataSource);
            if (pool == null) {
                throw new SQLException("主库 '" + primaryDataSource + "' 不存在");
            }
            int connId = counter.incrementAndGet();
            Connection conn = pool.getConnection();
            logger.debug("获取主库连接 [" + connId + "] from " + primaryDataSource);
            return conn;
        }

        /**
         * 获取从库连接（随机负载均衡）
         */
        public Connection getSecondaryConnection(AtomicInteger counter) throws SQLException {
            if (secondaryDataSources.isEmpty()) {
                // 如果没有配置从库，回退到主库
                logger.warn("组 '" + name + "' 没有配置从库，使用主库");
                return getPrimaryConnection(counter);
            }

            int index = ThreadLocalRandom.current().nextInt(secondaryDataSources.size());
            String dataSourceName = secondaryDataSources.get(index);

            DruidConnectionPool pool = dataSourceMap.get(dataSourceName);
            if (pool == null) {
                throw new SQLException("从库 '" + dataSourceName + "' 不存在");
            }

            int connId = counter.incrementAndGet();
            Connection conn = pool.getConnection();
            logger.debug("获取从库连接 [" + connId + "] from " + dataSourceName);
            return conn;
        }

        /**
         * 获取所有从库连接（用于批量操作）
         */
        public List<Connection> getAllSecondaryConnections() throws SQLException {
            List<Connection> connections = new ArrayList<Connection>();
            for (String dataSourceName : secondaryDataSources) {
                DruidConnectionPool pool = dataSourceMap.get(dataSourceName);
                if (pool != null) {
                    connections.add(pool.getConnection());
                }
            }
            return connections;
        }

        public String getPrimary() {
            return primaryDataSource;
        }

        public List<String> getSecondaries() {
            return new ArrayList<String>(secondaryDataSources);
        }
    }

    /**
     * 建造者模式
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private DruidConnectionPoolManager manager = new DruidConnectionPoolManager();

        public Builder addDataSource(String name, String configFilePath, String groupName) {
            try {
                manager.addDataSource(name, configFilePath, groupName);
            } catch (Exception e) {
                throw new RuntimeException("添加数据源失败: " + e.getMessage(), e);
            }
            return this;
        }

        public Builder addDataSource(String name, String configFilePath) {
            return addDataSource(name, configFilePath, null);
        }

        public DruidConnectionPoolManager build() {
            return manager;
        }
    }
}