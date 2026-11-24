/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
 * ****************************************************************************
 */

package io.github.dsheirer.gui.settings;

import io.github.dsheirer.gui.settings.application.AppearanceSettingsPage;
import io.github.dsheirer.gui.settings.application.GeneralSettingsPage;
import io.github.dsheirer.gui.settings.calibration.PerformanceSettingsPage;
import io.github.dsheirer.gui.settings.call.CallManagementSettingsPage;
import io.github.dsheirer.gui.settings.decoder.AudioCodecSettingsPage;
import io.github.dsheirer.gui.settings.directory.StorageSettingsPage;
import io.github.dsheirer.gui.settings.map.MapSettingsPage;
import io.github.dsheirer.gui.settings.playback.PlaybackSettingsPage;
import io.github.dsheirer.gui.settings.radioreference.RadioReferenceSettingsPage;
import io.github.dsheirer.gui.settings.record.RecordingSettingsPage;
import io.github.dsheirer.gui.settings.tuner.TunerSettingsPage;
import java.util.function.Function;
import jiconfont.icons.font_awesome.FontAwesome;

/**
 * Pages of the settings dialog, in sidebar order.  Each page belongs to a sidebar group and provides the title, icon
 * and description shown in the sidebar and page header, and creates its page.
 */
public enum SettingsPageType
{
    GENERAL(Group.GENERAL, "General", FontAwesome.SLIDERS,
        "Application health monitoring and channel start-up.",
        context -> new GeneralSettingsPage(context.settings())),
    APPEARANCE(Group.GENERAL, "Appearance", FontAwesome.PAINT_BRUSH,
        "Theme, alias colors and the size of the user interface.",
        context -> new AppearanceSettingsPage(context.settings())),
    PERFORMANCE(Group.GENERAL, "Performance", FontAwesome.TACHOMETER,
        "CPU vector (SIMD) operations and their calibration.",
        context -> new PerformanceSettingsPage(context.settings(), context.channelProcessingManager())),

    TUNERS(Group.RADIO, "Tuners", FontAwesome.SIGNAL,
        "How tuner samples are divided into channels, and tuner specific options.",
        context -> new TunerSettingsPage(context.settings())),
    AUDIO_CODECS(Group.RADIO, "Audio Codecs", FontAwesome.CUBES,
        "The JMBE library that decodes digital voice audio.",
        context -> new AudioCodecSettingsPage(context.settings())),
    FORMATS(Group.RADIO, "Formats", FontAwesome.HASHTAG,
        "How talkgroups, radio identifiers and event timestamps are displayed.",
        context -> new FormatSettingsPage(context.settings())),

    PLAYBACK(Group.AUDIO, "Playback & Tones", FontAwesome.HEADPHONES,
        "Audio output device and the tones inserted into call audio.",
        context -> new PlaybackSettingsPage(context.settings())),
    CALL_MANAGEMENT(Group.AUDIO, "Call Management", FontAwesome.CLONE,
        "Duplicate call detection and suppression, and patch group streaming.",
        context -> new CallManagementSettingsPage(context.settings())),
    RECORDING(Group.AUDIO, "Recording", FontAwesome.MICROPHONE,
        "File formats for audio and baseband I/Q recordings, and MP3 encoding.",
        context -> new RecordingSettingsPage(context.settings())),

    STORAGE(Group.DATA, "Storage", FontAwesome.FOLDER_OPEN,
        "Where sdrtrunk stores its files, and disk usage limits.",
        context -> new StorageSettingsPage(context.settings())),

    RADIO_REFERENCE(Group.SERVICES, "Radio Reference", FontAwesome.GLOBE,
        "Your Radio Reference account, imports and alias list updates.",
        RadioReferenceSettingsPage::new),
    MAP(Group.SERVICES, "Map", FontAwesome.MAP,
        "Map tiles and the layers shown on the Map tab.",
        context -> new MapSettingsPage(context.settings()));

    private final Group mGroup;
    private final String mTitle;
    private final FontAwesome mIcon;
    private final String mDescription;
    private final Function<SettingsContext,SettingsPage> mFactory;

    SettingsPageType(Group group, String title, FontAwesome icon, String description,
                     Function<SettingsContext,SettingsPage> factory)
    {
        mGroup = group;
        mTitle = title;
        mIcon = icon;
        mDescription = description;
        mFactory = factory;
    }

    /**
     * Sidebar group for the page
     */
    public Group getGroup()
    {
        return mGroup;
    }

    /**
     * Page title for the sidebar and the page header
     */
    public String getTitle()
    {
        return mTitle;
    }

    /**
     * Page icon for the sidebar and the page header
     */
    public FontAwesome getIcon()
    {
        return mIcon;
    }

    /**
     * One line description shown under the page title
     */
    public String getDescription()
    {
        return mDescription;
    }

    /**
     * Creates the page
     * @param context with the settings to edit
     * @return page
     */
    public SettingsPage createPage(SettingsContext context)
    {
        return mFactory.apply(context);
    }

    @Override
    public String toString()
    {
        return mTitle;
    }

    /**
     * Sidebar groups, in sidebar order
     */
    public enum Group
    {
        GENERAL("General"),
        RADIO("Radio"),
        AUDIO("Audio"),
        DATA("Data"),
        SERVICES("Services");

        private final String mLabel;

        Group(String label)
        {
            mLabel = label;
        }

        @Override
        public String toString()
        {
            return mLabel;
        }
    }
}
