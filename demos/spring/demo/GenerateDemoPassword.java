import com.dbpwd.crypto.Sm4GCMCipher;

/**
 * 辅工具类 - 生成Demo用的ENC格式密码
 *
 * 使用方法：
 * javac -cp ../db-pwd-core/target/classes GenerateDemoPassword.java
 * java -cp .;../db-pwd-core/target/classes GenerateDemoPassword
 */
public class GenerateDemoPassword {
    public static void main(String[] args) throws Exception {
        // 测试密钥（16字节）
        String sm4Key = "000102030405060708090a0b0c0d0e0f";

        // 明文密码
        String plainPassword = "sa";

        // 加密
        String encryptedPassword = Sm4GCMCipher.encrypt(plainPassword, sm4Key);

        // 生成ENC格式
        String encPassword = "ENC(SM4-GCM:" + encryptedPassword + ")";

        System.out.println("Plain password: " + plainPassword);
        System.out.println("SM4 Key: " + sm4Key);
        System.out.println("Encrypted password: " + encryptedPassword);
        System.out.println("ENC format password: " + encPassword);

        System.out.println();
        System.out.println("Copy this ENC password to your DataSource configuration:");
        System.out.println(encPassword);
    }
}
