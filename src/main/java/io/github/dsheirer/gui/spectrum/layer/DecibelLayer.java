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

package io.github.dsheirer.gui.spectrum.layer;

import io.github.dsheirer.gui.spectrum.ConfigurationRow;
import io.github.dsheirer.gui.spectrum.RenderArea;
import io.github.dsheirer.gui.spectrum.SpectralDisplayLayer;
import io.github.dsheirer.gui.spectrum.SpectralDisplayModel;
import io.github.dsheirer.gui.spectrum.SpectralDisplayTheme;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import java.util.List;

/**
 * Decibel scale layer.  Draws horizontal grid lines across the spectrum at even dBFS steps, with labels along the
 * left edge.  The step size (1, 2, 5, 10 or 20 dB) adapts to the spectrum height so the labels don't crowd.
 *
 * The spectrum plot runs from 0 dBFS at the top to minus the model's dynamic range at the bottom.
 */
public class DecibelLayer extends SpectralDisplayLayer
{
    private static final Font FONT = Font.font(11);
    private static final double[] STEPS = {1, 2, 5, 10, 20, 50};
    private static final double MIN_LINE_SPACING = 28;
    private static final double TOP_MARGIN = 22;

    private final BooleanProperty mVisible = new SimpleBooleanProperty(true);
    private final Text mMeasure = new Text();
    private final double mTextHeight;

    /**
     * Constructs an instance.
     */
    public DecibelLayer()
    {
        mMeasure.setFont(FONT);
        mMeasure.setText("-0123456789 dBFS");
        mTextHeight = Math.ceil(mMeasure.getLayoutBounds().getHeight());
        mVisible.addListener((ob, ov, nv) -> requestRender());
    }

    /**
     * Shows or hides the decibel scale.
     */
    public BooleanProperty visibleProperty()
    {
        return mVisible;
    }

    @Override
    public String getConfigurationTitle()
    {
        //A single switch - list it in the Display section instead of a section of its own
        return null;
    }

    @Override
    public List<ConfigurationRow> getConfigurationRows()
    {
        return List.of(ConfigurationRow.toggle("dB Scale", mVisible));
    }

    /**
     * Grid line spacing in dB so the lines are at least MIN_LINE_SPACING pixels apart.
     */
    static double getStep(double range, double plotHeight)
    {
        if(plotHeight <= 0 || range <= 0)
        {
            return STEPS[STEPS.length - 1];
        }

        double pixelsPerDb = plotHeight / range;

        for(double step: STEPS)
        {
            if(step * pixelsPerDb >= MIN_LINE_SPACING)
            {
                return step;
            }
        }

        return STEPS[STEPS.length - 1];
    }

    @Override
    public void renderSpectrumBackground(GraphicsContext gc, RenderArea area)
    {
        SpectralDisplayModel model = getModel();

        if(!mVisible.get() || model == null || !model.hasSource())
        {
            return;
        }

        double range = model.getDynamicRange();
        double step = getStep(range, area.plotHeight());
        gc.setStroke(getSpectralDisplay().getTheme().getLine(0.35));
        gc.setLineWidth(1.0);
        gc.setLineDashes(2.0, 4.0);

        for(double decibels = -step; decibels > -range; decibels -= step)
        {
            double y = Math.floor(getY(decibels, range, area.plotHeight())) + 0.5;
            gc.strokeLine(0, y, area.width(), y);
        }

        gc.setLineDashes((double[])null);
    }

    @Override
    public void renderSpectrumForeground(GraphicsContext gc, RenderArea area)
    {
        SpectralDisplayModel model = getModel();

        if(!mVisible.get() || model == null || !model.hasSource())
        {
            return;
        }

        SpectralDisplayTheme theme = getSpectralDisplay().getTheme();
        double range = model.getDynamicRange();
        double plotHeight = area.plotHeight();
        double step = getStep(range, plotHeight);
        boolean unitShown = false;

        gc.setFont(FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);

        for(double decibels = -step; decibels > -range; decibels -= step)
        {
            double y = getY(decibels, range, plotHeight);

            //Leave room for the zoom indicator at the top and the frequency labels at the bottom
            if(y < TOP_MARGIN || y > plotHeight - (mTextHeight / 2.0))
            {
                continue;
            }

            String label = format(decibels) + (unitShown ? "" : " dBFS");
            unitShown = true;
            mMeasure.setText(label);
            double width = mMeasure.getLayoutBounds().getWidth();
            gc.setFill(theme.getBackground(0.6));
            gc.fillRoundRect(2, y - (mTextHeight / 2.0), width + 6, mTextHeight, 4, 4);
            gc.setFill(theme.getLine());
            gc.fillText(label, 5, y);
        }
    }

    private static double getY(double decibels, double range, double plotHeight)
    {
        return -decibels / range * plotHeight;
    }

    private static String format(double decibels)
    {
        return decibels == Math.rint(decibels) ? String.valueOf((long)decibels) : String.valueOf(decibels);
    }
}
