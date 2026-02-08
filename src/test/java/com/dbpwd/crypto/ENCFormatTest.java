package com.dbpwd.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ENCFormat单元测试
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class ENCFormatTest {

    private static final String VALID_SM4_PASSWORD = "ENC(SM4:r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)";
    private static final String VALID_DES_PASSWORD = "ENC(DES:AABBCCDD1234567890ABCDEF)";
    private static final String INVALID_NO_ENC_PREFIX = "plainPassword";
    private static final String INVALID_MALFORMED = "ENC(SM4)";
    private static final String VALID_CIPHER_TEXT = "r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=";

    @Test
    public void testIsEncrypted() {
        assertTrue(ENCFormat.isEncrypted(VALID_SM4_PASSWORD), "ENC格式应被识别为加密");
        assertTrue(ENCFormat.isEncrypted(VALID_DES_PASSWORD), "ENC格式应被识别为加密");
        assertFalse(ENCFormat.isEncrypted(INVALID_NO_ENC_PREFIX), "明文密码不应被识别为加密");
        assertFalse(ENCFormat.isEncrypted(null), "null不应被识别为加密");
        assertFalse(ENCFormat.isEncrypted(""), "空字符串不应被识别为加密");
    }

    @Test
    public void testExtractAlgorithm() {
        assertEquals("SM4", ENCFormat.extractAlgorithm(VALID_SM4_PASSWORD), "SM4算法应被正确提取");
        assertEquals("DES", ENCFormat.extractAlgorithm(VALID_DES_PASSWORD), "DES算法应被正确提取");
        assertNull(ENCFormat.extractAlgorithm(INVALID_NO_ENC_PREFIX), "非ENC格式应返回null");
        assertNull(ENCFormat.extractAlgorithm(INVALID_MALFORMED), "格式错误时应返回null");
    }

    @Test
    public void testExtractCipherText() {
        assertEquals(VALID_CIPHER_TEXT, ENCFormat.extractCipherText(VALID_SM4_PASSWORD), "密文应被正确提取");
        assertEquals("AABBCCDD1234567890ABCDEF", ENCFormat.extractCipherText(VALID_DES_PASSWORD), "密文应被正确提取");
        assertNull(ENCFormat.extractCipherText(INVALID_NO_ENC_PREFIX), "非ENC格式应返回null");
        assertNull(ENCFormat.extractCipherText(INVALID_MALFORMED), "格式错误时应返回null");
    }

    @Test
    public void testFormatEncrypted() {
        String formatted = ENCFormat.formatEncrypted("SM4", VALID_CIPHER_TEXT);
        assertEquals("ENC(SM4:" + VALID_CIPHER_TEXT + ")", formatted, "格式化结果应正确");
    }

    @Test
    public void testFormatEncryptedWithNullAlgorithm() {
        assertThrows(IllegalArgumentException.class, () -> {
            ENCFormat.formatEncrypted(null, VALID_CIPHER_TEXT);
        });
    }

    @Test
    public void testFormatEncryptedWithNullCipherText() {
        assertThrows(IllegalArgumentException.class, () -> {
            ENCFormat.formatEncrypted("SM4", null);
        });
    }

    @Test
    public void testFormatSM4() {
        String formatted = ENCFormat.formatSM4(VALID_CIPHER_TEXT);
        assertEquals("ENC(SM4:" + VALID_CIPHER_TEXT + ")", formatted, "SM4格式化应正确");
    }

    @Test
    public void testIsSM4Encrypted() {
        assertTrue(ENCFormat.isSM4Encrypted(VALID_SM4_PASSWORD), "SM4加密应被识别");
        assertFalse(ENCFormat.isSM4Encrypted(VALID_DES_PASSWORD), "DES加密不应被识别为SM4");
        assertFalse(ENCFormat.isSM4Encrypted(INVALID_NO_ENC_PREFIX), "明文不应被识别为SM4");
    }

    @Test
    public void testIsValid() {
        assertTrue(ENCFormat.isValid(VALID_SM4_PASSWORD), "有效的ENC格式应返回true");
        assertTrue(ENCFormat.isValid(VALID_DES_PASSWORD), "有效的ENC格式应返回true");
        assertFalse(ENCFormat.isValid(INVALID_NO_ENC_PREFIX), "非ENC格式应返回false");
        assertFalse(ENCFormat.isValid(INVALID_MALFORMED), "格式错误应返回false");
        assertFalse(ENCFormat.isValid(null), "null应返回false");
        assertFalse(ENCFormat.isValid(""), "空字符串应返回false");
    }

    @Test
    public void testFormatThenParseRoundTrip() {
        String algorithm = "SM4";
        String cipherText = "someCipherTextValue123";

        String formatted = ENCFormat.formatEncrypted(algorithm, cipherText);

        assertEquals(algorithm, ENCFormat.extractAlgorithm(formatted), "算法应正确");
        assertEquals(cipherText, ENCFormat.extractCipherText(formatted), "密文应正确");
    }

    @Test
    public void testSpecialCharactersInCipherText() {
        String specialCipher = "aBc123!@#$%^&*()_+-=[]{}|;':\",./<>?";
        String formatted = ENCFormat.formatSM4(specialCipher);
        String extracted = ENCFormat.extractCipherText(formatted);
        assertEquals(specialCipher, extracted, "特殊字符应被正确保留");
    }
}
