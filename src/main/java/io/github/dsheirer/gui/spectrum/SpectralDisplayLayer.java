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

import java.util.Collections;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.MenuItem;

/**
 * Optional layer that draws on the spectral display.  The display calls the render methods on the JavaFX application
 * thread each time it redraws, in the order the layers were added.  Override only the methods the layer needs.
 *
 * Spectrum drawing order: background color, each layer's background, spectrum trace, each layer's foreground.
 * Waterfall drawing order: waterfall image, each layer's waterfall overlay.
 */
public abstract class SpectralDisplayLayer
{
    private SpectralDisplay mSpectralDisplay;

    /**
     * Called by the display when the layer is added.
     */
    void attach(SpectralDisplay spectralDisplay)
    {
        mSpectralDisplay = spectralDisplay;
        attached();
    }

    /**
     * Called by the display when the layer is removed.
     */
    void detach()
    {
        detached();
        mSpectralDisplay = null;
    }

    /**
     * Hook for subclasses after the layer is added to a display.
     */
    protected void attached()
    {
    }

    /**
     * Hook for subclasses before the layer is removed from a display.
     */
    protected void detached()
    {
    }

    /**
     * Display this layer is added to, or null.
     */
    protected SpectralDisplay getSpectralDisplay()
    {
        return mSpectralDisplay;
    }

    /**
     * Frequency model of the display this layer is added to, or null.
     */
    protected SpectralDisplayModel getModel()
    {
        return mSpectralDisplay != null ? mSpectralDisplay.getModel() : null;
    }

    /**
     * Asks the display to redraw on the next frame.  Safe to call from any thread.
     */
    protected void requestRender()
    {
        if(mSpectralDisplay != null)
        {
            mSpectralDisplay.requestRender();
        }
    }

    /**
     * Pixels this layer needs along the bottom of the spectrum section, below the spectrum plot.  The display reserves
     * the largest inset that any layer asks for.
     */
    public double getSpectrumBottomInset()
    {
        return 0;
    }

    /**
     * Draws behind the spectrum trace.
     */
    public void renderSpectrumBackground(GraphicsContext gc, RenderArea area)
    {
    }

    /**
     * Draws on top of the spectrum trace.
     */
    public void renderSpectrumForeground(GraphicsContext gc, RenderArea area)
    {
    }

    /**
     * Draws on top of the waterfall.
     */
    public void renderWaterfall(GraphicsContext gc, RenderArea area)
    {
    }

    /**
     * Context menu items for a right-click at the frequency.
     * @param section that was clicked
     * @param frequency under the mouse
     * @return menu items, or an empty list
     */
    public List<MenuItem> getContextMenuItems(DisplaySection section, long frequency)
    {
        return Collections.emptyList();
    }

    /**
     * Title of this layer's section in the configuration panel, or null to place the layer's rows in the panel's
     * Display section instead of a section of their own.
     */
    public String getConfigurationTitle()
    {
        return getClass().getSimpleName();
    }

    /**
     * Labeled controls for this layer's section of the configuration panel, or an empty list if the layer has no
     * settings.
     */
    public List<ConfigurationRow> getConfigurationRows()
    {
        return Collections.emptyList();
    }

    /**
     * Releases resources and listeners.
     */
    public void dispose()
    {
    }
}
