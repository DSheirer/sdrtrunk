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

import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;
import controller.channel.ChannelProcessingManager;
import icon.IconManager;
import icon.IconTableModel;
import module.decode.event.CallEventPanel;
import module.decode.event.MessageActivityPanel;
import net.miginfocom.swing.MigLayout;
import settings.SettingsManager;
import spectrum.ChannelSpectrumPanel;

import javax.swing.*;

public class ChannelMetadataViewer extends JPanel
{
    private CallEventPanel mCallEventPanel;
    private MessageActivityPanel mMessageActivityPanel;
    private ChannelSpectrumPanel mChannelSpectrumPanel;
    private ChannelMetadataPanel mChannelMetadataPanel;

    /**
     * GUI panel that combines the currently decoding channels metadata table and viewers for channel details,
     * messages, events, and spectral view.
     */
    public ChannelMetadataViewer(ChannelProcessingManager channelProcessingManager, IconManager iconManager,
                                 SettingsManager settingsManager)
    {
        mCallEventPanel = new CallEventPanel(iconManager, channelProcessingManager);
        mMessageActivityPanel = new MessageActivityPanel(channelProcessingManager);
        mChannelSpectrumPanel = new ChannelSpectrumPanel(settingsManager, channelProcessingManager);
        mChannelMetadataPanel = new ChannelMetadataPanel(channelProcessingManager.getChannelMetadataModel(),
            iconManager);

        init();
    }

    private void init()
    {
        setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[grow,fill]") );

        JideTabbedPane tabbedPane = new JideTabbedPane();
        tabbedPane.addTab("Details", new JPanel());
        tabbedPane.addTab("Events", mCallEventPanel);
        tabbedPane.addTab("Messages", mMessageActivityPanel);
        tabbedPane.addTab("Spectrum", mChannelSpectrumPanel);

        JideSplitPane splitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
        splitPane.add(mChannelMetadataPanel);
        splitPane.add(tabbedPane);
        add(splitPane);

//TODO: move this change listener to the metadata table view
//        /**
//         * Change listener to enable/disable the channel spectrum display
//         * only when the tab is visible, and a channel has been selected
//         */
//        mTabbedPane.addChangeListener(new ChangeListener()
//        {
//            @Override
//            public void stateChanged(ChangeEvent event)
//            {
//                int index = mTabbedPane.getSelectedIndex();
//
//                Component component = mTabbedPane.getComponentAt(index);
//
//                if(component instanceof ChannelSpectrumPanel)
//                {
//                    mChannelSpectrumPanel.setEnabled(true);
//                }
//                else
//                {
//                    mChannelSpectrumPanel.setEnabled(false);
//                }
//            }
//        });

//TODO: call, message and spectrum should listen to the metadata table now
//
//
//    	/* Register each of the components to receive channel events when the
//         * channels are selected or change */
//        mChannelModel.addListener(mCallEventPanel);
//        mChannelModel.addListener(mChannelSpectrumPanel);
//        mChannelModel.addListener(mMessageActivityPanel);

    }
}
