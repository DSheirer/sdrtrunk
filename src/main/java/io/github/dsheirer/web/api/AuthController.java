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

import io.github.dsheirer.database.account.UserRecord;
import io.github.dsheirer.web.account.AccountService;
import io.github.dsheirer.web.api.AccountViews.AuthOptionsView;
import io.github.dsheirer.web.api.AccountViews.CreatedTokenView;
import io.github.dsheirer.web.api.AccountViews.LoginRequest;
import io.github.dsheirer.web.api.AccountViews.MeView;
import io.github.dsheirer.web.api.AccountViews.PasswordChangeRequest;
import io.github.dsheirer.web.api.AccountViews.RegisterRequest;
import io.github.dsheirer.web.api.AccountViews.RegisteredView;
import io.github.dsheirer.web.api.AccountViews.TokenRequest;
import io.github.dsheirer.web.api.AccountViews.TokenView;
import io.github.dsheirer.web.auth.AccessManager;
import io.github.dsheirer.web.auth.AccessRule;
import io.github.dsheirer.web.auth.Permission;
import io.github.dsheirer.web.auth.Principal;
import io.github.dsheirer.web.auth.Session;
import io.github.dsheirer.web.auth.SessionManager;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.SameSite;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import static io.github.dsheirer.web.api.ApiSupport.API_V1;

/**
 * Sign-in, sign-out, the caller's own account, password and API tokens: /api/v1/auth/...
 */
public class AuthController
{
    private final AccountService mAccountService;
    private final SessionManager mSessionManager;
    private final BooleanSupplier mSecureCookies;
    private final LongSupplier mClock;

    /**
     * Constructs an instance
     * @param secureCookies true when the server uses TLS, so the session cookie is only sent over HTTPS
     */
    public AuthController(AccountService accountService, SessionManager sessionManager, BooleanSupplier secureCookies,
                          LongSupplier clock)
    {
        mAccountService = accountService;
        mSessionManager = sessionManager;
        mSecureCookies = secureCookies;
        mClock = clock;
    }

    public void register(RoutesConfig routes)
    {
        routes.post(API_V1 + "/auth/login", this::login, Permission.PUBLIC);
        routes.get(API_V1 + "/auth/options", this::options, Permission.PUBLIC);
        routes.post(API_V1 + "/auth/register", this::register, Permission.PUBLIC);
        routes.post(API_V1 + "/auth/logout", this::logout, Permission.AUTHENTICATED,
            AccessRule.ALLOW_PENDING_PASSWORD_CHANGE);
        routes.get(API_V1 + "/auth/me", this::me, Permission.AUTHENTICATED, AccessRule.ALLOW_PENDING_PASSWORD_CHANGE);
        routes.post(API_V1 + "/auth/password", this::changePassword, Permission.AUTHENTICATED,
            AccessRule.ALLOW_PENDING_PASSWORD_CHANGE);
        routes.get(API_V1 + "/auth/tokens", this::listTokens, Permission.AUTHENTICATED);
        routes.post(API_V1 + "/auth/tokens", this::createToken, Permission.AUTHENTICATED);
        routes.delete(API_V1 + "/auth/tokens/{id}", this::revokeToken, Permission.AUTHENTICATED);
    }

