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

package io.github.dsheirer.gui.spectrum;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Text and placement of the cursor readout: the frequency under the mouse, and the level in the spectrum, in a box
 * beside the mouse.  The frequency layer draws the readout.  A layer that replaces the readout with a larger callout
 * (see {@link SpectralDisplayLayer#replacesCursorReadout}) uses the same text and placement, so the readout appears to
 * grow into the callout.
 */
public final class CursorReadout
{
    /**
     * Horizontal distance between the mouse and the box
     */
    public static final double GAP_X = 8;

    /**
     * Vertical distance between the box and the mouse below it
     */
    public static final double GAP_Y = 4;

    private static final DecimalFormat FREQUENCY_FORMAT = new DecimalFormat("0.00000");
    private static final DecimalFormat LEVEL_FORMAT = new DecimalFormat("0.0");

    private CursorReadout()
    {
    }

    /**
     * Readout lines: the frequency under the mouse, and the level when the mouse is over the spectrum plot
     */
    public static List<String> lines(DisplayCursor cursor, RenderArea area, SpectralDisplayModel model)
    {
        List<String> lines = new ArrayList<>(2);
        lines.add(FREQUENCY_FORMAT.format(cursor.frequency() / 1E6) + " MHz");

        if(cursor.section() == DisplaySection.SPECTRUM && cursor.y() <= area.plotHeight() && area.plotHeight() > 0)
        {
            double decibels = -(cursor.y() / area.plotHeight()) * model.getDynamicRange();
            lines.add(LEVEL_FORMAT.format(decibels) + " dBFS");
        }

        return lines;
    }

    /**
     * Left edge of a box beside the mouse: to the right of the mouse, or to the left when it doesn't fit
     */
    public static double x(DisplayCursor cursor, double width, RenderArea area)
    {
        double x = cursor.x() + GAP_X;

        if(x + width > area.width())
        {
            x = cursor.x() - GAP_X - width;
        }

        return x;
    }

    /**
     * Top edge of a box above the mouse, kept inside the plot
     */
    public static double y(DisplayCursor cursor, double height, RenderArea area)
    {
        return Math.max(0, Math.min(area.plotHeight() - height, cursor.y() - height - GAP_Y));
    }
}
