package HospitalManagementSystem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DebugDumpUsers {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = DBConfig.getUrl();
        String user = DBConfig.getUser();
        String pass = DBConfig.getPass();
        try (Connection conn = DriverManager.getConnection(url, user, pass)){
            try (PreparedStatement ps = conn.prepareStatement("SELECT id, username, password, role, approved FROM users")){
                try (ResultSet rs = ps.executeQuery()){
                    System.out.println("Users table contents:");
                    while(rs.next()){
                        System.out.printf("id=%d username=%s password=%s role=%s approved=%d\n", rs.getInt("id"), rs.getString("username"), rs.getString("password"), rs.getString("role"), rs.getInt("approved"));
                    }
                }
            }
        }
    }
}
