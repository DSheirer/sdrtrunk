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

package io.github.dsheirer.web;

import io.github.dsheirer.database.CallDatabase;
import io.github.dsheirer.database.DatabaseConfiguration;
import io.github.dsheirer.database.DatabaseType;
import io.github.dsheirer.database.account.AccountRepository;
import io.github.dsheirer.database.account.UserRecord;
import io.github.dsheirer.web.account.AccountService;
import io.github.dsheirer.web.api.ApiException;
import io.github.dsheirer.web.auth.PasswordHasher;
import io.github.dsheirer.web.auth.Permission;
import io.github.dsheirer.web.auth.Principal;
import io.github.dsheirer.web.auth.Role;
import io.github.dsheirer.web.auth.Session;
import io.github.dsheirer.web.auth.SessionManager;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Account rules against the built-in H2 database: first-start administrator, sign-in and lockout, password changes,
 * the last administrator guard, API tokens, and copying accounts to another database.
 */
public class AccountServiceTest
{
    private static final String ADDRESS = "192.0.2.10";

    @TempDir
    Path mTempDir;

    private final AtomicLong mClock = new AtomicLong(1_800_000_000_000L);
    private CallDatabase mDatabase;
    private SessionManager mSessions;
    private AccountService mService;

    @BeforeEach
    void setUp() throws Exception
    {
        mDatabase = open("web_unit_test");
        mSessions = new SessionManager(() -> 60, mClock::get);
        mService = new AccountService(() -> mDatabase, new PasswordHasher(1000), mSessions, mClock::get);
    }

    @AfterEach
    void tearDown()
    {
        mDatabase.close();
    }

    private CallDatabase open(String schema) throws Exception
    {
        CallDatabase database = new CallDatabase(new DatabaseConfiguration(DatabaseType.H2, "", 0, "", schema, "",
            ""), mTempDir);
        database.install();
        return database;
    }

    private Principal signIn(String username, String password)
    {
        UserRecord user = mService.login(username, password, ADDRESS);
        Session session = mSessions.create(user.id(), user.username(), ADDRESS);
        return mService.getPrincipal(session).orElseThrow();
    }

    private Principal admin()
    {
        String password = mService.ensureInitialAdmin().orElseThrow();
        Principal pending = signIn("admin", password);
        mService.changePassword(pending, password, "admin password 1", ADDRESS);
        return signIn("admin", "admin password 1");
    }

    @Test
    void initialAdminIsCreatedOnceAndMustChangePassword()
    {
        String password = mService.ensureInitialAdmin().orElseThrow();
        assertTrue(mService.ensureInitialAdmin().isEmpty(), "only when there are no accounts");
        assertEquals(password, mService.getPendingInitialPassword().orElseThrow());

        Principal pending = signIn("ADMIN", password);
        assertTrue(pending.mustChangePassword());
        assertTrue(pending.permissions().isEmpty(), "no permissions until the password is changed");

        mService.changePassword(pending, password, "a new admin password", ADDRESS);
        assertTrue(mService.getPendingInitialPassword().isEmpty());
        Principal admin = signIn("admin", "a new admin password");
        assertFalse(admin.mustChangePassword());
        assertEquals(Role.ADMIN.getPermissions(), admin.permissions());
    }

    @Test
    void wrongPasswordsLockTheAccount()
    {
        Principal admin = admin();
        AccountService.CreatedUser created = mService.createUser(admin, "Op1", "Operator One", "operator", ADDRESS);
        assertEquals("op1", created.user().username());

        for(int x = 0; x < AccountService.LOCKOUT_THRESHOLD; x++)
        {
            assertEquals(401, assertThrows(ApiException.class, () ->
                mService.login("op1", "wrong password", "198.51.100." + mClock.incrementAndGet() % 200)).getStatus());
        }

        //Right password is refused while locked
        assertEquals(401, assertThrows(ApiException.class, () ->
            mService.login("op1", created.temporaryPassword(), "203.0.113.1")).getStatus());

        mClock.addAndGet(AccountService.LOCKOUT_MS + 1);
        assertTrue(mService.login("op1", created.temporaryPassword(), "203.0.113.1").mustChangePassword());
    }

    @Test
    void addressesAreRateLimited()
    {
        mService.ensureInitialAdmin();

        for(int x = 0; x < 10; x++)
        {
            assertThrows(ApiException.class, () -> mService.login("nobody", "whatever pw", ADDRESS));
        }

        assertEquals(429, assertThrows(ApiException.class, () -> mService.login("nobody", "whatever pw", ADDRESS))
            .getStatus());
    }

