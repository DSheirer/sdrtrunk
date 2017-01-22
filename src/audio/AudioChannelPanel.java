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
package audio;

import alias.Alias;
import audio.output.AudioOutput;
import channel.metadata.Metadata;
import icon.IconManager;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;
import sample.Listener;
import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingChangeListener;
import settings.SettingsManager;

import javax.swing.*;
import java.awt.*;

public class AudioChannelPanel extends JPanel
    implements Listener<AudioEvent>, SettingChangeListener
{
    private static final long serialVersionUID = 1L;
    private static final Logger mLog = LoggerFactory.getLogger(AudioChannelPanel.class);

    public static final String PROPERTY_PREFIX = "audio.channel.panel.color.";
    public static final String PROPERTY_COLOR_BACKGROUND = PROPERTY_PREFIX + "background";
    public static final String PROPERTY_COLOR_LABEL = PROPERTY_PREFIX + "label";
    public static final String PROPERTY_COLOR_MUTED = PROPERTY_PREFIX + "muted";
    public static final String PROPERTY_COLOR_VALUE = PROPERTY_PREFIX + "value";

    private Font mFont = new Font(Font.MONOSPACED, Font.PLAIN, 16);

    private Color mBackgroundColor;
    private Color mLabelColor;
    private Color mMutedColor;
    private Color mValueColor;

    private IconManager mIconManager;
    private SettingsManager mSettingsManager;
    private AudioOutput mAudioOutput;

    private JLabel mMutedLabel = new JLabel("M");
    private JLabel mChannelName = new JLabel(" ");
    private JLabel mToAlias = new JLabel(" ");
    private JLabel mTo = new JLabel("-----");

    private boolean mConfigured = false;

    public AudioChannelPanel(IconManager iconManager, SettingsManager settingsManager, AudioOutput audioOutput)
    {
        mIconManager = iconManager;
        mSettingsManager = settingsManager;
        mSettingsManager.addListener(this);

        mAudioOutput = audioOutput;

        if(mAudioOutput != null)
        {
            mAudioOutput.addAudioEventListener(this);
            mAudioOutput.setMetadataListener(new AudioMetadataProcessor());
        }

        mBackgroundColor = SystemProperties.getInstance().get(PROPERTY_COLOR_BACKGROUND, Color.BLACK);
        mLabelColor = SystemProperties.getInstance().get(PROPERTY_COLOR_LABEL, Color.LIGHT_GRAY);
        mMutedColor = SystemProperties.getInstance().get(PROPERTY_COLOR_MUTED, Color.RED);
        mValueColor = SystemProperties.getInstance().get(PROPERTY_COLOR_VALUE, Color.GREEN);

        init();
    }

    private void init()
    {
        setLayout(new MigLayout("align center center, insets 0 0 0 0", "[][][align right]0[grow,fill]", ""));
        setBackground(mBackgroundColor);

        mMutedLabel.setFont(mFont);
        mMutedLabel.setForeground(mMutedColor);
        mMutedLabel.setVisible(false);
        add(mMutedLabel);

        mChannelName = new JLabel(mAudioOutput != null ? mAudioOutput.getChannelName() : " ");
        mChannelName.setFont(mFont);
        mChannelName.setForeground(mLabelColor);
        add(mChannelName);

        mToAlias.setFont(mFont);
        mToAlias.setForeground(mValueColor);
        add(mToAlias);

        mTo.setFont(mFont);
        mTo.setForeground(mValueColor);
        add(mTo);
    }

    @Override
    public void receive(final AudioEvent audioEvent)
    {
        switch(audioEvent.getType())
        {
            case AUDIO_STOPPED:
                EventQueue.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        resetLabels();
                    }
                });
                break;
            case AUDIO_MUTED:
            case AUDIO_UNMUTED:
                EventQueue.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mMutedLabel.setVisible(mAudioOutput.isMuted());
                    }
                });
                break;
            default:
                break;
        }
    }

    /**
     * Resets the from and to labels.  Note: this does not happen on the swing
     * event thread.  Only invoke from the swing thread.
     */
    private void resetLabels()
    {
        updateLabel(mTo, null, mToAlias, null);

        mConfigured = false;
    }

    /**
     * Updates the alias label with text and icon from the alias.  Note: this
     * does not occur on the Swing event thread -- wrap any calls to this
     * method with an event thread call.
     */
    private void updateLabel(JLabel valueLabel, String value, JLabel aliasLabel, Alias alias)
    {
        if(value != null)
        {

            if(alias != null)
            {
                valueLabel.setText(alias.getName());
                aliasLabel.setIcon(mIconManager.getIcon(alias.getIconName(), 18));
            }
            else
            {
                valueLabel.setText(value);
                aliasLabel.setIcon(null);
            }
        }
        else
        {
            valueLabel.setText("-----");
            aliasLabel.setIcon(null);
        }
    }


    /**
     * Processes audio metadata to update this panel's display values
     */
    public class AudioMetadataProcessor implements Listener<Metadata>
    {
        @Override
        public void receive(final Metadata metadata)
        {
            if(metadata.isUpdated() || !mConfigured)
            {
                EventQueue.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        updateLabel(mTo, metadata.getPrimaryAddressTo().getIdentifier(),
                            mToAlias, metadata.getPrimaryAddressTo().getAlias());

                        mConfigured = true;
                    }
                });

            }
        }
    }


    @Override
    public void settingChanged(Setting setting)
    {
        if(setting instanceof ColorSetting)
        {
            ColorSetting colorSetting = (ColorSetting) setting;

            switch(colorSetting.getColorSettingName())
            {
                case CHANNEL_STATE_LABEL_DECODER:
                    if(mTo != null)
                    {
                        mTo.setForeground(mLabelColor);
                    }
                    if(mToAlias != null)
                    {
                        mToAlias.setForeground(mLabelColor);
                    }
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
}
