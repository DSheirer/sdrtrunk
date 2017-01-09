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
package module.decode.ltrstandard;

import alias.Alias;
import icon.IconManager;
import module.decode.state.ChangedAttribute;
import module.decode.state.DecoderPanel;
import net.miginfocom.swing.MigLayout;
import settings.ColorSetting;
import settings.Setting;
import settings.SettingsManager;

import javax.swing.*;
import java.awt.*;

public class LTRStandardDecoderPanel extends DecoderPanel
{
    private static final long serialVersionUID = 1L;

    private JLabel mProtocol = new JLabel("LTR Standard");
    private JLabel mTalkgroup = new JLabel();
    private JLabel mTalkgroupAlias = new JLabel();

    private JLabel mLCN = new JLabel("LCN:");

    public LTRStandardDecoderPanel(IconManager iconManager, SettingsManager settingsManager,
                                   LTRStandardDecoderState decoderState)
    {
        super(iconManager, settingsManager, decoderState);

        init();
    }

    public void dispose()
    {
        super.dispose();
    }

    public void init()
    {
        super.init();

        setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[]0[]0[]"));

        mProtocol.setFont(mFontDecoder);
        mProtocol.setForeground(mColorLabelDecoder);

        mTalkgroup.setFont(mFontDecoder);
        mTalkgroup.setForeground(mColorLabelDecoder);

        mTalkgroupAlias.setFont(mFontDecoder);
        mTalkgroupAlias.setForeground(mColorLabelDecoder);

        mLCN.setFont(mFontDecoder);
        mLCN.setForeground(mColorLabelDecoder);

        add(mProtocol);
        add(mTalkgroup);
        add(mTalkgroupAlias, "wrap");

        add(mLCN, "wrap");
    }

    public LTRStandardDecoderState getDecoderState()
    {
        return (LTRStandardDecoderState) super.getDecoderState();
    }

    @Override
    public void receive(final ChangedAttribute attribute)
    {
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                LTRStandardDecoderState state = getDecoderState();

                switch(attribute)
                {
                    case CHANNEL_NUMBER:
                        if(state.getChannelNumber() != 0)
                        {
                            mLCN.setText("LCN: " + state.getChannelNumber());
                        }
                        break;
                    case TO_TALKGROUP:
                        mTalkgroup.setText(state.getTalkgroup());
                        /* fall-through */
                    case TO_TALKGROUP_ALIAS:
                        Alias alias = state.getTalkgroupAlias();

                        if(alias != null)
                        {
                            mTalkgroupAlias.setText(alias.getName());
                            mTalkgroupAlias.setIcon(mIconManager.getIcon(alias.getIconName(),
                                IconManager.DEFAULT_ICON_SIZE));
                        }
                        else
                        {
                            mTalkgroupAlias.setText(null);
                            mTalkgroupAlias.setIcon(null);
                        }
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
                    if(mProtocol != null)
                    {
                        mProtocol.setForeground(mColorLabelDecoder);
                    }
                    if(mTalkgroup != null)
                    {
                        mTalkgroup.setForeground(mColorLabelDecoder);
                    }
                    if(mTalkgroupAlias != null)
                    {
                        mTalkgroupAlias.setForeground(mColorLabelDecoder);
                    }
                    if(mLCN != null)
                    {
                        mLCN.setForeground(mColorLabelDecoder);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
