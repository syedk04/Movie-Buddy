package ryerson.ca.addservice.Persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConfig {

    private static final Logger logger = Logger.getLogger(DBConfig.class.getName());

    public static Connection getCon() throws ClassNotFoundException, SQLException {
        return ConnectionPool.getCon();
    }
}
