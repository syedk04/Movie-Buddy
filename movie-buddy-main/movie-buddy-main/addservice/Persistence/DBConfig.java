package ryerson.ca.addservice.Persistence;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConfig {
    private static final String URL;
    private static final String USERNAME;
    private static final String PASSWORD;

    static {
        Properties props = new Properties();
        try (InputStream input = DBConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("❌ config.properties not found in classpath");
            }
            props.load(input);

            URL = props.getProperty("db.url");
            USERNAME = props.getProperty("db.username");
            PASSWORD = props.getProperty("db.password");

        } catch (Exception e) {
            throw new RuntimeException("❌ Could not load database configuration: " + e.getMessage(), e);
        }
    }

     public static Connection getCon() throws ClassNotFoundException, SQLException{
         Connection con=null;
         try {
             Class.forName("com.mysql.cj.jdbc.Driver");
             String connection = System.getenv("DB_URL");
             con=DriverManager.getConnection("jdbc:mysql://"+connection+"/ADDMOVIE_DB?allowPublicKeyRetrieval=true&useSSL=false", "root", "student" );
             
             System.out.println("connection establshed");
         
         } catch (Exception e) {
             System.out.println(e);      
         }
       return con;
    }
}