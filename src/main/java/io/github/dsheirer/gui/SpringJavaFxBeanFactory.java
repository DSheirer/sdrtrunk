/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.gui;

import io.github.dsheirer.audio.broadcast.openmhz.OpenMHzEditor;
import io.github.dsheirer.audio.call.CallView;
import io.github.dsheirer.audio.playbackfx.AudioPlaybackChannelsView;
import io.github.dsheirer.gui.icon.IconManager;
import io.github.dsheirer.gui.playlist.PlaylistEditor;
import io.github.dsheirer.gui.playlist.alias.AliasBulkEditor;
import io.github.dsheirer.gui.playlist.alias.AliasConfigurationEditor;
import io.github.dsheirer.gui.playlist.alias.AliasEditor;
import io.github.dsheirer.gui.playlist.alias.AliasItemEditor;
import io.github.dsheirer.gui.playlist.alias.AliasViewByIdentifierEditor;
import io.github.dsheirer.gui.playlist.alias.AliasViewByRecordingEditor;
import io.github.dsheirer.gui.playlist.channel.AMConfigurationEditor;
import io.github.dsheirer.gui.playlist.channel.ChannelEditor;
import io.github.dsheirer.gui.playlist.channel.DMRConfigurationEditor;
import io.github.dsheirer.gui.playlist.channel.LTRConfigurationEditor;
import io.github.dsheirer.gui.playlist.channel.LTRNetConfigurationEditor;
import io.github.dsheirer.gui.playlist.channel.MPT1327ConfigurationEditor;
import io.github.dsheirer.gui.playlist.channel.NBFMConfigurationEditor;
import io.github.dsheirer.gui.playlist.channel.P25P1ConfigurationEditor;
import io.github.dsheirer.gui.playlist.channel.P25P2ConfigurationEditor;
import io.github.dsheirer.gui.playlist.channel.PassportConfigurationEditor;
import io.github.dsheirer.gui.playlist.channel.UnknownConfigurationEditor;
import io.github.dsheirer.gui.playlist.channelMap.ChannelMapEditor;
import io.github.dsheirer.gui.playlist.manager.PlaylistManagerEditor;
import io.github.dsheirer.gui.playlist.radioreference.CountyAgencyEditor;
import io.github.dsheirer.gui.playlist.radioreference.CountySystemEditor;
import io.github.dsheirer.gui.playlist.radioreference.NationalAgencyEditor;
import io.github.dsheirer.gui.playlist.radioreference.RadioReferenceEditor;
import io.github.dsheirer.gui.playlist.radioreference.SiteEditor;
import io.github.dsheirer.gui.playlist.radioreference.StateAgencyEditor;
import io.github.dsheirer.gui.playlist.radioreference.StateSystemEditor;
import io.github.dsheirer.gui.playlist.radioreference.SystemSiteSelectionEditor;
import io.github.dsheirer.gui.playlist.radioreference.SystemTalkgroupSelectionEditor;
import io.github.dsheirer.gui.playlist.radioreference.TalkgroupEditor;
import io.github.dsheirer.gui.playlist.streaming.BroadcastifyCallEditor;
import io.github.dsheirer.gui.playlist.streaming.BroadcastifyStreamEditor;
import io.github.dsheirer.gui.playlist.streaming.IcecastHTTPStreamEditor;
import io.github.dsheirer.gui.playlist.streaming.IcecastTCPStreamEditor;
import io.github.dsheirer.gui.playlist.streaming.RdioScannerEditor;
import io.github.dsheirer.gui.playlist.streaming.ShoutcastV1StreamEditor;
import io.github.dsheirer.gui.playlist.streaming.ShoutcastV2StreamEditor;
import io.github.dsheirer.gui.playlist.streaming.StreamAliasSelectionEditor;
import io.github.dsheirer.gui.playlist.streaming.StreamingEditor;
import io.github.dsheirer.gui.playlist.streaming.UnknownStreamEditor;
import io.github.dsheirer.gui.preference.DecodeEventViewPreferenceEditor;
import io.github.dsheirer.gui.preference.TalkgroupFormatPreferenceEditor;
import io.github.dsheirer.gui.preference.UserPreferencesEditor;
import io.github.dsheirer.gui.preference.application.ApplicationPreferenceEditor;
import io.github.dsheirer.gui.preference.calibration.VectorCalibrationPreferenceEditor;
import io.github.dsheirer.gui.preference.call.CallManagementPreferenceEditor;
import io.github.dsheirer.gui.preference.decoder.JmbeLibraryPreferenceEditor;
import io.github.dsheirer.gui.preference.directory.DirectoryPreferenceEditor;
import io.github.dsheirer.gui.preference.mp3.MP3PreferenceEditor;
import io.github.dsheirer.gui.preference.playback.PlaybackPreferenceEditor;
import io.github.dsheirer.gui.preference.record.RecordPreferenceEditor;
import io.github.dsheirer.gui.preference.tuner.TunerPreferenceEditor;
import io.github.dsheirer.jmbe.JmbeEditor;
import java.awt.GraphicsEnvironment;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import jiconfont.javafx.IconFontFX;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

