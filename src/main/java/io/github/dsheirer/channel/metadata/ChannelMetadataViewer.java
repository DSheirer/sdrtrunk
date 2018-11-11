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
package io.github.dsheirer.channel.metadata;

import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.channel.details.ChannelDetailPanel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.icon.IconManager;
import io.github.dsheirer.module.decode.event.CallEventPanel;
import io.github.dsheirer.module.decode.event.MessageActivityPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import java.awt.Color;

public class ChannelMetadataViewer extends JPanel
{
    private ChannelMetadataPanel mChannelMetadataPanel;

    private ChannelDetailPanel mChannelDetailPanel;
    private CallEventPanel mCallEventPanel;
    private MessageActivityPanel mMessageActivityPanel;

    /**
     * GUI panel that combines the currently decoding channels metadata table and viewers for channel details,
     * messages, events, and spectral view.
     */
    public ChannelMetadataViewer(ChannelProcessingManager channelProcessingManager, IconManager iconManager,
                                 AliasModel aliasModel)
    {
        mChannelDetailPanel = new ChannelDetailPanel(channelProcessingManager);
        mCallEventPanel = new CallEventPanel(iconManager);
        mMessageActivityPanel = new MessageActivityPanel(channelProcessingManager);
        mChannelMetadataPanel = new ChannelMetadataPanel(channelProcessingManager, iconManager, aliasModel);

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
