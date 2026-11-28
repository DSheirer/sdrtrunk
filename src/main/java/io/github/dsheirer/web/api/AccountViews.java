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

package io.github.dsheirer.web.api;

import io.github.dsheirer.database.account.ApiTokenRecord;
import io.github.dsheirer.database.account.AuditRecord;
import io.github.dsheirer.database.account.UserRecord;
import io.github.dsheirer.web.account.AccountService;
import io.github.dsheirer.web.auth.Permission;
import io.github.dsheirer.web.auth.Principal;
import io.github.dsheirer.web.auth.Role;
import java.util.Comparator;
import java.util.List;

/**
 * JSON representations of the account resources.  Timestamps are ISO-8601 UTC strings.
 */
public final class AccountViews
{
    private AccountViews()
    {
    }

    /**
     * An account, as administrators see it
     * @param pendingApproval true when a user requested the account from the sign-in page and an administrator
     * hasn't approved it yet
     * @param sessions count of active browser sessions
     * @param lastWebAccess time of the last request from a signed-in browser, or null
     * @param lastApiAccess time of the last request with one of the account's API tokens, or null
     * @param active true when the account made a request in the last five minutes
     */
    public record UserView(long id, String username, String displayName, String role, boolean enabled,
                           boolean pendingApproval, boolean mustChangePassword, boolean locked, String created,
                           String lastLogin, int sessions, String lastWebAccess, Long lastWebAccessMs,
                           String lastApiAccess, Long lastApiAccessMs, boolean active)
    {
        public static UserView of(UserRecord user, AccountService.Activity activity, long now)
        {
            return new UserView(user.id(), user.username(), user.displayName(), user.role(), user.enabled(),
                user.pendingApproval(), user.mustChangePassword(),
                user.lockedUntil() != null && user.lockedUntil() > now, ApiSupport.iso(user.createdTime()),
                ApiSupport.iso(user.lastLoginTime()), activity.sessions(), ApiSupport.iso(activity.lastWebTime()),
                activity.lastWebTime(), ApiSupport.iso(activity.lastApiTime()), activity.lastApiTime(),
                activity.active());
        }
    }

    /**
     * What the sign-in page offers
     * @param registration true when users may request accounts
     */
    public record AuthOptionsView(boolean registration) {}

    /**
     * Result of an account request
     */
    public record RegisteredView(String username, String message) {}

    /**
     * The caller's own account and effective permissions
     * @param csrfToken that a browser session sends in the X-CSRF-Token header of state-changing requests, or null for
     * an API token
     */
    public record MeView(long id, String username, String displayName, String role, List<String> permissions,
                         boolean mustChangePassword, String authMethod, String csrfToken)
    {
        public static MeView of(Principal principal)
        {
            return new MeView(principal.userId(), principal.username(), principal.displayName(),
                principal.role().name(), principal.permissions().stream().sorted(Comparator.naturalOrder())
                .map(Permission::getKey).toList(), principal.mustChangePassword(), principal.method().name(),
                principal.csrfToken());
        }
    }

    /**
     * An API token, without the secret
     * @param permissions that limit the token, or null for all of the owner's permissions
     */
    public record TokenView(long id, String name, String prefix, List<String> permissions, String created,
                            String expires, String lastUsed, boolean expired)
    {
        public static TokenView of(ApiTokenRecord token, long now)
        {
            List<String> permissions = token.permissions() == null ? null :
                AccountService.parsePermissions(token.permissions()).stream().map(Permission::getKey).toList();
            return new TokenView(token.id(), token.name(), token.prefix(), permissions,
                ApiSupport.iso(token.createdTime()), ApiSupport.iso(token.expiresTime()),
                ApiSupport.iso(token.lastUsedTime()), token.isExpired(now));
        }
    }

    /**
     * A new API token, with the secret that is shown only once
     */
    public record CreatedTokenView(String token, TokenView details) {}

    /**
     * A new account with its temporary password, which must be changed at the first sign-in
     */
    public record CreatedUserView(UserView user, String temporaryPassword) {}

    /**
     * A new temporary password
     */
    public record TemporaryPasswordView(String temporaryPassword) {}

    /**
     * An audit log entry
     */
    public record AuditView(long id, String time, long timeMs, String username, String action, String target,
                            boolean success, String address, String detail)
    {
        public static AuditView of(AuditRecord audit)
        {
            return new AuditView(audit.id(), ApiSupport.iso(audit.time()), audit.time(), audit.username(),
                audit.action(), audit.target(), audit.success(), audit.address(), audit.detail());
        }
    }

    /**
     * An active browser session
     */
    public record SessionView(String created, String lastAccess, String address) {}

    /**
     * An account's active sessions and API tokens
     */
    public record UserAccessView(List<SessionView> sessions, List<TokenView> tokens) {}

    /**
     * Count of sessions and tokens revoked
     */
    public record RevokedView(int revoked) {}

    /**
     * A role and the permissions it grants
     */
    public record RoleView(String name, String label, List<String> permissions)
    {
        public static RoleView of(Role role)
        {
            return new RoleView(role.name(), role.getLabel(), role.getPermissions().stream()
                .sorted(Comparator.naturalOrder()).map(Permission::getKey).toList());
        }
    }

    //Request bodies

    public record LoginRequest(String username, String password) {}

    public record PasswordChangeRequest(String currentPassword, String newPassword) {}

    /**
     * @param expiresInDays lifetime, or null for no expiry
     * @param permissions to limit the token to, or null for all of the caller's permissions
     */
    public record TokenRequest(String name, Integer expiresInDays, List<String> permissions) {}

    public record UserCreateRequest(String username, String displayName, String role) {}

    public record RegisterRequest(String username, String displayName, String password) {}

    /**
     * @param role for the approved account, or null for Listener
     */
    public record ApproveRequest(String role) {}

    /**
     * Fields that are absent (or null) stay unchanged
     */
    public record UserUpdateRequest(String displayName, String role, Boolean enabled) {}
}
