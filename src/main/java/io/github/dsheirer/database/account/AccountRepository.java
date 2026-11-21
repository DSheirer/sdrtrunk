/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.database.account;

import io.github.dsheirer.database.SqlSupport;
import io.github.dsheirer.database.dialect.IDatabaseDialect;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Web API accounts, API tokens and the audit log, stored in the sdrtrunk schema of the call database.
 *
 * Usernames are stored in lower case so that sign-in isn't case sensitive.
 */
public class AccountRepository
{
    public static final String USER_TABLE = "web_user";
    public static final String TOKEN_TABLE = "web_api_token";
    public static final String AUDIT_TABLE = "web_audit";
    public static final int LENGTH_USERNAME = 64;
    public static final int LENGTH_NAME = 128;
    public static final int LENGTH_ROLE = 16;
    public static final int LENGTH_HASH = 256;
    public static final int LENGTH_TOKEN_HASH = 64;
    public static final int LENGTH_PREFIX = 16;
    public static final int LENGTH_PERMISSIONS = 1024;
    public static final int LENGTH_ACTION = 64;
    public static final int LENGTH_TARGET = 255;
    public static final int LENGTH_ADDRESS = 64;
    public static final int LENGTH_DETAIL = 1024;

    private static final String USER_COLUMNS = "username, display_name, role, password_hash, must_change_password, " +
        "enabled, created_time, last_login_time, failed_logins, locked_until, pending_approval, last_web_time, " +
        "last_api_time";
    private static final String TOKEN_COLUMNS = "user_id, name, token_hash, prefix, permissions, created_time, " +
        "expires_time, last_used_time";
    private static final String AUDIT_COLUMNS = "event_time, username, action, target, success, address, detail";

    private final String mUserTable;
    private final String mTokenTable;
    private final String mAuditTable;

    /**
     * Constructs an instance
     * @param schema that holds the tables
     */
    public AccountRepository(String schema)
    {
        mUserTable = schema + "." + USER_TABLE;
        mTokenTable = schema + "." + TOKEN_TABLE;
        mAuditTable = schema + "." + AUDIT_TABLE;
    }

    /**
     * Statements that create the account, token and audit tables.
     */
    public static List<String> getCreateStatements(IDatabaseDialect dialect, String schema)
    {
        String users = schema + "." + USER_TABLE;
        String tokens = schema + "." + TOKEN_TABLE;
        String audit = schema + "." + AUDIT_TABLE;
        List<String> statements = new ArrayList<>();
        statements.add("CREATE TABLE IF NOT EXISTS " + users + " (" +
            "id " + dialect.getIdentityColumn() + ", " +
            "username VARCHAR(" + LENGTH_USERNAME + ") NOT NULL UNIQUE, " +
            "display_name VARCHAR(" + LENGTH_NAME + "), " +
            "role VARCHAR(" + LENGTH_ROLE + ") NOT NULL, " +
            "password_hash VARCHAR(" + LENGTH_HASH + ") NOT NULL, " +
            "must_change_password BOOLEAN NOT NULL, " +
            "enabled BOOLEAN NOT NULL, " +
            "created_time BIGINT NOT NULL, " +
            "last_login_time BIGINT, " +
            "failed_logins INT NOT NULL, " +
            "locked_until BIGINT, " +
            "pending_approval BOOLEAN DEFAULT FALSE NOT NULL, " +
            "last_web_time BIGINT, " +
            "last_api_time BIGINT)");
        statements.add("CREATE TABLE IF NOT EXISTS " + tokens + " (" +
            "id " + dialect.getIdentityColumn() + ", " +
            "user_id BIGINT NOT NULL REFERENCES " + users + "(id) ON DELETE CASCADE, " +
            "name VARCHAR(" + LENGTH_NAME + "), " +
            "token_hash VARCHAR(" + LENGTH_TOKEN_HASH + ") NOT NULL UNIQUE, " +
            "prefix VARCHAR(" + LENGTH_PREFIX + "), " +
            "permissions VARCHAR(" + LENGTH_PERMISSIONS + "), " +
            "created_time BIGINT NOT NULL, " +
            "expires_time BIGINT, " +
            "last_used_time BIGINT)");
        statements.add("CREATE INDEX IF NOT EXISTS web_api_token_user_idx ON " + tokens + " (user_id)");
        statements.add("CREATE TABLE IF NOT EXISTS " + audit + " (" +
            "id " + dialect.getIdentityColumn() + ", " +
            "event_time BIGINT NOT NULL, " +
            "username VARCHAR(" + LENGTH_USERNAME + "), " +
            "action VARCHAR(" + LENGTH_ACTION + ") NOT NULL, " +
            "target VARCHAR(" + LENGTH_TARGET + "), " +
            "success BOOLEAN NOT NULL, " +
            "address VARCHAR(" + LENGTH_ADDRESS + "), " +
            "detail VARCHAR(" + LENGTH_DETAIL + "))");
        statements.add("CREATE INDEX IF NOT EXISTS web_audit_time_idx ON " + audit + " (event_time)");
        return statements;
    }

