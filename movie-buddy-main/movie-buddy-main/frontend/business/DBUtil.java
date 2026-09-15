package ryerson.ca.business;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBUtil {

    private static final Logger logger = Logger.getLogger(DBUtil.class.getName());

    private static final String DB_HOST = System.getenv("DB_URL");
    private static final String DB_NAME = "FrontendDB";
    private static final String DB_USER =
            System.getenv("DB_USERNAME") != null ? System.getenv("DB_USERNAME") : "root";
    private static final String DB_PASSWORD =
            System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "student";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found", e);
        }
        String url = "jdbc:mysql://" + DB_HOST + "/" + DB_NAME
                + "?allowPublicKeyRetrieval=true&useSSL=false";
        return DriverManager.getConnection(url, DB_USER, DB_PASSWORD);
    }
}
