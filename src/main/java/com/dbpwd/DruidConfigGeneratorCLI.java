package com.dbpwd;

import java.io.Console;
import java.io.File;
import java.util.Scanner;

/**
 * Druid配置生成器命令行工具
 * 提供交互式命令行界面生成Druid配置文件
 */
public class DruidConfigGeneratorCLI {

    private static final String DEFAULT_ALGORITHM = "SM4";
    private static final String DEFAULT_ENV = "prod";

    public static void main(String[] args) {
        System.out.println("===============================================");
        System.out.println("   Druid 数据库连接池配置生成器");
        System.out.println("===============================================");
        System.out.println();

        try {
            Scanner scanner = new Scanner(System.in);
            DruidConfigGenerator generator = new DruidConfigGenerator();

            // 收集基本信息
            System.out.println("请输入数据库连接信息:");

            System.out.print("驱动类名 (留空自动检测, 如: com.mysql.cj.jdbc.Driver): ");
            String driverClassName = scanner.nextLine().trim();

            System.out.print("JDBC URL (如: jdbc:mysql://localhost:3306/testdb): ");
            String url = scanner.nextLine().trim();

            System.out.print("用户名: ");
            String username = scanner.nextLine().trim();

            String password = "";
            Console console = System.console();
            if (console != null) {
                char[] passwordChars = console.readPassword("密码: ");
                password = new String(passwordChars);
            } else {
                System.out.print("密码: ");
                password = scanner.nextLine().trim();
            }

            System.out.print("加密算法 (SM4/AES, 默认: " + DEFAULT_ALGORITHM + "): ");
            String algorithm = scanner.nextLine().trim();
            if (algorithm.isEmpty()) {
                algorithm = DEFAULT_ALGORITHM;
            }

            System.out.print("加密密钥 (留空自动生成): ");
            String encryptionKey = scanner.nextLine().trim();

            // 设置基本信息
            if (!driverClassName.isEmpty()) {
                generator.setDriverClassName(driverClassName);
            }
            generator.setUrl(url);
            generator.setUsername(username);
            generator.setPassword(password);
            generator.setAlgorithm(algorithm);
            if (!encryptionKey.isEmpty()) {
                generator.setEncryptionKey(encryptionKey);
            }

            // 自动检测配置
            generator.autoDetectDatabase();

            // 收集连接池配置
            System.out.println();
            System.out.println("请输入连接池配置 (留空使用默认值):");

            System.out.print("初始连接数 (默认: 5): ");
            String initialSizeStr = scanner.nextLine();
            if (!initialSizeStr.isEmpty()) {
                generator.setInitialSize(Integer.parseInt(initialSizeStr));
            }

            System.out.print("最小空闲连接 (默认: 5): ");
            String minIdleStr = scanner.nextLine();
            if (!minIdleStr.isEmpty()) {
                generator.setMinIdle(Integer.parseInt(minIdleStr));
            }

            System.out.print("最大活跃连接 (默认: 20): ");
            String maxActiveStr = scanner.nextLine();
            if (!maxActiveStr.isEmpty()) {
                generator.setMaxActive(Integer.parseInt(maxActiveStr));
            }

            System.out.print("最大等待时间毫秒 (默认: 60000): ");
            String maxWaitStr = scanner.nextLine();
            if (!maxWaitStr.isEmpty()) {
                generator.setMaxWait(Long.parseLong(maxWaitStr));
            }

            System.out.print("连接验证SQL (默认: 自动): ");
            String validationQuery = scanner.nextLine();
            if (!validationQuery.isEmpty()) {
                generator.setValidationQuery(validationQuery);
            }

            // 收集输出配置
            System.out.println();
            System.out.println("请输入输出配置:");

            System.out.print("环境 (prod/test/dev, 默认: " + DEFAULT_ENV + "): ");
            String env = scanner.nextLine().trim();
            if (env.isEmpty()) {
                env = DEFAULT_ENV;
            }

            System.out.print("是否使用自动生成的文件名? (y/n, 默认: y): ");
            String autoFileName = scanner.nextLine().trim();
            String outputPath;

            if (autoFileName.isEmpty() || autoFileName.equalsIgnoreCase("y")) {
                // 使用自动生成的文件名
                ConfigFileNameGenerator fileNameGen = new ConfigFileNameGenerator(env);
                String fileName = fileNameGen.generate(url);
                String fullPath = fileNameGen.getFullPath(url);

                System.out.println("自动生成的文件名: " + fileName);
                System.out.println("完整路径: config/druid/" + fullPath);

                System.out.print("输出目录 (默认: config/druid): ");
                String outputDir = scanner.nextLine().trim();
                if (outputDir.isEmpty()) {
                    outputDir = "config/druid/" + fullPath.substring(0, fullPath.lastIndexOf('/'));
                } else {
                    outputDir = outputDir + "/" + fullPath.substring(0, fullPath.lastIndexOf('/'));
                }

                File dir = new File(outputDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                outputPath = outputDir + "/" + fileName;
            } else {
                // 使用自定义文件名
                System.out.print("输出文件路径: ");
                outputPath = scanner.nextLine().trim();
            }

            System.out.print("是否加密密码? (y/n, 默认: y): ");
            String encryptChoice = scanner.nextLine().trim();
            boolean doEncrypt = encryptChoice.isEmpty() || encryptChoice.equalsIgnoreCase("y");

            // 生成配置
            System.out.println();
            System.out.println("正在生成配置...");
            generator.createConfigDirectory(outputPath);

            if (doEncrypt) {
                generator.encryptPassword();
                System.out.println("密码已使用 " + algorithm + " 算法加密");
                System.out.println("加密密钥: " + generator.getEncryptionKey());
            }

            generator.saveToFile(outputPath);

            System.out.println();
            System.out.println("配置文件已生成: " + outputPath);
            System.out.println();
            System.out.println(generator.getConfigSummary());

            // 询问是否上传到配置中心
            System.out.print("是否上传到配置中心? (y/n, 默认: n): ");
            String uploadChoice = scanner.nextLine().trim();
            if (uploadChoice.equalsIgnoreCase("y")) {
                System.out.print("配置中心URL: ");
                String configCenterUrl = scanner.nextLine().trim();

                System.out.print("API Key: ");
                console = System.console();
                String apiKey = "";
                if (console != null) {
                    char[] apiKeyChars = console.readPassword("API Key: ");
                    apiKey = new String(apiKeyChars);
                } else {
                    apiKey = scanner.nextLine().trim();
                }

                if (!configCenterUrl.isEmpty() && !apiKey.isEmpty()) {
                    ConfigFileNameGenerator fileNameGen = new ConfigFileNameGenerator(env);
                    String fileName = fileNameGen.generate(url);
                    String directory = fileNameGen.getFullPath(url).substring(0, fileNameGen.getFullPath(url).lastIndexOf('/'));

                    DruidConfigUploader uploader = new DruidConfigUploader(configCenterUrl, apiKey, directory);
                    boolean success = uploader.upload(outputPath, fileName);
                    if (success) {
                        System.out.println("配置已成功上传到配置中心");
                    } else {
                        System.out.println("配置上传失败");
                    }
                }
            }

            System.out.println();
            System.out.println("配置生成完成!");

        } catch (Exception e) {
            System.err.println("生成配置时出错: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 简化版快速生成配置（用于脚本调用）
     */
    public static void quickGenerate(String url, String username, String password, String outputPath) throws Exception {
        DruidConfigGenerator generator = new DruidConfigGenerator();
        generator.setUrl(url);
        generator.setUsername(username);
        generator.setPassword(password);
        generator.autoDetectDatabase();
        generator.encryptPassword();
        generator.saveToFile(outputPath);
        System.out.println("配置已生成: " + outputPath);
        System.out.println("加密密钥: " + generator.getEncryptionKey());
    }
}