    //----------------------------------------------------------------------------------------------------------------
    // Users
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Count of accounts
     */
    public long countUsers(Connection connection) throws SQLException
    {
        return count(connection, "SELECT COUNT(*) FROM " + mUserTable);
    }

    /**
     * Count of enabled accounts with the role
     */
    public long countEnabled(Connection connection, String role) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + mUserTable +
            " WHERE role = ? AND enabled = ?"))
        {
            statement.setString(1, role);
            statement.setBoolean(2, true);

            try(ResultSet rs = statement.executeQuery())
            {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    /**
     * Stores a new account.
     * @return the account with its database id
     * @throws SQLException on error, including a duplicate username
     */
    public UserRecord insertUser(Connection connection, UserRecord user) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("INSERT INTO " + mUserTable + " (" +
            USER_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new String[]{"id"}))
        {
            bindUser(statement, user);
            statement.executeUpdate();

            try(ResultSet keys = statement.getGeneratedKeys())
            {
                if(keys.next())
                {
                    return user.withId(keys.getLong(1));
                }
            }
        }

        throw new SQLException("Database didn't return the id of the new account");
    }

    /**
     * Count of accounts that wait for an administrator's approval
     */
    public long countPending(Connection connection) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + mUserTable +
            " WHERE pending_approval = ?"))
        {
            statement.setBoolean(1, true);

            try(ResultSet rs = statement.executeQuery())
            {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    /**
     * Updates every column of the account except the username, creation time and last access times, which
     * {@link #touchUser} updates.
     * @return true if the account exists
     */
    public boolean updateUser(Connection connection, UserRecord user) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("UPDATE " + mUserTable + " SET " +
            "display_name = ?, role = ?, password_hash = ?, must_change_password = ?, enabled = ?, " +
            "last_login_time = ?, failed_logins = ?, locked_until = ?, pending_approval = ? WHERE id = ?"))
        {
            SqlSupport.setString(statement, 1, user.displayName(), LENGTH_NAME);
            statement.setString(2, user.role());
            statement.setString(3, user.passwordHash());
            statement.setBoolean(4, user.mustChangePassword());
            statement.setBoolean(5, user.enabled());
            SqlSupport.setLong(statement, 6, user.lastLoginTime());
            statement.setInt(7, user.failedLogins());
            SqlSupport.setLong(statement, 8, user.lockedUntil());
            statement.setBoolean(9, user.pendingApproval());
            statement.setLong(10, user.id());
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Records the time of the account's last request from a signed-in browser or with an API token.
     * @param api true for an API token request, false for a browser request
     */
    public void touchUser(Connection connection, long id, boolean api, long time) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("UPDATE " + mUserTable + " SET " +
            (api ? "last_api_time" : "last_web_time") + " = ? WHERE id = ?"))
        {
            statement.setLong(1, time);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    /**
     * Deletes the account and its API tokens.
     * @return true if the account existed
     */
    public boolean deleteUser(Connection connection, long id) throws SQLException
    {
        //Delete the tokens explicitly as well as through the foreign key, in case a server doesn't cascade
        deleteTokens(connection, id);

        try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + mUserTable + " WHERE id = ?"))
        {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    public Optional<UserRecord> findUser(Connection connection, long id) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("SELECT id, " + USER_COLUMNS + " FROM " +
            mUserTable + " WHERE id = ?"))
        {
            statement.setLong(1, id);
            return readUser(statement);
        }
    }

    /**
     * Finds an account by username, ignoring case.
     */
    public Optional<UserRecord> findUser(Connection connection, String username) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("SELECT id, " + USER_COLUMNS + " FROM " +
            mUserTable + " WHERE username = ?"))
        {
            statement.setString(1, normalize(username));
            return readUser(statement);
        }
    }

    /**
     * Accounts in id order.
     * @param afterId only accounts with a larger id (0 for the first page)
     * @param limit maximum count
     */
    public List<UserRecord> listUsers(Connection connection, long afterId, int limit) throws SQLException
    {
        List<UserRecord> users = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement("SELECT id, " + USER_COLUMNS + " FROM " +
            mUserTable + " WHERE id > ? ORDER BY id"))
        {
            statement.setLong(1, afterId);
            statement.setMaxRows(limit);

            try(ResultSet rs = statement.executeQuery())
            {
                while(rs.next())
                {
                    users.add(readUser(rs));
                }
            }
        }

        return users;
    }

    /**
     * Lower case form of a username as stored
     */
    public static String normalize(String username)
    {
        return username == null ? null : username.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private Optional<UserRecord> readUser(PreparedStatement statement) throws SQLException
    {
        try(ResultSet rs = statement.executeQuery())
        {
            return rs.next() ? Optional.of(readUser(rs)) : Optional.empty();
        }
    }

    private static UserRecord readUser(ResultSet rs) throws SQLException
    {
        return new UserRecord(rs.getLong("id"), rs.getString("username"), rs.getString("display_name"),
            rs.getString("role"), rs.getString("password_hash"), rs.getBoolean("must_change_password"),
            rs.getBoolean("enabled"), rs.getLong("created_time"), SqlSupport.getLong(rs, "last_login_time"),
            rs.getInt("failed_logins"), SqlSupport.getLong(rs, "locked_until"), rs.getBoolean("pending_approval"),
            SqlSupport.getLong(rs, "last_web_time"), SqlSupport.getLong(rs, "last_api_time"));
    }

    private static void bindUser(PreparedStatement statement, UserRecord user) throws SQLException
    {
        statement.setString(1, normalize(user.username()));
        SqlSupport.setString(statement, 2, user.displayName(), LENGTH_NAME);
        statement.setString(3, user.role());
        statement.setString(4, user.passwordHash());
        statement.setBoolean(5, user.mustChangePassword());
        statement.setBoolean(6, user.enabled());
        statement.setLong(7, user.createdTime());
        SqlSupport.setLong(statement, 8, user.lastLoginTime());
        statement.setInt(9, user.failedLogins());
        SqlSupport.setLong(statement, 10, user.lockedUntil());
        statement.setBoolean(11, user.pendingApproval());
        SqlSupport.setLong(statement, 12, user.lastWebTime());
        SqlSupport.setLong(statement, 13, user.lastApiTime());
    }

    //----------------------------------------------------------------------------------------------------------------
    // API tokens
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Stores a new token.
     * @return the token with its database id
     */
    public ApiTokenRecord insertToken(Connection connection, ApiTokenRecord token) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("INSERT INTO " + mTokenTable + " (" +
            TOKEN_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)", new String[]{"id"}))
        {
            statement.setLong(1, token.userId());
            SqlSupport.setString(statement, 2, token.name(), LENGTH_NAME);
            statement.setString(3, token.tokenHash());
            statement.setString(4, token.prefix());
            SqlSupport.setString(statement, 5, token.permissions(), LENGTH_PERMISSIONS);
            statement.setLong(6, token.createdTime());
            SqlSupport.setLong(statement, 7, token.expiresTime());
            SqlSupport.setLong(statement, 8, token.lastUsedTime());
            statement.executeUpdate();

            try(ResultSet keys = statement.getGeneratedKeys())
            {
                if(keys.next())
                {
                    return new ApiTokenRecord(keys.getLong(1), token.userId(), token.name(), token.tokenHash(),
                        token.prefix(), token.permissions(), token.createdTime(), token.expiresTime(),
                        token.lastUsedTime());
                }
            }
        }

        throw new SQLException("Database didn't return the id of the new API token");
    }

    public Optional<ApiTokenRecord> findToken(Connection connection, String tokenHash) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("SELECT id, " + TOKEN_COLUMNS + " FROM " +
            mTokenTable + " WHERE token_hash = ?"))
        {
            statement.setString(1, tokenHash);

            try(ResultSet rs = statement.executeQuery())
            {
                return rs.next() ? Optional.of(readToken(rs)) : Optional.empty();
            }
        }
    }

    public Optional<ApiTokenRecord> findToken(Connection connection, long id) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("SELECT id, " + TOKEN_COLUMNS + " FROM " +
            mTokenTable + " WHERE id = ?"))
        {
            statement.setLong(1, id);

            try(ResultSet rs = statement.executeQuery())
            {
                return rs.next() ? Optional.of(readToken(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Tokens owned by the account, in id order.
     */
    public List<ApiTokenRecord> listTokens(Connection connection, long userId) throws SQLException
    {
        List<ApiTokenRecord> tokens = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement("SELECT id, " + TOKEN_COLUMNS + " FROM " +
            mTokenTable + " WHERE user_id = ? ORDER BY id"))
        {
            statement.setLong(1, userId);

            try(ResultSet rs = statement.executeQuery())
            {
                while(rs.next())
                {
                    tokens.add(readToken(rs));
                }
            }
        }

        return tokens;
    }

    /**
     * Deletes one of the account's tokens.
     * @return true if the token existed and belonged to the account
     */
    public boolean deleteToken(Connection connection, long userId, long tokenId) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + mTokenTable +
            " WHERE id = ? AND user_id = ?"))
        {
            statement.setLong(1, tokenId);
            statement.setLong(2, userId);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Deletes all of the account's tokens.
     * @return count deleted
     */
    public int deleteTokens(Connection connection, long userId) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + mTokenTable +
            " WHERE user_id = ?"))
        {
            statement.setLong(1, userId);
            return statement.executeUpdate();
        }
    }

    /**
     * Records the time that a token was last used.
     */
    public void touchToken(Connection connection, long tokenId, long time) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("UPDATE " + mTokenTable +
            " SET last_used_time = ? WHERE id = ?"))
        {
            statement.setLong(1, time);
            statement.setLong(2, tokenId);
            statement.executeUpdate();
        }
    }

    private static ApiTokenRecord readToken(ResultSet rs) throws SQLException
    {
        return new ApiTokenRecord(rs.getLong("id"), rs.getLong("user_id"), rs.getString("name"),
            rs.getString("token_hash"), rs.getString("prefix"), rs.getString("permissions"),
            rs.getLong("created_time"), SqlSupport.getLong(rs, "expires_time"),
            SqlSupport.getLong(rs, "last_used_time"));
    }

    //----------------------------------------------------------------------------------------------------------------
    // Audit log
    //----------------------------------------------------------------------------------------------------------------

    public void insertAudit(Connection connection, AuditRecord audit) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("INSERT INTO " + mAuditTable + " (" +
            AUDIT_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)"))
        {
            bindAudit(statement, audit);
            statement.executeUpdate();
        }
    }

    /**
     * Audit entries, newest first.
     * @param username only this account's entries, or null for all
     * @param action only this action, or null for all
     * @param since only entries at or after this epoch millisecond time, or null
     * @param until only entries before this epoch millisecond time, or null
     * @param beforeId only entries with a smaller id (paging), or null for the first page
     * @param limit maximum count
     */
    public List<AuditRecord> listAudit(Connection connection, String username, String action, Long since, Long until,
                                       Long beforeId, int limit) throws SQLException
    {
        StringBuilder sql = new StringBuilder("SELECT id, " + AUDIT_COLUMNS + " FROM " + mAuditTable + " WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();

        if(username != null)
        {
            sql.append(" AND username = ?");
            parameters.add(normalize(username));
        }

        if(action != null)
        {
            sql.append(" AND action = ?");
            parameters.add(action);
        }

        if(since != null)
        {
            sql.append(" AND event_time >= ?");
            parameters.add(since);
        }

        if(until != null)
        {
            sql.append(" AND event_time < ?");
            parameters.add(until);
        }

        if(beforeId != null)
        {
            sql.append(" AND id < ?");
            parameters.add(beforeId);
        }

        sql.append(" ORDER BY id DESC");
        List<AuditRecord> entries = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement(sql.toString()))
        {
            SqlSupport.bindParameters(statement, parameters);
            statement.setMaxRows(limit);

            try(ResultSet rs = statement.executeQuery())
            {
                while(rs.next())
                {
                    entries.add(readAudit(rs));
                }
            }
        }

        return entries;
    }

    /**
     * Deletes audit entries older than the time argument.
     * @return count deleted
     */
    public int deleteAuditBefore(Connection connection, long time) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + mAuditTable +
            " WHERE event_time < ?"))
        {
            statement.setLong(1, time);
            return statement.executeUpdate();
        }
    }

    private static AuditRecord readAudit(ResultSet rs) throws SQLException
    {
        return new AuditRecord(rs.getLong("id"), rs.getLong("event_time"), rs.getString("username"),
            rs.getString("action"), rs.getString("target"), rs.getBoolean("success"), rs.getString("address"),
            rs.getString("detail"));
    }

    private static void bindAudit(PreparedStatement statement, AuditRecord audit) throws SQLException
    {
        statement.setLong(1, audit.time());

        if(audit.username() != null)
        {
            SqlSupport.setString(statement, 2, normalize(audit.username()), LENGTH_USERNAME);
        }
        else
        {
            statement.setNull(2, Types.VARCHAR);
        }

        SqlSupport.setString(statement, 3, audit.action(), LENGTH_ACTION);
        SqlSupport.setString(statement, 4, audit.target(), LENGTH_TARGET);
        statement.setBoolean(5, audit.success());
        SqlSupport.setString(statement, 6, audit.address(), LENGTH_ADDRESS);
        SqlSupport.setString(statement, 7, audit.detail(), LENGTH_DETAIL);
    }

    //----------------------------------------------------------------------------------------------------------------
    // Transfer between databases
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Copies the accounts, their API tokens and the audit log from the source to the target database, when the target
     * has no accounts.  New ids are assigned in the target.
     * @param source repository for the source schema
     * @return count of accounts copied, or 0 when the target already has accounts
     */
    public long copyFrom(AccountRepository source, Connection sourceConnection, Connection targetConnection)
        throws SQLException
    {
        if(countUsers(targetConnection) > 0)
        {
            return 0;
        }

        boolean autoCommit = targetConnection.getAutoCommit();
        targetConnection.setAutoCommit(false);
        long copied = 0;

        try
        {
            Map<Long,Long> idMap = new HashMap<>();
            long afterId = 0;
            List<UserRecord> page = source.listUsers(sourceConnection, afterId, 500);

            while(!page.isEmpty())
            {
                for(UserRecord user: page)
                {
                    idMap.put(user.id(), insertUser(targetConnection, user).id());
                    copied++;
                }

                afterId = page.getLast().id();
                page = source.listUsers(sourceConnection, afterId, 500);
            }

            for(Map.Entry<Long,Long> entry: idMap.entrySet())
            {
                for(ApiTokenRecord token: source.listTokens(sourceConnection, entry.getKey()))
                {
                    insertToken(targetConnection, new ApiTokenRecord(0, entry.getValue(), token.name(),
                        token.tokenHash(), token.prefix(), token.permissions(), token.createdTime(),
                        token.expiresTime(), token.lastUsedTime()));
                }
            }

            try(Statement statement = sourceConnection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT id, " + AUDIT_COLUMNS + " FROM " + source.mAuditTable +
                    " ORDER BY id");
                PreparedStatement insert = targetConnection.prepareStatement("INSERT INTO " + mAuditTable + " (" +
                    AUDIT_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)"))
            {
                int batch = 0;

                while(rs.next())
                {
                    bindAudit(insert, readAudit(rs));
                    insert.addBatch();

                    if(++batch >= 500)
                    {
                        insert.executeBatch();
                        batch = 0;
                    }
                }

                if(batch > 0)
                {
                    insert.executeBatch();
                }
            }

            targetConnection.commit();
        }
        catch(SQLException se)
        {
            targetConnection.rollback();
            throw se;
        }
        finally
        {
            targetConnection.setAutoCommit(autoCommit);
        }

        return copied;
    }

    private static long count(Connection connection, String sql) throws SQLException
    {
        try(Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql))
        {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }
}
