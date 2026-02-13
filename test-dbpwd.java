import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.crypto.ENCFormat;

public class TestDbpwd {
    public static void main(String[] args) {
        try {
            Sm4KeyLoader loader = new Sm4KeyLoader();
            String sm4Key = loader.loadKey(true);
            System.out.println("SM4 Key loaded");

            PasswordFileManager manager = PasswordFileManager.loadFromFile(System.getProperty("user.home") + "/.dbpwd/db.pwd");

            var result = manager.getById("druid_demo_db");
            if (result == null) {
                System.out.println("druid_demo_db NOT FOUND");
            } else {
                System.out.println("druid_demo_db FOUND: " + result.getName());
            }

            // Try simple_jdbc_db
            result = manager.getById("simple_jdbc_db");
            if (result == null) {
                System.out.println("simple_jdbc_db NOT FOUND");
            } else {
                System.out.println("simple_jdbc_db FOUND: " + result.getName());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
