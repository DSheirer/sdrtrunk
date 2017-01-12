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
package module.decode.p25;

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

public class P25DecoderPanel extends DecoderPanel
{
    private static final long serialVersionUID = 1L;

    private JLabel mProtocol;
    private JLabel mTo = new JLabel(" ");
    private JLabel mToAlias = new JLabel(" ");

    private JLabel mNAC = new JLabel("NAC:");
    private JLabel mFrom = new JLabel(" ");
    private JLabel mFromAlias = new JLabel(" ");

    private JLabel mSystem = new JLabel("SYS:");
    private JLabel mSite = new JLabel("Site:");
    private JLabel mSiteAlias = new JLabel("");


    public P25DecoderPanel(IconManager iconManager, SettingsManager settingsManager, P25DecoderState decoderState)
    {
        super(iconManager, settingsManager, decoderState);

        mProtocol = new JLabel("P25-1 " + decoderState.getModulation().getShortLabel());

        init();
    }

    public void init()
    {
        super.init();

        setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[]0[]0[]"));

        mProtocol.setFont(mFontDecoder);
        mProtocol.setForeground(mColorLabelDecoder);

        mTo.setFont(mFontDecoder);
        mTo.setForeground(mColorLabelDecoder);

        mToAlias.setFont(mFontDecoder);
        mToAlias.setForeground(mColorLabelDecoder);

        mNAC.setFont(mFontDetails);
        mNAC.setForeground(mColorLabelDecoder);

        mFrom.setFont(mFontDecoder);
        mFrom.setForeground(mColorLabelDecoder);

        mFromAlias.setFont(mFontDecoder);
        mFromAlias.setForeground(mColorLabelDecoder);

        mSystem.setFont(mFontDetails);
        mSystem.setForeground(mColorLabelDecoder);

        mSiteAlias.setFont(mFontDecoder);
        mSiteAlias.setForeground(mColorLabelDecoder);

        mSite.setFont(mFontDetails);
        mSite.setForeground(mColorLabelDecoder);

        add(mProtocol);
        add(mTo);
        add(mToAlias, "wrap");

        add(mNAC);
        add(mFrom);
        add(mFromAlias, "wrap");

        add(mSystem);
        add(mSite);
        add(mSiteAlias, "wrap");
    }

    public P25DecoderState getDecoderState()
    {
        return (P25DecoderState) super.getDecoderState();
    }

    @Override
    public void receive(final ChangedAttribute changedAttribute)
    {
        final P25DecoderState state = getDecoderState();

        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {

                switch(changedAttribute)
                {
                    case NAC:
                        mNAC.setText("NAC:" + state.getNAC());
                        break;
                    case SYSTEM:
                        mSystem.setText("SYS:" + state.getSystem());
                        break;
                    case SITE:
                        mSite.setText("SITE:" + state.getSite());
                        break;
                    case SITE_ALIAS:
                        mSiteAlias.setText(state.getSiteAlias());
                        break;
                    case FROM_TALKGROUP:
                        if(state.getFromTalkgroup() == null)
                        {
                            mFrom.setText(" ");
                        }
                        else
                        {
                            mFrom.setText(state.getFromTalkgroup());
                        }
                        break;
                    case FROM_TALKGROUP_ALIAS:
                        Alias from = state.getFromAlias();

                        if(from != null)
                        {
                            mFromAlias.setText(from.getName());

                            final String icon = from.getIconName();

                            if(icon != null && mIconManager != null)
                            {
                                mFromAlias.setIcon(mIconManager.getIcon(icon, IconManager.DEFAULT_ICON_SIZE));
                            }
                        }
                        else
                        {
                            mFromAlias.setText(" ");
                            mFromAlias.setIcon(null);
                        }
                        break;
                    case TO_TALKGROUP:
                        if(state.getToTalkgroup() == null)
                        {
                            mTo.setText(" ");
                        }
                        else
                        {
                            mTo.setText(state.getToTalkgroup());
                        }
                        break;
                    case TO_TALKGROUP_ALIAS:
                        final Alias to = state.getToAlias();

                        if(to != null)
                        {
                            mToAlias.setText(to.getName());

                            final String icon = to.getIconName();

                            if(icon != null && mIconManager != null)
                            {
                                mToAlias.setIcon(mIconManager.getIcon(icon, IconManager.DEFAULT_ICON_SIZE));
                            }
                        }
                        else
                        {
                            mToAlias.setText(" ");
                            mToAlias.setIcon(null);
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
                    if(mProtocol != null)
                    {
                        mProtocol.setForeground(mColorLabelDecoder);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