    @Test
    void passwordChangeRules()
    {
        Principal admin = admin();
        assertThrows(ApiException.class, () -> mService.changePassword(admin, "wrong current", "another password",
            ADDRESS));
        assertThrows(ApiException.class, () -> mService.changePassword(admin, "admin password 1", "short",
            ADDRESS));
        assertThrows(ApiException.class, () -> mService.changePassword(admin, "admin password 1",
            "admin password 1", ADDRESS));
    }

    @Test
    void passwordChangeEndsOtherSessions()
    {
        Principal admin = admin();
        UserRecord user = mService.login("admin", "admin password 1", ADDRESS);
        Session other = mSessions.create(user.id(), user.username(), ADDRESS);
        mService.changePassword(admin, "admin password 1", "admin password 2", ADDRESS);
        assertTrue(mSessions.touch(other.id()).isEmpty());
        assertTrue(mSessions.touch(admin.sessionId()).isPresent(), "the session that changed it continues");
    }

    @Test
    void lastAdminIsProtected()
    {
        Principal admin = admin();
        assertEquals(409, assertThrows(ApiException.class, () ->
            mService.updateUser(admin, admin.userId(), null, "OPERATOR", null, ADDRESS)).getStatus());
        assertEquals(409, assertThrows(ApiException.class, () ->
            mService.updateUser(admin, admin.userId(), null, null, false, ADDRESS)).getStatus());
        assertEquals(409, assertThrows(ApiException.class, () ->
            mService.deleteUser(admin, admin.userId(), ADDRESS)).getStatus());

        AccountService.CreatedUser second = mService.createUser(admin, "admin2", null, "ADMIN", ADDRESS);
        mService.updateUser(admin, admin.userId(), null, "OPERATOR", null, ADDRESS);
        assertEquals(409, assertThrows(ApiException.class, () ->
            mService.deleteUser(admin, second.user().id(), ADDRESS)).getStatus());
    }

    @Test
    void duplicateAndInvalidAccountsAreRefused()
    {
        Principal admin = admin();
        mService.createUser(admin, "listener", null, "LISTENER", ADDRESS);
        assertEquals(409, assertThrows(ApiException.class, () ->
            mService.createUser(admin, "Listener", null, "LISTENER", ADDRESS)).getStatus());
        ApiException invalid = assertThrows(ApiException.class, () ->
            mService.createUser(admin, "a b", null, "SUPERUSER", ADDRESS));
        assertEquals(400, invalid.getStatus());
        assertEquals(2, invalid.getErrors().size());
    }

    @Test
    void disablingEndsSessionsAndRefusesSignIn()
    {
        Principal admin = admin();
        AccountService.CreatedUser user = mService.createUser(admin, "viewer", null, "VIEWER", ADDRESS);
        UserRecord record = mService.login("viewer", user.temporaryPassword(), ADDRESS);
        Session session = mSessions.create(record.id(), record.username(), ADDRESS);

        mService.updateUser(admin, record.id(), null, null, false, ADDRESS);
        assertTrue(mSessions.touch(session.id()).isEmpty());
        assertThrows(ApiException.class, () -> mService.login("viewer", user.temporaryPassword(), ADDRESS));
    }

    @Test
    void roleChangeAppliesToExistingSession()
    {
        Principal admin = admin();
        AccountService.CreatedUser created = mService.createUser(admin, "op", null, "OPERATOR", ADDRESS);
        Principal pending = signIn("op", created.temporaryPassword());
        mService.changePassword(pending, created.temporaryPassword(), "operator password", ADDRESS);
        Session session = mSessions.create(created.user().id(), "op", ADDRESS);
        assertTrue(mService.getPrincipal(session).orElseThrow().has(Permission.CHANNEL_CONTROL));

        mService.updateUser(admin, created.user().id(), null, "VIEWER", null, ADDRESS);
        assertFalse(mService.getPrincipal(session).orElseThrow().has(Permission.CHANNEL_CONTROL));
    }

