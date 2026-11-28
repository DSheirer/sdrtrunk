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

import io.github.dsheirer.database.account.AuditRecord;
import io.github.dsheirer.database.account.UserRecord;
import io.github.dsheirer.web.account.AccountService;
import io.github.dsheirer.web.api.AccountViews.ApproveRequest;
import io.github.dsheirer.web.api.AccountViews.AuditView;
import io.github.dsheirer.web.api.AccountViews.CreatedTokenView;
import io.github.dsheirer.web.api.AccountViews.CreatedUserView;
import io.github.dsheirer.web.api.AccountViews.RevokedView;
import io.github.dsheirer.web.api.AccountViews.RoleView;
import io.github.dsheirer.web.api.AccountViews.SessionView;
import io.github.dsheirer.web.api.AccountViews.TemporaryPasswordView;
import io.github.dsheirer.web.api.AccountViews.TokenRequest;
import io.github.dsheirer.web.api.AccountViews.TokenView;
import io.github.dsheirer.web.api.AccountViews.UserAccessView;
import io.github.dsheirer.web.api.AccountViews.UserCreateRequest;
import io.github.dsheirer.web.api.AccountViews.UserUpdateRequest;
import io.github.dsheirer.web.api.AccountViews.UserView;
import io.github.dsheirer.web.auth.AccessManager;
import io.github.dsheirer.web.auth.Permission;
import io.github.dsheirer.web.auth.Role;
import io.github.dsheirer.web.auth.SessionManager;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongSupplier;

import static io.github.dsheirer.web.api.ApiSupport.API_V1;

/**
 * Account administration and the audit log (users.manage): /api/v1/users/..., /api/v1/roles and /api/v1/audit.
 * Administrators approve the accounts that users request, see who is active, reset passwords and give API tokens to
 * accounts.
 */
public class UserController
{
    private final AccountService mAccountService;
    private final SessionManager mSessionManager;
    private final LongSupplier mClock;

    public UserController(AccountService accountService, SessionManager sessionManager, LongSupplier clock)
    {
        mAccountService = accountService;
        mSessionManager = sessionManager;
        mClock = clock;
    }

    public void register(RoutesConfig routes)
    {
        routes.get(API_V1 + "/roles", this::listRoles, Permission.AUTHENTICATED);
        routes.get(API_V1 + "/users", this::listUsers, Permission.USERS_MANAGE);
        routes.post(API_V1 + "/users", this::createUser, Permission.USERS_MANAGE);
        routes.get(API_V1 + "/users/{id}", this::getUser, Permission.USERS_MANAGE);
        routes.patch(API_V1 + "/users/{id}", this::updateUser, Permission.USERS_MANAGE);
        routes.delete(API_V1 + "/users/{id}", this::deleteUser, Permission.USERS_MANAGE);
        routes.post(API_V1 + "/users/{id}/reset-password", this::resetPassword, Permission.USERS_MANAGE);
        routes.post(API_V1 + "/users/{id}/approve", this::approveUser, Permission.USERS_MANAGE);
        routes.get(API_V1 + "/users/{id}/tokens", this::listTokens, Permission.USERS_MANAGE);
        routes.post(API_V1 + "/users/{id}/tokens", this::createToken, Permission.USERS_MANAGE);
        routes.delete(API_V1 + "/users/{id}/tokens/{tokenId}", this::revokeToken, Permission.USERS_MANAGE);
        routes.get(API_V1 + "/users/{id}/sessions", this::getAccess, Permission.USERS_MANAGE);
        routes.delete(API_V1 + "/users/{id}/sessions", this::revokeAll, Permission.USERS_MANAGE);
        routes.get(API_V1 + "/audit", this::listAudit, Permission.USERS_MANAGE);
    }

