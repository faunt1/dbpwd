package com.dbpwd.spring;

import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.model.DatabaseEntry;
import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.crypto.Sm4GCMCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DataSource密码处理器
 *
 * 通过BeanPostProcessor拦截DataSource Bean，自动解密密码
 *
 * 使用场景：
 * - 传统Spring应用（非Spring Boot）
 * - XML配置或Java配置方式
 * - 需要自动处理DataSource密码的应用
 *
 * 工作流程：
 * 1. Spring容器创建DataSource Bean
 * 2. BeanPostProcessor后处理阶段拦截
 * 3. 检测密码是否为占位符格式
 * 4. 从db.pwd中查找对应的数据库配置
 * 5. 使用sm4.key解密密码
 * 6. 将明文密码设置回DataSource
 *
 * 配置方式：
 * <pre>
 * &lt;bean class="com.dbpwd.spring.DataSourcePasswordProcessor"/&gt;
 *
 * &lt;bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource"&gt;
 *     &lt;property name="url" value="$\{db.url}"/&gt;
 *     &lt;property name="username" value="$\{db.username}"/&gt;
 *     &lt;property name="password" value="db_placeholder"/&gt;  &lt;!-- 占位符 --&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class DataSourcePasswordProcessor implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DataSourcePasswordProcessor.class);

    private static final String DEFAULT_KEY_PATH = System.getProperty("user.home") + "/.dbpwd/sm4.key";
    private static final String DEFAULT_PWD_PATH = System.getProperty("user.home") + "/.dbpwd/db.pwd";

    private String keyFilePath = DEFAULT_KEY_PATH;
    private String pwdFilePath = DEFAULT_PWD_PATH;

    // 排除的DataSource bean名称
    private final Set<String> excludedBeanNames = new HashSet<>();

    // 缓存：SM4密钥
    private String cachedSm4Key;

    // 缓存：密码文件管理器
    private PasswordFileManager cachedPasswordManager;

    // 缓存：明文密码缓存 (dbId -> plainPassword)
    private final Map<String, String> passwordCache = new ConcurrentHashMap<>();

    /**
     * 构造函数
     */
    public DataSourcePasswordProcessor() {
        logger.info("DataSourcePasswordProcessor initialized");
    }

    /**
     * 设置SM4密钥文件路径
     *
     * @param keyFilePath SM4密钥文件路径
     */
    public void setKeyFilePath(String keyFilePath) {
        this.keyFilePath = keyFilePath;
        // 清除缓存以强制重新加载
        this.cachedSm4Key = null;
    }

    /**
     * 设置密码文件路径
     *
     * @param pwdFilePath 密码文件路径
     */
    public void setPwdFilePath(String pwdFilePath) {
        this.pwdFilePath = pwdFilePath;
        // 清除缓存以强制重新加载
        this.cachedPasswordManager = null;
        this.passwordCache.clear();
    }

    /**
     * 添加排除的DataSource bean名称
     *
     * @param beanName bean名称
     */
    public void addExcludedBeanName(String beanName) {
        excludedBeanNames.add(beanName);
    }

    /**
     * 设置排除的DataSource bean名称列表
     *
     * @param excludedBeanNames bean名称列表
     */
    public void setExcludedBeanNames(Set<String> excludedBeanNames) {
        if (excludedBeanNames != null) {
            this.excludedBeanNames.addAll(excludedBeanNames);
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        // 不处理，返回原bean
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 只处理DataSource类型的bean
        if (!isDataSource(bean)) {
            return bean;
        }

        // 跳过排除的bean
        if (isExcluded(beanName)) {
            logger.debug("Skipping excluded DataSource bean: {}", beanName);
            return bean;
        }

        try {
            // 获取当前密码
            String currentPassword = getPasswordFromDataSource((DataSource) bean);

            // 如果密码为空，跳过
            if (currentPassword == null || currentPassword.isEmpty()) {
                logger.debug("DataSource [{}] password is empty, skipping", beanName);
                return bean;
            }

            logger.info("Processing DataSource [{}] with password", beanName);

            // 解密密码（支持ENC格式和占位符格式）
            String plainPassword = decryptPassword(currentPassword);

            // 如果解密失败（返回原密码），跳过
            if (plainPassword == null || plainPassword.equals(currentPassword)) {
                logger.debug("DataSource [{}] password not encrypted, skipping", beanName);
                return bean;
            }

            // 将明文密码设置回DataSource
            setPasswordToDataSource((DataSource) bean, plainPassword);

            logger.info("Successfully decrypted password for DataSource [{}]", beanName);

        } catch (Exception e) {
            logger.error("Failed to process DataSource [" + beanName + "]: " + e.getMessage(), e);
            // 根据需求决定是否抛出异常
            // throw new RuntimeException("Failed to process DataSource: " + e.getMessage(), e);
        }

        return bean;
    }

    /**
     * 判断对象是否为DataSource
     */
    private boolean isDataSource(Object bean) {
        return bean instanceof DataSource;
    }

    /**
     * 判断是否在排除列表中
     */
    private boolean isExcluded(String beanName) {
        return excludedBeanNames.contains(beanName);
    }

    /**
     * 从DataSource获取密码
     */
    private String getPasswordFromDataSource(DataSource dataSource) {
        try {
            // 尝试通过反射获取password字段
            Class<?> clazz = dataSource.getClass();
            Method getPasswordMethod = clazz.getMethod("getPassword");
            return (String) getPasswordMethod.invoke(dataSource);
        } catch (Exception e) {
            logger.warn("Failed to get password from DataSource: " + e.getMessage());
            return null;
        }
    }

    /**
     * 将密码设置到DataSource
     */
    private void setPasswordToDataSource(DataSource dataSource, String password) {
        try {
            Class<?> clazz = dataSource.getClass();
            Method setPasswordMethod = clazz.getMethod("setPassword", String.class);
            setPasswordMethod.invoke(dataSource, password);
        } catch (Exception e) {
            logger.error("Failed to set password to DataSource: " + e.getMessage(), e);
        }
    }

    /**
     * 解密密码（从占位符或加密密文）
     * 使用缓存优化性能
     */
    private String decryptPassword(String passwordOrPlaceholder) throws Exception {
        // 如果是加密格式，直接解密
        if (passwordOrPlaceholder.startsWith("ENC(SM4-")) {
            return decryptGCMPassword(passwordOrPlaceholder);
        }

        // 否则，假设是占位符，从db.pwd查找并解密
        return getPlainPassword(passwordOrPlaceholder);
    }

    /**
     * 获取明文密码（带缓存）
     */
    private String getPlainPassword(String dbPlaceholder) throws Exception {
        // 先检查缓存
        if (passwordCache.containsKey(dbPlaceholder)) {
            return passwordCache.get(dbPlaceholder);
        }

        // 加载密码文件并查找数据库配置
        DatabaseEntry entry = loadDatabaseEntry(dbPlaceholder);

        if (entry == null) {
            // 未找到配置，返回原值（可能是明文密码）
            return dbPlaceholder;
        }

        String encryptedPassword = entry.getPassword();
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            throw new IllegalArgumentException("数据库密码为空: " + dbPlaceholder);
        }

        // 解密密码
        String plainPassword = decryptGCMPassword(encryptedPassword);

        // 缓存结果
        passwordCache.put(dbPlaceholder, plainPassword);

        return plainPassword;
    }

    /**
     * 解密GCM格式密码（带缓存）
     */
    private String decryptGCMPassword(String encryptedPassword) throws Exception {
        String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
        String cipherText = ENCFormat.extractCipherText(encryptedPassword);

        // 仅支持SM4-GCM格式
        if (!"SM4-GCM".equalsIgnoreCase(algorithm)) {
            throw new IllegalArgumentException(
                    "不支持的加密算法: " + algorithm + "。仅支持SM4-GCM格式（新应用必须使用GCM认证加密）");
        }

        String sm4Key = loadSm4Key();
        return Sm4GCMCipher.decrypt(cipherText, sm4Key);
    }

    /**
     * 加载SM4密钥（带缓存）
     */
    private String loadSm4Key() throws Exception {
        if (cachedSm4Key == null) {
            Sm4KeyLoader loader = new Sm4KeyLoader(keyFilePath);
            cachedSm4Key = loader.loadKey(true);
        }
        return cachedSm4Key;
    }

    /**
     * 加载数据库配置（带缓存）
     */
    private DatabaseEntry loadDatabaseEntry(String dbPlaceholder) throws Exception {
        if (cachedPasswordManager == null) {
            cachedPasswordManager = PasswordFileManager.loadFromFile(pwdFilePath);
        }
        return cachedPasswordManager.getById(dbPlaceholder);
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        cachedSm4Key = null;
        cachedPasswordManager = null;
        passwordCache.clear();
        logger.info("DataSourcePasswordProcessor cache cleared");
    }
}
