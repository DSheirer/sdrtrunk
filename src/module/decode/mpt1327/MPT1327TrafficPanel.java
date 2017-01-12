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
package module.decode.mpt1327;

import alias.Alias;
import icon.IconManager;
import module.decode.state.ChangedAttribute;
import module.decode.state.DecoderPanel;
import settings.ColorSetting;
import settings.Setting;
import settings.SettingsManager;

import javax.swing.*;
import java.awt.*;

public class MPT1327TrafficPanel extends DecoderPanel
{
    private static final long serialVersionUID = 1L;

    private JLabel mProtocolLabel;
    private JLabel mFromTalkgroupLabel;
    private JLabel mFromTalkgroupAliasLabel;

    private JLabel mToLabel;
    private JLabel mToTalkgroupLabel;
    private JLabel mToTalkgroupAliasLabel;

    public MPT1327TrafficPanel(IconManager iconManager, SettingsManager settingsManager,
                               MPT1327DecoderState decoderState)
    {
        super(iconManager, settingsManager, decoderState);

        init();
    }

    public MPT1327DecoderState getDecoderState()
    {
        return (MPT1327DecoderState) super.getDecoderState();
    }

    public void dispose()
    {
        super.dispose();
    }

    public void init()
    {
        mProtocolLabel = new JLabel("MPT1327");
        mProtocolLabel.setFont(mFontDecoder);
        mProtocolLabel.setForeground(mColorLabelDecoder);

        mFromTalkgroupLabel = new JLabel();
        mFromTalkgroupLabel.setFont(mFontDecoder);
        mFromTalkgroupLabel.setForeground(mColorLabelDecoder);

        mFromTalkgroupAliasLabel = new JLabel();
        mFromTalkgroupAliasLabel.setFont(mFontDecoder);
        mFromTalkgroupAliasLabel.setForeground(mColorLabelDecoder);

        mToLabel = new JLabel("TO:");
        mToLabel.setFont(mFontDetails);
        mToLabel.setForeground(mColorLabelDetails);

        mToTalkgroupLabel = new JLabel(getDecoderState().getFromTalkgroup());
        mToTalkgroupLabel.setFont(mFontDecoder);
        mToTalkgroupLabel.setForeground(mColorLabelDecoder);

        mToTalkgroupAliasLabel = new JLabel();
        mToTalkgroupAliasLabel.setFont(mFontDecoder);
        mToTalkgroupAliasLabel.setForeground(mColorLabelDecoder);

        add(mProtocolLabel);
        add(mFromTalkgroupLabel);
        add(mFromTalkgroupAliasLabel, "wrap");

        add(mToLabel);
        add(mToTalkgroupLabel);
        add(mToTalkgroupAliasLabel, "wrap");
    }

    @Override
    public void receive(final ChangedAttribute changedAttribute)
    {
        final MPT1327DecoderState state = getDecoderState();

        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                switch(changedAttribute)
                {
                    case FROM_TALKGROUP:
                        String talkgroup = state.getFromTalkgroup();

                        mFromTalkgroupLabel.setText(talkgroup);

                        Alias alias = state.getFromTalkgroupAlias();

                        if(alias != null)
                        {
                            mFromTalkgroupAliasLabel.setText(alias.getName());

                            String icon = alias.getIconName();

                            mFromTalkgroupAliasLabel.setIcon(mIconManager.getIcon(icon, IconManager.DEFAULT_ICON_SIZE));
                        }
                        break;
                    case TO_TALKGROUP:
                        String toTalkgroup = state.getToTalkgroup();

                        mToTalkgroupLabel.setText(toTalkgroup);

                        Alias toAlias = state.getToTalkgroupAlias();

                        if(toAlias != null)
                        {
                            mToTalkgroupAliasLabel.setText(toAlias.getName());

                            String icon = toAlias.getIconName();

                            if(icon != null)
                            {
                                mToTalkgroupAliasLabel.setIcon(mIconManager.getIcon(icon, IconManager.DEFAULT_ICON_SIZE));
                            }
                        }
                        break;
                    default:
                        break;
                }

                repaint();
            }
        });
    }

    @Override
    public void settingChanged(Setting setting)
    {
        super.settingChanged(setting);

        if(setting instanceof ColorSetting)
        {
            ColorSetting colorSetting = (ColorSetting) setting;

            switch(colorSetting.getColorSettingName())
            {
                case CHANNEL_STATE_LABEL_DECODER:
                    mProtocolLabel.setForeground(mColorLabelDecoder);
                    mFromTalkgroupLabel.setForeground(mColorLabelDecoder);
                    mFromTalkgroupAliasLabel.setForeground(mColorLabelDecoder);
                    mToTalkgroupLabel.setForeground(mColorLabelDecoder);
                    mToTalkgroupAliasLabel.setForeground(mColorLabelDecoder);
                    break;
                case CHANNEL_STATE_LABEL_DETAILS:
                    mToLabel.setForeground(mColorLabelDetails);
                    break;
                default:
                    break;
            }
        }
    }
}
