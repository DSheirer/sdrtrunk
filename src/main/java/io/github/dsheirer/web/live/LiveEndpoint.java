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

package io.github.dsheirer.web.live;

import io.github.dsheirer.web.account.AccountService;
import io.github.dsheirer.web.api.ApiException;
import io.github.dsheirer.web.auth.AccessManager;
import io.github.dsheirer.web.auth.PasswordHasher;
import io.github.dsheirer.web.auth.Permission;
import io.github.dsheirer.web.auth.Principal;
import io.github.dsheirer.web.auth.Session;
import io.github.dsheirer.web.auth.SessionManager;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import org.eclipse.jetty.websocket.api.Callback;

import static io.github.dsheirer.web.api.ApiSupport.API_V1;

/**
 * The WebSocket endpoint, /api/v1/ws, and the tickets that open it: /api/v1/auth/ws-ticket.
 *
 * A client authenticates the WebSocket upgrade in one of three ways:
 * <ul>
 *     <li>A one-use ticket from POST /auth/ws-ticket, as the ticket query parameter.  Works for any client,
 *     including a browser connecting to another node, since browsers can't set headers on a WebSocket.</li>
 *     <li>An Authorization: Bearer API token header, for scripts.</li>
 *     <li>The session cookie, only from a page of the same origin (the Origin header is checked).</li>
 * </ul>
 * The caller is checked again every minute while the connection is open.
 */
public class LiveEndpoint
{
    public static final String PATH = API_V1 + "/ws";
    public static final long TICKET_LIFETIME_MS = 30_000;
    private static final String PRINCIPAL_ATTRIBUTE = "sdrtrunk.ws.principal";
    private static final int MAX_TICKETS = 10_000;

    private final LiveHub mHub;
    private final AccountService mAccountService;
    private final SessionManager mSessionManager;
    private final LongSupplier mClock;
    private final Map<String,Ticket> mTickets = new ConcurrentHashMap<>();

    public LiveEndpoint(LiveHub hub, AccountService accountService, SessionManager sessionManager, LongSupplier clock)
    {
        mHub = hub;
        mAccountService = accountService;
        mSessionManager = sessionManager;
        mClock = clock;
    }

    public void register(RoutesConfig routes)
    {
        routes.post(API_V1 + "/auth/ws-ticket", this::createTicket, Permission.AUTHENTICATED);
        routes.wsBeforeUpgrade(PATH, this::authenticate);
        //The upgrade is authenticated above, so the access check treats the route as public
        routes.ws(PATH, ws -> {
            ws.onConnect(ctx -> {
                Principal principal = ctx.attribute(PRINCIPAL_ATTRIBUTE);

                if(principal == null)
                {
                    ctx.closeSession(LiveConnection.CLOSE_UNAUTHORIZED, "Not authenticated");
                    return;
                }

                ctx.session.setIdleTimeout(java.time.Duration.ofMinutes(5));
                ctx.enableAutomaticPings(30, TimeUnit.SECONDS);
                mHub.connect(ctx.sessionId(), principal, new SessionSink(ctx.session));
            });
            ws.onMessage(ctx -> {
                LiveConnection connection = mHub.getConnection(ctx.sessionId());

                if(connection != null)
                {
                    mHub.onMessage(connection, ctx.message());
                }
            });
            ws.onClose(ctx -> mHub.disconnect(ctx.sessionId()));
            ws.onError(ctx -> mHub.disconnect(ctx.sessionId()));
        }, Permission.PUBLIC);
    }

