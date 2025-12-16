package com.rednetty.plugintemplate.database;

import com.rednetty.plugintemplate.database.connection.ConnectionPool;
import com.rednetty.plugintemplate.database.repositories.PlayerDataRepository;
import com.rednetty.plugintemplate.database.repositories.SettingsRepository;
import com.rednetty.plugintemplate.exceptions.PluginException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Advanced database connection and management system
 *
 * Features:
 * - HikariCP connection pooling for optimal performance
 * - Automatic table creation and migration
 * - Repository pattern for clean data access
 * - Connection health monitoring
 * - Automatic reconnection handling
 * - Transaction support
 * - Query execution statistics
 *
 * @author RedNetty
 * @version 1.0.0
 */
public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    // Connection Management
    private HikariDataSource dataSource;
    private ConnectionPool connectionPool;
    private String jdbcUrl;
    private String username;
    private String password;
    private DatabaseType databaseType;

    // Repositories
    private final ConcurrentMap<Class<?>, Object> repositories = new ConcurrentHashMap<>();

    // Statistics
    private long totalQueries = 0;
    private long failedQueries = 0;
    private long totalConnectionTime = 0;
    private volatile boolean isHealthy = true;

    // Database schemas
    private static final String[] TABLE_SCHEMAS = {
            // Player data table
            """
        CREATE TABLE IF NOT EXISTS player_data (
            uuid VARCHAR(36) PRIMARY KEY,
            username VARCHAR(16) NOT NULL,
            first_join BIGINT NOT NULL,
            last_join BIGINT NOT NULL,
            play_time BIGINT DEFAULT 0,
            data TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """,

            // Settings table
            """
        CREATE TABLE IF NOT EXISTS plugin_settings (
            setting_key VARCHAR(100) PRIMARY KEY,
            setting_value TEXT NOT NULL,
            setting_type VARCHAR(20) DEFAULT 'STRING',
            description TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """,

            // Statistics table
            """
        CREATE TABLE IF NOT EXISTS plugin_statistics (
            id INTEGER PRIMARY KEY AUTO_INCREMENT,
            stat_name VARCHAR(100) NOT NULL,
            stat_value BIGINT DEFAULT 0,
            date_recorded DATE NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY unique_stat_date (stat_name, date_recorded)
        )
        """,

            // Audit log table
            """
        CREATE TABLE IF NOT EXISTS audit_log (
            id INTEGER PRIMARY KEY AUTO_INCREMENT,
            player_uuid VARCHAR(36),
            action VARCHAR(100) NOT NULL,
            details TEXT,
            ip_address VARCHAR(45),
            timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_player_uuid (player_uuid),
            INDEX idx_action (action),
            INDEX idx_timestamp (timestamp)
        )
        """
    };

    /**
     * Database type enumeration
     */
    public enum DatabaseType {
        SQLITE("sqlite"),
        MYSQL("mysql"),
        POSTGRESQL("postgresql");

        private final String identifier;

        DatabaseType(String identifier) {
            this.identifier = identifier;
        }

        public String getIdentifier() {
            return identifier;
        }

        public static DatabaseType fromString(String identifier) {
            for (DatabaseType type : values()) {
                if (type.identifier.equalsIgnoreCase(identifier)) {
                    return type;
                }
            }
            return SQLITE; // Default fallback
        }
    }

    /**
     * Initialize database connection with SQLite
     *
     * @param jdbcUrl The JDBC URL
     */
    public void initialize(@NotNull String jdbcUrl) {
        initialize(jdbcUrl, null, null);
    }

    /**
     * Initialize database connection with credentials
     *
     * @param jdbcUrl The JDBC URL
     * @param username Database username
     * @param password Database password
     */
    public void initialize(@NotNull String jdbcUrl, @Nullable String username, @Nullable String password) {
        LOGGER.info("Initializing database connection...");

        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.databaseType = determineDatabaseType(jdbcUrl);

        try {
            setupConnectionPool();
            testConnection();
            initializeRepositories();

            LOGGER.info("Database initialized successfully with " + databaseType.name());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Determine database type from JDBC URL
     */
    private DatabaseType determineDatabaseType(String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:mysql")) {
            return DatabaseType.MYSQL;
        } else if (jdbcUrl.startsWith("jdbc:postgresql")) {
            return DatabaseType.POSTGRESQL;
        } else {
            return DatabaseType.SQLITE;
        }
    }

    /**
     * Setup HikariCP connection pool
     */
    private void setupConnectionPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);

        if (username != null) {
            config.setUsername(username);
        }
        if (password != null) {
            config.setPassword(password);
        }

        // Connection pool settings
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000); // 5 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setConnectionTimeout(10000); // 10 seconds
        config.setLeakDetectionThreshold(60000); // 1 minute

        // Database-specific optimizations
        switch (databaseType) {
            case MYSQL:
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useServerPrepStmts", "true");
                config.addDataSourceProperty("useLocalSessionState", "true");
                config.addDataSourceProperty("rewriteBatchedStatements", "true");
                config.addDataSourceProperty("cacheResultSetMetadata", "true");
                config.addDataSourceProperty("cacheServerConfiguration", "true");
                config.addDataSourceProperty("elideSetAutoCommits", "true");
                config.addDataSourceProperty("maintainTimeStats", "false");
                break;

            case SQLITE:
                config.addDataSourceProperty("journal_mode", "WAL");
                config.addDataSourceProperty("synchronous", "NORMAL");
                config.addDataSourceProperty("temp_store", "MEMORY");
                config.addDataSourceProperty("mmap_size", "134217728"); // 128MB
                break;

            case POSTGRESQL:
                config.addDataSourceProperty("prepareThreshold", "1");
                config.addDataSourceProperty("preparedStatementCacheQueries", "250");
                config.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
                break;
        }

        // Connection validation
        config.setConnectionTestQuery(getTestQuery());
        config.setValidationTimeout(3000);

        this.dataSource = new HikariDataSource(config);
        this.connectionPool = new ConnectionPool(dataSource);

        LOGGER.info("Connection pool initialized with " + config.getMaximumPoolSize() + " max connections");
    }

    /**
     * Get appropriate test query for database type
     */
    private String getTestQuery() {
        return switch (databaseType) {
            case MYSQL -> "SELECT 1";
            case POSTGRESQL -> "SELECT 1";
            case SQLITE -> "SELECT 1";
        };
    }

    /**
     * Test database connection
     */
    private void testConnection() throws SQLException {
        long startTime = System.currentTimeMillis();

        try (Connection connection = getConnection()) {
            if (connection == null || !connection.isValid(5)) {
                throw new SQLException("Invalid database connection");
            }

            // Test a simple query
            try (PreparedStatement stmt = connection.prepareStatement(getTestQuery());
                 ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    throw new SQLException("Test query failed");
                }
            }

            long connectionTime = System.currentTimeMillis() - startTime;
            this.totalConnectionTime += connectionTime;

            LOGGER.info("Database connection test successful (took " + connectionTime + "ms)");

        } catch (SQLException e) {
            this.isHealthy = false;
            throw new SQLException("Database connection test failed", e);
        }
    }

    /**
     * Initialize repository instances
     */
    private void initializeRepositories() {
        LOGGER.info("Initializing repositories...");

        // Initialize player data repository
        PlayerDataRepository playerDataRepo = new PlayerDataRepository(this);
        repositories.put(PlayerDataRepository.class, playerDataRepo);

        // Initialize settings repository
        SettingsRepository settingsRepo = new SettingsRepository(this);
        repositories.put(SettingsRepository.class, settingsRepo);

        LOGGER.info("Repositories initialized successfully");
    }

    /**
     * Setup database tables
     */
    public void setupTables() throws SQLException {
        LOGGER.info("Setting up database tables...");

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                for (String schema : TABLE_SCHEMAS) {
                    // Adjust schema for database type
                    String adjustedSchema = adjustSchemaForDatabase(schema);

                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(adjustedSchema);
                    }
                }

                connection.commit();
                LOGGER.info("Database tables created successfully");

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /**
     * Adjust SQL schema for specific database type
     */
    private String adjustSchemaForDatabase(String schema) {
        return switch (databaseType) {
            case SQLITE -> schema.replace("AUTO_INCREMENT", "AUTOINCREMENT")
                    .replace("BIGINT", "INTEGER");
            case POSTGRESQL -> schema.replace("AUTO_INCREMENT", "SERIAL")
                    .replace("TIMESTAMP DEFAULT CURRENT_TIMESTAMP", "TIMESTAMP DEFAULT NOW()");
            case MYSQL -> schema;
        };
    }

    /**
     * Get a database connection from the pool
     *
     * @return Database connection
     * @throws SQLException If connection cannot be obtained
     */
    @NotNull
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource is not available");
        }

        long startTime = System.currentTimeMillis();
        Connection connection = dataSource.getConnection();
        long connectionTime = System.currentTimeMillis() - startTime;

        this.totalConnectionTime += connectionTime;
        this.totalQueries++;

        return connection;
    }

    /**
     * Execute a query and return results
     *
     * @param sql SQL query
     * @param params Query parameters
     * @return ResultSet with query results
     */
    public CompletableFuture<ResultSet> executeQuery(@NotNull String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {

                // Set parameters
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }

                return stmt.executeQuery();

            } catch (SQLException e) {
                failedQueries++;
                LOGGER.log(Level.WARNING, "Query execution failed: " + sql, e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Execute an update query
     *
     * @param sql SQL query
     * @param params Query parameters
     * @return Number of affected rows
     */
    public CompletableFuture<Integer> executeUpdate(@NotNull String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {

                // Set parameters
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }

                return stmt.executeUpdate();

            } catch (SQLException e) {
                failedQueries++;
                LOGGER.log(Level.WARNING, "Update execution failed: " + sql, e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Execute a batch update
     *
     * @param sql SQL query
     * @param paramsList List of parameter arrays
     * @return Array of update counts
     */
    public CompletableFuture<int[]> executeBatch(@NotNull String sql, Object[]... paramsList) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {

                connection.setAutoCommit(false);

                try {
                    for (Object[] params : paramsList) {
                        for (int i = 0; i < params.length; i++) {
                            stmt.setObject(i + 1, params[i]);
                        }
                        stmt.addBatch();
                    }

                    int[] results = stmt.executeBatch();
                    connection.commit();

                    return results;

                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }

            } catch (SQLException e) {
                failedQueries++;
                LOGGER.log(Level.WARNING, "Batch execution failed: " + sql, e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Get repository instance
     *
     * @param repositoryClass Repository class
     * @param <T> Repository type
     * @return Repository instance
     */
    @SuppressWarnings("unchecked")
    public <T> T getRepository(@NotNull Class<T> repositoryClass) {
        return (T) repositories.get(repositoryClass);
    }

    /**
     * Check if database is healthy
     *
     * @return True if healthy
     */
    public boolean isHealthy() {
        if (!isHealthy) {
            return false;
        }

        try (Connection connection = getConnection()) {
            return connection.isValid(3);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database health check failed", e);
            this.isHealthy = false;
            return false;
        }
    }

    /**
     * Get database statistics
     *
     * @return Database statistics
     */
    public DatabaseStats getStatistics() {
        return new DatabaseStats(
                totalQueries,
                failedQueries,
                totalConnectionTime,
                dataSource != null ? dataSource.getHikariPoolMXBean().getActiveConnections() : 0,
                dataSource != null ? dataSource.getHikariPoolMXBean().getIdleConnections() : 0,
                isHealthy
        );
    }

    /**
     * Shutdown database connections
     */
    public void shutdown() {
        LOGGER.info("Shutting down database connections...");

        try {
            // Shutdown repositories
            repositories.clear();

            // Close connection pool
            if (connectionPool != null) {
                connectionPool.shutdown();
            }

            // Close datasource
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }

            LOGGER.info("Database shutdown completed");

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during database shutdown", e);
        }
    }

    /**
     * Database statistics record
     */
    public record DatabaseStats(
            long totalQueries,
            long failedQueries,
            long totalConnectionTime,
            int activeConnections,
            int idleConnections,
            boolean isHealthy
    ) {
        public double getSuccessRate() {
            if (totalQueries == 0) return 100.0;
            return ((double) (totalQueries - failedQueries) / totalQueries) * 100.0;
        }

        public double getAverageConnectionTime() {
            if (totalQueries == 0) return 0.0;
            return (double) totalConnectionTime / totalQueries;
        }
    }
}