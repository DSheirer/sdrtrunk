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

import io.github.dsheirer.gui.spectrum.RenderArea;
import io.github.dsheirer.gui.spectrum.SpectralDisplayLayer;
import io.github.dsheirer.gui.spectrum.SpectralDisplayModel;
import io.github.dsheirer.gui.spectrum.SpectralDisplayTheme;
import java.text.DecimalFormat;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Channel tuning layer for a single channel's spectrum, centered on the channel frequency.  Shades the channel
 * passband and marks its edges with the theme's processing channel color, and draws the estimated carrier position
 * from the decoder's frequency correction (PLL) as a line in the theme's correction color, labeled with the offset
 * from the channel center.  Both markers also show on the waterfall so carrier drift is easy to follow.
 */
public class ChannelTuningLayer extends SpectralDisplayLayer
{
    private static final Font FONT = Font.font(11);
    private static final DecimalFormat KHZ_FORMAT = new DecimalFormat("0.###");
    private static final DecimalFormat OFFSET_FORMAT = new DecimalFormat("+0;-0");

    private final IntegerProperty mChannelBandwidth = new SimpleIntegerProperty(0);
    private final LongProperty mCarrierOffset = new SimpleLongProperty(0);
    private final BooleanProperty mCarrierOffsetValid = new SimpleBooleanProperty(false);
    private final Text mMeasure = new Text();
    private final double mTextHeight;

    /**
     * Constructs an instance.
     */
    public ChannelTuningLayer()
    {
        mMeasure.setFont(FONT);
        mMeasure.setText("0123456789 kHz");
        mTextHeight = Math.ceil(mMeasure.getLayoutBounds().getHeight());
        mChannelBandwidth.addListener((ob, ov, nv) -> requestRender());
        mCarrierOffset.addListener((ob, ov, nv) -> requestRender());
        mCarrierOffsetValid.addListener((ob, ov, nv) -> requestRender());
    }

    /**
     * Channel bandwidth in Hertz, or zero for no passband markers.  JavaFX thread only.
     */
    public IntegerProperty channelBandwidthProperty()
    {
        return mChannelBandwidth;
    }

    /**
     * Estimated carrier offset from the channel center in Hertz.  JavaFX thread only.
     */
    public LongProperty carrierOffsetProperty()
    {
        return mCarrierOffset;
    }

    /**
     * Indicates if the decoder has reported a carrier offset.  The carrier marker only shows when true.
     */
    public BooleanProperty carrierOffsetValidProperty()
    {
        return mCarrierOffsetValid;
    }

    @Override
    public String getConfigurationTitle()
    {
        return null;
    }

    @Override
    public void renderSpectrumBackground(GraphicsContext gc, RenderArea area)
    {
        double[] edges = getEdges(area.width());

        if(edges != null)
        {
            gc.setFill(fade(getTheme().getChannelProcessing(), 0.6));
            gc.fillRect(edges[0], 0, edges[1] - edges[0], area.plotHeight());
        }
    }

    @Override
    public void renderSpectrumForeground(GraphicsContext gc, RenderArea area)
    {
        SpectralDisplayTheme theme = getTheme();
        double[] edges = getEdges(area.width());

        gc.setFont(FONT);
        gc.setTextBaseline(VPos.TOP);
        gc.setLineWidth(1.0);

        if(edges != null)
        {
            Color edge = opaque(theme.getChannelProcessing());
            gc.setStroke(edge);
            gc.strokeLine(snap(edges[0]), 0, snap(edges[0]), area.plotHeight());
            gc.strokeLine(snap(edges[1]), 0, snap(edges[1]), area.plotHeight());

            String label = formatBandwidth(mChannelBandwidth.get());
            mMeasure.setText(label);

            if(mMeasure.getLayoutBounds().getWidth() + 8 < edges[1] - edges[0])
            {
                gc.setFill(edge);
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(label, (edges[0] + edges[1]) / 2.0, 2);
            }
        }

        Double carrier = getCarrierX(area.width());

        if(carrier != null)
        {
            Color color = theme.getCorrection();
            gc.setStroke(color);
            gc.strokeLine(snap(carrier), 0, snap(carrier), area.plotHeight());

            String label = "PLL " + OFFSET_FORMAT.format(mCarrierOffset.get()) + " Hz";
            mMeasure.setText(label);
            double width = mMeasure.getLayoutBounds().getWidth();
            double x = carrier + 5;

            if(x + width + 4 > area.width())
            {
                x = carrier - 5 - width;
            }

            double y = mTextHeight + 6;
            gc.setFill(theme.getBackground(0.7));
            gc.fillRoundRect(x - 3, y - 1, width + 6, mTextHeight + 2, 4, 4);
            gc.setFill(color);
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(label, x, y);
        }
    }

    @Override
    public void renderWaterfall(GraphicsContext gc, RenderArea area)
    {
        SpectralDisplayTheme theme = getTheme();
        double[] edges = getEdges(area.width());
        gc.setLineWidth(1.0);

        if(edges != null)
        {
            gc.setStroke(fade(opaque(theme.getChannelProcessing()), 0.7));
            gc.setLineDashes(4.0, 4.0);
            gc.strokeLine(snap(edges[0]), 0, snap(edges[0]), area.height());
            gc.strokeLine(snap(edges[1]), 0, snap(edges[1]), area.height());
            gc.setLineDashes((double[])null);
        }

        Double carrier = getCarrierX(area.width());

        if(carrier != null)
        {
            gc.setStroke(fade(theme.getCorrection(), 0.6));
            gc.strokeLine(snap(carrier), 0, snap(carrier), area.height());
        }
    }

    /**
     * Screen x positions of the lower and upper channel edges, or null when there is no channel bandwidth.
     */
    private double[] getEdges(double width)
    {
        SpectralDisplayModel model = getModel();
        int bandwidth = mChannelBandwidth.get();

        if(model == null || !model.hasSource() || bandwidth <= 0)
        {
            return null;
        }

        long center = model.getCenterFrequency();
        return new double[]{model.getX(center - (bandwidth / 2.0), width), model.getX(center + (bandwidth / 2.0),
                width)};
    }

    /**
     * Screen x position of the estimated carrier, or null when there is no carrier estimate.
     */
    private Double getCarrierX(double width)
    {
        SpectralDisplayModel model = getModel();

        if(model == null || !model.hasSource() || !mCarrierOffsetValid.get())
        {
            return null;
        }

        return model.getX(model.getCenterFrequency() + mCarrierOffset.get(), width);
    }

    private SpectralDisplayTheme getTheme()
    {
        return getSpectralDisplay().getTheme();
    }

    /**
     * Formats a bandwidth in Hz or kHz.
     */
    static String formatBandwidth(int hertz)
    {
        return hertz >= 1000 ? KHZ_FORMAT.format(hertz / 1000.0) + " kHz" : hertz + " Hz";
    }

    private static Color opaque(Color color)
    {
        return Color.color(color.getRed(), color.getGreen(), color.getBlue());
    }

    private static Color fade(Color color, double fraction)
    {
        return Color.color(color.getRed(), color.getGreen(), color.getBlue(), color.getOpacity() * fraction);
    }

    private static double snap(double x)
    {
        return Math.floor(x) + 0.5;
    }
}
