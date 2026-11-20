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

package io.github.dsheirer.gui;

import java.awt.image.BufferedImage;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

/**
 * Starts the JavaFX toolkit, runs work on the JavaFX application thread and converts images for ImageIO.
 */
public class FxToolkit
{
    private static boolean sStarted;

    private FxToolkit()
    {
    }

    /**
     * Starts the JavaFX toolkit if it isn't running.  Safe to call more than once and from any thread except the
     * JavaFX application thread before the toolkit starts.
     */
    public static synchronized void ensureStarted()
    {
        if(sStarted)
        {
            return;
        }

        try
        {
            Platform.startup(() -> {});
        }
        catch(IllegalStateException alreadyStarted)
        {
            //The toolkit is already running
        }

        Platform.setImplicitExit(false);
        ApplicationIcons.installJavaFx();
        sStarted = true;
    }

    /**
     * Indicates if the toolkit was started.
     */
    public static synchronized boolean isStarted()
    {
        return sStarted;
    }

    /**
     * Runs the callable on the JavaFX application thread and waits for the result.
     * @throws Exception thrown by the callable
     */
    public static <T> T callAndWait(Callable<T> callable) throws Exception
    {
        if(Platform.isFxApplicationThread())
        {
            return callable.call();
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try
            {
                future.complete(callable.call());
            }
            catch(Throwable t)
            {
                future.completeExceptionally(t);
            }
        });

        try
        {
            return future.get();
        }
        catch(ExecutionException e)
        {
            if(e.getCause() instanceof Exception exception)
            {
                throw exception;
            }

            throw e;
        }
    }

    /**
     * Converts the JavaFX image to an AWT image for writing with ImageIO.
     */
    public static BufferedImage toBufferedImage(Image image)
    {
        int width = (int)image.getWidth();
        int height = (int)image.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        int[] row = new int[width];

        for(int y = 0; y < height; y++)
        {
            reader.getPixels(0, y, width, 1, javafx.scene.image.PixelFormat.getIntArgbInstance(), row, 0, width);
            bufferedImage.setRGB(0, y, width, 1, row, 0, width);
        }

        return bufferedImage;
    }
}
