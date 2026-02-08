package com.dbpwd.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sm4KeyLoader单元测试
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class Sm4KeyLoaderTest {

    @TempDir
    Path tempDir;

    private Path testKeyFile;

    @BeforeEach
    public void setUp() {
        testKeyFile = tempDir.resolve("test-sm4.key");
    }

    // ==================== 构造函数测试 ====================

    @Test
    public void testDefaultConstructor() {
        Sm4KeyLoader loader = new Sm4KeyLoader();

        assertNotNull(loader);
        // 默认路径应该是 ~/.dbpwd/sm4.key
        assertTrue(loader.getKeyFilePath().contains(".dbpwd"));
        assertTrue(loader.getKeyFilePath().contains("sm4.key"));
    }

    @Test
    public void testConstructorWithCustomPath() {
        Path customPath = tempDir.resolve("custom-key.key");
        Sm4KeyLoader loader = new Sm4KeyLoader(customPath.toString());

        assertNotNull(loader);
        assertEquals(customPath.toString(), loader.getKeyFilePath());
    }

    @Test
    public void testConstructorWithEmptyPath() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> new Sm4KeyLoader("")
        );

        assertTrue(e.getMessage().contains("不能为空"));
    }

    @Test
    public void testConstructorWithNullPath() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> new Sm4KeyLoader(null)
        );

        assertTrue(e.getMessage().contains("不能为空"));
    }

    @Test
    public void testConstructorWithTildePath() {
        Sm4KeyLoader loader = new Sm4KeyLoader("~/.dbpwd/test.key");
        String expected = System.getProperty("user.home") + "/.dbpwd/test.key";

        assertEquals(expected, loader.getKeyFilePath());
    }

    // ==================== loadKey 基础测试 ====================

    @Test
    public void testLoadKeyValidKey() throws Exception {
        // 创建32个字符的hex密钥
        String validKey = "00112233445566778899AABBCCDDEEFF";
        Files.write(testKeyFile, validKey.getBytes());

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyFile.toString());
        String loadedKey = loader.loadKey();

        assertEquals(validKey, loadedKey);
    }

    @Test
    public void testLoadKeyWithCache() throws Exception {
        String validKey = "00112233445566778899AABBCCDDEEFF";
        Files.write(testKeyFile, validKey.getBytes());

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyFile.toString());

        // 第一次加载（不使用缓存）
        String key1 = loader.loadKey(false);

        // 第二次加载（使用缓存）
        String key2 = loader.loadKey(true);

        assertEquals(validKey, key1);
        assertEquals(validKey, key2);
    }

    @Test
    public void testLoadKeyWithCacheFileChanged() throws Exception {
        String validKey1 = "00112233445566778899AABBCCDDEEFF";
        Files.write(testKeyFile, validKey1.getBytes());

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyFile.toString());

        // 第一次加载并缓存
        String key1 = loader.loadKey(true);
        assertEquals(validKey1, key1);

        // 修改文件
        Thread.sleep(10); // 确保时间戳不同
        String validKey2 = "FFEEDDCCBBAA99887766554433221100";
        Files.write(testKeyFile, validKey2.getBytes());

        // 再次加载（文件已修改，应重新读取）
        String key2 = loader.loadKey(true);

        assertEquals(validKey2, key2);
    }

    @Test
    public void testLoadKeyFileNotExists() {
        Sm4KeyLoader loader = new Sm4KeyLoader("/nonexistent/path/sm4.key");

        Sm4KeyLoader.FileNotFoundException e = assertThrows(
            Sm4KeyLoader.FileNotFoundException.class,
            () -> loader.loadKey()
        );

        assertTrue(e.getMessage().contains("不存在"));
    }

    @Test
    public void testLoadKeyFileIsDirectory() throws IOException {
        Path dirPath = tempDir.resolve("test-dir");
        Files.createDirectory(dirPath);

        Sm4KeyLoader loader = new Sm4KeyLoader(dirPath.toString());

        IOException e = assertThrows(
            IOException.class,
            () -> loader.loadKey()
        );

        assertTrue(e.getMessage().contains("是目录"));
    }

    // ==================== 密钥内容验证测试 ====================

    @Test
    public void testLoadKeyEmptyFile() throws IOException {
        Files.write(testKeyFile, "".getBytes());

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyFile.toString());

        SecurityException e = assertThrows(
            SecurityException.class,
            () -> loader.loadKey()
        );

        assertTrue(e.getMessage().contains("为空"));
    }

    @Test
    public void testLoadKeyInvalidHexFormat() throws IOException {
        Files.write(testKeyFile, "GHIJKLMNOPQRSTUVWXYZ12345".getBytes());

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyFile.toString());

        Sm4KeyLoader.InvalidKeyException e = assertThrows(
            Sm4KeyLoader.InvalidKeyException.class,
            () -> loader.loadKey()
        );

        assertTrue(e.getMessage().contains("格式不正确"));
    }

    @Test
    public void testLoadKeyWrongLength() throws IOException {
        // 只有16个字符 (8字节)
        String shortKey = "0011223344556677";
        Files.write(testKeyFile, shortKey.getBytes());

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyFile.toString());

        Sm4KeyLoader.InvalidKeyException e = assertThrows(
            Sm4KeyLoader.InvalidKeyException.class,
            () -> loader.loadKey()
        );

        assertTrue(e.getMessage().contains("长度不正确"));
    }

    @Test
    public void testLoadKeyTooLong() throws IOException {
        // 48个字符 (24字节)
        String longKey = "00112233445566778899AABBCCDDEEFF001122334455";
        Files.write(testKeyFile, longKey.getBytes());

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyFile.toString());

        Sm4KeyLoader.InvalidKeyException e = assertThrows(
            Sm4KeyLoader.InvalidKeyException.class,
            () -> loader.loadKey()
        );

        assertTrue(e.getMessage().contains("长度不正确"));
    }

    // ==================== 注释和空白处理测试 ====================

    @Test
    public void testLoadKeyWithComments() throws Exception {
        String content = "# This is a comment\n00112233445566778899AABBCCDDEEFF\n# Another comment";
        Files.write(testKeyFile, content.getBytes());

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyFile.toString());
        String key = loader.loadKey();

        assertEquals("00112233445566778899AABBCCDDEEFF", key);
    }

    @Test
    public void testLoadKeyWithWhitespace() throws Exception {
        String content = "\n 00112233445566778899AABBCCDDEEFF \n";
        Files.write(testKeyFile, content.getBytes());

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyFile.toString());
        String key = loader.loadKey();

        assertEquals("00112233445566778899AABBCCDDEEFF", key);
    }

    // ==================== 大小写处理测试 ====================

    @Test
    public void testLoadKeyUpperCase() throws Exception {
        String key = "00112233445566778899AABBCCDDEEFF";
        Files.write(testKeyFile, key.getBytes());

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyFile.toString());
        String loaded = loader.loadKey();

        assertEquals(key, loaded);
    }

    @Test
    public void testLoadKeyLowerCase() throws Exception {
        String key = "00112233445566778899aabbccddeeff";
        Files.write(testKeyFile, key.getBytes());

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyFile.toString());
        String loaded = loader.loadKey();

        assertEquals(key, loaded);
    }

    @Test
    public void testLoadKeyMixedCase() throws Exception {
        String key = "00112233445566778899AaBbCcDdEeFf";
        Files.write(testKeyFile, key.getBytes());

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyFile.toString());
        String loaded = loader.loadKey();

        assertEquals(key, loaded);
    }

    // ==================== generateKey 测试 ====================

    @Test
    public void testGenerateKey() throws Exception {
        Path keyPath = tempDir.resolve("generated-key.key");
        String hexKey = Sm4KeyLoader.generateKey(keyPath.toString());

        assertNotNull(hexKey);
        assertEquals(32, hexKey.length(), "生成的密钥应该是32个hex字符");
        assertTrue(isValidHex(hexKey), "生成的密钥应该是有效的hex编码");

        // 验证文件已创建
        assertTrue(Files.exists(keyPath));

        // 验证文件内容
        byte[] fileBytes = Files.readAllBytes(keyPath);
        String fileContent = new String(fileBytes);
        assertEquals(hexKey, fileContent);
    }

    @Test
    public void testGenerateKeyWithOverwrite() throws Exception {
        Path keyPath = tempDir.resolve("overwrite-key.key");

        // 第一次生成
        String key1 = Sm4KeyLoader.generateKey(keyPath.toString());
        assertNotNull(key1);

        // 第二次生成（应该报错）
        IOException e = assertThrows(
            IOException.class,
            () -> Sm4KeyLoader.generateKey(keyPath.toString())
        );

        assertTrue(e.getMessage().contains("已存在"));

        // 使用overwrite=true
        String key2 = Sm4KeyLoader.generateKey(keyPath.toString(), true);
        assertNotNull(key2);
        // 两个密钥不太可能相同（随机生成）
        assertNotEquals(key1, key2);
    }

    @Test
    public void testGenerateKeyRandomness() throws Exception {
        Path keyPath1 = tempDir.resolve("random-key-1.key");
        Path keyPath2 = tempDir.resolve("random-key-2.key");

        String key1 = Sm4KeyLoader.generateKey(keyPath1.toString());
        String key2 = Sm4KeyLoader.generateKey(keyPath2.toString());

        // 随机生成的两个密钥应该不同（极小概率相同）
        assertNotEquals(key1, key2, "随机生成的密钥应该不同");
    }

    // ==================== clearCache测试 ====================

    @Test
    public void testClearCache() throws Exception {
        String validKey = "00112233445566778899AABBCCDDEEFF";
        Files.write(testKeyFile, validKey.getBytes());

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyFile.toString());

        // 加载并缓存
        loader.loadKey(true);

        // 清除缓存
        loader.clearCache();

        // 再次加载应该重新读取文件
        String loaded = loader.loadKey(true);
        assertEquals(validKey, loaded);
    }

    // ==================== 权限测试 ====================

    @Test
    public void testLoadKeyInsecurePermissions() throws Exception {
        if (isWindows()) {
            // Windows系统跳过此测试
            return;
        }

        String validKey = "00112233445566778899AABBCCDDEEFF";
        Files.write(testKeyFile, validKey.getBytes());

        // 设置为644不安全权限
        java.util.Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-r--r--");
        Files.setPosixFilePermissions(testKeyFile, permissions);

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyFile.toString());

        IOException e = assertThrows(
            IOException.class,
            () -> loader.loadKey()
        );

        assertTrue(e.getMessage().contains("权限不安全") || e.getMessage().contains("600"));
    }

    @Test
    public void testLoadKeySecurePermissions() throws Exception {
        if (isWindows()) {
            // Windows系统跳过此测试
            return;
        }

        String validKey = "00112233445566778899AABBCCDDEEFF";
        Files.write(testKeyFile, validKey.getBytes());

        // 设置为600安全权限
        java.util.Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-------");
        Files.setPosixFilePermissions(testKeyFile, permissions);

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyFile.toString());

        // 不应该抛出异常
        String loaded = loader.loadKey();
        assertEquals(validKey, loaded);
    }

    // ==================== Getter测试 ====================

    @Test
    public void testGetKeyFilePath() {
        String customPath = "/path/to/key.key";
        Sm4KeyLoader loader = new Sm4KeyLoader(customPath);

        assertEquals(customPath, loader.getKeyFilePath());
    }

    // ==================== 异常类测试 ====================

    @Test
    public void testInvalidKeyException() {
        Sm4KeyLoader.InvalidKeyException e =
            new Sm4KeyLoader.InvalidKeyException("Invalid key format");

        assertNotNull(e);
        assertTrue(e.getMessage().contains("Invalid key format"));
    }

    @Test
    public void testFileNotFoundException() {
        Sm4KeyLoader.FileNotFoundException e =
            new Sm4KeyLoader.FileNotFoundException("Key file not found");

        assertNotNull(e);
        assertTrue(e.getMessage().contains("Key file not found"));
    }

    // ==================== 辅助方法 ====================

    private boolean isValidHex(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }

        String upper = s.toUpperCase();
        for (char c : upper.toCharArray()) {
            if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }

        return true;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
