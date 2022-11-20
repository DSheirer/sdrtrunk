/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.awt.Color;

public class ColorSetting extends Setting
{
    private static final int NO_TRANSLUCENCY = 255;
    private static final int SPECTRUM_TRANSLUCENCY = 128;
    private static final int CONFIG_TRANSLUCENCY = 60;

    private String mRGB;
    private int mAlpha;
    private ColorSettingName mColorSettingName = ColorSettingName.UNKNOWN;

    public ColorSetting()
    {
        setColor(mColorSettingName.getDefaultColor());
    }

    public ColorSetting(ColorSettingName name)
    {
        setColor(name.getDefaultColor());
        setColorSettingName(name);
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public SettingType getType()
    {
        return SettingType.COLOR_SETTING;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "colorSettingName")
    public ColorSettingName getColorSettingName()
    {
        return mColorSettingName;
    }

    public void setColorSettingName(ColorSettingName name)
    {
        mColorSettingName = name;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "rgb")
    public String getRgb()
    {
        return mRGB;
    }

    public void setRgb(String value)
    {
        mRGB = value;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "alpha")
    public int getAlpha()
    {
        return mAlpha;
    }

    public void setAlpha(int value)
    {
        mAlpha = value;
    }

    @JsonIgnore
    public Color getColor()
    {
        Color temp = Color.decode(mRGB);

        return new Color(temp.getRed(),
            temp.getGreen(),
            temp.getBlue(),
            getColorSettingName().getTranslucency());
    }

    public void setColor(Color color)
    {
        mRGB = Integer.toHexString(color.getRGB());
        mRGB = "#" + mRGB.substring(2, mRGB.length());
        mAlpha = color.getAlpha();
    }

    public static Color getTranslucent(Color color, int translucency)
    {
        return new Color(color.getRed(),
            color.getGreen(),
            color.getBlue(),
            translucency);
    }

    public enum ColorSettingName
    {
        CHANNEL_CONFIG(Color.LIGHT_GRAY, CONFIG_TRANSLUCENCY, "Channel", "Channel Color"),
        CHANNEL_CONFIG_PROCESSING(Color.GREEN, CONFIG_TRANSLUCENCY, "Channel Processing", "Processing Channel Color"),
        CHANNEL_CONFIG_SELECTED(Color.BLUE, CONFIG_TRANSLUCENCY, "Channel Selected", "Selected Channel Color"),
        CHANNEL_STATE_BACKGROUND(Color.BLACK, NO_TRANSLUCENCY, "Background", "Channel State Background"),
        CHANNEL_STATE_GRADIENT_TOP_CALL(Color.BLACK, NO_TRANSLUCENCY, "Call Gradient Top", "Channel Call State Gradient Top"),
        CHANNEL_STATE_GRADIENT_MIDDLE_CALL(Color.BLUE, NO_TRANSLUCENCY, "Call Gradient Middle", "Channel Call State Gradient Middle"),
        CHANNEL_STATE_GRADIENT_TOP_CONTROL(Color.BLACK, NO_TRANSLUCENCY, "Control Gradient Top", "Channel Control State Gradient Top"),
        CHANNEL_STATE_GRADIENT_MIDDLE_CONTROL(new Color(0xC64F00), NO_TRANSLUCENCY, "Control Gradient Middle", "Channel Control State Gradient Middle"),
        CHANNEL_STATE_GRADIENT_TOP_DATA(Color.BLACK, NO_TRANSLUCENCY, "Data Gradient Top", "Channel Data State Gradient Top"),
        CHANNEL_STATE_GRADIENT_MIDDLE_DATA(new Color(0xCC00CC), NO_TRANSLUCENCY, "Data Gradient Middle", "Channel Data State Gradient Middle"),
        CHANNEL_STATE_GRADIENT_TOP_FADE(Color.BLACK, NO_TRANSLUCENCY, "Fade Gradient Top", "Channel Fade State Gradient Top"),
        CHANNEL_STATE_GRADIENT_MIDDLE_FADE(Color.DARK_GRAY, NO_TRANSLUCENCY, "Fade Gradient Middle", "Channel Fade State Gradient Middle"),
        CHANNEL_STATE_GRADIENT_TOP_IDLE(Color.BLACK, NO_TRANSLUCENCY, "Idle Gradient Top", "Channel Idle State Gradient Top"),
        CHANNEL_STATE_GRADIENT_MIDDLE_IDLE(Color.DARK_GRAY, NO_TRANSLUCENCY, "Idle Gradient Middle", "Channel Idle State Gradient Middle"),
        CHANNEL_STATE_GRADIENT_TOP_NO_TUNER(Color.RED, NO_TRANSLUCENCY, "No Tuner Gradient Top", "Channel No Tuner State Gradient Top"),
        CHANNEL_STATE_GRADIENT_MIDDLE_NO_TUNER(new Color(0x990000), NO_TRANSLUCENCY, "No Tuner Gradient Middle", "Channel No Tuner State Gradient Middle"),
        CHANNEL_STATE_LABEL_DETAILS(Color.LIGHT_GRAY, NO_TRANSLUCENCY, "Details", "Details Label Color"),
        CHANNEL_STATE_LABEL_DECODER(Color.GREEN, NO_TRANSLUCENCY, "Decoder", "Decoder Label Color"),
        CHANNEL_STATE_LABEL_AUX_DECODER(Color.YELLOW, NO_TRANSLUCENCY, "Aux Decoder", "Aux Decoder Label Color"),
        CHANNEL_STATE_SELECTED_CHANNEL(Color.YELLOW, NO_TRANSLUCENCY, "Selected Channel Indicator", "Selected Channel Indicator Color"),
        SPECTRUM_BACKGROUND(Color.BLACK, NO_TRANSLUCENCY, "Background", "Spectrum Background Color"),
        SPECTRUM_CURSOR(Color.ORANGE, NO_TRANSLUCENCY, "Cursor", "Spectrum Cursor Color"),
        SPECTRUM_GRADIENT_BOTTOM(Color.GREEN, SPECTRUM_TRANSLUCENCY, "Gradient Bottom", "Spectrum Gradient Bottom Color"),
        SPECTRUM_GRADIENT_TOP(Color.WHITE, SPECTRUM_TRANSLUCENCY, "Gradient Top", "Spectrum Gradient Top Color"),
        SPECTRUM_LINE(Color.LIGHT_GRAY, SPECTRUM_TRANSLUCENCY, "Line", "Spectrum Lines and Text Color"),
        UNKNOWN(Color.RED, NO_TRANSLUCENCY,"Unknown", "Unknown Setting Color");

        private Color mDefaultColor;
        private int mTranslucency;
        private String mLabel;
        private String mDialogTitle;

        /**
         * Enumeration of color names used in user interface, primarily in the spectral displays.
         * @param defaultColor to use and for resets.
         * @param translucency value for the color.
         * @param label for menu items
         * @param dialogTitle for selecting in the color chooser.
         */
        ColorSettingName(Color defaultColor, int translucency, String label, String dialogTitle)
        {
            mDefaultColor = defaultColor;
            mTranslucency = translucency;
            mLabel = label;
            mDialogTitle = dialogTitle;
        }

        /**
         * Default color to use when resetting to default.
         */
        public Color getDefaultColor()
        {
            return mDefaultColor;
        }

        /**
         * Translucency to apply to the color
         */
        public int getTranslucency()
        {
            return mTranslucency;
        }

        /**
         * Label for the setting
         */
        public String getLabel()
        {
            return mLabel;
        }

        /**
         * Title to use in dialogs
         * @return title
         */
        public String getDialogTitle()
        {
            return mDialogTitle;
        }
    }
}
