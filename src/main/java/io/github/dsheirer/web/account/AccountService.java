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

package io.github.dsheirer.web.account;

import io.github.dsheirer.database.CallDatabase;
import io.github.dsheirer.database.account.AccountRepository;
import io.github.dsheirer.database.account.ApiTokenRecord;
import io.github.dsheirer.database.account.AuditRecord;
import io.github.dsheirer.database.account.UserRecord;
import io.github.dsheirer.web.api.ApiException;
import io.github.dsheirer.web.auth.LoginRateLimiter;
import io.github.dsheirer.web.auth.PasswordHasher;
import io.github.dsheirer.web.auth.Permission;
import io.github.dsheirer.web.auth.Principal;
import io.github.dsheirer.web.auth.Role;
import io.github.dsheirer.web.auth.Session;
import io.github.dsheirer.web.auth.SessionManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web API accounts: sign-in with lockout, the first-start administrator, password changes, account administration,
 * API tokens and the audit log.  Accounts are stored in the call database (see {@link AccountRepository}).
 *
 * The web handlers call this service and only translate requests and responses, so the rules live here.
 */
public class AccountService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountService.class);
    public static final String INITIAL_ADMIN_USERNAME = "admin";
    public static final int LOCKOUT_THRESHOLD = 5;
    public static final long LOCKOUT_MS = 15 * 60_000L;
    public static final int MAX_TOKENS_PER_USER = 50;
    public static final int MAX_TOKEN_DAYS = 3650;
    public static final String TOKEN_PREFIX = "sdrt_";
    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_DISPLAY_PREFIX_LENGTH = 12;
    private static final long TOKEN_TOUCH_INTERVAL_MS = 60_000;
    /**
     * How often the last access times of an account are written to the database.  The times in between are kept in
     * memory, so the account list shows them at once.
     */
    private static final long ACCESS_WRITE_INTERVAL_MS = 60_000;
    /**
     * An account counts as active when it made a request within this time
     */
    public static final long ACTIVE_WINDOW_MS = 5 * 60_000L;
    /**
     * Most accounts that can wait for approval at once, so that sign-up requests can't fill the database
     */
    public static final int MAX_PENDING_ACCOUNTS = 100;
    public static final Role REGISTERED_ROLE = Role.LISTENER;
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9][a-z0-9._@-]{1,63}");
    private static final String SIGN_IN_FAILED = "Invalid username or password.  After " + LOCKOUT_THRESHOLD +
        " failed attempts an account is locked for " + (LOCKOUT_MS / 60_000) + " minutes.";

    private final Supplier<CallDatabase> mDatabase;
    private final PasswordHasher mHasher;
    private final SessionManager mSessionManager;
    private final LoginRateLimiter mRateLimiter;
    private final LoginRateLimiter mRegistrationLimiter;
    private final LongSupplier mClock;
    private final BooleanSupplier mRegistrationAllowed;
    private final Map<Long,Access> mAccess = new ConcurrentHashMap<>();
    private final Object mAdminLock = new Object();
    private volatile String mInitialPassword;
    private volatile long mInitialAdminId;

    /**
     * Constructs an instance
     * @param database supplies the open call database, or null when it isn't available
     * @param hasher for passwords
     * @param sessionManager to end sessions when an account changes
     * @param clock epoch milliseconds
     */
    public AccountService(Supplier<CallDatabase> database, PasswordHasher hasher, SessionManager sessionManager,
                          LongSupplier clock)
    {
        this(database, hasher, sessionManager, clock, () -> true);
    }

    /**
     * Constructs an instance
     * @param database supplies the open call database, or null when it isn't available
     * @param hasher for passwords
     * @param sessionManager to end sessions when an account changes
     * @param clock epoch milliseconds
     * @param registrationAllowed true when users may request accounts from the sign-in page, read on each request
     */
    public AccountService(Supplier<CallDatabase> database, PasswordHasher hasher, SessionManager sessionManager,
                          LongSupplier clock, BooleanSupplier registrationAllowed)
    {
        mDatabase = database;
        mHasher = hasher;
        mSessionManager = sessionManager;
        mRateLimiter = new LoginRateLimiter(clock);
        mRegistrationLimiter = new LoginRateLimiter(clock);
        mClock = clock;
        mRegistrationAllowed = registrationAllowed;
    }

    /**
     * Indicates if users may request accounts from the sign-in page
     */
    public boolean isRegistrationAllowed()
    {
        return mRegistrationAllowed.getAsBoolean();
    }

    //----------------------------------------------------------------------------------------------------------------
    // First start
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Creates the administrator account with a one-time password when the database has no accounts: on first start,
     * and after the database is cleared or a new empty database is selected.  The password is written to the
     * application log and the console, and must be changed at the first sign-in.
     * @return the one-time password, or empty when accounts already exist or the database isn't available
     */
    public Optional<String> ensureInitialAdmin()
    {
        CallDatabase database = mDatabase.get();

        if(database == null)
        {
            return Optional.empty();
        }

        synchronized(mAdminLock)
        {
            try(Connection connection = database.getConnection())
            {
                AccountRepository repository = database.getAccountRepository();

                if(repository.countUsers(connection) > 0)
                {
                    return Optional.empty();
                }

                String password = PasswordHasher.createTemporaryPassword();
                UserRecord admin = repository.insertUser(connection, UserRecord.create(INITIAL_ADMIN_USERNAME,
                    "Administrator", Role.ADMIN.name(), mHasher.hash(password), true, true, mClock.getAsLong(),
                    false));
                mInitialPassword = password;
                mInitialAdminId = admin.id();
                repository.insertAudit(connection, new AuditRecord(0, mClock.getAsLong(), null, "user.create",
                    INITIAL_ADMIN_USERNAME, true, null, "Initial administrator account"));

                String message = "Web API: created the administrator account '" + INITIAL_ADMIN_USERNAME +
                    "' with the one-time password [" + password + "].  It must be changed at the first sign-in.";
                LOGGER.warn(message);
                System.out.println(message);
                return Optional.of(password);
            }
            catch(SQLException se)
            {
                LOGGER.error("Unable to create the initial web administrator account", se);
                return Optional.empty();
            }
        }
    }

    /**
     * One-time password of the initial administrator, until it is changed, so the settings dialog can show it.
     */
    public Optional<String> getPendingInitialPassword()
    {
        return Optional.ofNullable(mInitialPassword);
    }

    //----------------------------------------------------------------------------------------------------------------
    // Sign-in and request authentication
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Checks a sign-in.  Refusals all give the same message, so they don't reveal whether the account exists or is
     * locked.
     * @param username entered
     * @param password entered
     * @param address of the client
     * @return the account
     * @throws ApiException 401 when refused, 429 when the address made too many attempts
     */
    public UserRecord login(String username, String password, String address)
    {
        if(!mRateLimiter.tryAcquire(address))
        {
            audit(username, "login.rate-limited", null, false, address, null);
            throw ApiException.tooManyRequests("Too many sign-in attempts from this address.  Wait a minute.");
        }

        if(username == null || username.isBlank() || password == null || password.isEmpty())
        {
            throw ApiException.unauthorized(SIGN_IN_FAILED);
        }

        return withConnection((connection, repository) -> {
            long now = mClock.getAsLong();
            Optional<UserRecord> found = repository.findUser(connection, username);

            if(found.isEmpty())
            {
                mHasher.verify(password, null);
                audit(connection, repository, username, "login.failed", null, false, address, "Unknown account");
                throw ApiException.unauthorized(SIGN_IN_FAILED);
            }

            UserRecord user = found.get();

            if(!user.enabled())
            {
                //Only someone who knows the password learns that the account waits for approval
                if(user.pendingApproval() && mHasher.verify(password, user.passwordHash()))
                {
                    audit(connection, repository, user.username(), "login.failed", null, false, address,
                        "Account waits for approval");
                    throw ApiException.unauthorized("Your account waits for an administrator's approval.  Try " +
                        "again later.");
                }

                mHasher.verify(password, null);
                audit(connection, repository, user.username(), "login.failed", null, false, address,
                    "Account disabled");
                throw ApiException.unauthorized(SIGN_IN_FAILED);
            }

            if(user.lockedUntil() != null && user.lockedUntil() > now)
            {
                mHasher.verify(password, null);
                audit(connection, repository, user.username(), "login.failed", null, false, address,
                    "Account locked");
                throw ApiException.unauthorized(SIGN_IN_FAILED);
            }

            if(!mHasher.verify(password, user.passwordHash()))
            {
                int failures = user.failedLogins() + 1;
                Long lockedUntil = failures >= LOCKOUT_THRESHOLD ? now + LOCKOUT_MS : null;
                repository.updateUser(connection, user.withLoginFailure(lockedUntil != null ? 0 : failures,
                    lockedUntil));
                audit(connection, repository, user.username(), "login.failed", null, false, address,
                    lockedUntil != null ? "Wrong password - account locked" : "Wrong password");
                throw ApiException.unauthorized(SIGN_IN_FAILED);
            }

            UserRecord updated = user.withLoginSuccess(now);

            if(mHasher.needsRehash(user.passwordHash()))
            {
                updated = updated.withPassword(mHasher.hash(password), updated.mustChangePassword());
            }

            repository.updateUser(connection, updated);
            audit(connection, repository, user.username(), "login", null, true, address, null);
            return updated;
        });
    }

    /**
     * Resolves a browser session to the current state of its account.  The account is read on every request, so a
     * role change or a disabled account applies at once.
     * @return principal, or empty when the account no longer exists, was replaced or is disabled
     */
    public Optional<Principal> getPrincipal(Session session)
    {
        return withConnection((connection, repository) -> {
            Optional<Principal> principal = repository.findUser(connection, session.userId())
                .filter(user -> user.enabled() && user.username().equals(session.username()))
                .flatMap(user -> Role.parse(user.role()).map(role -> new Principal(user.id(), user.username(),
                    user.displayName(), role, user.mustChangePassword() ? Set.of() : role.getPermissions(),
                    Principal.Method.SESSION, session.id(), 0, session.csrfToken(), user.mustChangePassword())));
            principal.ifPresent(p -> recordAccess(connection, repository, p.userId(), false));
            return principal;
        });
    }

    /**
     * Resolves an API token from an Authorization: Bearer header.
     * @return principal, or empty when the token is unknown, expired or its account is disabled
     */
    public Optional<Principal> getPrincipal(String token)
    {
        if(token == null || !token.startsWith(TOKEN_PREFIX))
        {
            return Optional.empty();
        }

        String hash = PasswordHasher.sha256(token);

        return withConnection((connection, repository) -> {
            Optional<ApiTokenRecord> found = repository.findToken(connection, hash);
            long now = mClock.getAsLong();

            if(found.isEmpty() || found.get().isExpired(now))
            {
                return Optional.<Principal>empty();
            }

            ApiTokenRecord record = found.get();
            Optional<UserRecord> user = repository.findUser(connection, record.userId()).filter(UserRecord::enabled);
            Optional<Role> role = user.flatMap(u -> Role.parse(u.role()));

            if(user.isEmpty() || role.isEmpty())
            {
                return Optional.<Principal>empty();
            }

            if(record.lastUsedTime() == null || now - record.lastUsedTime() > TOKEN_TOUCH_INTERVAL_MS)
            {
                repository.touchToken(connection, record.id(), now);
            }

            recordAccess(connection, repository, user.get().id(), true);

            Set<Permission> permissions = EnumSet.noneOf(Permission.class);

            if(!user.get().mustChangePassword())
            {
                permissions.addAll(role.get().getPermissions());

                if(record.permissions() != null)
                {
                    permissions.retainAll(parsePermissions(record.permissions()));
                }
            }

            return Optional.of(new Principal(user.get().id(), user.get().username(), user.get().displayName(),
                role.get(), Set.copyOf(permissions), Principal.Method.TOKEN, null, record.id(), null,
                user.get().mustChangePassword()));
        });
    }

    /**
     * Checks a principal again, for a long-lived connection (WebSocket): the session must still be active (this counts
     * as activity) or the API token still valid, and the account enabled.  The current role applies.
     * @return the principal with current permissions, or empty when it is no longer valid
     */
    public Optional<Principal> refresh(Principal principal)
    {
        if(principal.method() == Principal.Method.SESSION)
        {
            return mSessionManager.touch(principal.sessionId()).flatMap(this::getPrincipal);
        }

        return withConnection((connection, repository) -> {
            Optional<ApiTokenRecord> token = repository.findToken(connection, principal.tokenId());

            if(token.isEmpty() || token.get().isExpired(mClock.getAsLong()) ||
                token.get().userId() != principal.userId())
            {
                return Optional.<Principal>empty();
            }

            Optional<UserRecord> user = repository.findUser(connection, principal.userId()).filter(UserRecord::enabled);
            Optional<Role> role = user.flatMap(u -> Role.parse(u.role()));

            if(user.isEmpty() || role.isEmpty())
            {
                return Optional.<Principal>empty();
            }

            recordAccess(connection, repository, user.get().id(), true);
            Set<Permission> permissions = EnumSet.noneOf(Permission.class);

            if(!user.get().mustChangePassword())
            {
                permissions.addAll(role.get().getPermissions());

                if(token.get().permissions() != null)
                {
                    permissions.retainAll(parsePermissions(token.get().permissions()));
                }
            }

            return Optional.of(new Principal(user.get().id(), user.get().username(), user.get().displayName(),
                role.get(), Set.copyOf(permissions), Principal.Method.TOKEN, null, token.get().id(), null,
                user.get().mustChangePassword()));
        });
    }

    /**
     * Changes the caller's own password.  Other sessions of the account end; this one continues.
     * @throws ApiException 400 when the current password is wrong or the new one breaks the rules
     */
    public void changePassword(Principal principal, String currentPassword, String newPassword, String address)
    {
        String rule = PasswordHasher.checkPasswordRules(newPassword);

        if(rule != null)
        {
            throw ApiException.invalid("newPassword", rule);
        }

        withConnection((connection, repository) -> {
            UserRecord user = repository.findUser(connection, principal.userId())
                .orElseThrow(() -> ApiException.unauthorized("The account no longer exists"));

            if(currentPassword == null || !mHasher.verify(currentPassword, user.passwordHash()))
            {
                audit(connection, repository, user.username(), "password.change", user.username(), false, address,
                    "Wrong current password");
                throw ApiException.invalid("currentPassword", "isn't the current password");
            }

            if(newPassword.equals(currentPassword))
            {
                throw ApiException.invalid("newPassword", "must be different from the current password");
            }

            repository.updateUser(connection, user.withPassword(mHasher.hash(newPassword), false));
            audit(connection, repository, user.username(), "password.change", user.username(), true, address, null);
            return null;
        });

        mSessionManager.invalidateUser(principal.userId(), principal.sessionId());
        clearInitialPassword(principal.userId());
    }

    //----------------------------------------------------------------------------------------------------------------
    // Account administration
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Accounts in id order.
     * @param afterId paging cursor (0 for the first page)
     * @param limit page size: reads one extra row so the caller knows whether there is another page
     */
    public List<UserRecord> listUsers(long afterId, int limit)
    {
        return withConnection((connection, repository) -> repository.listUsers(connection, afterId, limit + 1));
    }

    public UserRecord getUser(long id)
    {
        return withConnection((connection, repository) -> repository.findUser(connection, id))
            .orElseThrow(() -> ApiException.notFound("No account with id " + id));
    }

    /**
     * Creates an account with a temporary password that must be changed at the first sign-in.
     * @return the account and its temporary password
     */
    public CreatedUser createUser(Principal actor, String username, String displayName, String roleName,
                                  String address)
    {
        String normalized = AccountRepository.normalize(username);
        List<ApiException.FieldError> errors = new ArrayList<>();
        checkUsername(normalized, errors);
        Optional<Role> role = Role.parse(roleName);

        if(role.isEmpty())
        {
            errors.add(new ApiException.FieldError("role", "must be one of " + Arrays.toString(Role.values())));
        }

        checkDisplayName(displayName, errors);

        if(!errors.isEmpty())
        {
            throw ApiException.invalid(errors);
        }

        String password = PasswordHasher.createTemporaryPassword();
        String hash = mHasher.hash(password);

        synchronized(mAdminLock)
        {
            UserRecord user = withConnection((connection, repository) -> {
                if(repository.findUser(connection, normalized).isPresent())
                {
                    throw ApiException.conflict("An account named '" + normalized + "' already exists");
                }

                UserRecord created = repository.insertUser(connection, UserRecord.create(normalized,
                    blankToNull(displayName), role.get().name(), hash, true, true, mClock.getAsLong(), false));
                audit(connection, repository, actor.username(), "user.create", normalized, true, address,
                    "Role " + role.get().name());
                return created;
            });

            return new CreatedUser(user, password);
        }
    }

    /**
     * A user's request for an account, from the sign-in page.  The account is created disabled with the password the
     * user chose, and waits until an administrator approves it ({@link #approveUser}) or deletes it.
     * @return the pending account
     * @throws ApiException 403 when sign-up is turned off, 429 when the address made too many requests, 409 when the
     * username is taken or too many accounts already wait for approval, 400 for invalid fields
     */
    public UserRecord register(String username, String displayName, String password, String address)
    {
        if(!isRegistrationAllowed())
        {
            throw ApiException.forbidden("This server doesn't accept account requests.  Ask its administrator for " +
                "an account.");
        }

        if(!mRegistrationLimiter.tryAcquire(address))
        {
            audit(username, "user.register", username, false, address, "Rate limited");
            throw ApiException.tooManyRequests("Too many account requests from this address.  Wait a minute.");
        }

        String normalized = AccountRepository.normalize(username);
        List<ApiException.FieldError> errors = new ArrayList<>();
        checkUsername(normalized, errors);
        checkDisplayName(displayName, errors);
        String rule = PasswordHasher.checkPasswordRules(password);

        if(rule != null)
        {
            errors.add(new ApiException.FieldError("password", rule));
        }

        if(!errors.isEmpty())
        {
            throw ApiException.invalid(errors);
        }

        String hash = mHasher.hash(password);

        synchronized(mAdminLock)
        {
            return withConnection((connection, repository) -> {
                if(repository.findUser(connection, normalized).isPresent())
                {
                    audit(connection, repository, normalized, "user.register", normalized, false, address,
                        "Username taken");
                    throw ApiException.conflict("The username '" + normalized + "' is taken.  Choose another.");
                }

                if(repository.countPending(connection) >= MAX_PENDING_ACCOUNTS)
                {
                    audit(connection, repository, normalized, "user.register", normalized, false, address,
                        "Too many accounts wait for approval");
                    throw ApiException.conflict("Too many account requests wait for approval.  Try again later.");
                }

                UserRecord created = repository.insertUser(connection, UserRecord.create(normalized,
                    blankToNull(displayName), REGISTERED_ROLE.name(), hash, false, false, mClock.getAsLong(), true));
                audit(connection, repository, normalized, "user.register", normalized, true, address,
                    "Waits for approval");
                LOGGER.info("Web API: '" + normalized + "' requested an account, which waits for approval");
                return created;
            });
        }
    }

    /**
     * Approves an account that a user requested, enabling it with the role argument.
     * @param roleName for the account, or null for the role it requested with ({@link #REGISTERED_ROLE})
     * @throws ApiException 409 when the account doesn't wait for approval
     */
    public UserRecord approveUser(Principal actor, long id, String roleName, String address)
    {
        Optional<Role> role = roleName == null ? Optional.of(REGISTERED_ROLE) : Role.parse(roleName);

        if(role.isEmpty())
        {
            throw ApiException.invalid("role", "must be one of " + Arrays.toString(Role.values()));
        }

        synchronized(mAdminLock)
        {
            return withConnection((connection, repository) -> {
                UserRecord user = repository.findUser(connection, id)
                    .orElseThrow(() -> ApiException.notFound("No account with id " + id));

                if(!user.pendingApproval())
                {
                    throw ApiException.conflict("The account '" + user.username() + "' doesn't wait for approval");
                }

                UserRecord approved = user.withApproval(role.get().name());
                repository.updateUser(connection, approved);
                audit(connection, repository, actor.username(), "user.approve", user.username(), true, address,
                    "Role " + role.get().name());
                return approved;
            });
        }
    }

    /**
     * Count of accounts that wait for approval
     */
    public long countPending()
    {
        return withConnection((connection, repository) -> repository.countPending(connection));
    }

    /**
     * Changes an account's display name, role or enabled state.  Null leaves a field unchanged.  The last enabled
     * administrator can't be demoted or disabled.  Disabling an account ends its sessions.
     */
    public UserRecord updateUser(Principal actor, long id, String displayName, String roleName, Boolean enabled,
                                 String address)
    {
        Optional<Role> role = roleName == null ? Optional.empty() : Role.parse(roleName);

        if(roleName != null && role.isEmpty())
        {
            throw ApiException.invalid("role", "must be one of " + Arrays.toString(Role.values()));
        }

        if(displayName != null && displayName.length() > AccountRepository.LENGTH_NAME)
        {
            throw ApiException.invalid("displayName", "must be at most " + AccountRepository.LENGTH_NAME +
                " characters");
        }

        UserRecord updated;

        synchronized(mAdminLock)
        {
            updated = withConnection((connection, repository) -> {
                UserRecord user = repository.findUser(connection, id)
                    .orElseThrow(() -> ApiException.notFound("No account with id " + id));
                String newRole = role.map(Enum::name).orElse(user.role());
                boolean newEnabled = enabled != null ? enabled : user.enabled();
                checkLastAdmin(connection, repository, user, newRole, newEnabled);
                UserRecord changed = user.withDetails(displayName != null ? blankToNull(displayName) :
                    user.displayName(), newRole, newEnabled);
                repository.updateUser(connection, changed);
                audit(connection, repository, actor.username(), "user.update", user.username(), true, address,
                    describeChange(user, changed));
                return changed;
            });
        }

        if(!updated.enabled())
        {
            mSessionManager.invalidateUser(id, null);
        }

        return updated;
    }

    /**
     * Issues a new temporary password, which must be changed at the next sign-in, and ends the account's sessions.
     * The account's lockout is cleared.
     * @return the temporary password
     */
    public String resetPassword(Principal actor, long id, String address)
    {
        String password = PasswordHasher.createTemporaryPassword();
        String hash = mHasher.hash(password);

        withConnection((connection, repository) -> {
            UserRecord user = repository.findUser(connection, id)
                .orElseThrow(() -> ApiException.notFound("No account with id " + id));
            repository.updateUser(connection, user.withPassword(hash, true));
            audit(connection, repository, actor.username(), "user.reset-password", user.username(), true, address,
                null);
            return null;
        });

        mSessionManager.invalidateUser(id, null);
        clearInitialPassword(id);
        return password;
    }

    /**
     * Deletes the account with its API tokens and ends its sessions.  The last enabled administrator can't be deleted.
     */
    public void deleteUser(Principal actor, long id, String address)
    {
        synchronized(mAdminLock)
        {
            withConnection((connection, repository) -> {
                UserRecord user = repository.findUser(connection, id)
                    .orElseThrow(() -> ApiException.notFound("No account with id " + id));
                checkLastAdmin(connection, repository, user, null, false);
                repository.deleteUser(connection, id);
                mAccess.remove(id);
                audit(connection, repository, actor.username(), "user.delete", user.username(), true, address, null);
                return null;
            });
        }

        mSessionManager.invalidateUser(id, null);
        clearInitialPassword(id);
    }

    /**
     * Ends all of the account's sessions and deletes its API tokens.
     * @return count of sessions and tokens revoked
     */
    public int revokeAll(Principal actor, long id, String address)
    {
        int tokens = withConnection((connection, repository) -> {
            UserRecord user = repository.findUser(connection, id)
                .orElseThrow(() -> ApiException.notFound("No account with id " + id));
            int deleted = repository.deleteTokens(connection, id);
            audit(connection, repository, actor.username(), "user.revoke-sessions", user.username(), true, address,
                null);
            return deleted;
        });

        return tokens + mSessionManager.invalidateUser(id, null);
    }

    private void checkLastAdmin(Connection connection, AccountRepository repository, UserRecord user, String newRole,
                                boolean newEnabled) throws SQLException
    {
        boolean isEnabledAdmin = user.enabled() && Role.ADMIN.name().equals(user.role());
        boolean staysEnabledAdmin = newEnabled && Role.ADMIN.name().equals(newRole);

        if(isEnabledAdmin && !staysEnabledAdmin && repository.countEnabled(connection, Role.ADMIN.name()) <= 1)
        {
            throw ApiException.conflict("This is the last enabled Admin account.  Make another account an " +
                "enabled Admin first.");
        }
    }

    //----------------------------------------------------------------------------------------------------------------
    // API tokens
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Creates an API token for the caller.  The token is returned only once; only its hash is stored.  API tokens
     * can't create tokens: that needs a browser session.
     * @param name label for the token
     * @param expiresInDays lifetime, or null for no expiry
     * @param permissionKeys limits the token to these permissions (within the caller's role), or null for all of them
     */
    public CreatedToken createToken(Principal principal, String name, Integer expiresInDays,
                                    List<String> permissionKeys, String address)
    {
        if(principal.method() != Principal.Method.SESSION)
        {
            throw ApiException.forbidden("API tokens can only be created from a signed-in browser session");
        }

        return createToken(principal, principal.userId(), principal.username(), principal.role(), name,
            expiresInDays, permissionKeys, address);
    }

    /**
     * Creates an API token for another account, which an administrator then gives to the account's user.  The token
     * is returned only once.  The token's permissions are limited to the account's role.
     * @param actor administrator: from a browser session or the desktop application
     * @param userId account that owns the token
     */
    public CreatedToken createTokenFor(Principal actor, long userId, String name, Integer expiresInDays,
                                       List<String> permissionKeys, String address)
    {
        if(actor.method() == Principal.Method.TOKEN)
        {
            throw ApiException.forbidden("API tokens can only be created from a signed-in browser session");
        }

        UserRecord user = getUser(userId);
        Role role = Role.parse(user.role())
            .orElseThrow(() -> ApiException.conflict("The account has an unknown role: " + user.role()));
        return createToken(actor, user.id(), user.username(), role, name, expiresInDays, permissionKeys, address);
    }

    private CreatedToken createToken(Principal actor, long ownerId, String ownerUsername, Role ownerRole, String name,
                                     Integer expiresInDays, List<String> permissionKeys, String address)
    {
        List<ApiException.FieldError> errors = new ArrayList<>();

        if(name == null || name.isBlank() || name.length() > AccountRepository.LENGTH_NAME)
        {
            errors.add(new ApiException.FieldError("name", "is required, at most " + AccountRepository.LENGTH_NAME +
                " characters"));
        }

        if(expiresInDays != null && (expiresInDays < 1 || expiresInDays > MAX_TOKEN_DAYS))
        {
            errors.add(new ApiException.FieldError("expiresInDays", "must be from 1 to " + MAX_TOKEN_DAYS +
                ", or absent for no expiry"));
        }

        String permissions = null;

        if(permissionKeys != null)
        {
            Set<Permission> requested = EnumSet.noneOf(Permission.class);

            for(String key: permissionKeys)
            {
                Optional<Permission> permission = Permission.fromKey(key).filter(Permission::isGrantable);

                if(permission.isEmpty())
                {
                    errors.add(new ApiException.FieldError("permissions", "unknown permission '" + key + "'"));
                }
                else if(!ownerRole.getPermissions().contains(permission.get()))
                {
                    errors.add(new ApiException.FieldError("permissions", "'" + key + "' isn't granted to the " +
                        ownerRole.getLabel() + " role"));
                }
                else
                {
                    requested.add(permission.get());
                }
            }

            if(requested.isEmpty() && errors.isEmpty())
            {
                errors.add(new ApiException.FieldError("permissions", "must list at least one permission, or be " +
                    "absent for all of your permissions"));
            }

            permissions = requested.stream().map(Permission::getKey).collect(Collectors.joining(","));
        }

        if(!errors.isEmpty())
        {
            throw ApiException.invalid(errors);
        }

        String token = TOKEN_PREFIX + PasswordHasher.createSecret(TOKEN_BYTES);
        long now = mClock.getAsLong();
        Long expires = expiresInDays != null ? now + expiresInDays * 86_400_000L : null;
        String storedPermissions = permissions;

        ApiTokenRecord record = withConnection((connection, repository) -> {
            if(repository.listTokens(connection, ownerId).size() >= MAX_TOKENS_PER_USER)
            {
                throw ApiException.conflict("An account can have at most " + MAX_TOKENS_PER_USER + " API tokens");
            }

            ApiTokenRecord created = repository.insertToken(connection, new ApiTokenRecord(0, ownerId,
                name.trim(), PasswordHasher.sha256(token), token.substring(0, TOKEN_DISPLAY_PREFIX_LENGTH),
                storedPermissions, now, expires, null));
            String permissionsDetail = storedPermissions != null ? "Permissions " + storedPermissions :
                "All role permissions";
            audit(connection, repository, actor.username(), "token.create", name.trim(), true, address,
                ownerId == actor.userId() ? permissionsDetail : "For " + ownerUsername + ", " + permissionsDetail);
            return created;
        });

        return new CreatedToken(record, token);
    }

    public List<ApiTokenRecord> listTokens(long userId)
    {
        return withConnection((connection, repository) -> repository.listTokens(connection, userId));
    }

    /**
     * Deletes one of the account's tokens.
     * @throws ApiException 404 when the account has no such token
     */
    public void revokeToken(Principal principal, long tokenId, String address)
    {
        revokeToken(principal, principal.userId(), tokenId, address);
    }

    /**
     * Deletes one of an account's tokens, by an administrator or the account itself.
     * @throws ApiException 404 when the account has no such token
     */
    public void revokeToken(Principal actor, long userId, long tokenId, String address)
    {
        withConnection((connection, repository) -> {
            if(!repository.deleteToken(connection, userId, tokenId))
            {
                throw ApiException.notFound("No API token with id " + tokenId);
            }

            audit(connection, repository, actor.username(), "token.revoke", String.valueOf(tokenId), true,
                address, userId == actor.userId() ? null : "Of account id " + userId);
            return null;
        });
    }

    /**
     * Parses a stored comma separated permission list, ignoring unknown keys.
     */
    public static Set<Permission> parsePermissions(String permissions)
    {
        Set<Permission> parsed = EnumSet.noneOf(Permission.class);

        if(permissions != null)
        {
            for(String key: permissions.split(","))
            {
                Permission.fromKey(key.trim()).filter(Permission::isGrantable).ifPresent(parsed::add);
            }
        }

        return parsed;
    }

    //----------------------------------------------------------------------------------------------------------------
    // Audit log
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Writes an audit entry.  Errors are logged and don't fail the request.
     */
    public void audit(String username, String action, String target, boolean success, String address, String detail)
    {
        CallDatabase database = mDatabase.get();

        if(database == null)
        {
            LOGGER.info("Web audit (database unavailable): " + action + " user[" + username + "] target[" + target +
                "] success[" + success + "] address[" + address + "]");
            return;
        }

        try(Connection connection = database.getConnection())
        {
            audit(connection, database.getAccountRepository(), username, action, target, success, address, detail);
        }
        catch(SQLException se)
        {
            LOGGER.error("Unable to write the web audit entry for " + action, se);
        }
    }

    private void audit(Connection connection, AccountRepository repository, String username, String action,
                       String target, boolean success, String address, String detail)
    {
        try
        {
            repository.insertAudit(connection, new AuditRecord(0, mClock.getAsLong(), username, action, target,
                success, address, detail));
        }
        catch(SQLException se)
        {
            LOGGER.error("Unable to write the web audit entry for " + action, se);
        }
    }

    /**
     * Audit entries, newest first.  Reads one extra row so the caller knows whether there is another page.
     */
    public List<AuditRecord> listAudit(String username, String action, Long since, Long until, Long beforeId,
                                       int limit)
    {
        return withConnection((connection, repository) -> repository.listAudit(connection, username, action, since,
            until, beforeId, limit + 1));
    }

    //----------------------------------------------------------------------------------------------------------------
    // Activity
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Records a request by the account.  The time is kept in memory and written to the database at most once a
     * minute per account and kind.
     * @param api true for an API token request, false for a browser request
     */
    private void recordAccess(Connection connection, AccountRepository repository, long userId, boolean api)
    {
        long now = mClock.getAsLong();
        Access access = mAccess.computeIfAbsent(userId, id -> new Access());
        boolean write;

        synchronized(access)
        {
            if(api)
            {
                access.api = now;
                write = now - access.apiWritten >= ACCESS_WRITE_INTERVAL_MS;
                access.apiWritten = write ? now : access.apiWritten;
            }
            else
            {
                access.web = now;
                write = now - access.webWritten >= ACCESS_WRITE_INTERVAL_MS;
                access.webWritten = write ? now : access.webWritten;
            }
        }

        if(write)
        {
            try
            {
                repository.touchUser(connection, userId, api, now);
            }
            catch(SQLException se)
            {
                LOGGER.error("Unable to record the last access time of web account id " + userId, se);
            }
        }
    }

    /**
     * Who uses the account now: its browser sessions and when it last made a browser or API token request,
     * including requests since the database was last updated.
     */
    public Activity getActivity(UserRecord user)
    {
        long now = mClock.getAsLong();
        List<SessionManager.SessionInfo> sessions = mSessionManager.getSessions(user.id());
        Long web = user.lastWebTime();
        Long api = user.lastApiTime();
        Access access = mAccess.get(user.id());

        if(access != null)
        {
            synchronized(access)
            {
                web = max(web, access.web);
                api = max(api, access.api);
            }
        }

        for(SessionManager.SessionInfo session: sessions)
        {
            web = max(web, session.lastAccess());
        }

        boolean active = (web != null && now - web <= ACTIVE_WINDOW_MS) || (api != null && now - api <= ACTIVE_WINDOW_MS);
        return new Activity(sessions.size(), web, api, active);
    }

    /**
     * Forgets the activity kept in memory, when a different database (with different accounts) is selected
     */
    public void clearActivity()
    {
        mAccess.clear();
    }

    private static Long max(Long a, long b)
    {
        return b <= 0 ? a : a == null ? Long.valueOf(b) : Long.valueOf(Math.max(a, b));
    }

    //----------------------------------------------------------------------------------------------------------------
    // Support
    //----------------------------------------------------------------------------------------------------------------

    private static void checkUsername(String normalized, List<ApiException.FieldError> errors)
    {
        if(normalized == null || !USERNAME.matcher(normalized).matches())
        {
            errors.add(new ApiException.FieldError("username", "must be 2 to 64 characters: letters, digits and " +
                ". _ @ -, starting with a letter or digit"));
        }
    }

    private static void checkDisplayName(String displayName, List<ApiException.FieldError> errors)
    {
        if(displayName != null && displayName.length() > AccountRepository.LENGTH_NAME)
        {
            errors.add(new ApiException.FieldError("displayName", "must be at most " + AccountRepository.LENGTH_NAME +
                " characters"));
        }
    }

    private void clearInitialPassword(long userId)
    {
        if(mInitialAdminId == userId)
        {
            mInitialPassword = null;
        }
    }

    private static String blankToNull(String text)
    {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static String describeChange(UserRecord before, UserRecord after)
    {
        List<String> changes = new ArrayList<>();

        if(!before.role().equals(after.role()))
        {
            changes.add("role " + before.role() + " -> " + after.role());
        }

        if(before.enabled() != after.enabled())
        {
            changes.add(after.enabled() ? "enabled" : "disabled");
        }

        if(!java.util.Objects.equals(before.displayName(), after.displayName()))
        {
            changes.add("display name");
        }

        return changes.isEmpty() ? "no change" : String.join(", ", changes);
    }

    private <T> T withConnection(SqlWork<T> work)
    {
        CallDatabase database = mDatabase.get();

        if(database == null)
        {
            throw ApiException.unavailable("The database isn't available, so accounts can't be checked.  See the " +
                "Database settings.");
        }

        try(Connection connection = database.getConnection())
        {
            return work.run(connection, database.getAccountRepository());
        }
        catch(SQLException se)
        {
            LOGGER.error("Web API account database error", se);
            throw new ApiException(500, "Database error", "The database request failed.  See the application log.");
        }
    }

    @FunctionalInterface
    private interface SqlWork<T>
    {
        T run(Connection connection, AccountRepository repository) throws SQLException;
    }

    /**
     * Account activity
     * @param sessions count of active browser sessions
     * @param lastWebTime epoch milliseconds of the last browser request, or null
     * @param lastApiTime epoch milliseconds of the last API token request, or null
     * @param active true when the account made a request within {@link #ACTIVE_WINDOW_MS}
     */
    public record Activity(int sessions, Long lastWebTime, Long lastApiTime, boolean active) {}

    /**
     * Last access times in memory, and when each was last written to the database
     */
    private static class Access
    {
        private long web;
        private long webWritten;
        private long api;
        private long apiWritten;
    }

    /**
     * New account and its temporary password
     */
    public record CreatedUser(UserRecord user, String temporaryPassword) {}

    /**
     * New API token record and the token itself, which is shown only once
     */
    public record CreatedToken(ApiTokenRecord record, String token) {}
}
