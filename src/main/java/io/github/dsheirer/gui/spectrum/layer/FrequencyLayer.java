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

import io.github.dsheirer.gui.spectrum.DisplayCursor;
import io.github.dsheirer.gui.spectrum.DisplaySection;
import io.github.dsheirer.gui.spectrum.RenderArea;
import io.github.dsheirer.gui.spectrum.SpectralDisplayTheme;
import io.github.dsheirer.gui.spectrum.SpectralDisplayLayer;
import io.github.dsheirer.gui.spectrum.SpectralDisplayModel;
import java.text.DecimalFormat;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Frequency axis layer.  Draws frequency labels and tick marks along the bottom of the spectrum, faint vertical grid
 * lines at each label, and a cursor line with a frequency readout that follows the mouse across the spectrum and the
 * waterfall.  Over the spectrum, the readout also shows the level under the mouse in dB.
 */
public class FrequencyLayer extends SpectralDisplayLayer
{
    private static final Font FONT = Font.font(11);
    private static final double INSET_PADDING = 8.0;
    private static final double MAJOR_TICK = 6.0;
    private static final double MINOR_TICK = 3.0;
    private static final DecimalFormat CURSOR_FREQUENCY_FORMAT = new DecimalFormat("0.00000");
    private static final DecimalFormat CURSOR_LEVEL_FORMAT = new DecimalFormat("0.0");

    private final Text mMeasure = new Text();
    private final double mTextHeight;
    private final DecimalFormat mLabelFormat = new DecimalFormat("0.0");

    /**
     * Constructs an instance.
     */
    public FrequencyLayer()
    {
        mMeasure.setFont(FONT);
        mMeasure.setText("0123456789.");
        mTextHeight = Math.ceil(mMeasure.getLayoutBounds().getHeight());
    }

    @Override
    public String getConfigurationTitle()
    {
        return "Frequency";
    }

    @Override
    public double getSpectrumBottomInset()
    {
        return mTextHeight + INSET_PADDING;
    }

    @Override
    public void renderSpectrumBackground(GraphicsContext gc, RenderArea area)
    {
        SpectralDisplayModel model = getModel();

        if(model == null || !model.hasSource())
        {
            return;
        }

        Scale scale = getScale(model, area.width());
        gc.setStroke(getTheme().getLine(0.35));
        gc.setLineWidth(1.0);
        gc.setLineDashes(2.0, 4.0);

        for(long frequency = firstMultiple(model.getDisplayMinFrequency(), scale.labelStep());
            frequency <= model.getDisplayMaxFrequency(); frequency += scale.labelStep())
        {
            double x = snap(model.getX(frequency, area.width()));
            gc.strokeLine(x, 0, x, area.plotHeight());
        }

        gc.setLineDashes((double[])null);
    }

    @Override
    public void renderSpectrumForeground(GraphicsContext gc, RenderArea area)
    {
        SpectralDisplayModel model = getModel();

        if(model != null && model.hasSource())
        {
            renderAxis(gc, area, model);
        }

        renderCursor(gc, area, DisplaySection.SPECTRUM);
    }

    @Override
    public void renderWaterfall(GraphicsContext gc, RenderArea area)
    {
        renderCursor(gc, area, DisplaySection.WATERFALL);
    }

    /**
     * Draws the tick marks and frequency labels in the inset below the spectrum plot.
     */
    private void renderAxis(GraphicsContext gc, RenderArea area, SpectralDisplayModel model)
    {
        double width = area.width();
        double base = Math.floor(area.plotHeight()) + 0.5;
        Scale scale = getScale(model, width);
        Color line = getTheme().getLine();

        gc.setStroke(line);
        gc.setLineWidth(1.0);

        for(long frequency = firstMultiple(model.getDisplayMinFrequency(), scale.minorStep());
            frequency <= model.getDisplayMaxFrequency(); frequency += scale.minorStep())
        {
            double x = snap(model.getX(frequency, width));
            double length = (frequency % scale.labelStep() == 0) ? MAJOR_TICK : MINOR_TICK;
            gc.strokeLine(x, base, x, base + length);
        }

        gc.setFill(line);
        gc.setFont(FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.BOTTOM);
        setPrecision(scale.decimals());

        for(long frequency = firstMultiple(model.getDisplayMinFrequency(), scale.labelStep());
            frequency <= model.getDisplayMaxFrequency(); frequency += scale.labelStep())
        {
            double x = model.getX(frequency, width);
            String label = mLabelFormat.format(frequency / 1E6);
            double half = measure(label) / 2.0;

            //Skip labels that would be cut off at the edges
            if(x - half >= 0 && x + half <= width)
            {
                gc.fillText(label, x, area.height() - 1);
            }
        }
    }