/**
 * Spring configuration factory for JavaFX UI components that require dependency injection (@Resource)
 *
 * Note: all bean creator methods are also annotated as @Lazy so that Spring doesn't auto-instantiate these beans when
 * we're running in a headless environment.
 */
@Configuration
public class SpringJavaFxBeanFactory
{
    private JFXPanel mJFXPanel;

    /**
     * Constructs an instance
     */
    public SpringJavaFxBeanFactory()
    {
        if(!GraphicsEnvironment.isHeadless())
        {
            //Initialize the JavafX platform by creating a JFXPanel.
            createJFXPanel();

            //Register JavaFX icon fonts
            IconFontFX.register(jiconfont.icons.font_awesome.FontAwesome.getIconFont());
            IconFontFX.register(jiconfont.icons.elusive.Elusive.getIconFont());
        }
    }

    /**
     * Initialize the JavaFX platform by creating a JavaFX panel for Swing application compatibility
     */
    private void createJFXPanel()
    {
        if(mJFXPanel == null)
        {
            mJFXPanel = new JFXPanel();
            Platform.setImplicitExit(false);
        }
    }

    /**
     * Call view Panel
     */
    @Bean
    @Lazy
    public CallView getCallViewPanel()
    {
        return new CallView();
    }

    /**
     * Audio playback channels view
     */
    @Bean
    @Lazy
    public AudioPlaybackChannelsView getAudioPlaybackChannelsView()
    {
        return new AudioPlaybackChannelsView();
    }

    /**
     * Alias Item Editor
     */
    @Bean
    @Lazy
    public AliasItemEditor getAliasItemEditor()
    {
        return new AliasItemEditor();
    }

    /**
     * Alias Bulk Editor
     */
    @Bean
    @Lazy
    public AliasBulkEditor getAliasBulkEditor()
    {
        return new AliasBulkEditor();
    }

    /**
     * Alias Configuration Editor
     */
    @Bean
    @Lazy
    public AliasConfigurationEditor getAliasConfigurationEditor()
    {
        return new AliasConfigurationEditor();
    }

    /**
     * Alias Editor
     */
    @Bean
    @Lazy
    public AliasEditor getAliasEditor()
    {
        return new AliasEditor();
    }

    /**
     * Alias View By Identifier Editor
     */
    @Bean
    @Lazy
    public AliasViewByIdentifierEditor getAliasViewByIdentifierEditor()
    {
        return new AliasViewByIdentifierEditor();
    }

    /**
     * Channel Editor
     */
    @Bean
    @Lazy
    public ChannelEditor getChannelEditor()
    {
        return new ChannelEditor();
    }

    /**
     * Playlist Editor
     */
    @Bean
    @Lazy
    public PlaylistEditor getPlaylistEditor()
    {
        return new PlaylistEditor();
    }

    /**
     * Playlist Manager Editor
     */
    @Bean
    @Lazy
    public PlaylistManagerEditor getPlaylistManagerEditor()
    {
        return new PlaylistManagerEditor();
    }

    /**
     * Radio Reference Editor
     */
    @Bean
    @Lazy
    public RadioReferenceEditor getRadioReferenceEditor()
    {
        return new RadioReferenceEditor();
    }

    /**
     * Stream alias selection editor
     */
    @Bean
    @Lazy
    public StreamAliasSelectionEditor getStreamAliasSelectionEditor()
    {
        return new StreamAliasSelectionEditor();
    }

    /**
     * Streaming editor
     */
    @Bean
    @Lazy
    public StreamingEditor getStreamingEditor()
    {
        return new StreamingEditor();
    }

    /**
     * AM channel configuration editor
     */
    @Bean
    @Lazy
    public AMConfigurationEditor getAMConfigurationEditor()
    {
        return new AMConfigurationEditor();
    }

    /**
     * NBFM channel configuration editor
     */
    @Bean
    @Lazy
    public NBFMConfigurationEditor getNBFMConfigurationEditor()
    {
        return new NBFMConfigurationEditor();
    }

