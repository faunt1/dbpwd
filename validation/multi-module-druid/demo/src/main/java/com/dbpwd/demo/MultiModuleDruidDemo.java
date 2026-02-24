package com.dbpwd.demo;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.druid.DbPwdDruidPasswordCallback;
import com.dbpwd.demo.common.User;
import com.dbpwd.demo.service.UserService;

import java.util.List;

public class MultiModuleDruidDemo {
    
    private static final String DB_URL = "jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_druid_demo_spring;MODE=MySQL";
    private static final String DB_USERNAME = "sa";
    private static final String DB_PASSWORD = "@druid_demo_spring_db@";

    public static void main(String[] args) {
        System.out.println("=== Multi-Module Druid Demo ===");
        
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(DB_URL);
        dataSource.setUsername(DB_USERNAME);
        dataSource.setPassword(DB_PASSWORD);
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setInitialSize(2);
        dataSource.setMaxActive(10);
        
        // Integrate db-pwd: configure PasswordCallback for automatic decryption
        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
        callback.setDataSource(dataSource);
        dataSource.setPasswordCallback(callback);
        
        try {
            UserService userService = new UserService(dataSource);
            List<User> users = userService.findAllUsers();
            
            System.out.println("Found " + users.size() + " users:");
            for (User user : users) {
                System.out.println("  " + user);
            }
            
            System.out.println("Demo completed successfully!");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dataSource.close();
        }
    }
}
