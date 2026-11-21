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

import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.web.auth.Permission;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.function.Supplier;

import static io.github.dsheirer.web.api.ApiSupport.API_V1;

/**
 * Instance identity, version and resource use: /api/v1/system
 */
public class SystemController
{
    private final Supplier<String> mInstanceId;
    private final DatabaseManager mDatabaseManager;
    private final String mVersion;
    private final String mNodeName;

    /**
     * Constructs an instance
     * @param instanceId persistent id of this instance
     * @param databaseManager for the database status, or null
     */
    public SystemController(Supplier<String> instanceId, DatabaseManager databaseManager)
    {
        mInstanceId = instanceId;
        mDatabaseManager = databaseManager;
        String version = SystemController.class.getPackage().getImplementationVersion();
        mVersion = version != null ? version : "development";
        mNodeName = getHostName();
    }

    public void register(RoutesConfig routes)
    {
        routes.get(API_V1 + "/system", this::getSystem, Permission.AUTHENTICATED);
    }

    @OpenApi(path = API_V1 + "/system", methods = HttpMethod.GET, tags = "System",
        summary = "Instance identity, version, uptime and resource use",
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = SystemView.class)))
    private void getSystem(Context ctx)
    {
        Runtime runtime = Runtime.getRuntime();
        long start = ManagementFactory.getRuntimeMXBean().getStartTime();
        DatabaseView database = mDatabaseManager == null ? null :
            new DatabaseView(mDatabaseManager.getConfiguration().type().name(), mDatabaseManager.isConnected());

        ctx.json(new SystemView(mInstanceId.get(), mNodeName, mVersion, ApiSupport.iso(start),
            System.currentTimeMillis() - start, System.getProperty("java.version"),
            System.getProperty("os.name") + " " + System.getProperty("os.version"), System.getProperty("os.arch"),
            runtime.availableProcessors(), new MemoryView(runtime.totalMemory() - runtime.freeMemory(),
            runtime.maxMemory()), database));
    }

    private static String getHostName()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch(Exception e)
        {
            return "sdrtrunk";
        }
    }

    /**
     * @param instanceId persistent UUID of this instance
     * @param nodeName host name, for display
     * @param startTime when the application started
     */
    public record SystemView(String instanceId, String nodeName, String version, String startTime, long uptimeMs,
                             String javaVersion, String os, String arch, int processors, MemoryView memory,
                             DatabaseView database) {}

    public record MemoryView(long heapUsedBytes, long heapMaxBytes) {}

    public record DatabaseView(String type, boolean connected) {}
}
