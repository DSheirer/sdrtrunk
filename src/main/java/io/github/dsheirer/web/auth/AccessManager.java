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

package io.github.dsheirer.web.auth;

import io.github.dsheirer.web.account.AccountService;
import io.github.dsheirer.web.api.ApiException;
import io.github.dsheirer.web.api.ApiSupport;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.security.RouteRole;
import java.util.Optional;
import java.util.Set;

/**
 * Checks every web API request before its handler runs: resolves the caller from the bearer token or the session
 * cookie, then checks the route's {@link Permission}, the pending password change rule and, for browser sessions,
 * the CSRF token on state-changing requests.
 *
 * A route without a permission is refused, so a forgotten declaration fails closed.
 */
public class AccessManager implements Handler
{
    public static final String SESSION_COOKIE = "sdrtrunk_session";
    public static final String CSRF_HEADER = "X-CSRF-Token";
    private static final String PRINCIPAL_ATTRIBUTE = "sdrtrunk.principal";
    private static final String BEARER = "Bearer ";

    private final AccountService mAccountService;
    private final SessionManager mSessionManager;

    public AccessManager(AccountService accountService, SessionManager sessionManager)
    {
        mAccountService = accountService;
        mSessionManager = sessionManager;
    }

    @Override
    public void handle(Context ctx)
    {
        Set<RouteRole> roles = ctx.routeRoles();
        Permission required = roles.stream().filter(Permission.class::isInstance).map(Permission.class::cast)
            .findFirst().orElse(null);

        if(required == null)
        {
            throw ApiException.forbidden("This endpoint doesn't declare a permission");
        }

        if(required == Permission.PUBLIC)
        {
            return;
        }

        Principal principal = resolve(ctx)
            .orElseThrow(() -> ApiException.unauthorized("Sign in, or send an API token as Authorization: Bearer"));

        if(principal.mustChangePassword() && !roles.contains(AccessRule.ALLOW_PENDING_PASSWORD_CHANGE))
        {
            throw new ApiException(403, "Password change required", "Change the temporary password with POST " +
                ApiSupport.API_V1 + "/auth/password first");
        }

        if(!principal.has(required))
        {
            mAccountService.audit(principal.username(), "access.denied", ctx.method() + " " + ctx.path(), false,
                ctx.ip(), "Requires " + required.getKey());
            throw ApiException.forbidden(required.getKey() + " required");
        }

        if(principal.method() == Principal.Method.SESSION && ApiSupport.isStateChanging(ctx) &&
            !principal.csrfToken().equals(ctx.header(CSRF_HEADER)))
        {
            throw ApiException.forbidden("The " + CSRF_HEADER + " header is missing or wrong.  Use the csrfToken " +
                "from GET " + ApiSupport.API_V1 + "/auth/me.");
        }

        ctx.attribute(PRINCIPAL_ATTRIBUTE, principal);
    }

    /**
     * Caller of the request, set by the access check.
     * @throws ApiException 401 for a public route, which has no caller
     */
    public static Principal principal(Context ctx)
    {
        Principal principal = ctx.attribute(PRINCIPAL_ATTRIBUTE);

        if(principal == null)
        {
            throw ApiException.unauthorized("Sign in first");
        }

        return principal;
    }

    /**
     * Resolves the caller from the Authorization header, or else the session cookie.  A bearer token that isn't valid
     * is refused even when there is also a valid session cookie, so a client never acts with credentials it didn't
     * mean to use.
     */
    private Optional<Principal> resolve(Context ctx)
    {
        String authorization = ctx.header("Authorization");

        if(authorization != null)
        {
            if(!authorization.regionMatches(true, 0, BEARER, 0, BEARER.length()))
            {
                return Optional.empty();
            }

            return mAccountService.getPrincipal(authorization.substring(BEARER.length()).trim());
        }

        Optional<Session> session = mSessionManager.touch(ctx.cookie(SESSION_COOKIE));

        if(session.isEmpty())
        {
            return Optional.empty();
        }

        Optional<Principal> principal = mAccountService.getPrincipal(session.get());

        if(principal.isEmpty())
        {
            mSessionManager.invalidate(session.get().id());
        }

        return principal;
    }
}
