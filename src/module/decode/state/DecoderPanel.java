/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package module.decode.state;

import alias.Alias;
import icon.IconManager;
import sample.Listener;
import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingChangeListener;
import settings.SettingsManager;

import javax.swing.*;
import java.awt.*;

public abstract class DecoderPanel extends JPanel
    implements Listener<ChangedAttribute>, SettingChangeListener
{
    private static final long serialVersionUID = 1L;

    protected Font mFontDetails = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    protected Font mFontDecoder = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    protected Font mFontAuxDecoder = new Font(Font.MONOSPACED, Font.PLAIN, 10);
    public static final int ICON_SIZE = 12;

    protected Color mColorLabelDetails;
    protected Color mColorLabelDecoder;
    protected Color mColorLabelAuxDecoder;

    protected IconManager mIconManager;
    protected SettingsManager mSettingsManager;
    protected DecoderState mDecoderState;

    public DecoderPanel(IconManager iconManager, SettingsManager settingsManager, DecoderState decoderState)
    {
        mIconManager = iconManager;
        mSettingsManager = settingsManager;
        mSettingsManager.addListener(this);

        mDecoderState = decoderState;

        mDecoderState.setChangedAttributeListener(this);
    }

    public void dispose()
    {
        mSettingsManager.removeListener(this);
        mSettingsManager = null;

        mDecoderState.removeChangedAttributeListener();
        mDecoderState = null;
    }

    public DecoderState getDecoderState()
    {
        return mDecoderState;
    }

    protected void init()
    {
        getColors();

        setOpaque(false); //Use the parent panel's background color
    }

    private void getColors()
    {
        mColorLabelDetails = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_LABEL_DETAILS).getColor();
        mColorLabelDecoder = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_LABEL_DECODER).getColor();
        mColorLabelAuxDecoder = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_LABEL_AUX_DECODER).getColor();
    }


    @Override
    public void settingChanged(Setting setting)
    {
        if(setting instanceof ColorSetting)
        {
            ColorSetting colorSetting = (ColorSetting) setting;

            switch(colorSetting.getColorSettingName())
            {
                case CHANNEL_STATE_LABEL_AUX_DECODER:
                    mColorLabelAuxDecoder = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_LABEL_DECODER:
                    mColorLabelDecoder = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_LABEL_DETAILS:
                    mColorLabelDetails = colorSetting.getColor();
                    repaint();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void settingDeleted(Setting setting)
    {
    }

    protected void setAliasLabel(final JLabel label, final Alias alias)
    {
        setAliasLabel(label, alias, true);
    }

    protected void setAliasLabel(final JLabel label, final Alias alias, boolean includeIcon)
    {
        if(alias != null)
        {
            label.setText(alias.getName());

            if(includeIcon)
            {
                String iconName = alias.getIconName();

                if(iconName != null)
                {
                    label.setIcon(mIconManager.getIcon(iconName, IconManager.DEFAULT_ICON_SIZE));
                }
                else
                {
                    label.setIcon(null);
                }
            }
            else
            {
                label.setIcon(null);
            }
        }
        else
        {
            label.setText("");
            label.setIcon(null);
        }
    }

}
