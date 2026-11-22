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

import java.awt.Image;
import java.awt.Taskbar;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * sdrtrunk application icon for JavaFX stages and the macOS Dock.
 *
 * Call installJavaFx() once the JavaFX toolkit is running, which also sets the macOS Dock icon.  After that, each
 * window that opens without its own icon gets the sdrtrunk icon, so individual windows don't have to set it.
 */
public final class ApplicationIcons
{
    private static final Logger mLog = LoggerFactory.getLogger(ApplicationIcons.class);
    private static final String RESOURCE_PATTERN = "/images/sdrtrunk/sdrtrunk-icon-%d.png";
    private static final int[] SIZES = {16, 24, 32, 48, 64, 128, 256, 512};
    private static final int TASKBAR_SIZE = 512;

    private static List<Image> sAwtImages;
    private static List<javafx.scene.image.Image> sFxImages;
    private static boolean sTaskbarInstalled;
    private static boolean sJavaFxInstalled;

    private ApplicationIcons()
    {
    }

    /**
     * Application icon in each available size, smallest first, as AWT images for the macOS Dock.
     * @return images, or an empty list if the icon resources can't be loaded.
     */
    public static synchronized List<Image> getAwtImages()
    {
        if(sAwtImages == null)
        {
            List<Image> images = new ArrayList<>();

            for(int size: SIZES)
            {
                String path = String.format(RESOURCE_PATTERN, size);

                try(InputStream in = ApplicationIcons.class.getResourceAsStream(path))
                {
                    if(in != null)
                    {
                        Image image = ImageIO.read(in);

                        if(image != null)
                        {
                            images.add(image);
                        }
                    }
                    else
                    {
                        mLog.warn("Application icon resource not found [" + path + "]");
                    }
                }
                catch(IOException ioe)
                {
                    mLog.warn("Unable to load application icon [" + path + "]", ioe);
                }
            }

            sAwtImages = Collections.unmodifiableList(images);
        }

        return sAwtImages;
    }

    /**
     * Application icon in each available size, smallest first, for JavaFX stages.
     * @return images, or an empty list if the icon resources can't be loaded.
     */
    public static synchronized List<javafx.scene.image.Image> getFxImages()
    {
        if(sFxImages == null)
        {
            List<javafx.scene.image.Image> images = new ArrayList<>();

            for(int size: SIZES)
            {
                String path = String.format(RESOURCE_PATTERN, size);

                try(InputStream in = ApplicationIcons.class.getResourceAsStream(path))
                {
                    if(in != null)
                    {
                        javafx.scene.image.Image image = new javafx.scene.image.Image(in);

                        if(!image.isError())
                        {
                            images.add(image);
                        }
                    }
                }
                catch(IOException ioe)
                {
                    mLog.warn("Unable to load application icon [" + path + "]", ioe);
                }
            }

            sFxImages = Collections.unmodifiableList(images);
        }

        return sFxImages;
    }

    /**
     * Sets the application icon on the JavaFX stage if it doesn't already have one.  Call on the FX thread.
     */
    public static void apply(Stage stage)
    {
        if(stage != null && stage.getIcons().isEmpty() && !getFxImages().isEmpty())
        {
            stage.getIcons().setAll(getFxImages());
        }
    }

    /**
     * Sets the macOS Dock icon.  Windows and Linux take the icon from each window instead.  Safe to call more than
     * once.  Does nothing in a headless environment.
     */
    public static synchronized void installTaskbarIcon()
    {
        if(sTaskbarInstalled || java.awt.GraphicsEnvironment.isHeadless())
        {
            return;
        }

        sTaskbarInstalled = true;

        try
        {
            if(Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE))
            {
                List<Image> images = getAwtImages();

                for(int x = 0; x < SIZES.length && x < images.size(); x++)
                {
                    if(SIZES[x] == TASKBAR_SIZE)
                    {
                        Taskbar.getTaskbar().setIconImage(images.get(x));
                    }
                }
            }
        }
        catch(Exception e)
        {
            mLog.debug("Unable to set taskbar icon", e);
        }
    }

    /**
     * Gives each JavaFX stage that shows without an icon the application icon, and closes the startup splash screen
     * if a JavaFX window opens before the main window does.  Safe to call more than once.  The JavaFX toolkit must
     * already be running.
     */
    public static synchronized void installJavaFx()
    {
        if(sJavaFxInstalled)
        {
            return;
        }

        sJavaFxInstalled = true;
        installTaskbarIcon();

        Platform.runLater(() -> {
            for(Window window: Window.getWindows())
            {
                onFxWindowShown(window);
            }

            Window.getWindows().addListener((ListChangeListener<Window>)change -> {
                while(change.next())
                {
                    for(Window window: change.getAddedSubList())
                    {
                        onFxWindowShown(window);
                    }
                }
            });
        });
    }

    /**
     * Handles a JavaFX window that is showing.
     */
    private static void onFxWindowShown(Window window)
    {
        //Only stages are top-level windows.  Popups (menus, tooltips) are other Window types.
        if(window instanceof Stage stage)
        {
            apply(stage);

            //A JavaFX window (e.g. an error dialog) that shows before the main window would sit behind the splash.
            Splash.close();
        }
    }
}
