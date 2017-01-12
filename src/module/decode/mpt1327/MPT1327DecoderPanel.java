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

import controller.channel.Channel.ChannelType;
import icon.IconManager;
import module.decode.state.ChangedAttribute;
import module.decode.state.DecoderPanel;
import net.miginfocom.swing.MigLayout;
import settings.ColorSetting;
import settings.Setting;
import settings.SettingsManager;

import javax.swing.*;
import java.awt.*;

public class MPT1327DecoderPanel extends DecoderPanel
{
    private static final long serialVersionUID = 1L;

    private JLabel mProtocol = new JLabel("");
    private JLabel mSiteOrToTalkgroup = new JLabel("");
    private JLabel mSiteOrToTalkgroupAlias = new JLabel("");

    private JLabel mFromTalkgroupLabel = new JLabel("");
    private JLabel mFromTalkgroup = new JLabel("");
    private JLabel mFromTalkgroupAliasLabel = new JLabel("");

    public MPT1327DecoderPanel(IconManager iconManager, SettingsManager settingsManager, MPT1327DecoderState decoderState)
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
        super.init();

        setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[]0[]0[]"));

        mProtocol.setText("MPT1327");
        mProtocol.setFont(mFontDecoder);
        mProtocol.setForeground(mColorLabelDecoder);

        MPT1327DecoderState mptState = getDecoderState();

        mSiteOrToTalkgroup.setFont(mFontDecoder);
        mSiteOrToTalkgroup.setForeground(mColorLabelDecoder);

        mSiteOrToTalkgroupAlias.setFont(mFontDecoder);
        mSiteOrToTalkgroupAlias.setForeground(mColorLabelDecoder);

        mFromTalkgroupLabel.setFont(mFontDetails);
        mFromTalkgroupLabel.setForeground(mColorLabelDetails);

        mFromTalkgroup.setFont(mFontDecoder);
        mFromTalkgroup.setForeground(mColorLabelDecoder);

        mFromTalkgroupAliasLabel.setFont(mFontDecoder);
        mFromTalkgroupAliasLabel.setForeground(mColorLabelDecoder);


        switch(mptState.getChannelType())
        {
            case STANDARD:
                mSiteOrToTalkgroup.setText(mptState.getSite());
                setAliasLabel(mSiteOrToTalkgroupAlias, mptState.getSiteAlias());
                break;
            case TRAFFIC:
                mSiteOrToTalkgroup.setText(mptState.getToTalkgroup());
                setAliasLabel(mSiteOrToTalkgroupAlias, mptState.getToTalkgroupAlias());

                mFromTalkgroupLabel.setText("From:");
                mFromTalkgroup.setText(getDecoderState().getFromTalkgroup());
                setAliasLabel(mFromTalkgroupAliasLabel, mptState.getFromTalkgroupAlias());
                break;
            default:
                throw new IllegalArgumentException("Unrecognized channel "
                    + "type in MPT1327 Decoder [" + getDecoderState()
                    .getChannelType().name() + "]");
        }

        add(mProtocol);
        add(mSiteOrToTalkgroup);
        add(mSiteOrToTalkgroupAlias, "wrap");

        if(mptState.getChannelType() == ChannelType.TRAFFIC)
        {
            add(mFromTalkgroupLabel);
            add(mFromTalkgroup);
            add(mFromTalkgroupAliasLabel, "wrap");
        }
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
                    case CHANNEL_SITE_NUMBER:
                        mSiteOrToTalkgroup.setText(String.valueOf(state.getSite()));
                        setAliasLabel(mSiteOrToTalkgroupAlias, state.getSiteAlias(), false);
                        repaint();
                        break;
                    case TO_TALKGROUP:
                        mSiteOrToTalkgroup.setText(state.getToTalkgroup());
                        setAliasLabel(mSiteOrToTalkgroupAlias, state.getToTalkgroupAlias());
                        repaint();
                        break;
                    case FROM_TALKGROUP:
                        mFromTalkgroup.setText(state.getFromTalkgroup());
                        setAliasLabel(mFromTalkgroupAliasLabel, state.getFromTalkgroupAlias());
                        repaint();
                        break;
                    default:
                        break;
                }
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
                    if(mProtocol != null)
                    {
                        mProtocol.setForeground(mColorLabelDecoder);
                    }
                    if(mSiteOrToTalkgroup != null)
                    {
                        mSiteOrToTalkgroup.setForeground(mColorLabelDecoder);
                    }
                    if(mSiteOrToTalkgroupAlias != null)
                    {
                        mSiteOrToTalkgroupAlias.setForeground(mColorLabelDecoder);
                    }
                    if(mFromTalkgroup != null)
                    {
                        mFromTalkgroup.setForeground(mColorLabelDecoder);
                    }
                    if(mFromTalkgroupAliasLabel != null)
                    {
                        mFromTalkgroupAliasLabel.setForeground(mColorLabelDecoder);
                    }
                    break;
                case CHANNEL_STATE_LABEL_DETAILS:
                    if(mFromTalkgroupLabel != null)
                    {
                        mFromTalkgroupLabel.setForeground(mColorLabelDetails);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
