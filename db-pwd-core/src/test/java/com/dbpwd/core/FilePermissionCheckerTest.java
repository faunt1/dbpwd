package com.dbpwd.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FilePermissionChecker单元测试
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class FilePermissionCheckerTest {

    @TempDir
    Path tempDir;


    @BeforeEach
    public void setUp() {
    }

    // Windows系统测试
    @Test
    public void testSetPermission600OnWindows() {
        if (!isWindows()) {
            return; // 只在Windows上测试
        }

        Path filePath = tempDir.resolve("test.txt");
        try {
            Files.createFile(filePath);

            // 在Windows上应该静默跳过，不抛出异常
            FilePermissionChecker.setPermission600(filePath.toString());
        } catch (Exception e) {
            fail("Windows上应该静默跳过权限设置: " + e.getMessage());
        }
    }

    @Test
    public void testIsPermission600OnWindows() {
        if (!isWindows()) {
            return; // 只在Windows上测试
        }

        Path filePath = tempDir.resolve("test.txt");
        try {
            Files.createFile(filePath);

            // 在Windows上应该静默跳过
            boolean result = FilePermissionChecker.isPermission600(filePath.toString());
            // Windows上无法设置600权限，返回true表示"不适用"
            assertTrue(result, "Windows上应该返回true（不适用）");
        } catch (Exception e) {
            fail("Windows上应该静默跳过权限检查: " + e.getMessage());
        }
    }

    @Test
    public void testIsPermission600WithNullPath() {
        assertFalse(FilePermissionChecker.isPermission600((String) null));
    }

    @Test
    public void testIsPermission600WithEmptyPath() {
        assertFalse(FilePermissionChecker.isPermission600(""));

    }

    // Unix-like系统测试（如果可运行）
    @Test
    public void testIsPermission600Linux() {
        if (isWindows()) {
            return; // 跳过Windows测试
        }

        try {
            Path filePath = tempDir.resolve("test-600.txt");
            Files.createFile(filePath);

            // 设置600权限
            Files.setPosixFilePermissions(filePath,
                PosixFilePermissions.fromString("rw-------"));

            // 检查权限
            boolean result = FilePermissionChecker.isPermission600(filePath.toString());
            assertTrue(result, "600权限检查应返回true");
        } catch (UnsupportedOperationException | IOException e) {
            // 某些文件系统不支持POSIX权限，这是预期的
        }
    }

    @Test
    public void testIsPermission600LinuxWrongPermission() {
        if (isWindows()) {
            return;
        }

        try {
            Path filePath = tempDir.resolve("test-644.txt");
            Files.createFile(filePath);

            // 设置644权限（允许组和其他用户读）
            Files.setPosixFilePermissions(filePath,
                PosixFilePermissions.fromString("rw-r--r--"));

            // 检查权限，应该返回false
            boolean result = FilePermissionChecker.isPermission600(filePath.toString());
            assertFalse(result, "644权限检查应返回false");

            // 应该触发警告
            // 注意：isPermission600是静默检查，不会抛出异常
            // 如果需要严格权限控制，直接抛出异常的情况需要在实际使用中考虑
        } catch (UnsupportedOperationException | IOException e) {
            // 某些文件系统不支持，这是预期的
        }
    }

    @Test
    public void testSetPermission600Linux() {
        if (isWindows()) {
            return;
        }

        try {
            Path filePath = tempDir.resolve("test-set600.txt");
            Files.createFile(filePath);

            // 设置600权限
            FilePermissionChecker.setPermission600(filePath.toString());

            // 验证权限
            java.util.Set<PosixFilePermission> permissions =
                Files.getPosixFilePermissions(filePath);

            java.util.Set<PosixFilePermission> expected = new java.util.HashSet<>();
            expected.add(PosixFilePermission.OWNER_READ);
            expected.add(PosixFilePermission.OWNER_WRITE);

            // 应该只包含owner的读写权限
            assertEquals(2, permissions.size(), "应该只有2个权限");
            assertTrue(permissions.containsAll(expected), "应包含owner读和写");
        } catch (UnsupportedOperationException | IOException e) {
            // 某些文件系统不支持
        }
    }

    @Test
    public void testSetPermission600OnNonExistentFile() {
        Path nonExistent = tempDir.resolve("nonexistent.txt");

        if (isWindows()) {
            // Windows允许对不存在的文件调用setPermission600（静默跳过）
            try {
                FilePermissionChecker.setPermission600(nonExistent.toString());
            } catch (Exception e) {
                fail("Windows上应该静默跳过: " + e.getMessage());
            }
        } else {
            // Unix上会抛出异常
            assertThrows(java.nio.file.FileSystemException.class, () ->
                FilePermissionChecker.setPermission600(nonExistent.toString())
            );
        }
    }

    @Test
    public void testIsPermission600OnNonExistentFile() {
        Path nonExistent = tempDir.resolve("nonexistent.txt");

        // 不存在的文件应该返回false
        assertFalse(
            FilePermissionChecker.isPermission600(nonExistent.toString()));
    }


    // 辅助方法
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