    /**
     * DMR channel configuration editor
     */
    @Bean
    @Lazy
    public DMRConfigurationEditor getDMRConfigurationEditor()
    {
        return new DMRConfigurationEditor();
    }

    /**
     * LTR channel configuration editor
     */
    @Bean
    @Lazy
    public LTRConfigurationEditor getLTRConfigurationEditor()
    {
        return new LTRConfigurationEditor();
    }

    /**
     * LTR-Net channel configuration editor
     */
    @Bean
    @Lazy
    public LTRNetConfigurationEditor getLTRNetConfigurationEditor()
    {
        return new LTRNetConfigurationEditor();
    }

    /**
     * MPT-1327 channel configuration editor
     */
    @Bean
    @Lazy
    public MPT1327ConfigurationEditor getMPT1327ConfigurationEditor()
    {
        return new MPT1327ConfigurationEditor();
    }

    /**
     * P25 Phase 1 channel configuration editor
     */
    @Bean
    @Lazy
    public P25P1ConfigurationEditor getP25P1ConfigurationEditor()
    {
        return new P25P1ConfigurationEditor();
    }

    /**
     * P25 Phase 2 channel configuration editor
     */
    @Bean
    @Lazy
    public P25P2ConfigurationEditor getP25P2ConfigurationEditor()
    {
        return new P25P2ConfigurationEditor();
    }

    /**
     * Passport channel configuration editor
     */
    @Bean
    @Lazy
    public PassportConfigurationEditor getPassportConfigurationEditor()
    {
        return new PassportConfigurationEditor();
    }

    /**
     * Unknown channel configuration editor
     */
    @Bean
    @Lazy
    public UnknownConfigurationEditor getUnknownConfigurationEditor()
    {
        return new UnknownConfigurationEditor();
    }

    /**
     * Broadcastify stream editor
     */
    @Bean
    @Lazy
    public BroadcastifyStreamEditor getBroadcastifyStreamEditor()
    {
        return new BroadcastifyStreamEditor();
    }

    /**
     * Broadcastify calls stream editor
     */
    @Bean
    @Lazy
    public BroadcastifyCallEditor getBroadcastifyCallEditor()
    {
        return new BroadcastifyCallEditor();
    }


    /**
     * OpenMHZ stream editor
     */
    @Bean
    @Lazy
    public OpenMHzEditor getOpenMhzEditor()
    {
        return new OpenMHzEditor();
    }

    /**
     * Rdio scanner stream editor
     */
    @Bean
    @Lazy
    public RdioScannerEditor getRdioScannerEditor()
    {
        return new RdioScannerEditor();
    }

    /**
     * Icecast TCP stream editor
     */
    @Bean
    @Lazy
    public IcecastTCPStreamEditor getIcecastTcpStreamEditor()
    {
        return new IcecastTCPStreamEditor();
    }

    /**
     * Icecast HTTP stream editor
     */
    @Bean
    @Lazy
    public IcecastHTTPStreamEditor getIcecastHTTPStreamEditor()
    {
        return new IcecastHTTPStreamEditor();
    }

    /**
     * Shoutcast V1 stream editor
     */
    @Bean
    @Lazy
    public ShoutcastV1StreamEditor getShoutcastV1StreamEditor()
    {
        return new ShoutcastV1StreamEditor();
    }

    /**
     * Shoutcast V1 stream editor
     */
    @Bean
    @Lazy
    public ShoutcastV2StreamEditor getShoutcastV2StreamEditor()
    {
        return new ShoutcastV2StreamEditor();
    }

    /**
     * Unknown stream editor
     */
    @Bean
    @Lazy
    public UnknownStreamEditor getUnknownStreamEditor()
    {
        return new UnknownStreamEditor();
    }

    /**
     * County agency editor
     */
    @Bean
    @Lazy
    public CountyAgencyEditor getCountyAgencyEditor()
    {
        return new CountyAgencyEditor();
    }

    /**
     * County system editor
     */
    @Bean
    @Lazy
    public CountySystemEditor getCountySystemEditor()
    {
        return new CountySystemEditor();
    }

    /**
     * State agency editor
     */
    @Bean
    @Lazy
    public StateAgencyEditor getStateAgencyEditor()
    {
        return new StateAgencyEditor();
    }

    /**
     * State system editor
     */
    @Bean
    @Lazy
    public StateSystemEditor getStateSystemEditor()
    {
        return new StateSystemEditor();
    }

