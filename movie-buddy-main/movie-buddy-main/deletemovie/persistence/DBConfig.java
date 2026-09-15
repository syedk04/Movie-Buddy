package ryerson.ca.deletemovie.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConfig {

    private static final Logger logger = Logger.getLogger(DBConfig.class.getName());

    public static Connection getCon() {
        try {
            return ConnectionPool.getCon();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to obtain database connection", e);
            return null;
        }
    }
}
