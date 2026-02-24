package com.dbpwd.demo;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.druid.DbPwdDruidPasswordCallback;
import com.dbpwd.demo.common.User;
import com.dbpwd.demo.service.UserService;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class MultiModuleDruidPropsDemo {
    private static final String PROPERTIES_FILE = "db.properties";

    public static void main(String[] args) {
        System.out.println("=== Multi-Module Druid Props Demo ===");
        try {
            Properties props = new Properties();
            try (InputStream is = MultiModuleDruidPropsDemo.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
                if (is == null) throw new RuntimeException("Cannot find: " + PROPERTIES_FILE);
                props.load(is);
            }
            DruidDataSource dataSource = UserService.createDataSourceFromProperties(props);
            
            // Integrate db-pwd: configure PasswordCallback for automatic decryption
            DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
            callback.setDataSource(dataSource);
            dataSource.setPasswordCallback(callback);
            
            UserService userService = new UserService(dataSource);
            List<User> users = userService.findAllUsers();
            System.out.println("Found " + users.size() + " users:");
            for (User user : users) System.out.println("  " + user);
            System.out.println("Demo completed successfully!");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
