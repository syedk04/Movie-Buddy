package ryerson.ca.addservice.Persistence;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionPool {

    private static final Logger logger = Logger.getLogger(ConnectionPool.class.getName());
    private static final int POOL_SIZE = 5;
    private static final String DB_NAME = "ADDMOVIE_DB";
    private static final BlockingQueue<Connection> pool = new ArrayBlockingQueue<>(POOL_SIZE);
    private static volatile boolean initialized = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL JDBC driver not found on classpath");
        }
    }

    private static synchronized void ensureInitialized() {
        if (initialized) return;
        for (int i = 0; i < POOL_SIZE; i++) {
            try {
                pool.offer(newRealConnection());
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to pre-create pool connection {0}", i);
            }
        }
        initialized = true;
    }

    public static Connection getCon() throws SQLException {
        ensureInitialized();
        try {
            Connection conn = pool.poll(5, TimeUnit.SECONDS);
            if (conn == null) {
                logger.warning("Connection pool exhausted; creating temporary connection");
                return newRealConnection();
            }
            if (!isValid(conn)) {
                conn = newRealConnection();
            }
            return wrap(conn);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a connection from pool", e);
        }
    }

    private static Connection wrap(final Connection real) {
        return (Connection) Proxy.newProxyInstance(
            ConnectionPool.class.getClassLoader(),
            new Class[]{Connection.class},
            (proxy, method, args) -> {
                if ("close".equals(method.getName())) {
                    if (isValid(real)) {
                        pool.offer(real);
                    }
                    return null;
                }
                try {
                    return method.invoke(real, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        );
    }

    private static boolean isValid(Connection conn) {
        try {
            return conn != null && !conn.isClosed() && conn.isValid(1);
        } catch (SQLException e) {
            return false;
        }
    }

    private static Connection newRealConnection() throws SQLException {
        String host = System.getenv("DB_URL");
        String user = System.getenv("DB_USERNAME");
        String pass = System.getenv("DB_PASSWORD");
        if (user == null || user.isEmpty()) user = "root";
        if (pass == null || pass.isEmpty()) pass = "student";
        String url = "jdbc:mysql://" + host + "/" + DB_NAME
                + "?allowPublicKeyRetrieval=true&useSSL=false";
        return DriverManager.getConnection(url, user, pass);
    }
}
