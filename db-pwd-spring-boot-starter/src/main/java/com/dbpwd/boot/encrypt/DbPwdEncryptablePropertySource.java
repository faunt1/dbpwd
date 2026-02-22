package com.dbpwd.boot.encrypt;

import org.springframework.core.env.PropertySource;

import java.util.Map;

/**
 * 可解密的 PropertySource 包装器
 *
 * 包装原始 PropertySource，拦截所有属性读取
 * 如果属性值是占位符或加密格式，自动解密
 *
 * @author Database Password Manager
 * @version 6.0
 */
public class DbPwdEncryptablePropertySource extends PropertySource<Object> {
    
    private final PropertySource<?> delegate;
    private final DbPwdPropertyResolver resolver;
    
    /**
     * 构造函数
     *
     * @param delegate 被装饰的原始 PropertySource
     * @param resolver 属性解析器
     */
    public DbPwdEncryptablePropertySource(PropertySource<?> delegate, DbPwdPropertyResolver resolver) {
        super(delegate.getName());
        this.delegate = delegate;
        this.resolver = resolver;
    }
    
    /**
     * 拦截属性读取，自动解密
     */
    @Override
    public Object getProperty(String name) {
        Object value = delegate.getProperty(name);
        
        if (value instanceof String) {
            String strValue = (String) value;
            // 仅针对密码相关的属性进行解密
            if (isPasswordProperty(name)) {
                String resolved = resolver.resolvePropertyValue(strValue);
                if (resolved != null && !resolved.equals(strValue)) {
                    return resolved;
                }
            }
        }
        
        return value;
    }
    
    /**
     * 判断是否是密码相关的属性
     */
    private boolean isPasswordProperty(String name) {
        String lowerName = name.toLowerCase();
        return lowerName.contains("password") || 
               lowerName.contains("pwd") ||
               lowerName.contains("secret") ||
               lowerName.contains("key");
    }
    
    /**
     * 获取被装饰的原始 PropertySource
     */
    public PropertySource<?> getDelegate() {
        return delegate;
    }
}
