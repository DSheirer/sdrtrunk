/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

import com.google.common.base.Joiner;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.AudioEvent;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.identifier.TalkgroupFormatPreference;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    private IconModel mIconModel;
    private SettingsManager mSettingsManager;
    private UserPreferences mUserPreferences;
    private TalkgroupFormatPreference mTalkgroupFormatPreference;
    private AudioOutput mAudioOutput;

    private JLabel mMutedLabel = new JLabel("M");
    private JLabel mChannelName = new JLabel(" ");
    private JLabel mIconLabel = new JLabel(" ");
    private JLabel mIdentifierLabel = new JLabel("-----");
    private Identifier mIdentifier;
    private List<Alias> mAliases = Collections.EMPTY_LIST;
    private AliasModel mAliasModel;
    private Lock mLock = new ReentrantLock();

    public AudioChannelPanel(IconModel iconModel, UserPreferences userPreferences, SettingsManager settingsManager,
                             AudioOutput audioOutput, AliasModel aliasModel)
    {
        mIconModel = iconModel;
        mSettingsManager = settingsManager;
        mSettingsManager.addListener(this);
        mAliasModel = aliasModel;
        mUserPreferences = userPreferences;
        mTalkgroupFormatPreference = mUserPreferences.getTalkgroupFormatPreference();
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

    /**
     * Receives preference update notifications via the event bus
     * @param preferenceType that was updated
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.TALKGROUP_FORMAT)
        {
            updateLabels();
        }
    }

    public void dispose()
    {
        //Deregister from receiving preference update notifications
        MyEventBus.getGlobalEventBus().unregister(this);

        if(mAudioOutput != null)
        {
            mAudioOutput.removeAudioEventListener(this);
            mAudioOutput.removeAudioMetadataListener();
        }
    }

    private void init()
    {
        //Register to receive preference updates
        MyEventBus.getGlobalEventBus().register(this);

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
     * Resets the from and to labels.
     */
    private void resetLabels()
    {
        //Protect access to mIdentifier and mAliases
        mLock.lock();

        try
        {
            boolean updated = mIdentifier != null;
            mIdentifier = null;
            mAliases = Collections.EMPTY_LIST;

            //Hold the lock through the label update
            if(updated)
            {
                updateLabels();
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    private void updateIdentifiers(IdentifierCollection identifierCollection)
    {
        if(identifierCollection == null || identifierCollection.isEmpty())
        {
            resetLabels();
            return;
        }

        List<Identifier> toIds = identifierCollection.getIdentifiers(IdentifierClass.USER, Role.TO);

        if(toIds.isEmpty())
        {
            resetLabels();
            return;
        }

        boolean updated = false;

        //Protect access to mIdentifier and mAliases
        mLock.lock();

        try
        {
            if(toIds.size() == 1)
            {
                Identifier currentIdentifier = mIdentifier;

                if(currentIdentifier == null || currentIdentifier != toIds.get(0))
                {
                    mIdentifier = toIds.get(0);
                    AliasList aliasList = mAliasModel.getAliasList(identifierCollection);

                    if(aliasList != null)
                    {
                        mAliases = aliasList.getAliases(mIdentifier);
                    }
                    updated = true;
                }
            }
            else
            {
                mIdentifier = toIds.get(0);
                AliasList aliasList = mAliasModel.getAliasList(identifierCollection);

                if(aliasList != null)
                {
                    mAliases = aliasList.getAliases(mIdentifier);
                }
                updated = true;
            }

            //Hold the lock through the label update
            if(updated)
            {
                updateLabels();
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Updates the alias label with text and icon from the alias.
     */
    private void updateLabels()
    {
        String identifier = null;
        String iconName = null;

        //Protect access to mIdentifier and mAliases
        mLock.lock();

        try
        {
            if(mAliases.size() == 1)
            {
                identifier = mAliases.get(0).getName();
                iconName = mAliases.get(0).getIconName();
            }
            else if(mAliases.size() > 1)
            {
                identifier = Joiner.on(", ").skipNulls().join(mAliases);
            }

            if(identifier == null && mIdentifier != null)
            {
                identifier = mTalkgroupFormatPreference.format(mIdentifier);
            }

            if(identifier == null)
            {
                identifier = "-----";
            }

            final ImageIcon icon = iconName != null ? mIconModel.getIcon(iconName, 18) : null;
            final String identifierText = identifier;

            EventQueue.invokeLater(() -> {
                mIdentifierLabel.setText(identifierText);
                mIconLabel.setIcon(icon);
            });
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Processes audio metadata to update this panel's display values
     */
    public class AudioMetadataProcessor implements Listener<IdentifierCollection>
    {
        @Override
        public void receive(final IdentifierCollection identifierCollection)
        {
            updateIdentifiers(identifierCollection);
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
                    EventQueue.invokeLater(() -> {
                        if(mIdentifierLabel != null)
                        {
                            mIdentifierLabel.setForeground(mLabelColor);
                        }
                        if(mIconLabel != null)
                        {
                            mIconLabel.setForeground(mLabelColor);
                        }
                    });
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
