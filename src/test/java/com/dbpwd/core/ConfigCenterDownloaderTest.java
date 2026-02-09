package com.dbpwd.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfigCenterDownloader测试类
 */
class ConfigCenterDownloaderTest {

    @TempDir
    Path tempDir;

    private String localDbPwdPath;
    private String localKeyPath;

    @BeforeEach
    void setUp() {
        localDbPwdPath = tempDir.resolve("db.pwd").toString();
        localKeyPath = tempDir.resolve("sm4.key").toString();
    }

    @Test
    void testBuilderWithRequiredFields() {
        ConfigCenterDownloader downloader = new ConfigCenterDownloader.Builder()
                .physicalGroup("测试物理组")
                .application("测试应用")
                .environment("test")
                .localDbPwdPath(localDbPwdPath)
                .localKeyPath(localKeyPath)
                .build();

        assertNotNull(downloader);
    }

    @Test
    void testBuilderWithoutPhysicalGroup() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConfigCenterDownloader.Builder()
                        .application("测试应用")
                        .build());
    }

    @Test
    void testBuilderWithoutApplication() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConfigCenterDownloader.Builder()
                        .physicalGroup("测试物理组")
                        .build());
    }

    @Test
    void testBuilderFromProperties() {
        Properties props = new Properties();
        props.setProperty("config.center.url", "http://config-center.example.com");
        props.setProperty("config.center.api.key", "test-api-key");
        props.setProperty("config.center.base.path", "/databases");
        props.setProperty("config.center.physical.group", "物理组A");
        props.setProperty("config.center.application", "应用名称");
        props.setProperty("config.center.environment", "test");
        props.setProperty("dbpwd.password.file", localDbPwdPath);
        props.setProperty("dbpwd.key.file", localKeyPath);

        ConfigCenterDownloader downloader = ConfigCenterDownloader.Builder
                .fromProperties(props)
                .build();

        assertNotNull(downloader);
    }

    @Test
    void testDownloadWithCompare() throws IOException {
        ConfigCenterDownloader downloader = new ConfigCenterDownloader.Builder()
                .physicalGroup("测试物理组")
                .application("测试应用")
                .environment("test")
                .localDbPwdPath(localDbPwdPath)
                .localKeyPath(localKeyPath)
                .build();

        ConfigCenterDownloader.DownloadResult result = downloader.downloadWithCompare();

        assertNotNull(result);
        assertNotNull(result.getDbPwdResult());
        assertNotNull(result.getKeyResult());
    }

    @Test
    void testDownload() throws IOException {
        ConfigCenterDownloader downloader = new ConfigCenterDownloader.Builder()
                .physicalGroup("测试物理组")
                .application("测试应用")
                .environment("test")
                .localDbPwdPath(localDbPwdPath)
                .localKeyPath(localKeyPath)
                .build();

        ConfigCenterDownloader.DownloadResult result = downloader.download();

        assertNotNull(result);
        assertTrue(result.hasNewFiles());

        // 验证文件已创建
        File dbPwdFile = new File(localDbPwdPath);
        File keyFile = new File(localKeyPath);
        assertTrue(dbPwdFile.exists());
        assertTrue(keyFile.exists());
    }

    @Test
    void testDownloadWithExistingFiles() throws IOException {
        // 创建本地文件
        String oldContent = "{\n  \"version\": \"1.0\",\n  \"lastModified\": \"" +
                java.time.Instant.now().minusSeconds(7200) + "\",\n  \"databases\": []\n}";
        Files.write(new File(localDbPwdPath).toPath(), oldContent.getBytes());
        Files.write(new File(localKeyPath).toPath(), "00000000000000000000000000000000".getBytes());

        ConfigCenterDownloader downloader = new ConfigCenterDownloader.Builder()
                .physicalGroup("测试物理组")
                .application("测试应用")
                .environment("test")
                .localDbPwdPath(localDbPwdPath)
                .localKeyPath(localKeyPath)
                .build();

        ConfigCenterDownloader.DownloadResult result = downloader.downloadWithCompare();

        assertNotNull(result);
        assertTrue(result.hasNewFiles());
    }

    @Test
    void testFilePathExpansion() {
        ConfigCenterDownloader.Builder builder = new ConfigCenterDownloader.Builder()
                .physicalGroup("测试")
                .application("测试")
                .localDbPwdPath("~/test.db.pwd")
                .localKeyPath("~/test.key");

        ConfigCenterDownloader downloader = builder.build();
        assertNotNull(downloader);
    }

    @Test
    void testRemotePathBuilding() {
        ConfigCenterDownloader downloader = new ConfigCenterDownloader.Builder()
                .physicalGroup("财务系统")
                .application("支付应用")
                .environment("prod")
                .configCenterUrl("http://test.com")
                .build();

        assertNotNull(downloader);
    }

    @Test
    void testDownloadWithWebApiUrl() throws IOException {
        String testUrl = "http://config-center.example.com";

        ConfigCenterDownloader downloader = new ConfigCenterDownloader.Builder()
                .configCenterUrl(testUrl)
                .physicalGroup("测试物理组")
                .application("测试应用")
                .environment("test")
                .localDbPwdPath(localDbPwdPath)
                .localKeyPath(localKeyPath)
                .build();

        assertTrue(downloader.isUsingWebApi());
        assertEquals("WEB_API (HTTP)", downloader.getModeDescription());
    }

    @Test
    void testDownloadWithFilePath() throws IOException {
        String testPath = tempDir.resolve("config-center").toString();

        ConfigCenterDownloader downloader = new ConfigCenterDownloader.Builder()
                .configCenterUrl(testPath)
                .physicalGroup("测试物理组")
                .application("测试应用")
                .environment("test")
                .localDbPwdPath(localDbPwdPath)
                .localKeyPath(localKeyPath)
                .build();

        assertFalse(downloader.isUsingWebApi());
        assertEquals("FILE_COPY (文件目录)", downloader.getModeDescription());
    }

    @Test
    void testDownloadResultSummary() {
        ConfigCenterDownloader.FileCompareResult dbPwdResult =
                new ConfigCenterDownloader.FileCompareResult("db.pwd", true, true, false, "下载成功");
        ConfigCenterDownloader.FileCompareResult keyResult =
                new ConfigCenterDownloader.FileCompareResult("sm4.key", true, true, false, "下载成功");

        ConfigCenterDownloader.DownloadResult result = new ConfigCenterDownloader.DownloadResult();
        result.setDbPwdResult(dbPwdResult);
        result.setKeyResult(keyResult);

        assertTrue(result.hasNewFiles());
        assertFalse(result.hasErrors());
        assertNotNull(result.getSummary());
        assertTrue(result.getSummary().contains("db.pwd"));
        assertTrue(result.getSummary().contains("sm4.key"));
    }

    @Test
    void testDownloadResultWithErrors() {
        ConfigCenterDownloader.FileCompareResult dbPwdResult =
                new ConfigCenterDownloader.FileCompareResult("db.pwd", true, false, true, "下载失败");
        ConfigCenterDownloader.FileCompareResult keyResult =
                new ConfigCenterDownloader.FileCompareResult("sm4.key", true, false, true, "下载失败");

        ConfigCenterDownloader.DownloadResult result = new ConfigCenterDownloader.DownloadResult();
        result.setDbPwdResult(dbPwdResult);
        result.setKeyResult(keyResult);

        assertTrue(result.hasErrors());
        assertFalse(result.hasNewFiles());
    }

    @Test
    void testDownloadResultMixed() {
        // 验证混合结果：一个错误，一个成功
        ConfigCenterDownloader.FileCompareResult dbPwdResult =
                new ConfigCenterDownloader.FileCompareResult("db.pwd", true, false, true, "下载失败");
        ConfigCenterDownloader.FileCompareResult keyResult =
                new ConfigCenterDownloader.FileCompareResult("sm4.key", true, true, false, "下载成功");

        ConfigCenterDownloader.DownloadResult result = new ConfigCenterDownloader.DownloadResult();
        result.setDbPwdResult(dbPwdResult);
        result.setKeyResult(keyResult);

        // 理应有：有错误，也有新文件
        assertTrue(result.hasErrors());
        assertTrue(result.hasNewFiles());
    }
}
