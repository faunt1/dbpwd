package com.dbpwd.cli;

import com.dbpwd.core.ConnectionTester;
import com.dbpwd.core.FilePermissionChecker;
import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.crypto.PasswordDecryptor;
import com.dbpwd.crypto.PasswordEncryptor;
import com.dbpwd.crypto.Sm4Cipher;
import com.dbpwd.model.DatabaseEntry;
import com.dbpwd.model.ENCFormat;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * 数据库密码管理命令行工具
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class DBPasswordCLI {

    private static final Logger logger = Logger.getLogger(DBPasswordCLI.class.getName());
    private static final String DEFAULT_PASSWORD_FILE = System.getProperty("user.home") + "/.dbpwd/db.pwd";
    private static final String DEFAULT_KEY_FILE = System.getProperty("user.home") + "/.dbpwd/sm4.key";

    private final String passwordFile;
    private final String keyFile;

    public DBPasswordCLI(String passwordFile, String keyFile) {
        this.passwordFile = passwordFile;
        this.keyFile = keyFile;
    }

    public static void main(String[] args) {
        // 配置日志
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());
        rootLogger.addHandler(handler);

        // 解析参数
        String passwordFile = DEFAULT_PASSWORD_FILE;
        String keyFile = DEFAULT_KEY_FILE;

        for (int i = 0; i < args.length; i++) {
            if ("--password-file".equals(args[i]) && i + 1 < args.length) {
                passwordFile = args[++i];
            } else if ("--key-file".equals(args[i]) && i + 1 < args.length) {
                keyFile = args[++i];
            } else if ("--help".equals(args[i]) || "-h".equals(args[i])) {
                printHelp();
                System.exit(0);
            }
        }

        DBPasswordCLI cli = new DBPasswordCLI(passwordFile, keyFile);

        if (args.length == 0) {
            // 交互式模式
            cli.interactiveMode();
        } else {
            // 命令模式
            cli.commandMode(args);
        }
    }

    private void interactiveMode() {
        System.out.println("=".repeat(60));
        System.out.println("   数据库密码管理命令行工具 v3.0");
        System.out.println("=".repeat(60));
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        try {
            String sm4Key = loadOrGenerateKey(scanner);

            while (true) {
                System.out.println();
                System.out.println("可用命令:");
                System.out.println("  1. 列出所有数据库 (list)");
                System.out.println("  2. 添加数据库 (add)");
                System.out.println("  3. 更新数据库 (update)");
                System.out.println("  4. 删除数据库 (delete)");
                System.out.println("  5. 测试连接 (test)");
                System.out.println("  6. 加密密码 (encrypt)");
                System.out.println("  7. 解密密码 (decrypt)");
                System.out.println("  8. 显示信息 (info)");
                System.out.println("  9. 退出 (exit/quit)");
                System.out.println();
                System.out.print("请输入命令: ");

                String command = scanner.nextLine().trim().toLowerCase();

                switch (command) {
                    case "1":
                    case "list":
                        listDatabases();
                        break;
                    case "2":
                    case "add":
                        addDatabase(scanner, sm4Key);
                        break;
                    case "3":
                    case "update":
                        updateDatabase(scanner, sm4Key);
                        break;
                    case "4":
                    case "delete":
                        deleteDatabase(scanner);
                        break;
                    case "5":
                    case "test":
                        testConnection(scanner, sm4Key);
                        break;
                    case "6":
                    case "encrypt":
                        encryptPassword(scanner, sm4Key);
                        break;
                    case "7":
                    case "decrypt":
                        decryptPassword(scanner, sm4Key);
                        break;
                    case "8":
                    case "info":
                        showInfo();
                        break;
                    case "9":
                    case "exit":
                    case "quit":
                        System.out.println("退出程序");
                        return;
                    default:
                        System.out.println("未知命令: " + command);
                }
            }
        } catch (Exception e) {
            System.err.println("发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void commandMode(String[] args) {
        // 暂未实现命令行参数模式
        System.err.println("命令行参数模式暂未实现，请使用交互式模式");
        System.exit(1);
    }

    private String loadOrGenerateKey(Scanner scanner) throws Exception {
        Sm4KeyLoader keyLoader = new Sm4KeyLoader(keyFile);

        try {
            return keyLoader.loadKey();
        } catch (Sm4KeyLoader.FileNotFoundException e) {
            System.out.println("SM4密钥文件不存在: " + keyFile);
            System.out.print("是否生成新的密钥？(y/n): ");
            String choice = scanner.nextLine().trim().toLowerCase();

            if ("y".equals(choice) || "yes".equals(choice)) {
                String newKey = Sm4Cipher.generateKey();
                Files.write(Paths.get(keyFile), newKey.getBytes());

                // 设置权限（Unix-like系统）
                try {
                    FilePermissionChecker.setPermission600(keyFile);
                } catch (Exception ex) {
                    logger.warning("无法设置文件权限: " + ex.getMessage());
                }

                System.out.println("新的SM4密钥已生成: " + keyFile);
                return newKey;
            } else {
                throw new RuntimeException("无法继续：缺少SM4密钥");
            }
        }
    }

    private void listDatabases() {
        try {
            PasswordFileManager manager = PasswordFileManager.loadFromFile(passwordFile);
            List<DatabaseEntry> databases = manager.getAllDatabases();

            if (databases.isEmpty()) {
                System.out.println("没有配置任何数据库");
                return;
            }

            System.out.println();
            System.out.println("数据库列表:");
            System.out.println("-".repeat(60));
            System.out.printf("%-15s | %-20s | %-20s | %s%n", "ID", "名称", "类型", "用户名");
            System.out.println("-".repeat(60));

            for (DatabaseEntry db : databases) {
                System.out.printf("%-15s | %-20s | %-20s | %s%n",
                    db.getId(), db.getName(), db.getType(), db.getUsername());
            }

            System.out.println("-".repeat(60));
            System.out.println("总计: " + databases.size() + " 个数据库");
        } catch (Exception e) {
            System.err.println("加载数据库列表失败: " + e.getMessage());
        }
    }

    private void addDatabase(Scanner scanner, String sm4Key) {
        try {
            System.out.println();
            System.out.println("添加新数据库");
            System.out.println("-".repeat(30));

            System.out.print("数据库ID: ");
            String id = scanner.nextLine().trim();

            System.out.print("数据库名称: ");
            String name = scanner.nextLine().trim();

            System.out.print("数据库类型 (mysql/postgresql/oracle/sqlserver): ");
            String type = scanner.nextLine().trim();

            System.out.print("JDBC URL: ");
            String url = scanner.nextLine().trim();

            System.out.print("用户名: ");
            String username = scanner.nextLine().trim();

            String plainPassword = PasswordEncryptor.readPasswordInteractive("密码: ");

            // 验证密码强度
            PasswordEncryptor.PasswordStrength strength = PasswordEncryptor.validatePassword(plainPassword);
            System.out.println("密码强度: " + strength);

            // 加密密码
            String encryptedPassword = PasswordEncryptor.encryptSM4ENC(plainPassword, sm4Key);
            System.out.println("加密后的密码: " + encryptedPassword);

            // 创建数据库条目
            DatabaseEntry entry = new DatabaseEntry(id, name, type, url, username, encryptedPassword);
            entry.setEncrypted(true);

            // 保存
            PasswordFileManager manager;
            if (Files.exists(Paths.get(passwordFile))) {
                manager = PasswordFileManager.loadFromFile(passwordFile);
            } else {
                manager = new PasswordFileManager(passwordFile);
            }

            manager.addDatabase(entry);
            manager.save();

            System.out.println();
            System.out.println("数据库配置已保存: " + id);
        } catch (Exception e) {
            System.err.println("添加数据库失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateDatabase(Scanner scanner, String sm4Key) {
        try {
            System.out.println();
            System.out.println("更新数据库配置");
            System.out.println("-".repeat(30));

            System.out.print("数据库ID: ");
            String id = scanner.nextLine().trim();

            PasswordFileManager manager = PasswordFileManager.loadFromFile(passwordFile);
            DatabaseEntry entry = manager.getById(id);

            if (entry == null) {
                System.out.println("未找到数据库: " + id);
                return;
            }

            System.out.println("当前配置:");
            System.out.println("  名称: " + entry.getName());
            System.out.println("  类型: " + entry.getType());
            System.out.println("  URL: " + entry.getUrl());
            System.out.println("  用户名: " + entry.getUsername());

            System.out.println();
            System.out.print("是否更新密码？(y/n): ");
            String choice = scanner.nextLine().trim().toLowerCase();

            if ("y".equals(choice) || "yes".equals(choice)) {
                String plainPassword = PasswordEncryptor.readPasswordInteractive("新密码: ");

                // 验证密码强度
                PasswordEncryptor.PasswordStrength strength = PasswordEncryptor.validatePassword(plainPassword);
                System.out.println("密码强度: " + strength);

                // 加密密码
                String encryptedPassword = PasswordEncryptor.encryptSM4ENC(plainPassword, sm4Key);
                entry.setPassword(encryptedPassword);
                entry.setEncrypted(true);
                System.out.println("密码已更新");
            }

            // 保存
            manager.updateDatabase(entry);
            manager.save();

            System.out.println("数据库配置已更新: " + id);
        } catch (Exception e) {
            System.err.println("更新数据库失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteDatabase(Scanner scanner) {
        try {
            System.out.println();
            System.out.println("删除数据库配置");
            System.out.println("-".repeat(30));

            System.out.print("数据库ID: ");
            String id = scanner.nextLine().trim();

            PasswordFileManager manager = PasswordFileManager.loadFromFile(passwordFile);

            if (manager.removeDatabase(id)) {
                manager.save();
                System.out.println("数据库配置已删除: " + id);
            } else {
                System.out.println("未找到数据库: " + id);
            }
        } catch (Exception e) {
            System.err.println("删除数据库失败: " + e.getMessage());
        }
    }

    private void testConnection(Scanner scanner, String sm4Key) {
        try {
            System.out.println();
            System.out.println("测试数据库连接");
            System.out.println("-".repeat(30));

            System.out.print("数据库ID (或 'all' 测试所有): ");
            String input = scanner.nextLine().trim();

            PasswordFileManager manager = PasswordFileManager.loadFromFile(passwordFile);

            if ("all".equalsIgnoreCase(input)) {
                String summary = ConnectionTester.testAllWithSummary(manager, sm4Key);
                System.out.println();
                System.out.println(summary);
            } else {
                ConnectionTester.TestResult result = ConnectionTester.test(manager, sm4Key, input);
                System.out.println(result.toString());

                if (!result.isSuccess() && result.getError() != null) {
                    result.getError().printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("测试连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void encryptPassword(Scanner scanner, String sm4Key) {
        try {
            System.out.println();
            System.out.println("加密密码");
            System.out.println("-".repeat(30));

            String plainPassword = PasswordEncryptor.readPasswordInteractive("要加密的密码: ");

            String encryptedPassword = PasswordEncryptor.encryptSM4ENC(plainPassword, sm4Key);

            System.out.println();
            System.out.println("ENC格式: " + encryptedPassword);
        } catch (Exception e) {
            System.err.println("加密密码失败: " + e.getMessage());
        }
    }

    private void decryptPassword(Scanner scanner, String sm4Key) {
        try {
            System.out.println();
            System.out.println("解密密码");
            System.out.println("-".repeat(30));

            System.out.print("要解密的密码: ");
            String encryptedPassword = scanner.nextLine().trim();

            String plainPassword = PasswordDecryptor.decrypt(encryptedPassword, sm4Key);

            System.out.println();
            System.out.println("明文密码: " + plainPassword);
        } catch (Exception e) {
            System.err.println("解密密码失败: " + e.getMessage());
        }
    }

    private void showInfo() {
        System.out.println();
        System.out.println("配置信息");
        System.out.println("-".repeat(30));
        System.out.println("密码文件: " + passwordFile);
        System.out.println("密钥文件: " + keyFile);

        try {
            PasswordFileManager manager = PasswordFileManager.loadFromFile(passwordFile);
            System.out.println("数据库数量: " + manager.size());
            System.out.println("最后修改: " + manager.getPasswordFile().getLastModified());
        } catch (Exception e) {
            System.out.println("无法加载数据库: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("数据库密码管理命令行工具 v3.0");
        System.out.println();
        System.out.println("用法: java -jar dbpwd-cli.jar [选项]");
        System.out.println();
        System.out.println("选项:");
        System.out.println("  --password-file <path>  密码文件路径 (默认: ~/.dbpwd/db.pwd)");
        System.out.println("  --key-file <path>       SM4密钥文件路径 (默认: ~/.dbpwd/sm4.key)");
        System.out.println("  -h, --help               显示帮助信息");
        System.out.println();
        System.out.println("交互式命令:");
        System.out.println("  list     列出所有数据库");
        System.out.println("  add      添加新数据库");
        System.out.println("  update   更新数据库配置");
        System.out.println("  delete   删除数据库配置");
        System.out.println("  test     测试数据库连接");
        System.out.println("  encrypt  加密密码");
        System.out.println("  decrypt  解密密码");
        System.out.println("  info     显示配置信息");
        System.out.println("  exit     退出程序");
    }
}