    @Test
    void apiTokens()
    {
        Principal admin = admin();
        AccountService.CreatedToken full = mService.createToken(admin, "script", null, null, ADDRESS);
        assertTrue(full.token().startsWith(AccountService.TOKEN_PREFIX));
        Principal viaToken = mService.getPrincipal(full.token()).orElseThrow();
        assertEquals(Principal.Method.TOKEN, viaToken.method());
        assertEquals(Role.ADMIN.getPermissions(), viaToken.permissions());

        //Reduced permission set
        AccountService.CreatedToken limited = mService.createToken(admin, "dashboard", 1,
            List.of("events.read", "map.read"), ADDRESS);
        assertEquals(Set.of(Permission.EVENTS_READ, Permission.MAP_READ),
            mService.getPrincipal(limited.token()).orElseThrow().permissions());

        //Expiry
        mClock.addAndGet(86_400_000L + 1);
        assertTrue(mService.getPrincipal(limited.token()).isEmpty());

        //Tokens can't create tokens
        assertEquals(403, assertThrows(ApiException.class, () ->
            mService.createToken(viaToken, "nested", null, null, ADDRESS)).getStatus());

        //Unknown, malformed and revoked tokens
        assertTrue(mService.getPrincipal("sdrt_unknown").isEmpty());
        assertTrue(mService.getPrincipal("not-a-token").isEmpty());
        mService.revokeToken(admin, full.record().id(), ADDRESS);
        assertTrue(mService.getPrincipal(full.token()).isEmpty());
    }

    @Test
    void tokenPermissionsMustBeWithinTheRole()
    {
        Principal admin = admin();
        AccountService.CreatedUser created = mService.createUser(admin, "listen", null, "LISTENER", ADDRESS);
        Principal pending = signIn("listen", created.temporaryPassword());
        mService.changePassword(pending, created.temporaryPassword(), "listener password", ADDRESS);
        Principal listener = signIn("listen", "listener password");

        assertEquals(400, assertThrows(ApiException.class, () -> mService.createToken(listener, "x", null,
            List.of("channel.control"), ADDRESS)).getStatus());
        assertEquals(400, assertThrows(ApiException.class, () -> mService.createToken(listener, "x", null,
            List.of("no.such.permission"), ADDRESS)).getStatus());
        assertEquals(400, assertThrows(ApiException.class, () -> mService.createToken(listener, "x", 0,
            null, ADDRESS)).getStatus());
    }

    @Test
    void requestedAccountsWaitForApproval()
    {
        Principal admin = admin();
        UserRecord requested = mService.register("New.User", "New User", "my own password", ADDRESS);
        assertTrue(requested.pendingApproval());
        assertFalse(requested.enabled());
        assertEquals(1, mService.countPending());

        //Only the right password reveals that the account waits for approval
        ApiException waiting = assertThrows(ApiException.class, () ->
            mService.login("new.user", "my own password", ADDRESS));
        assertTrue(waiting.getMessage().contains("approval") || String.valueOf(waiting.getDetail())
            .contains("approval"));
        ApiException wrong = assertThrows(ApiException.class, () -> mService.login("new.user", "wrong password!",
            ADDRESS));
        assertFalse(String.valueOf(wrong.getDetail()).contains("approval"));

        //Taken usernames, short passwords
        assertEquals(409, assertThrows(ApiException.class, () -> mService.register("new.user", null,
            "another password", ADDRESS)).getStatus());
        assertEquals(400, assertThrows(ApiException.class, () -> mService.register("other", null, "short",
            ADDRESS)).getStatus());

        UserRecord approved = mService.approveUser(admin, requested.id(), "OPERATOR", ADDRESS);
        assertTrue(approved.enabled());
        assertFalse(approved.pendingApproval());
        assertEquals(0, mService.countPending());
        Principal user = signIn("new.user", "my own password");
        assertFalse(user.mustChangePassword(), "the user chose the password");
        assertEquals(Role.OPERATOR, user.role());
        assertEquals(409, assertThrows(ApiException.class, () -> mService.approveUser(admin, requested.id(), null,
            ADDRESS)).getStatus());
    }

    @Test
    void registrationCanBeTurnedOff()
    {
        AccountService service = new AccountService(() -> mDatabase, new PasswordHasher(1000), mSessions,
            mClock::get, () -> false);
        assertFalse(service.isRegistrationAllowed());
        assertEquals(403, assertThrows(ApiException.class, () -> service.register("someone", null,
            "a long password", ADDRESS)).getStatus());
    }

