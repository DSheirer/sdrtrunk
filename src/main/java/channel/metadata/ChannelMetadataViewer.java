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
package channel.metadata;

import channel.details.ChannelDetailPanel;
import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;
import controller.channel.ChannelProcessingManager;
import icon.IconManager;
import module.decode.event.CallEventPanel;
import module.decode.event.MessageActivityPanel;
import net.miginfocom.swing.MigLayout;
import settings.SettingsManager;
import spectrum.ChannelSpectrumPanel;

import javax.swing.*;
import java.awt.*;

public class ChannelMetadataViewer extends JPanel
{
    private ChannelMetadataPanel mChannelMetadataPanel;

    private ChannelDetailPanel mChannelDetailPanel;
    private CallEventPanel mCallEventPanel;
    private MessageActivityPanel mMessageActivityPanel;
    private ChannelSpectrumPanel mChannelSpectrumPanel;

    /**
     * GUI panel that combines the currently decoding channels metadata table and viewers for channel details,
     * messages, events, and spectral view.
     */
    public ChannelMetadataViewer(ChannelProcessingManager channelProcessingManager, IconManager iconManager,
                                 SettingsManager settingsManager)
    {
        mChannelDetailPanel = new ChannelDetailPanel(channelProcessingManager);
        mCallEventPanel = new CallEventPanel(iconManager);
        mMessageActivityPanel = new MessageActivityPanel(channelProcessingManager);
//        mChannelSpectrumPanel = new ChannelSpectrumPanel(settingsManager, channelProcessingManager);
        mChannelMetadataPanel = new ChannelMetadataPanel(channelProcessingManager, iconManager);

        init();
    }

    private void init()
    {
        setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[grow,fill]") );

        JideTabbedPane tabbedPane = new JideTabbedPane();
        tabbedPane.addTab("Details", mChannelDetailPanel);
        tabbedPane.addTab("Events", mCallEventPanel);
        tabbedPane.addTab("Messages", mMessageActivityPanel);
//        tabbedPane.addTab("Spectrum", mChannelSpectrumPanel);
        tabbedPane.setFont(this.getFont());
        tabbedPane.setForeground(Color.BLACK);

        JideSplitPane splitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
        splitPane.setShowGripper(true);
        splitPane.add(mChannelMetadataPanel);
        splitPane.add(tabbedPane);
        add(splitPane);

        mChannelMetadataPanel.addProcessingChainSelectionListener(mChannelDetailPanel);
        mChannelMetadataPanel.addProcessingChainSelectionListener(mCallEventPanel);
        mChannelMetadataPanel.addProcessingChainSelectionListener(mMessageActivityPanel);
    }
}
