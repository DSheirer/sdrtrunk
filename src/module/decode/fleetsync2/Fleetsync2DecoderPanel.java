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
package module.decode.fleetsync2;

import icon.IconManager;
import module.decode.state.ChangedAttribute;
import module.decode.state.DecoderPanel;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingsManager;

import javax.swing.*;
import java.awt.*;

public class Fleetsync2DecoderPanel extends DecoderPanel
{
    private final static Logger mLog = LoggerFactory.getLogger(Fleetsync2DecoderPanel.class);

    private static final long serialVersionUID = 1L;
    private static final String PROTOCOL = "FSync II";

    private JLabel mToLabel = new JLabel(" ");
    private JLabel mTo = new JLabel(" ");
    private JLabel mToAlias = new JLabel(" ");

    private JLabel mFromLabel = new JLabel(" ");
    private JLabel mFrom = new JLabel(" ");
    private JLabel mFromAlias = new JLabel(" ");

    private JLabel mProtocol = new JLabel(PROTOCOL);
    private JLabel mMessage = new JLabel(" ");
    private JLabel mMessageType = new JLabel(" ");

    public Fleetsync2DecoderPanel(IconManager iconManager, SettingsManager settingsManager,
                                  Fleetsync2DecoderState decoderState)
    {
        super(iconManager, settingsManager, decoderState);

        init();
    }

    @Override
    public Fleetsync2DecoderState getDecoderState()
    {
        return (Fleetsync2DecoderState) super.getDecoderState();
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

        setLayout(new MigLayout("insets 1 0 0 0", "[grow,fill]", "[]0[]0[]"));

        mToLabel.setFont(mFontAuxDecoder);
        mToLabel.setForeground(mColorLabelAuxDecoder);
        add(mToLabel);

        mTo.setFont(mFontAuxDecoder);
        mTo.setForeground(mColorLabelAuxDecoder);
        add(mTo);

        mToAlias.setFont(mFontAuxDecoder);
        mToAlias.setForeground(mColorLabelAuxDecoder);
        add(mToAlias, "wrap");

        mFromLabel.setFont(mFontAuxDecoder);
        mFromLabel.setForeground(mColorLabelAuxDecoder);
        add(mFromLabel);

        mFrom.setFont(mFontAuxDecoder);
        mFrom.setForeground(mColorLabelAuxDecoder);
        add(mFrom);

        mFromAlias.setFont(mFontAuxDecoder);
        mFromAlias.setForeground(mColorLabelAuxDecoder);
        add(mFromAlias, "wrap");

        mProtocol.setFont(mFontAuxDecoder);
        mProtocol.setForeground(mColorLabelAuxDecoder);
        add(mProtocol);

        mMessageType.setFont(mFontAuxDecoder);
        mMessageType.setForeground(mColorLabelAuxDecoder);
        add(mMessageType);

        mMessage.setFont(mFontAuxDecoder);
        mMessage.setForeground(mColorLabelAuxDecoder);
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
                        String from = getDecoderState().getFromID();

                        if(from != null)
                        {
                            mFromLabel.setText("FROM:");
                            mFrom.setText(from);
                        }
                        else
                        {
                            mFromLabel.setText(" ");
                            mFrom.setText(" ");
                        }
                        break;
                    case FROM_TALKGROUP_ALIAS:
                        setAliasLabel(mFromAlias, getDecoderState().getFromIDAlias());
                        break;
                    case MESSAGE:
                        mMessage.setText(getDecoderState().getMessage());
                        break;
                    case MESSAGE_TYPE:
                        mMessageType.setText(getDecoderState().getMessageType());
                        break;
                    case TO_TALKGROUP:
                        String to = getDecoderState().getToID();

                        if(to != null)
                        {
                            mToLabel.setText("TO:");
                            mTo.setText(to);
                        }
                        else
                        {
                            mToLabel.setText(" ");
                            mTo.setText(" ");
                        }
                        break;
                    case TO_TALKGROUP_ALIAS:
                        setAliasLabel(mToAlias, getDecoderState().getToIDAlias());
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
