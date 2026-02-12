package com.dbpwd.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebPasswordUtil单元测试
 */
public class WebPasswordUtilTest {

    @Test
    public void testPasswordServiceKeyConstant() {
        assertEquals("dbpwd.password.service", WebPasswordUtil.PASSWORD_SERVICE_KEY);
    }

    @Test
    public void testDefaultKeyPathConstant() {
        String expected = System.getProperty("user.home") + "/.dbpwd/sm4.key";
        assertEquals(expected, WebPasswordUtil.DEFAULT_KEY_PATH);
    }

    @Test
    public void testDefaultPwdPathConstant() {
        String expected = System.getProperty("user.home") + "/.dbpwd/db.pwd";
        assertEquals(expected, WebPasswordUtil.DEFAULT_PWD_PATH);
    }

    @Test
    public void testConstantsNotEmpty() {
        assertNotNull(WebPasswordUtil.PASSWORD_SERVICE_KEY);
        assertNotNull(WebPasswordUtil.DEFAULT_KEY_PATH);
        assertNotNull(WebPasswordUtil.DEFAULT_PWD_PATH);

        assertFalse(WebPasswordUtil.PASSWORD_SERVICE_KEY.isEmpty());
        assertFalse(WebPasswordUtil.DEFAULT_KEY_PATH.isEmpty());
        assertFalse(WebPasswordUtil.DEFAULT_PWD_PATH.isEmpty());
    }

    @Test
    public void testPathContainsDirectory() {
        assertTrue(WebPasswordUtil.DEFAULT_KEY_PATH.contains(".dbpwd"));
        assertTrue(WebPasswordUtil.DEFAULT_KEY_PATH.contains("sm4.key"));

        assertTrue(WebPasswordUtil.DEFAULT_PWD_PATH.contains(".dbpwd"));
        assertTrue(WebPasswordUtil.DEFAULT_PWD_PATH.contains("db.pwd"));
    }
}
