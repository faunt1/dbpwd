package com.dbpwd.boot.encrypt;

import com.dbpwd.core.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * db-pwd 属性解析器
 *
 * 负责检测并解密属性中的占位符格式（@xxx@）
 *
 * 支持的格式：
 * - @database_name@ - 占位符，从 db.pwd 查找并解密
 * - ENC(SM4-GCM:xxx) - 加密格式，直接解密
 *
 * @author Database Password Manager
 * @version 6.0
 */
public class DbPwdPropertyResolver {
    
    private static final Logger log = LoggerFactory.getLogger(DbPwdPropertyResolver.class);
    
    private final String placeholderPrefix;
    private final String placeholderSuffix;
    
    public DbPwdPropertyResolver(String placeholderPrefix, String placeholderSuffix) {
        this.placeholderPrefix = placeholderPrefix;
        this.placeholderSuffix = placeholderSuffix;
    }
    
    /**
     * 解析属性值
     *
     * @param value 原始值（可能是占位符、加密格式或明文）
     * @return 解析后的明文值
     */
    public String resolvePropertyValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // 检查是否是加密格式 ENC(SM4-GCM:xxx)
        if (isEncrypted(value)) {
            log.debug("Detected encrypted value, decrypting...");
            try {
                return PasswordService.decrypt(value);
            } catch (Exception e) {
                log.warn("Failed to decrypt encrypted value: {}", e.getMessage());
                return value; // 解密失败，返回原值
            }
        }
        
        // 检查是否是占位符格式 @xxx@
        if (isPlaceholder(value)) {
            String dbId = extractDbId(value);
            log.debug("Detected placeholder: {}", dbId);
            try {
                return PasswordService.getDatabasePassword(dbId);
            } catch (Exception e) {
                log.warn("Failed to resolve placeholder {}: {}", dbId, e.getMessage());
                return value; // 解析失败，返回原值
            }
        }
        
        // 明文，直接返回
        return value;
    }
    
    /**
     * 检查是否是加密格式
     */
    private boolean isEncrypted(String value) {
        return value.startsWith("ENC(SM4-GCM:") && value.endsWith(")");
    }
    
    /**
     * 检查是否是占位符格式
     */
    private boolean isPlaceholder(String value) {
        return value.startsWith(placeholderPrefix) && 
               value.endsWith(placeholderSuffix) && 
               value.length() > placeholderPrefix.length() + placeholderSuffix.length();
    }
    
    /**
     * 从占位符中提取数据库ID
     */
    private String extractDbId(String placeholder) {
        return placeholder.substring(
            placeholderPrefix.length(), 
            placeholder.length() - placeholderSuffix.length()
        );
    }
}
