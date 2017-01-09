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
package module.decode.lj1200;

import alias.Alias;
import icon.IconManager;
import module.decode.state.ChangedAttribute;
import module.decode.state.DecoderPanel;
import net.miginfocom.swing.MigLayout;
import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingsManager;

import javax.swing.*;

public class LJ1200DecoderPanel extends DecoderPanel
{
    private static final long serialVersionUID = 1L;
    private static final String PROTOCOL = "LJ-1200";

    private JLabel mProtocol = new JLabel(PROTOCOL);
    private JLabel mTo = new JLabel();
    private JLabel mToAlias = new JLabel();

    public LJ1200DecoderPanel(IconManager iconManager, SettingsManager settingsManager, LJ1200DecoderState decoderState)
    {
        super(iconManager, settingsManager, decoderState);

        init();
    }

    public void dispose()
    {
        super.dispose();
    }

    protected void init()
    {
        /* Calling super init will get and broadcast color settings to properly
         * setup the jlabel colors */
        super.init();

        setLayout(new MigLayout("insets 1 0 0 0", "[grow,fill]", ""));

        mProtocol.setFont(mFontDecoder);
        mProtocol.setForeground(mColorLabelDecoder);

        mTo.setFont(mFontDecoder);
        mTo.setForeground(mColorLabelDecoder);

        mToAlias.setFont(mFontDecoder);
        mToAlias.setForeground(mColorLabelDecoder);

        add(mProtocol);
        add(mTo);
        add(mToAlias, "wrap");
    }

    public LJ1200DecoderState getDecoderState()
    {
        return (LJ1200DecoderState) super.getDecoderState();
    }

    @Override
    public void receive(final ChangedAttribute changedAttribute)
    {
        switch(changedAttribute)
        {
            case TO_TALKGROUP:
                mTo.setText(getDecoderState().getAddress());
                repaint();
                break;
            case TO_TALKGROUP_ALIAS:
                Alias alias = getDecoderState().getAddressAlias();

                if(alias != null)
                {
                    mToAlias.setText(alias.getName());
                }
                else
                {
                    mToAlias.setText("");
                }

                repaint();
                break;
            default:
                break;
        }
    }

    @Override
    public void settingChanged(Setting setting)
    {
        super.settingChanged(setting);

        if(setting instanceof ColorSetting)
        {
            ColorSetting color = (ColorSetting) setting;

            if(color.getColorSettingName() ==
                ColorSettingName.CHANNEL_STATE_LABEL_AUX_DECODER)
            {
                mProtocol.setForeground(color.getColor());
                mTo.setForeground(color.getColor());
                mToAlias.setForeground(color.getColor());
            }
        }
    }
}