    @OpenApi(path = API_V1 + "/roles", methods = HttpMethod.GET, tags = "Users",
        summary = "Roles and the permissions each one grants",
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = RoleView[].class)))
    private void listRoles(Context ctx)
    {
        ctx.json(new Page<>(Arrays.stream(Role.values()).map(RoleView::of).toList(), null));
    }

    @OpenApi(path = API_V1 + "/users", methods = HttpMethod.GET, tags = "Users", summary = "List accounts",
        queryParams = {
            @OpenApiParam(name = "limit", type = Integer.class, description = "1 to 500, default 50"),
            @OpenApiParam(name = "cursor", description = "nextCursor from the previous page")
        },
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserView[].class)))
    private void listUsers(Context ctx)
    {
        int limit = ApiSupport.getLimit(ctx);
        Long cursor = ApiSupport.getIdCursor(ctx);
        long now = mClock.getAsLong();
        List<UserRecord> rows = mAccountService.listUsers(cursor != null ? cursor : 0, limit);
        ctx.json(Page.of(rows, limit, user -> String.valueOf(user.id()), user -> view(user, now)));
    }

    @OpenApi(path = API_V1 + "/users", methods = HttpMethod.POST, tags = "Users",
        summary = "Create an account",
        description = "The account gets a temporary password, returned once, that must be changed at the first " +
            "sign-in.",
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = UserCreateRequest.class), required = true),
        responses = {
            @OpenApiResponse(status = "201", content = @OpenApiContent(from = CreatedUserView.class)),
            @OpenApiResponse(status = "409", description = "The username is taken")
        })
    private void createUser(Context ctx)
    {
        UserCreateRequest request = ApiSupport.body(ctx, UserCreateRequest.class);
        AccountService.CreatedUser created = mAccountService.createUser(AccessManager.principal(ctx),
            request.username(), request.displayName(), request.role(), ctx.ip());
        ctx.status(201).json(new CreatedUserView(view(created.user(), mClock.getAsLong()),
            created.temporaryPassword()));
    }

    @OpenApi(path = API_V1 + "/users/{id}", methods = HttpMethod.GET, tags = "Users", summary = "One account",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserView.class)))
    private void getUser(Context ctx)
    {
        ctx.json(view(mAccountService.getUser(ApiSupport.getIdParam(ctx, "id")), mClock.getAsLong()));
    }

    @OpenApi(path = API_V1 + "/users/{id}", methods = HttpMethod.PATCH, tags = "Users",
        summary = "Change an account's display name, role or enabled state",
        description = "Absent fields stay unchanged.  The last enabled Admin can't be demoted or disabled.",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = UserUpdateRequest.class), required = true),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserView.class)),
            @OpenApiResponse(status = "409", description = "Would leave no enabled Admin")
        })
    private void updateUser(Context ctx)
    {
        UserUpdateRequest request = ApiSupport.body(ctx, UserUpdateRequest.class);
        UserRecord user = mAccountService.updateUser(AccessManager.principal(ctx), ApiSupport.getIdParam(ctx, "id"),
            request.displayName(), request.role(), request.enabled(), ctx.ip());
        ctx.json(view(user, mClock.getAsLong()));
    }

    @OpenApi(path = API_V1 + "/users/{id}", methods = HttpMethod.DELETE, tags = "Users",
        summary = "Delete an account and its API tokens",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        responses = {
            @OpenApiResponse(status = "204"),
            @OpenApiResponse(status = "409", description = "The last enabled Admin can't be deleted")
        })
    private void deleteUser(Context ctx)
    {
        mAccountService.deleteUser(AccessManager.principal(ctx), ApiSupport.getIdParam(ctx, "id"), ctx.ip());
        ctx.status(204);
    }

    @OpenApi(path = API_V1 + "/users/{id}/reset-password", methods = HttpMethod.POST, tags = "Users",
        summary = "Issue a temporary password",
        description = "Ends the account's sessions and clears a lockout.  The password must be changed at the next " +
            "sign-in.",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = TemporaryPasswordView.class)))
    private void resetPassword(Context ctx)
    {
        String password = mAccountService.resetPassword(AccessManager.principal(ctx),
            ApiSupport.getIdParam(ctx, "id"), ctx.ip());
        ctx.json(new TemporaryPasswordView(password));
    }

    @OpenApi(path = API_V1 + "/users/{id}/approve", methods = HttpMethod.POST, tags = "Users",
        summary = "Approve an account that a user requested",
        description = "Enables the account with the role, Listener when absent.",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = ApproveRequest.class)),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserView.class)),
            @OpenApiResponse(status = "409", description = "The account doesn't wait for approval")
        })
    private void approveUser(Context ctx)
    {
        ApproveRequest request = ctx.body().isBlank() ? new ApproveRequest(null) :
            ApiSupport.body(ctx, ApproveRequest.class);
        UserRecord user = mAccountService.approveUser(AccessManager.principal(ctx), ApiSupport.getIdParam(ctx, "id"),
            request.role(), ctx.ip());
        ctx.json(view(user, mClock.getAsLong()));
    }

    @OpenApi(path = API_V1 + "/users/{id}/tokens", methods = HttpMethod.GET, tags = "Users",
        summary = "An account's API tokens",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = TokenView[].class)))
    private void listTokens(Context ctx)
    {
        long id = mAccountService.getUser(ApiSupport.getIdParam(ctx, "id")).id();
        long now = mClock.getAsLong();
        ctx.json(new Page<>(mAccountService.listTokens(id).stream().map(token -> TokenView.of(token, now)).toList(),
            null));
    }

    @OpenApi(path = API_V1 + "/users/{id}/tokens", methods = HttpMethod.POST, tags = "Users",
        summary = "Create an API token for an account",
        description = "Needs a browser session.  The token is in the response only: give it to the account's user, " +
            "who sends it as Authorization: Bearer.  Its permissions are limited to the account's role.",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = TokenRequest.class), required = true),
        responses = @OpenApiResponse(status = "201", content = @OpenApiContent(from = CreatedTokenView.class)))
    private void createToken(Context ctx)
    {
        TokenRequest request = ApiSupport.body(ctx, TokenRequest.class);
        AccountService.CreatedToken created = mAccountService.createTokenFor(AccessManager.principal(ctx),
            ApiSupport.getIdParam(ctx, "id"), request.name(), request.expiresInDays(), request.permissions(),
            ctx.ip());
        ctx.status(201).json(new CreatedTokenView(created.token(), TokenView.of(created.record(),
            mClock.getAsLong())));
    }

    @OpenApi(path = API_V1 + "/users/{id}/tokens/{tokenId}", methods = HttpMethod.DELETE, tags = "Users",
        summary = "Revoke one of an account's API tokens",
        pathParams = {
            @OpenApiParam(name = "id", type = Long.class, required = true),
            @OpenApiParam(name = "tokenId", type = Long.class, required = true)
        },
        responses = {
            @OpenApiResponse(status = "204"),
            @OpenApiResponse(status = "404")
        })
    private void revokeToken(Context ctx)
    {
        mAccountService.revokeToken(AccessManager.principal(ctx), ApiSupport.getIdParam(ctx, "id"),
            ApiSupport.getIdParam(ctx, "tokenId"), ctx.ip());
        ctx.status(204);
    }

    private UserView view(UserRecord user, long now)
    {
        return UserView.of(user, mAccountService.getActivity(user), now);
    }

    @OpenApi(path = API_V1 + "/users/{id}/sessions", methods = HttpMethod.GET, tags = "Users",
        summary = "An account's active sessions and API tokens",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserAccessView.class)))
    private void getAccess(Context ctx)
    {
        long id = mAccountService.getUser(ApiSupport.getIdParam(ctx, "id")).id();
        long now = mClock.getAsLong();
        List<SessionView> sessions = mSessionManager.getSessions(id).stream()
            .map(info -> new SessionView(ApiSupport.iso(info.session().createdTime()),
                ApiSupport.iso(info.lastAccess()), info.session().address()))
            .toList();
        List<TokenView> tokens = mAccountService.listTokens(id).stream().map(token -> TokenView.of(token, now))
            .toList();
        ctx.json(new UserAccessView(sessions, tokens));
    }

    @OpenApi(path = API_V1 + "/users/{id}/sessions", methods = HttpMethod.DELETE, tags = "Users",
        summary = "End all of an account's sessions and delete its API tokens",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = RevokedView.class)))
    private void revokeAll(Context ctx)
    {
        ctx.json(new RevokedView(mAccountService.revokeAll(AccessManager.principal(ctx),
            ApiSupport.getIdParam(ctx, "id"), ctx.ip())));
    }

    @OpenApi(path = API_V1 + "/audit", methods = HttpMethod.GET, tags = "Users",
        summary = "Audit log, newest first",
        queryParams = {
            @OpenApiParam(name = "user", description = "Only this account"),
            @OpenApiParam(name = "action", description = "Only this action, for example login.failed"),
            @OpenApiParam(name = "since", description = "ISO-8601 time or epoch milliseconds"),
            @OpenApiParam(name = "until", description = "ISO-8601 time or epoch milliseconds"),
            @OpenApiParam(name = "limit", type = Integer.class, description = "1 to 500, default 50"),
            @OpenApiParam(name = "cursor", description = "nextCursor from the previous page")
        },
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = AuditView[].class)))
    private void listAudit(Context ctx)
    {
        int limit = ApiSupport.getLimit(ctx);
        List<AuditRecord> rows = mAccountService.listAudit(ApiSupport.getTextParam(ctx, "user"),
            ApiSupport.getTextParam(ctx, "action"), ApiSupport.getTimeParam(ctx, "since"),
            ApiSupport.getTimeParam(ctx, "until"), ApiSupport.getIdCursor(ctx), limit);
        ctx.json(Page.of(rows, limit, audit -> String.valueOf(audit.id()), AuditView::of));
    }
}
