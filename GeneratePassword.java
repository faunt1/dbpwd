import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.crypto.ENCFormat;

public class GeneratePassword {
    public static void main(String[] args) throws Exception {
        String SM4_KEY = "0123456789abcdef0123456789abcdef";
        String plainPassword = "dbpwd123";

        String cipherText = Sm4GCMCipher.encrypt(plainPassword, SM4_KEY);
        String formattedEncrypted = ENCFormat.formatSM4GCM(cipherText);

        System.out.println(formattedEncrypted);
    }
}
