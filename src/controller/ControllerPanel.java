/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package controller;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import audio.broadcast.BroadcastModel;
import audio.broadcast.BroadcastPanel;
import icon.IconManager;
import map.MapPanel;
import map.MapService;
import module.decode.event.CallEventPanel;
import module.decode.event.MessageActivityPanel;
import module.decode.state.ChannelList;
import net.miginfocom.swing.MigLayout;
import settings.SettingsManager;
import source.SourceManager;
import source.tuner.TunerModel;
import source.tuner.TunerViewPanel;
import spectrum.ChannelSpectrumPanel;
import alias.AliasController;
import alias.AliasModel;
import audio.AudioManager;
import audio.AudioPanel;

import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;

import controller.channel.ChannelController;
import controller.channel.ChannelModel;
import controller.channel.ChannelProcessingManager;
import controller.channel.map.ChannelMapModel;

public class ControllerPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    private AliasController mAliasController;
    private AudioPanel mAudioPanel;
    private BroadcastPanel mBroadcastPanel;
    private CallEventPanel mCallEventPanel;
    private ChannelController mChannelController;
    private ChannelList mChannelStateList;
    private ChannelModel mChannelModel;
    private ChannelSpectrumPanel mChannelSpectrumPanel;
    private MapPanel mMapPanel;
    private MessageActivityPanel mMessageActivityPanel;
    private TunerViewPanel mTunerManagerPanel;

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
        mChannelModel = channelModel;

        mAudioPanel = new AudioPanel(iconManager, settingsManager, sourceManager, audioManager);

        mMapPanel = new MapPanel(mapService, iconManager, settingsManager);

        mMessageActivityPanel = new MessageActivityPanel(channelProcessingManager);

        mBroadcastPanel = new BroadcastPanel(broadcastModel, aliasModel, iconManager);

        mCallEventPanel = new CallEventPanel(iconManager, channelProcessingManager);

        mChannelSpectrumPanel = new ChannelSpectrumPanel(settingsManager,
                channelProcessingManager);

        mChannelStateList = new ChannelList(channelModel, channelProcessingManager, iconManager,
                settingsManager, mAudioPanel);

        mChannelController = new ChannelController(channelModel, channelMapModel,
                sourceManager, aliasModel);

        mAliasController = new AliasController(aliasModel, broadcastModel, iconManager);
        mTunerManagerPanel = new TunerViewPanel(tunerModel);

        init();
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0 ",
                "[grow,fill]",
                "[grow,fill]"));

        //Tabbed View - configuration, calls, messages, map
        mTabbedPane = new JideTabbedPane();
        mTabbedPane.setFont(this.getFont());
        mTabbedPane.setForeground(Color.BLACK);
        mTabbedPane.addTab("Tuners", mTunerManagerPanel);
        mTabbedPane.addTab("Channels", mChannelController);
        mTabbedPane.addTab("Aliases", mAliasController);
        mTabbedPane.addTab("Channel Spectrum", mChannelSpectrumPanel);
        mTabbedPane.addTab("Events", mCallEventPanel);
        mTabbedPane.addTab("Map", mMapPanel);
        mTabbedPane.addTab("Messages", mMessageActivityPanel);
        mTabbedPane.addTab("Streaming", mBroadcastPanel);

        /**
         * Change listener to enable/disable the channel spectrum display
         * only when the tab is visible, and a channel has been selected
         */
        mTabbedPane.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent event)
            {
                int index = mTabbedPane.getSelectedIndex();

                Component component = mTabbedPane.getComponentAt(index);

                if (component instanceof ChannelSpectrumPanel)
                {
                    mChannelSpectrumPanel.setEnabled(true);
                }
                else
                {
                    mChannelSpectrumPanel.setEnabled(false);
                }
            }
        });

    	/* Register each of the components to receive channel events when the
    	 * channels are selected or change */
        mChannelModel.addListener(mCallEventPanel);
        mChannelModel.addListener(mChannelStateList);
        mChannelModel.addListener(mChannelSpectrumPanel);
        mChannelModel.addListener(mMessageActivityPanel);

        JScrollPane channelStateListScroll = new JScrollPane();
        channelStateListScroll.getViewport().setView(mChannelStateList);
        channelStateListScroll.setPreferredSize(new Dimension(400, 500));

        //Set preferred size to influence the split between these panels
        mTabbedPane.setPreferredSize(new Dimension(880, 500));

        JideSplitPane channelSplit = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
        channelSplit.setDividerSize(5);
        channelSplit.add(channelStateListScroll);
        channelSplit.add(mTabbedPane);

        add(channelSplit);
    }
}
