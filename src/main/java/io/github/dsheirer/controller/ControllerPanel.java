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
package io.github.dsheirer.controller;

import com.jidesoft.swing.JideTabbedPane;
import io.github.dsheirer.alias.AliasController;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.audio.broadcast.BroadcastPanel;
import io.github.dsheirer.audio.playback.AudioPanel;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.channel.metadata.NowPlayingPanel;
import io.github.dsheirer.controller.channel.ChannelController;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.icon.IconManager;
import io.github.dsheirer.map.MapPanel;
import io.github.dsheirer.map.MapService;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.SourceManager;
import io.github.dsheirer.source.tuner.TunerModel;
import io.github.dsheirer.source.tuner.TunerViewPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;

public class ControllerPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    private AliasController mAliasController;
    private AudioPanel mAudioPanel;
    private BroadcastPanel mBroadcastPanel;
    private ChannelController mChannelController;
    private NowPlayingPanel mNowPlayingPanel;
    private ChannelModel mChannelModel;
    private MapPanel mMapPanel;
    private TunerViewPanel mTunerManagerPanel;
    private BroadcastModel mBroadcastModel;

    private JideTabbedPane mTabbedPane;

    public ControllerPanel(AudioPlaybackManager audioPlaybackManager, AliasModel aliasModel, BroadcastModel broadcastModel,
                           ChannelModel channelModel, ChannelMapModel channelMapModel, ChannelProcessingManager channelProcessingManager,
                           IconManager iconManager, MapService mapService, SettingsManager settingsManager,
                           SourceManager sourceManager, TunerModel tunerModel, UserPreferences userPreferences)
    {
        mBroadcastModel = broadcastModel;
        mChannelModel = channelModel;

        mAudioPanel = new AudioPanel(iconManager, userPreferences, settingsManager, sourceManager, audioPlaybackManager,
            aliasModel);

        mNowPlayingPanel = new NowPlayingPanel(channelModel, channelProcessingManager, iconManager,
            aliasModel, userPreferences);

        mMapPanel = new MapPanel(mapService, aliasModel, iconManager, settingsManager);

        mBroadcastPanel = new BroadcastPanel(broadcastModel, aliasModel, iconManager, userPreferences);

        mChannelController = new ChannelController(channelModel, channelProcessingManager, channelMapModel,
            sourceManager, aliasModel, userPreferences);

        mAliasController = new AliasController(aliasModel, broadcastModel, iconManager, userPreferences);

        mTunerManagerPanel = new TunerViewPanel(tunerModel, userPreferences);

        init();
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0 ", "[grow,fill]", "[]0[grow,fill]0[]"));

        add(mAudioPanel, "wrap");

        //Tabbed View - configuration, calls, messages, map
        mTabbedPane = new JideTabbedPane();
        mTabbedPane.setFont(this.getFont());
        mTabbedPane.setForeground(Color.BLACK);
        mTabbedPane.addTab("Now Playing", mNowPlayingPanel);
        mTabbedPane.addTab("Aliases", mAliasController);
        mTabbedPane.addTab("Channels", mChannelController);
        mTabbedPane.addTab("Map", mMapPanel);
        mTabbedPane.addTab("Streaming", mBroadcastPanel);
        mTabbedPane.addTab("Tuners", mTunerManagerPanel);

        //Set preferred size to influence the split between these panels
        mTabbedPane.setPreferredSize(new Dimension(880, 500));

        add(mTabbedPane,"wrap");
    }
}
