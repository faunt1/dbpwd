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
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.FileHandler;

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
    }

    /**
     * 设置密码文件路径
     *
     * @param pwdFilePath 密码文件路径
     */
    public void setPwdFilePath(String pwdFilePath) {
        this.pwdFilePath = pwdFilePath;
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
            String plainPassword = decryptPassword(currentPassword, keyFilePath);

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
     * 判断密码是否为加密格式（占位符格式）
     *
     * 判断逻辑：
     * - 密码格式为 "ENC(SM4-GCM:xxx)" - 不是占位符，需要解密
     * - 密码不是该格式 - 可能是占位符或明文密码
     *
     * 这里我们简化处理：如果密码包含加密格式标记，则解密
     * 否则假设是占位符，从db.pwd查找
     */
    private boolean isEncryptedPassword(String password) {
        if (password == null) {
            return false;
        }

        // 如果密码格式为 ENC(SM4-GCM:...)，直接返回true
        if (password.startsWith("ENC(SM4-")) {
            return true;
        }

        // 否则，假设密码是占位符，需要从db.pwd查找
        return false;
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
     * 获取明文密码
     */
    private String getPlainPassword(String dbPlaceholder) throws Exception {
        // 1. 加载SM4密钥
        String sm4Key = loadSm4Key(keyFilePath);

        // 2. 加载密码文件并查找数据库配置
        DatabaseEntry entry = loadDatabaseEntry(pwdFilePath, dbPlaceholder);

        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置，占位符: " + dbPlaceholder);
        }

        String encryptedPassword = entry.getPassword();
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            throw new IllegalArgumentException("数据库密码为空: " + dbPlaceholder);
        }

        // 3. 解密密码
        return decryptPassword(encryptedPassword, sm4Key);
    }

    /**
     * 解密密码（从占位符或加密密文）
     */
    private String decryptPassword(String passwordOrPlaceholder, String keyFilePath) throws Exception {
        // 如果是加密格式，直接解密
        if (passwordOrPlaceholder.startsWith("ENC(SM4-")) {
            String sm4Key = loadSm4Key(keyFilePath);
            return decryptGCMPassword(passwordOrPlaceholder, sm4Key);
        }

        // 否则，假设是占位符，从db.pwd查找并解密
        return getPlainPassword(passwordOrPlaceholder);
    }

    /**
     * 解密GCM格式密码
     */
    private String decryptGCMPassword(String encryptedPassword, String sm4Key) throws Exception {
        String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
        String cipherText = ENCFormat.extractCipherText(encryptedPassword);

        // 仅支持SM4-GCM格式
        if (!"SM4-GCM".equalsIgnoreCase(algorithm)) {
            throw new IllegalArgumentException(
                    "不支持的加密算法: " + algorithm + "。仅支持SM4-GCM格式（新应用必须使用GCM认证加密）");
        }

        return Sm4GCMCipher.decrypt(cipherText, sm4Key);
    }

    /**
     * 加载SM4密钥
     */
    private String loadSm4Key(String keyFilePath) throws Exception {
        Sm4KeyLoader loader = new Sm4KeyLoader(keyFilePath);
        return loader.loadKey(true);
    }

    /**
     * 加载数据库配置
     */
    private DatabaseEntry loadDatabaseEntry(String pwdFilePath, String dbPlaceholder) throws Exception {
        PasswordFileManager manager = PasswordFileManager.loadFromFile(pwdFilePath);
        return manager.getById(dbPlaceholder);
    }
}
