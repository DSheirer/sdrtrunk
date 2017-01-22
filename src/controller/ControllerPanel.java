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
package controller;

import alias.AliasController;
import alias.AliasModel;
import audio.AudioManager;
import audio.AudioPanel;
import audio.broadcast.BroadcastModel;
import audio.broadcast.BroadcastPanel;
import audio.broadcast.BroadcastStatusPanel;
import channel.metadata.ChannelMetadataViewer;
import com.jidesoft.swing.JideTabbedPane;
import controller.channel.ChannelController;
import controller.channel.ChannelModel;
import controller.channel.ChannelProcessingManager;
import controller.channel.map.ChannelMapModel;
import icon.IconManager;
import map.MapPanel;
import map.MapService;
import net.miginfocom.swing.MigLayout;
import properties.SystemProperties;
import settings.SettingsManager;
import source.SourceManager;
import source.tuner.TunerModel;
import source.tuner.TunerViewPanel;

import javax.swing.*;
import java.awt.*;

public class ControllerPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    private AliasController mAliasController;
    private AudioPanel mAudioPanel;
    private BroadcastPanel mBroadcastPanel;
    private ChannelController mChannelController;
    private ChannelMetadataViewer mChannelMetadataViewer;
    private ChannelModel mChannelModel;
    private MapPanel mMapPanel;
    private TunerViewPanel mTunerManagerPanel;
    private BroadcastModel mBroadcastModel;

    private JideTabbedPane mTabbedPane;
    protected JTable mChannelActivityTable = new JTable();

    public ControllerPanel(AudioManager audioManager,
                           AliasModel aliasModel,
                           BroadcastModel broadcastModel,
                           ChannelModel channelModel,
                           ChannelMapModel channelMapModel,
                           ChannelProcessingManager channelProcessingManager,
                           IconManager iconManager,
                           MapService mapService,
                           SettingsManager settingsManager,
                           SourceManager sourceManager,
                           TunerModel tunerModel)
    {
        mBroadcastModel = broadcastModel;
        mChannelModel = channelModel;

        mAudioPanel = new AudioPanel(iconManager, settingsManager, sourceManager, audioManager);

        mChannelMetadataViewer = new ChannelMetadataViewer(channelProcessingManager, iconManager, settingsManager);

        mMapPanel = new MapPanel(mapService, iconManager, settingsManager);

        mBroadcastPanel = new BroadcastPanel(broadcastModel, aliasModel, iconManager);

        mChannelController = new ChannelController(channelModel, channelMapModel, sourceManager, aliasModel);

        mAliasController = new AliasController(aliasModel, broadcastModel, iconManager);

        mTunerManagerPanel = new TunerViewPanel(tunerModel);

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
        mTabbedPane.addTab("Now Playing", mChannelMetadataViewer);
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
