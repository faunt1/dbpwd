package com.dbpwd.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 文件权限检查工具
 *
 * 用于检查和设置文件权限，特别是600权限
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class FilePermissionChecker {

    private static final Logger logger = Logger.getLogger(FilePermissionChecker.class.getName());

    /**
     * 检查文件权限是否为600（仅所有者可读写）
     *
     * @param filePath 文件路径
     * @return 权限是否符合要求
     */
    public static boolean isPermission600(String filePath) {
        try {
            return isPermission600(new File(filePath));
        } catch (Exception e) {
            logger.warning("检查文件权限失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查文件权限是否为600（仅所有者可读写）
     *
     * @param file 文件对象
     * @return 权限是否符合要求
     */
    public static boolean isPermission600(File file) {
        if (!file.exists()) {
            return false;
        }

        try {
            Path path = file.toPath();

            // 在Unix-like系统上检查Posix权限
            if (!isWindows()) {
                Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);

                // 600权限：OWNER_READ + OWNER_WRITE
                Set<PosixFilePermission> requiredPermissions = new HashSet<>();
                requiredPermissions.add(PosixFilePermission.OWNER_READ);
                requiredPermissions.add(PosixFilePermission.OWNER_WRITE);

                // 检查是否有其他用户的权限
                boolean hasOtherPermission = permissions.stream()
                    .filter(p -> !requiredPermissions.contains(p))
                    .count() > 0;

                return !hasOtherPermission;
            } else {
                logger.fine("Windows系统，无法检查精确的600权限");
                return true;
            }
        } catch (UnsupportedOperationException e) {
            logger.warning("当前文件系统不支持Posix权限");
            return true;
        } catch (Exception e) {
            logger.severe("检查文件权限时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 设置文件权限为600（仅所有者可读写）
     *
     * @param filePath 文件路径
     * @throws IOException 设置失败
     */
    public static void setPermission600(String filePath) throws IOException {
        setPermission600(new File(filePath));
    }

    /**
     * 设置文件权限为600（仅所有者可读写）
     *
     * @param file 文件对象
     * @throws IOException 设置失败
     */
    public static void setPermission600(File file) throws IOException {
        if (isWindows()) {
            logger.fine("Windows系统，跳过600权限设置");
            return;
        }

        if (!file.exists()) {
            throw new IOException("文件不存在: " + file.getAbsolutePath());
        }

        try {
            Path path = file.toPath();
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(path, permissions);

            logger.info("文件权限已设置为600: " + file.getAbsolutePath());
        } catch (UnsupportedOperationException e) {
            throw new IOException("当前文件系统不支持Posix权限: " + e.getMessage(), e);
        }
    }

    /**
     * 验证文件权限，不符合要求时抛出异常
     *
     * @param filePath 文件路径
     * @param throwException 是否抛出异常
     * @return 权限是否符合要求
     * @throws IOException 权限不符合要求
     */
    public static boolean verifyPermission600(String filePath, boolean throwException) throws IOException {
        File file = new File(filePath);

        if (!file.exists()) {
            String msg = "文件不存在: " + filePath;
            if (throwException) {
                throw new IOException(msg);
            }
            logger.severe(msg);
            return false;
        }

        boolean valid = isPermission600(file);

        if (!valid) {
            String msg = "文件权限不符合600要求: " + filePath;
            if (throwException) {
                throw new IOException(msg + "。请运行: chmod 600 " + filePath);
            }
            logger.warning(msg);
        }

        return valid;
    }

    /**
     * 验证文件权限，不符合要求时抛出异常
     *
     * @param filePath 文件路径
     * @return 权限是否符合要求
     * @throws IOException 权限不符合要求
     */
    public static boolean verifyPermission600(String filePath) throws IOException {
        return verifyPermission600(filePath, true);
    }

    /**
     * 获取文件权限字符串（类似ls -l输出）
     *
     * @param filePath 文件路径
     * @return 权限字符串，如 "rw-------"
     */
    public static String getPermissionString(String filePath) {
        try {
            return getPermissionString(new File(filePath));
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * 获取文件权限字符串（类似ls -l输出）
     *
     * @param file 文件对象
     * @return 权限字符串，如 "rw-------"
     */
    public static String getPermissionString(File file) {
        if (!file.exists()) {
            return "FILE_NOT_EXISTS";
        }

        try {
            Path path = file.toPath();

            if (!isWindows()) {
                Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
                return PosixFilePermissions.toString(permissions);
            } else {
                return "WINDOWS: " + (file.canRead() ? "r" : "-") +
                                   (file.canWrite() ? "w" : "-") +
                                   (file.canExecute() ? "x" : "-");
            }
        } catch (UnsupportedOperationException e) {
            return "NOT_SUPPORTED";
        } catch (Exception e) {
            return "ERROR";
        }
    }

    /**
     * 检查是否为Windows系统
     */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * 确保文件存在，如果不存在则创建父目录
     *
     * @param filePath 文件路径
     * @throws IOException 创建失败
     */
    public static void ensureFileExists(String filePath) throws IOException {
        File file = new File(filePath);

        if (!file.exists()) {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("无法创建目录: " + parentDir.getAbsolutePath());
                }
            }

            if (!file.createNewFile()) {
                throw new IOException("无法创建文件: " + file.getAbsolutePath());
            }

            logger.fine("文件已创建: " + filePath);
        }
    }

    /**
     * 确保父目录存在
     *
     * @param filePath 文件路径
     * @throws IOException 创建失败
     */
    public static void ensureParentDirectoryExists(String filePath) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("无法创建目录: " + parentDir.getAbsolutePath());
            }

            logger.fine("目录已创建: " + parentDir.getAbsolutePath());
        }
    }
}