    /**
     * National agency editor
     */
    @Bean
    @Lazy
    public NationalAgencyEditor getNationalAgencyEditor()
    {
        return new NationalAgencyEditor();
    }

    /**
     * System talkgroup selection editor
     *
     * Note: editor is used in multiple places so we create a new instance for each (ie scope=prototype)
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Lazy
    public SystemTalkgroupSelectionEditor getSystemTalkgroupSelectionEditor()
    {
        return new SystemTalkgroupSelectionEditor();
    }

    /**
     * Site editor
     *
     * Note: editor is used in multiple places so we create a new instance for each (ie scope=prototype)
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Lazy
    public SiteEditor getSiteEditor()
    {
        return new SiteEditor();
    }

    /**
     * Talkgroup editor
     *
     * Note: editor is used in multiple places so we create a new instance for each (ie scope=prototype)
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Lazy
    public TalkgroupEditor getTalkgroupEditor()
    {
        return new TalkgroupEditor();
    }

    /**
     * System site selection editor
     *
     * Note: editor is used in multiple places so we create a new instance for each (ie scope=prototype)
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Lazy
    public SystemSiteSelectionEditor getSystemSiteSelectionEditor()
    {
        return new SystemSiteSelectionEditor();
    }

    /**
     * Channel map editor
     */
    @Bean
    @Lazy
    public ChannelMapEditor getChannelMapEditor()
    {
        return new ChannelMapEditor();
    }

    /**
     * Channel map editor
     */
    @Bean
    @Lazy
    public IconManager getIconManager()
    {
        return new IconManager();
    }

    /**
     * User preferences editor
     */
    @Bean
    @Lazy
    public UserPreferencesEditor getUserPreferencesEditor()
    {
        return new UserPreferencesEditor();
    }

    /**
     * Jmbe editor
     */
    @Bean
    @Lazy
    public JmbeEditor getJmbeEditor()
    {
        return new JmbeEditor();
    }

    /**
     * Alias view-by recording editor
     */
    @Bean
    @Lazy
    public AliasViewByRecordingEditor getAliasViewByRecordingEditor()
    {
        return new AliasViewByRecordingEditor();
    }

    /**
     * Application preference editor
     */
    @Bean
    @Lazy
    public ApplicationPreferenceEditor getApplicationPreferenceEditor()
    {
        return new ApplicationPreferenceEditor();
    }

    /**
     * Call management preference editor
     */
    @Bean
    @Lazy
    public CallManagementPreferenceEditor getCallManagementPreferenceEditor()
    {
        return new CallManagementPreferenceEditor();
    }

    /**
     * Application preference editor
     */
    @Bean
    @Lazy
    public MP3PreferenceEditor getMp3PreferenceEditor()
    {
        return new MP3PreferenceEditor();
    }

    /**
     * Playback preference editor
     */
    @Bean
    @Lazy
    public PlaybackPreferenceEditor getPlaybackPreferenceEditor()
    {
        return new PlaybackPreferenceEditor();
    }

    /**
     * Record preference editor
     */
    @Bean
    @Lazy
    public RecordPreferenceEditor getRecordPreferenceEditor()
    {
        return new RecordPreferenceEditor();
    }

    /**
     * Decode event view preference editor
     */
    @Bean
    @Lazy
    public DecodeEventViewPreferenceEditor getDecodeEventViewPreferenceEditor()
    {
        return new DecodeEventViewPreferenceEditor();
    }

    /**
     * Directory preference editor
     */
    @Bean
    @Lazy
    public DirectoryPreferenceEditor getDirectoryPreferenceEditor()
    {
        return new DirectoryPreferenceEditor();
    }

    /**
     * Playback preference editor
     */
    @Bean
    @Lazy
    public JmbeLibraryPreferenceEditor getJmbeLibraryPreferenceEditor()
    {
        return new JmbeLibraryPreferenceEditor();
    }

    /**
     * Tuner preference editor
     */
    @Bean
    @Lazy
    public TunerPreferenceEditor getTunerPreferenceEditor()
    {
        return new TunerPreferenceEditor();
    }

    /**
     * Talkgroup format preference editor
     */
    @Bean
    @Lazy
    public TalkgroupFormatPreferenceEditor getTalkgroupFormatPreferenceEditor()
    {
        return new TalkgroupFormatPreferenceEditor();
    }

    /**
     * Vector calibration preference editor
     */
    @Bean
    @Lazy
    public VectorCalibrationPreferenceEditor getVectorCalibrationPreferenceEditor()
    {
        return new VectorCalibrationPreferenceEditor();
    }
}
