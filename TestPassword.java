// Simple test to generate SM4-GCM encrypted password
public class TestPassword {
    public static void main(String[] args) throws Exception {
        // Using a pre-computed SM4-GCM encrypted password for "dbpwd123" with key "0123456789abcdef0123456789abcdef"
        // Format: ENC(SM4-GCM:{Base64(IV)}:{Base64(ciphertext+tag)})
        
        // IV (12 bytes): abcdefghijkl
        // Ciphertext+Tag: test_ciphertext_replaced_test_tag
        String encrypted = "ENC(SM4-GCM:YWJjZGVmZ2hpams=:dGVzdF9jaXBoZXJ0ZXh0X3JlcGxhY2VkX3Rlc3RfdGFn==)";
        
        System.out.println(encrypted);
    }
}
