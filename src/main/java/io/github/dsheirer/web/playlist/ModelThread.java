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

package io.github.dsheirer.web.playlist;

import io.github.dsheirer.controller.NamingThreadFactory;
import io.github.dsheirer.gui.FxToolkit;
import io.github.dsheirer.web.api.ApiException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs web API changes to the playlist model (channels, aliases, channel processing) on the thread that owns it.
 *
 * With the user interface running, that is the JavaFX application thread: the editors and tables bind to the same
 * channel and alias objects, and the change notifications reach them there.  Headless, a single web edit thread
 * keeps the changes in order.  Running on one thread also makes a version check and the change that follows it
 * atomic with respect to other edits.
 */
public final class ModelThread
{
    private static final long TIMEOUT_SECONDS = 30;
    private static final ExecutorService HEADLESS = Executors.newSingleThreadExecutor(
        new NamingThreadFactory("sdrtrunk web playlist edits"));

    private ModelThread()
    {
    }

    /**
     * Runs the work on the model thread and waits for its result.
     * @throws ApiException thrown by the work, or 503 when the model thread doesn't respond
     */
    public static <T> T call(Callable<T> work)
    {
        try
        {
            if(FxToolkit.isStarted())
            {
                return FxToolkit.callAndWait(work);
            }

            Future<T> future = HEADLESS.submit(work);

            try
            {
                return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            catch(ExecutionException e)
            {
                throw e.getCause() instanceof Exception exception ? exception : e;
            }
        }
        catch(ApiException e)
        {
            throw e;
        }
        catch(TimeoutException e)
        {
            throw ApiException.unavailable("The playlist is busy; try again");
        }
        catch(RuntimeException e)
        {
            throw e;
        }
        catch(Exception e)
        {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