    @OpenApi(path = API_V1 + "/auth/login", methods = HttpMethod.POST, tags = "Auth",
        summary = "Start a browser session",
        description = "Sets the HttpOnly session cookie and returns the account with the CSRF token that " +
            "state-changing requests must send in the X-CSRF-Token header.",
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = LoginRequest.class), required = true),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = MeView.class)),
            @OpenApiResponse(status = "401", description = "Wrong username or password, or the account is locked"),
            @OpenApiResponse(status = "429", description = "Too many attempts from this address")
        })
    private void login(Context ctx)
    {
        LoginRequest request = ApiSupport.body(ctx, LoginRequest.class);
        UserRecord user = mAccountService.login(request.username(), request.password(), ctx.ip());

        //End the session that the browser held before, if any
        mSessionManager.invalidate(ctx.cookie(AccessManager.SESSION_COOKIE));
        Session session = mSessionManager.create(user.id(), user.username(), ctx.ip());
        ctx.cookie(new Cookie(AccessManager.SESSION_COOKIE, session.id(), "/", -1, mSecureCookies.getAsBoolean(),
            true, null, SameSite.STRICT));

        Principal principal = mAccountService.getPrincipal(session)
            .orElseThrow(() -> ApiException.unauthorized("The account changed during sign-in"));
        ctx.json(MeView.of(principal));
    }

    @OpenApi(path = API_V1 + "/auth/options", methods = HttpMethod.GET, tags = "Auth",
        summary = "What the sign-in page offers, such as account requests",
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = AuthOptionsView.class)))
    private void options(Context ctx)
    {
        ctx.json(new AuthOptionsView(mAccountService.isRegistrationAllowed()));
    }

    @OpenApi(path = API_V1 + "/auth/register", methods = HttpMethod.POST, tags = "Auth",
        summary = "Request an account",
        description = "Creates a disabled account with the chosen password.  It can sign in after an administrator " +
            "approves it.",
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = RegisterRequest.class), required = true),
        responses = {
            @OpenApiResponse(status = "201", content = @OpenApiContent(from = RegisteredView.class)),
            @OpenApiResponse(status = "400", description = "Invalid username or password"),
            @OpenApiResponse(status = "403", description = "The server doesn't accept account requests"),
            @OpenApiResponse(status = "409", description = "The username is taken"),
            @OpenApiResponse(status = "429", description = "Too many requests from this address")
        })
    private void register(Context ctx)
    {
        RegisterRequest request = ApiSupport.body(ctx, RegisterRequest.class);
        UserRecord user = mAccountService.register(request.username(), request.displayName(), request.password(),
            ctx.ip());
        ctx.status(201).json(new RegisteredView(user.username(), "Your account request was sent.  You can sign in " +
            "after an administrator approves it."));
    }

    @OpenApi(path = API_V1 + "/auth/logout", methods = HttpMethod.POST, tags = "Auth",
        summary = "End the browser session", responses = @OpenApiResponse(status = "204"))
    private void logout(Context ctx)
    {
        Principal principal = AccessManager.principal(ctx);
        mSessionManager.invalidate(principal.sessionId());
        ctx.removeCookie(AccessManager.SESSION_COOKIE, "/");
        mAccountService.audit(principal.username(), "logout", null, true, ctx.ip(), null);
        ctx.status(204);
    }

    @OpenApi(path = API_V1 + "/auth/me", methods = HttpMethod.GET, tags = "Auth",
        summary = "Current account, role and effective permissions",
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = MeView.class)),
            @OpenApiResponse(status = "401", description = "Not signed in")
        })
    private void me(Context ctx)
    {
        ctx.json(MeView.of(AccessManager.principal(ctx)));
    }

    @OpenApi(path = API_V1 + "/auth/password", methods = HttpMethod.POST, tags = "Auth",
        summary = "Change your own password",
        description = "Needs a browser session.  Other sessions of the account end.",
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = PasswordChangeRequest.class),
            required = true),
        responses = {
            @OpenApiResponse(status = "204"),
            @OpenApiResponse(status = "400", description = "Wrong current password, or the new one breaks the rules")
        })
    private void changePassword(Context ctx)
    {
        Principal principal = AccessManager.principal(ctx);

        if(principal.method() != Principal.Method.SESSION)
        {
            throw ApiException.forbidden("Change the password from a signed-in browser session");
        }

        PasswordChangeRequest request = ApiSupport.body(ctx, PasswordChangeRequest.class);
        mAccountService.changePassword(principal, request.currentPassword(), request.newPassword(), ctx.ip());
        ctx.status(204);
    }

    @OpenApi(path = API_V1 + "/auth/tokens", methods = HttpMethod.GET, tags = "Auth",
        summary = "Your API tokens",
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = TokenView[].class)))
    private void listTokens(Context ctx)
    {
        long now = mClock.getAsLong();
        List<TokenView> tokens = mAccountService.listTokens(AccessManager.principal(ctx).userId()).stream()
            .map(token -> TokenView.of(token, now)).toList();
        ctx.json(new Page<>(tokens, null));
    }

    @OpenApi(path = API_V1 + "/auth/tokens", methods = HttpMethod.POST, tags = "Auth",
        summary = "Create an API token",
        description = "Needs a browser session.  The token is in the response only; store it now.",
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = TokenRequest.class), required = true),
        responses = @OpenApiResponse(status = "201", content = @OpenApiContent(from = CreatedTokenView.class)))
    private void createToken(Context ctx)
    {
        TokenRequest request = ApiSupport.body(ctx, TokenRequest.class);
        AccountService.CreatedToken created = mAccountService.createToken(AccessManager.principal(ctx),
            request.name(), request.expiresInDays(), request.permissions(), ctx.ip());
        ctx.status(201).json(new CreatedTokenView(created.token(), TokenView.of(created.record(),
            mClock.getAsLong())));
    }

    @OpenApi(path = API_V1 + "/auth/tokens/{id}", methods = HttpMethod.DELETE, tags = "Auth",
        summary = "Revoke one of your API tokens",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        responses = {
            @OpenApiResponse(status = "204"),
            @OpenApiResponse(status = "404")
        })
    private void revokeToken(Context ctx)
    {
        mAccountService.revokeToken(AccessManager.principal(ctx), ApiSupport.getIdParam(ctx, "id"), ctx.ip());
        ctx.status(204);
    }
}
