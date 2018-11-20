/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.audio.playback;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.AudioEvent;
import io.github.dsheirer.icon.IconManager;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.identifier.IdentifierPreference;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.ColorSetting;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.SettingChangeListener;
import io.github.dsheirer.settings.SettingsManager;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.util.List;

public class AudioChannelPanel extends JPanel implements Listener<AudioEvent>, SettingChangeListener
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
    private UserPreferences mUserPreferences;
    private IdentifierPreference mIdentifierPreference;
    private PreferenceUpdateListener mPreferenceUpdateListener = new PreferenceUpdateListener();
    private AudioOutput mAudioOutput;

    private JLabel mMutedLabel = new JLabel("M");
    private JLabel mChannelName = new JLabel(" ");
    private JLabel mIconLabel = new JLabel(" ");
    private JLabel mIdentifierLabel = new JLabel("-----");
    private Identifier mIdentifier;
    private Alias mAlias;

    private boolean mConfigured = false;
    private AliasModel mAliasModel;

    public AudioChannelPanel(IconManager iconManager, UserPreferences userPreferences, SettingsManager settingsManager,
                             AudioOutput audioOutput, AliasModel aliasModel)
    {
        mIconManager = iconManager;
        mSettingsManager = settingsManager;
        mSettingsManager.addListener(this);
        mAliasModel = aliasModel;
        mUserPreferences = userPreferences;
        mIdentifierPreference = mUserPreferences.getIdentifierPreference();
        mUserPreferences.addPreferenceUpdateListener(mPreferenceUpdateListener);
        mAudioOutput = audioOutput;

        if(mAudioOutput != null)
        {
            mAudioOutput.addAudioEventListener(this);
            mAudioOutput.setIdentifierCollectionListener(new AudioMetadataProcessor());
        }

        mBackgroundColor = SystemProperties.getInstance().get(PROPERTY_COLOR_BACKGROUND, Color.BLACK);
        mLabelColor = SystemProperties.getInstance().get(PROPERTY_COLOR_LABEL, Color.LIGHT_GRAY);
        mMutedColor = SystemProperties.getInstance().get(PROPERTY_COLOR_MUTED, Color.RED);
        mValueColor = SystemProperties.getInstance().get(PROPERTY_COLOR_VALUE, Color.GREEN);

        init();
    }

    public void dispose()
    {
        //Deregister from receiving preference update notifications
        mUserPreferences.removePreferenceUpdateListener(mPreferenceUpdateListener);
    }

    private void init()
    {
        setLayout(new MigLayout("align center center, insets 0 0 0 0",
            "[][][align right]0[grow,fill]", ""));
        setBackground(mBackgroundColor);

        mMutedLabel.setFont(mFont);
        mMutedLabel.setForeground(mMutedColor);
        mMutedLabel.setVisible(false);
        add(mMutedLabel);

        mChannelName = new JLabel(mAudioOutput != null ? mAudioOutput.getChannelName() : " ");
        mChannelName.setFont(mFont);
        mChannelName.setForeground(mLabelColor);
        add(mChannelName);

        mIconLabel.setFont(mFont);
        mIconLabel.setForeground(mValueColor);
        add(mIconLabel);

        mIdentifierLabel.setFont(mFont);
        mIdentifierLabel.setForeground(mValueColor);
        add(mIdentifierLabel, "wmin 10lp");
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
        mIdentifier = null;
        mAlias = null;
        updateLabels();
        mConfigured = false;
    }

    /**
     * Truncates the string to the specified length using an ellipses at the end
     * @param value to optionally truncate
     * @param length of the final formatted string
     * @return formatted string or null
     */
    private static String truncate(String value, int length)
    {
        if(value != null && value.length() > length)
        {
            return value.substring(0, length - 3) + "...";
        }

        return value;
    }

    /**
     * Updates the alias label with text and icon from the alias.  Note: this
     * does not occur on the Swing event thread -- wrap any calls to this
     * method with an event thread call.
     */
    private void updateLabels()
    {
        String identifier = mAlias != null ? mAlias.getName() : mIdentifierPreference.format(mIdentifier);
        final ImageIcon icon = mAlias != null ? mIconManager.getIcon(mAlias.getIconName(), 18) : null;
        final String identifierText = (identifier != null ? truncate(identifier, 33) : "-----");

        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                mIdentifierLabel.setText(identifierText);
                mIconLabel.setIcon(icon);
                mConfigured = true;
            }
        });

    }

    /**
     * Processes audio metadata to update this panel's display values
     */
    public class AudioMetadataProcessor implements Listener<IdentifierCollection>
    {
        @Override
        public void receive(final IdentifierCollection identifierCollection)
        {
            if(identifierCollection != null && (identifierCollection.isUpdated() || !mConfigured))
            {
                List<Identifier> toIds = identifierCollection.getIdentifiers(IdentifierClass.USER, Role.TO);
                mIdentifier = !toIds.isEmpty() ? toIds.get(0) : null;
                AliasList aliasList = mAliasModel.getAliasList(identifierCollection);
                mAlias = (aliasList != null ? aliasList.getAlias(mIdentifier) : null);
                updateLabels();
            }
        }
    }


    @Override
    public void settingChanged(Setting setting)
    {
        if(setting instanceof ColorSetting)
        {
            ColorSetting colorSetting = (ColorSetting)setting;

            switch(colorSetting.getColorSettingName())
            {
                case CHANNEL_STATE_LABEL_DECODER:
                    if(mIdentifierLabel != null)
                    {
                        mIdentifierLabel.setForeground(mLabelColor);
                    }
                    if(mIconLabel != null)
                    {
                        mIconLabel.setForeground(mLabelColor);
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

    /**
     * Receives notifications when preferences have been updated.
     */
    public class PreferenceUpdateListener implements Listener<PreferenceType>
    {
        @Override
        public void receive(PreferenceType preferenceType)
        {
            if(preferenceType == PreferenceType.IDENTIFIER)
            {
                updateLabels();
            }
        }
    }
}
