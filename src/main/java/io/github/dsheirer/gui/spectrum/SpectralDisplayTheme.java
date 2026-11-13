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

import javafx.scene.paint.Color;

/**
 * Color themes for the spectral display.  Each theme sets every display color and the waterfall palette, so the user
 * picks one theme instead of setting each color.
 *
 * Color blind safe themes use perceptually uniform waterfall palettes (viridis, cividis, inferno) that keep an even
 * brightness ramp for each type of color vision deficiency, and Okabe-Ito colors for the channel highlights, which
 * also differ in brightness so they don't rely on hue alone.
 */
public enum SpectralDisplayTheme
{
    CLASSIC("Classic", Group.STANDARD,
            "#000000", "#FFFFFF80", "#00FF0080", "#FFFFFF80", "#C0C0C080", "#FFC800",
            "#C0C0C03C", "#00FF003C", "#0000FF3C", "#FFFF00",
            WaterfallPalette.classic()),

    DARK("Dark", Group.STANDARD,
            "#121417", "#64B5F68C", "#1565C059", "#90CAF9E6", "#8A939ECC", "#FFCA28",
            "#9AA4B138", "#26A69A59", "#7E57C273", "#FFEE58",
            WaterfallPalette.gradient("#0B0E24", "#152A6B", "#1F5FA8", "#2BA3B8", "#7BD389", "#F2E35B", "#F28C38",
                    "#E8443A")),

    MEDIUM("Medium", Group.STANDARD,
            "#3A3F47", "#E0E6EE99", "#8FA8C866", "#F0F4F8E6", "#C4CCD6CC", "#FFB74D",
            "#C4CCD640", "#66BB6A66", "#42A5F573", "#FFF176",
            WaterfallPalette.gradient("#2A2F38", "#31527A", "#3C8DAD", "#7BC8A4", "#F2D06B", "#F08A4B", "#E04F5F")),

    LIGHT("Light", Group.STANDARD,
            "#F7F8FA", "#1E88E559", "#90CAF940", "#1565C0", "#5F6B7AE6", "#D84315",
            "#607D8B2E", "#43A0474D", "#1E88E54D", "#E65100",
            WaterfallPalette.gradient("#FFFFFF", "#DCEBF7", "#9CC9E8", "#4F9BD0", "#1F6AA5", "#0B3C73", "#061A40")),

    GREEN_PHOSPHOR("Green Phosphor", Group.MONOCHROME,
            "#000A00", "#33FF6673", "#00661A59", "#66FF8C", "#2E9E4AD9", "#CCFFCC",
            "#1F7A354D", "#33FF664D", "#B3FFC666", "#CCFFCC",
            WaterfallPalette.gradient("#000A00", "#00290A", "#005C1A", "#00A332", "#33FF66", "#CCFFD9")),

    AMBER("Amber", Group.MONOCHROME,
            "#0D0700", "#FFB00073", "#7A4A0059", "#FFC233", "#B07A10D9", "#FFF0C2",
            "#8A5A004D", "#FFB00047", "#FFE08A66", "#FFF0C2",
            WaterfallPalette.gradient("#0D0700", "#2E1A00", "#5C3600", "#A66300", "#FFB000", "#FFE6A6")),

    GRAYSCALE("Grayscale", Group.MONOCHROME,
            "#000000", "#FFFFFF80", "#60606066", "#FFFFFFD9", "#A0A0A0D9", "#FFFFFF",
            "#80808040", "#C0C0C04D", "#FFFFFF66", "#FFFFFF",
            WaterfallPalette.gradient("#000000", "#404040", "#808080", "#C0C0C0", "#FFFFFF")),

    HIGH_CONTRAST_DARK("High Contrast Dark", Group.HIGH_CONTRAST,
            "#000000", "#FFFFFFBF", "#FFFFFF59", "#FFFFFF", "#FFFFFF", "#FFFF00",
            "#FFFFFF2E", "#00FF0059", "#00FFFF66", "#FF00FF",
            WaterfallPalette.gradient("#000000", "#0000FF", "#00FFFF", "#FFFF00", "#FFFFFF")),

    HIGH_CONTRAST_LIGHT("High Contrast Light", Group.HIGH_CONTRAST,
            "#FFFFFF", "#00000099", "#00000040", "#000000", "#000000", "#D00000",
            "#0000001F", "#0080004D", "#0000FF4D", "#C000C0",
            WaterfallPalette.gradient("#FFFFFF", "#FFFF00", "#FF8000", "#C00000", "#000000")),

    VIRIDIS("Viridis (red/green color blind)", Group.COLOR_BLIND,
            "#0A0A12", "#FDE72573", "#21918C59", "#FDE725", "#A0A0B0D9", "#FFFFFF",
            "#A0A0A038", "#0072B273", "#E69F0073", "#CC79A7",
            WaterfallPalette.gradient("#440154", "#482878", "#3E4A89", "#31688E", "#26828E", "#1F9E89", "#35B779",
                    "#6DCD59", "#B4DE2C", "#FDE725")),