    @OpenApi(path = API_V1 + "/auth/ws-ticket", methods = HttpMethod.POST, tags = "Auth",
        summary = "One-use ticket to open the WebSocket",
        description = "Valid for 30 seconds.  Open ws(s)://host" + PATH + "?ticket=<ticket>.  Topics: " +
            "channels.status (playlist.read), tuners.status (tuner.read), calls and events (events.read), " +
            "playlist (playlist.read), spectrum (spectrum.view, binary frames) and audio.live (audio.listen, binary " +
            "Opus frames).",
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = TicketView.class)))
    private void createTicket(Context ctx)
    {
        Principal principal = AccessManager.principal(ctx);
        purgeTickets();

        if(mTickets.size() >= MAX_TICKETS)
        {
            throw ApiException.tooManyRequests("Too many WebSocket tickets outstanding");
        }

        String ticket = PasswordHasher.createSecret(24);
        mTickets.put(ticket, new Ticket(principal, mClock.getAsLong() + TICKET_LIFETIME_MS));
        ctx.json(new TicketView(ticket, TICKET_LIFETIME_MS / 1000));
    }

    /**
     * Ticket for the WebSocket upgrade
     */
    public record TicketView(String ticket, long expiresInSeconds) {}

    private record Ticket(Principal principal, long expires) {}

    /**
     * Authenticates the WebSocket upgrade request.
     */
    private void authenticate(Context ctx)
    {
        Principal principal = resolve(ctx).orElseThrow(() -> ApiException.unauthorized("Open the WebSocket with " +
            "a ticket from POST " + API_V1 + "/auth/ws-ticket"));

        if(principal.mustChangePassword())
        {
            throw new ApiException(403, "Password change required", "Change the temporary password first");
        }

        mHub.checkConnectionLimit(principal);
        ctx.attribute(PRINCIPAL_ATTRIBUTE, principal);
    }

    private Optional<Principal> resolve(Context ctx)
    {
        String ticket = ctx.queryParam("ticket");

        if(ticket != null)
        {
            Ticket found = mTickets.remove(ticket);

            if(found == null || found.expires() < mClock.getAsLong())
            {
                return Optional.empty();
            }

            //The caller may have signed out or lost permissions since the ticket was issued
            return mAccountService.refresh(found.principal());
        }

        String authorization = ctx.header("Authorization");

        if(authorization != null)
        {
            return authorization.regionMatches(true, 0, "Bearer ", 0, 7) ?
                mAccountService.getPrincipal(authorization.substring(7).trim()) : Optional.empty();
        }

        Optional<Session> session = mSessionManager.touch(ctx.cookie(AccessManager.SESSION_COOKIE));

        if(session.isEmpty())
        {
            return Optional.empty();
        }

        if(!isSameOrigin(ctx))
        {
            throw ApiException.forbidden("A WebSocket from another origin needs a ticket");
        }

        return mAccountService.getPrincipal(session.get());
    }

    /**
     * Indicates if the upgrade request comes from a page served by this server.  Browsers always send the Origin
     * header on a WebSocket upgrade; a request without one isn't from a browser page.
     */
    static boolean isSameOrigin(Context ctx)
    {
        String origin = ctx.header("Origin");

        if(origin == null)
        {
            return true;
        }

        try
        {
            URI uri = URI.create(origin);
            String host = ctx.header("Host");
            String originHost = uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            return host != null && (host.equalsIgnoreCase(originHost) ||
                host.equalsIgnoreCase(uri.getHost()) && uri.getPort() < 0);
        }
        catch(Exception e)
        {
            return false;
        }
    }

    private void purgeTickets()
    {
        long now = mClock.getAsLong();
        mTickets.values().removeIf(ticket -> ticket.expires() < now);
    }

    /**
     * Sends through the Jetty WebSocket session
     */
    private record SessionSink(org.eclipse.jetty.websocket.api.Session session) implements MessageSink
    {
        @Override
        public void send(String text, Runnable succeeded, java.util.function.Consumer<Throwable> failed)
        {
            session.sendText(text, Callback.from(succeeded, failed));
        }

        @Override
        public void sendBinary(byte[] data, Runnable succeeded, java.util.function.Consumer<Throwable> failed)
        {
            session.sendBinary(java.nio.ByteBuffer.wrap(data), Callback.from(succeeded, failed));
        }

        @Override
        public void close(int code, String reason)
        {
            session.close(code, reason, Callback.NOOP);
        }
    }
}
