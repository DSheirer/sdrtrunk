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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.SplashScreen;

/**
 * Startup splash screen.
 *
 * The JVM shows the splash image named by the -splash:path option (set by the launch scripts) before any application
 * code runs, and closes it when the main window becomes visible.  This class draws the version on the splash and lets
 * the application close the splash early, for example when a dialog has to show before the main window.
 *
 * Headless operation: the java launcher doesn't show the splash when -Djava.awt.headless=true is on the command line
 * (for example in JAVA_OPTS or SDR_TRUNK_OPTS), and there is nothing to show it on without a display.  This class does
 * nothing when headless.
 */
public final class Splash
{
    //Splash image layout, in pixels of the 640 x 360 image.  The version goes under the tagline.
    private static final double IMAGE_WIDTH = 640.0;
    private static final double VERSION_X = 214.0;
    private static final double VERSION_Y = 232.0;
    private static final double VERSION_FONT_SIZE = 13.0;
    private static final Color VERSION_COLOR = new Color(0x2E, 0xE6, 0xA6);

    private Splash()
    {
    }

    /**
     * Draws the version text on the splash screen, if a splash screen is showing.
     * @param version to show, for example "v1.0.1", or null to leave the splash unchanged.
     */
    public static void showVersion(String version)
    {
        if(version == null || version.isBlank() || GraphicsEnvironment.isHeadless())
        {
            return;
        }

        try
        {
            SplashScreen splash = SplashScreen.getSplashScreen();

            if(splash == null || !splash.isVisible())
            {
                return;
            }

            //Overlay coordinates are in the splash's size, which can differ from the image (HiDPI @2x image).
            Dimension size = splash.getSize();
            double scale = size.width / IMAGE_WIDTH;

            Graphics2D g = splash.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(VERSION_COLOR);
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, (int)Math.round(VERSION_FONT_SIZE * scale)));
            g.drawString(version, (float)(VERSION_X * scale), (float)(VERSION_Y * scale));
            g.dispose();
            splash.update();
        }
        catch(Exception e)
        {
            //The splash is cosmetic.  Never let it stop the application from starting.
        }
    }

    /**
     * Closes the splash screen if it is still showing.
     */
    public static void close()
    {
        if(GraphicsEnvironment.isHeadless())
        {
            return;
        }

        try
        {
            SplashScreen splash = SplashScreen.getSplashScreen();

            if(splash != null && splash.isVisible())
            {
                splash.close();
            }
        }
        catch(Exception e)
        {
            //The splash is cosmetic.  Never let it stop the application from starting.
        }
    }
}