    @Test
    void administratorCreatesTokensForAccounts()
    {
        Principal admin = admin();
        AccountService.CreatedUser listener = mService.createUser(admin, "listen", null, "LISTENER", ADDRESS);
        long id = listener.user().id();

        AccountService.CreatedToken token = mService.createTokenFor(admin, id, "for listen", null, null, ADDRESS);
        Principal viaToken = mService.getPrincipal(token.token()).orElseThrow();
        assertEquals("listen", viaToken.username());
        assertTrue(viaToken.permissions().isEmpty(), "no permissions until the temporary password is changed");

        //From the desktop application, limited to the account's role
        assertEquals(400, assertThrows(ApiException.class, () -> mService.createTokenFor(Principal.DESKTOP, id, "x",
            null, List.of("channel.control"), ADDRESS)).getStatus());
        AccountService.CreatedToken desktop = mService.createTokenFor(Principal.DESKTOP, id, "desktop", 30,
            List.of("audio.listen"), ADDRESS);
        assertEquals(2, mService.listTokens(id).size());

        //API tokens can't hand out tokens
        AccountService.CreatedToken adminToken = mService.createToken(admin, "admin script", null, null, ADDRESS);
        Principal adminViaToken = mService.getPrincipal(adminToken.token()).orElseThrow();
        assertEquals(403, assertThrows(ApiException.class, () -> mService.createTokenFor(adminViaToken, id, "x",
            null, null, ADDRESS)).getStatus());

        mService.revokeToken(Principal.DESKTOP, id, desktop.record().id(), ADDRESS);
        assertTrue(mService.getPrincipal(desktop.token()).isEmpty());
        assertEquals(404, assertThrows(ApiException.class, () -> mService.revokeToken(admin, id,
            adminToken.record().id(), ADDRESS)).getStatus(), "the token belongs to another account");
    }

    @Test
    void activityRecordsWebAndApiAccess()
    {
        Principal admin = admin();
        UserRecord before = mService.getUser(admin.userId());
        AccountService.Activity activity = mService.getActivity(before);
        assertEquals(2, activity.sessions(), "the session that changed the password, and the new sign-in");
        assertTrue(activity.active());
        assertNotNull(activity.lastWebTime());
        assertNull(activity.lastApiTime());
        assertNotNull(before.lastWebTime(), "the first access is written at once");

        AccountService.CreatedToken token = mService.createToken(admin, "script", null, null, ADDRESS);
        mClock.addAndGet(10_000);
        mService.getPrincipal(token.token()).orElseThrow();
        assertEquals(mClock.get(), mService.getActivity(mService.getUser(admin.userId())).lastApiTime());

        //Inactive after the active window, and the times survive in the database
        mSessions.invalidateUser(admin.userId(), null);
        mClock.addAndGet(AccountService.ACTIVE_WINDOW_MS + 1);
        mService.clearActivity();
        UserRecord stored = mService.getUser(admin.userId());
        AccountService.Activity later = mService.getActivity(stored);
        assertFalse(later.active());
        assertEquals(0, later.sessions());
        assertEquals(mClock.get() - AccountService.ACTIVE_WINDOW_MS - 1, stored.lastApiTime());
    }

    @Test
    void auditLogRecordsSignIns()
    {
        mService.ensureInitialAdmin();
        assertThrows(ApiException.class, () -> mService.login("admin", "wrong password", ADDRESS));
        assertTrue(mService.listAudit(null, "login.failed", null, null, null, 10).stream()
            .anyMatch(audit -> "admin".equals(audit.username()) && !audit.success()));
    }

    @Test
    void accountsCopyToAnEmptyDatabase() throws Exception
    {
        Principal admin = admin();
        mService.createUser(admin, "op", null, "OPERATOR", ADDRESS);
        AccountService.CreatedToken token = mService.createToken(admin, "script", null, null, ADDRESS);

        try(CallDatabase target = open("web_unit_test_target");
            Connection source = mDatabase.getConnection(); Connection destination = target.getConnection())
        {
            AccountRepository repository = target.getAccountRepository();
            assertEquals(2, repository.copyFrom(mDatabase.getAccountRepository(), source, destination));
            assertEquals(0, repository.copyFrom(mDatabase.getAccountRepository(), source, destination),
                "a database with accounts isn't changed");
            assertTrue(repository.findUser(destination, "op").isPresent());

            AccountService copied = new AccountService(() -> target, new PasswordHasher(1000), mSessions,
                mClock::get);
            assertTrue(copied.getPrincipal(token.token()).isPresent(), "API tokens move with their account");
            assertTrue(copied.ensureInitialAdmin().isEmpty());
        }
    }
}
