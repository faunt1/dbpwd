package com.dbpwd;

import com.dbpwd.DruidConnectionPoolManager.DataSourceGroup;
import java.sql.*;

/**
 * 多数据源使用示例
 *
 * 演示场景:
 * 1. 读-写分离（主从数据库）
 * 2. 不同业务使用不同数据库
 * 3. 多数据库类型混合使用
 */
public class MultiDataSourceExample {

    public static void main(String[] args) {
        System.out.println("===============================================");
        System.out.println("   Druid 多数据库连接池使用示例");
        System.out.println("===============================================");
        System.out.println();

        try {
            // 示例1: 基本多数据源配置
            example1_BasicMultiDataSource();

            // 示例2: 读写分离配置
            example2_ReadWriteSeparation();

            // 示例3: 分库分表配置
            example3_ShardingByBusiness();

            System.out.println();
            System.out.println("所有示例执行完成!");

        } catch (Exception e) {
            System.err.println("执行出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 示例1: 基本多数据源
     *
     * 场景: 系统需要连接多个不同数据库
     */
    public static void example1_BasicMultiDataSource() {
        System.out.println("-----------------------------------------------");
        System.out.println("示例1: 基本多数据源配置");
        System.out.println("-----------------------------------------------");
        System.out.println();

        DruidConnectionPoolManager manager = new DruidConnectionPoolManager();

        try {
            // 添加MySQL用户数据库
            System.out.println("添加MySQL用户数据库...");
            // manager.addDataSource("userdb",
            //     "config/druid/MySQL/prod/192_168_1_100_3306_userdb.properties");

            // 添加PostgreSQL分析数据库
            System.out.println("添加PostgreSQL分析数据库...");
            // manager.addDataSource("analytics",
            //     "config/druid/PostgreSQL/prod/192_168_1_50_5432_analytics.properties");

            // 使用用户数据库
            if (manager.hasDataSource("userdb")) {
                try (Connection conn = manager.getConnection("userdb")) {
                    System.out.println("成功连接到用户数据库: " + conn.getMetaData().getURL());
                    // 执行用户相关操作
                }
            }

            // 使用分析数据库
            if (manager.hasDataSource("analytics")) {
                try (Connection conn = manager.getConnection("analytics")) {
                    System.out.println("成功连接到分析数据库: " + conn.getMetaData().getURL());
                    // 执行分析相关操作
                }
            }

            System.out.println();
            System.out.println("示例1执行完成!");

        } catch (Exception e) {
            System.err.println("示例1执行出错: " + e.getMessage());
        } finally {
            manager.closeAll();
        }
    }

    /**
     * 示例2: 读写分离配置
     *
     * 场景: 一个主库处理写操作，多个从库处理读操作
     */
    public static void example2_ReadWriteSeparation() {
        System.out.println();
        System.out.println("-----------------------------------------------");
        System.out.println("示例2: 读写分离配置");
        System.out.println("-----------------------------------------------");
        System.out.println();
        System.out.println("场景: 一个主库处理写操作，多个从库处理读操作");

        DruidConnectionPoolManager manager = new DruidConnectionPoolManager();

        try {
            // 创建订单组
            DataSourceGroup orderGroup = manager.createGroup("order");

            // 添加主库
            System.out.println("添加订单主库 (用于写操作)...");
            // manager.addDataSource("order_primary", "config/druid/MySQL/prod/192_168_1_100_3306_order.properties", "order");
            // orderGroup.setPrimary("order_primary");

            // 添加从库1
            System.out.println("添加订单从库1 (用于读操作)...");
            // manager.addDataSource("order_slave1", "config/druid/MySQL/prod/192_168_1_101_3306_order.properties", "order");
            // orderGroup.addSecondary("order_slave1");

            // 添加从库2
            System.out.println("添加订单从库2 (用于读操作)...");
            // manager.addDataSource("order_slave2", "config/druid/MySQL/prod/192_168_1_102_3306_order.properties", "order");
            // orderGroup.addSecondary("order_slave2");

            // 写操作 - 使用主库
            System.out.println();
            System.out.println("执行写操作 (使用主库)...");
            try (Connection conn = manager.getPrimaryConnection("order")) {
                System.out.println("成功连接到订单主库: " + conn.getMetaData().getURL());
                // 执行INSERT/UPDATE/DELETE操作
                try (Statement stmt = conn.createStatement()) {
                    // 示例: 创建订单
                    // String sql = "INSERT INTO orders (order_id, user_id, amount, status) VALUES (..., ..., ..., ...)";
                    // stmt.executeUpdate(sql);
                    System.out.println("订单已创建");
                }
            }

            // 读操作 - 使用从库（自动负载均衡）
            System.out.println();
            System.out.println("执行读操作 (使用从库)...");
            try (Connection conn = manager.getSecondaryConnection("order")) {
                System.out.println("成功连接到订单从库: " + conn.getMetaData().getURL());
                // 执行SELECT查询
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM orders")) {
                    if (rs.next()) {
                        System.out.println("订单总数: " + rs.getInt(1));
                    }
                }
            }

            System.out.println();
            System.out.println("示例2执行完成!");

        } catch (Exception e) {
            System.err.println("示例2执行出错: " + e.getMessage());
        } finally {
            manager.closeAll();
        }
    }

    /**
     * 示例3: 分库分表配置
     *
     * 场景: 不同业务使用独立的数据库
     */
    public static void example3_ShardingByBusiness() {
        System.out.println();
        System.out.println("-----------------------------------------------");
        System.out.println("示例3: 分库分表配置");
        System.out.println("-----------------------------------------------");
        System.out.println();
        System.out.println("场景: 不同业务使用独立的数据库");

        // 使用建造者模式配置
        DruidConnectionPoolManager manager = DruidConnectionPoolManager.builder()
                // .addDataSource("user_db", "config/druid/MySQL/prod/user/userdb.properties", "user")
                // .addDataSource("order_db", "config/druid/MySQL/prod/order/orderdb.properties", "order")
                // .addDataSource("product_db", "config/druid/MySQL/prod/product/productdb.properties", "product")
                .build();

        try {
            // 用户业务操作
            System.out.println("处理用户业务...");
            if (manager.hasDataSource("user_db")) {
                try (Connection conn = manager.getConnection("user_db")) {
                    System.out.println("成功连接到用户数据库: " + conn.getMetaData().getURL());
                    // 执行用户相关操作
                }
            }

            // 订单业务操作
            System.out.println();
            System.out.println("处理订单业务...");
            if (manager.hasDataSource("order_db")) {
                try (Connection conn = manager.getConnection("order_db")) {
                    System.out.println("成功连接到订单数据库: " + conn.getMetaData().getURL());
                    // 执行订单相关操作
                }
            }

            // 商品业务操作
            System.out.println();
            System.out.println("处理商品业务...");
            if (manager.hasDataSource("product_db")) {
                try (Connection conn = manager.getConnection("product_db")) {
                    System.out.println("成功连接到商品数据库: " + conn.getMetaData().getURL());
                    // 执行商品相关操作
                }
            }

            System.out.println();
            System.out.println("示例3执行完成!");

        } catch (Exception e) {
            System.err.println("示例3执行出错: " + e.getMessage());
        } finally {
            manager.closeAll();
        }
    }

    /**
     * 示例4: 查看连接池状态
     */
    public static void example4_ConnectionPoolStatus() {
        System.out.println();
        System.out.println("-----------------------------------------------");
        System.out.println("示例4: 查看连接池状态");
        System.out.println("-----------------------------------------------");
        System.out.println();

        DruidConnectionPoolManager manager = new DruidConnectionPoolManager();

        try {
            // 添加一些数据源
            // manager.addDataSource("userdb", "config/druid/MySQL/userdb.properties");
            // manager.addDataSource("orderdb", "config/druid/MySQL/orderdb.properties");

            // 获取所有连接池状态
            System.out.println("连接池状态:");
            java.util.Map<String, String> statusMap = manager.getAllStatus();
            for (java.util.Map.Entry<String, String> entry : statusMap.entrySet()) {
                System.out.println();
                System.out.println("数据源: " + entry.getKey());
                System.out.println(entry.getValue());
            }

            // 测试所有连接
            System.out.println();
            System.out.println("连接测试:");
            java.util.Map<String, Boolean> testResults = manager.testAllConnections();
            for (java.util.Map.Entry<String, Boolean> entry : testResults.entrySet()) {
                String status = entry.getValue() ? "正常" : "失败";
                System.out.println(entry.getKey() + ": " + status);
            }

        } catch (Exception e) {
            System.err.println("示例4执行出错: " + e.getMessage());
        } finally {
            manager.closeAll();
        }
    }
}