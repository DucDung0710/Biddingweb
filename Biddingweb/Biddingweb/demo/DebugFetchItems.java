import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DebugFetchItems {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://mysql-7603b93-vnu-d637.l.aivencloud.com:20763/biddingdb?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
        String user = "avnadmin";
        String pass = "AVNS_iQ9fobJ2RsRXOaJVIKQ";
        try (Connection c = DriverManager.getConnection(url, user, pass);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, name, description, type, status, seller_id, firstprice FROM items ORDER BY id DESC LIMIT 50")) {
            while (rs.next()) {
                System.out.printf("%d|%s|%s|%s|%s|%d|%f%n",
                        rs.getInt("id"), rs.getString("name"), rs.getString("description"), rs.getString("type"), rs.getString("status"), rs.getInt("seller_id"), rs.getDouble("firstprice"));
            }
        }
    }
}