    CIVIDIS("Cividis (red/green color blind)", Group.COLOR_BLIND,
            "#00122E", "#FEE83873", "#575D6D66", "#FEE838", "#A9B0BFD9", "#FFFFFF",
            "#A9B0BF38", "#56B4E966", "#E69F0073", "#CC79A7",
            WaterfallPalette.gradient("#00224E", "#123570", "#3B496C", "#575D6D", "#707173", "#8A8779", "#A69D75",
                    "#C4B56C", "#E4CF5B", "#FEE838")),

    INFERNO("Inferno (blue/yellow color blind)", Group.COLOR_BLIND,
            "#000004", "#FCFFA473", "#BC375466", "#FCFFA4", "#B0A8B8D9", "#FFFFFF",
            "#A0A0A038", "#D55E0073", "#56B4E973", "#FFFFFF",
            WaterfallPalette.gradient("#000004", "#1B0C41", "#4A0C6B", "#781C6D", "#A52C60", "#CF4446", "#ED6925",
                    "#FB9B06", "#F7D13D", "#FCFFA4"));

    /**
     * Theme groups, for ordering and labeling in the theme selection list.
     */
    public enum Group
    {
        STANDARD,
        MONOCHROME,
        HIGH_CONTRAST,
        COLOR_BLIND;
    }

    private final String mLabel;
    private final Group mGroup;
    private final Color mBackground;
    private final Color mGradientTop;
    private final Color mGradientBottom;
    private final Color mTrace;
    private final Color mLine;
    private final Color mCursor;
    private final Color mChannel;
    private final Color mChannelProcessing;
    private final Color mChannelSelected;
    private final Color mCorrection;
    private final int[] mWaterfallPalette;

    /**
     * Constructs a theme.  Colors are web color strings with an optional alpha byte (#RRGGBBAA).
     * @param label to show to the user
     * @param group the theme belongs to
     * @param background of the spectrum
     * @param gradientTop of the spectrum fill, at the middle of the spectrum height
     * @param gradientBottom of the spectrum fill, at the bottom of the spectrum
     * @param trace line along the top of the spectrum fill
     * @param line color of the axis, grid lines and text
     * @param cursor line and readout color
     * @param channel fill for channels that are not processing
     * @param channelProcessing fill for processing channels
     * @param channelSelected fill for the selected channel
     * @param correction color of the channel frequency correction marker
     * @param waterfallPalette 256 ARGB colors from the noise floor to the strongest signal
     */
    SpectralDisplayTheme(String label, Group group, String background, String gradientTop, String gradientBottom,
                         String trace, String line, String cursor, String channel, String channelProcessing,
                         String channelSelected, String correction, int[] waterfallPalette)
    {
        mLabel = label;
        mGroup = group;
        mBackground = Color.web(background);
        mGradientTop = Color.web(gradientTop);
        mGradientBottom = Color.web(gradientBottom);
        mTrace = Color.web(trace);
        mLine = Color.web(line);
        mCursor = Color.web(cursor);
        mChannel = Color.web(channel);
        mChannelProcessing = Color.web(channelProcessing);
        mChannelSelected = Color.web(channelSelected);
        mCorrection = Color.web(correction);
        mWaterfallPalette = waterfallPalette;
    }

    /**
     * Default theme: the original sdrtrunk spectral display colors.
     */
    public static SpectralDisplayTheme getDefault()
    {
        return CLASSIC;
    }

    public String getLabel()
    {
        return mLabel;
    }

    public Group getGroup()
    {
        return mGroup;
    }

    public Color getBackground()
    {
        return mBackground;
    }

    /**
     * Background color with the opacity, for readout boxes drawn over the display.
     */
    public Color getBackground(double opacity)
    {
        return Color.color(mBackground.getRed(), mBackground.getGreen(), mBackground.getBlue(), opacity);
    }

    public Color getGradientTop()
    {
        return mGradientTop;
    }

    public Color getGradientBottom()
    {
        return mGradientBottom;
    }

    public Color getTrace()
    {
        return mTrace;
    }

    public Color getLine()
    {
        return mLine;
    }

    /**
     * Line color with a fraction of its opacity, for grid lines.
     */
    public Color getLine(double opacityFraction)
    {
        return Color.color(mLine.getRed(), mLine.getGreen(), mLine.getBlue(), mLine.getOpacity() * opacityFraction);
    }

    public Color getCursor()
    {
        return mCursor;
    }

    public Color getChannel()
    {
        return mChannel;
    }

    public Color getChannelProcessing()
    {
        return mChannelProcessing;
    }

    public Color getChannelSelected()
    {
        return mChannelSelected;
    }

    public Color getCorrection()
    {
        return mCorrection;
    }

    /**
     * ARGB color for the waterfall intensity index (0 - 255).
     */
    public int getWaterfallColor(int index)
    {
        return mWaterfallPalette[index & 0xFF];
    }

    /**
     * Waterfall color at the lowest intensity, which is the waterfall background.
     */
    public Color getWaterfallBackground()
    {
        return WaterfallPalette.toColor(mWaterfallPalette[0]);
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