    /**
     * Draws the cursor line in both sections and the readout in the section under the mouse.
     */
    private void renderCursor(GraphicsContext gc, RenderArea area, DisplaySection section)
    {
        DisplayCursor cursor = getSpectralDisplay().getDisplayCursor();
        SpectralDisplayModel model = getModel();

        if(cursor == null || model == null || !model.hasSource())
        {
            return;
        }

        Color color = getTheme().getCursor();
        double x = snap(cursor.x());
        gc.setStroke(color);
        gc.setLineWidth(1.0);
        gc.strokeLine(x, 0, x, area.plotHeight());

        if(cursor.section() != section)
        {
            return;
        }

        String frequency = CURSOR_FREQUENCY_FORMAT.format(cursor.frequency() / 1E6) + " MHz";
        String level = null;

        if(section == DisplaySection.SPECTRUM && cursor.y() <= area.plotHeight() && area.plotHeight() > 0)
        {
            double decibels = -(cursor.y() / area.plotHeight()) * model.getDynamicRange();
            level = CURSOR_LEVEL_FORMAT.format(decibels) + " dBFS";
        }

        double textWidth = measure(frequency);

        if(level != null)
        {
            textWidth = Math.max(textWidth, measure(level));
        }

        double lines = level != null ? 2 : 1;
        double boxWidth = textWidth + 8;
        double boxHeight = (mTextHeight * lines) + 4;
        double boxX = cursor.x() + 8;

        if(boxX + boxWidth > area.width())
        {
            boxX = cursor.x() - 8 - boxWidth;
        }

        double boxY = Math.max(0, Math.min(area.plotHeight() - boxHeight, cursor.y() - boxHeight - 4));
        gc.setFill(getTheme().getBackground(0.7));
        gc.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 6, 6);
        gc.setFill(color);
        gc.setFont(FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText(frequency, boxX + 4, boxY + 2);

        if(level != null)
        {
            gc.fillText(level, boxX + 4, boxY + 2 + mTextHeight);
        }
    }

    private SpectralDisplayTheme getTheme()
    {
        return getSpectralDisplay().getTheme();
    }

    /**
     * Chooses the label spacing so the labels fill no more than half of the display width.
     */
    private Scale getScale(SpectralDisplayModel model, double width)
    {
        double bandwidth = model.getDisplayBandwidth();
        int decimals = 3;
        long step = 1;

        //Two passes: the label width depends on the number of decimals, which depends on the label spacing
        for(int pass = 0; pass < 2; pass++)
        {
            setPrecision(decimals);
            double labelWidth = measure(mLabelFormat.format(model.getDisplayMaxFrequency() / 1E6));
            double maxLabels = Math.max(1.0, width / (labelWidth * 2.0));
            step = niceStep(bandwidth / maxLabels);
            decimals = Math.max(1, Math.min(6, (int)Math.ceil(6.0 - Math.log10(step) - 1E-9)));
        }

        long mantissa = step / (long)Math.pow(10, Math.floor(Math.log10(step)));
        long minor = Math.max(1, step / (mantissa == 2 ? 4 : 5));
        return new Scale(step, minor, decimals);
    }

    /**
     * Smallest 1, 2 or 5 times a power of ten that is at least the value, with a minimum of 1 Hertz.
     */
    static long niceStep(double value)
    {
        if(value <= 1)
        {
            return 1;
        }

        double power = Math.pow(10, Math.floor(Math.log10(value)));

        for(int mantissa: new int[]{1, 2, 5, 10})
        {
            if(mantissa * power >= value)
            {
                return Math.round(mantissa * power);
            }
        }

        return Math.round(10 * power);
    }

    /**
     * First multiple of the step at or above the frequency.
     */
    private static long firstMultiple(double frequency, long step)
    {
        return (long)Math.ceil(frequency / step) * step;
    }

    /**
     * Centers a line on a pixel so one pixel wide lines are sharp.
     */
    private static double snap(double x)
    {
        return Math.floor(x) + 0.5;
    }

    private void setPrecision(int decimals)
    {
        mLabelFormat.setMinimumFractionDigits(decimals);
        mLabelFormat.setMaximumFractionDigits(decimals);
    }

    private double measure(String text)
    {
        mMeasure.setText(text);
        return mMeasure.getLayoutBounds().getWidth();
    }

    /**
     * Label and tick spacing in Hertz and the number of label decimal places (MHz).
     */
    private record Scale(long labelStep, long minorStep, int decimals)
    {
    }
}
