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
package module.decode.mdc1200;

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
import java.awt.*;

public class MDCDecoderPanel extends DecoderPanel
{
    private static final long serialVersionUID = 1L;
    private static final String sPROTOCOL = "MDC-1200";

    private JLabel mFromLabel = new JLabel(" ");
    private JLabel mFrom = new JLabel();
    private JLabel mFromAlias = new JLabel();

    private JLabel mToLabel = new JLabel(" ");
    private JLabel mTo = new JLabel();
    private JLabel mToAlias = new JLabel();

    private JLabel mProtocol = new JLabel(sPROTOCOL);
    private JLabel mMessage = new JLabel();
    private JLabel mMessageType = new JLabel();

    public MDCDecoderPanel(IconManager iconManager, SettingsManager settingsManager, MDCDecoderState decoderState)
    {
        super(iconManager, settingsManager, decoderState);

        init();
    }

    public MDCDecoderState getDecoderState()
    {
        return (MDCDecoderState) super.getDecoderState();
    }

    protected void init()
    {
        /* Calling super init will get and broadcast color settings to properly
         * setup the jlabel colors */
        super.init();

        setLayout(new MigLayout("insets 1 0 0 0", "[grow,fill]", "[]0[]0[]"));

        add(mFromLabel);
        add(mFrom);
        add(mFromAlias, "wrap");

        add(mToLabel);
        add(mTo);
        add(mToAlias, "wrap");

        add(mProtocol);
        add(mMessageType);
        add(mMessage, "wrap");
    }

    @Override
    public void receive(final ChangedAttribute changedAttribute)
    {
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                switch(changedAttribute)
                {
                    case FROM_TALKGROUP:
                        String from = getDecoderState().getFrom();

                        if(from != null)
                        {
                            mFromLabel.setText("FM:");
                        }
                        else
                        {
                            mFromLabel.setText(" ");
                        }
                        mFrom.setText(from);
                        break;
                    case FROM_TALKGROUP_ALIAS:
                        Alias fromAlias = getDecoderState().getFromAlias();

                        if(fromAlias != null)
                        {
                            mFromAlias.setText(fromAlias.getName());
                        }
                        else
                        {
                            mFromAlias.setText(null);
                        }
                        break;
                    case TO_TALKGROUP:
                        String to = getDecoderState().getTo();

                        if(to != null)
                        {
                            mToLabel.setText("TO:");
                        }
                        else
                        {
                            mToLabel.setText(" ");
                        }
                        mTo.setText(to);
                        break;
                    case TO_TALKGROUP_ALIAS:
                        Alias toAlias = getDecoderState().getToAlias();

                        if(toAlias != null)
                        {
                            mToAlias.setText(toAlias.getName());
                        }
                        else
                        {
                            mToAlias.setText(null);
                        }
                        break;
                    case MESSAGE:
                        mMessage.setText(getDecoderState().getMessage());
                        break;
                    case MESSAGE_TYPE:
                        mMessageType.setText(getDecoderState().getMessageType());
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
            ColorSetting color = (ColorSetting) setting;

            if(color.getColorSettingName() ==
                ColorSettingName.CHANNEL_STATE_LABEL_AUX_DECODER)
            {
                mFromLabel.setForeground(color.getColor());
                mFrom.setForeground(color.getColor());
                mFromAlias.setForeground(color.getColor());
                mToLabel.setForeground(color.getColor());
                mTo.setForeground(color.getColor());
                mToAlias.setForeground(color.getColor());
                mProtocol.setForeground(color.getColor());
                mMessageType.setForeground(color.getColor());
                mMessage.setForeground(color.getColor());
            }
        }
    }
}
