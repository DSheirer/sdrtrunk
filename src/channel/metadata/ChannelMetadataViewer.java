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
import icon.IconTableModel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class ChannelMetadataViewer extends JPanel
{
    private ChannelMetadataModel mChannelMetadataModel;
    private IconTableModel mIconTableModel;

    /**
     * GUI panel that combines the currently decoding channels metadata table and viewers for channel details,
     * messages, events, and spectral view.
     */
    public ChannelMetadataViewer(ChannelMetadataModel channelMetadataModel, IconTableModel iconTableModel)
    {
        mChannelMetadataModel = channelMetadataModel;
        mIconTableModel = iconTableModel;
    }

    private void init()
    {
        setLayout( new MigLayout( "", "[grow,fill]", "[grow,fill]") );

        ChannelMetadataPanel channelMetadataPanel = new ChannelMetadataPanel(mChannelMetadataModel, mIconTableModel);

        JideTabbedPane tabbedPane = new JideTabbedPane();
        tabbedPane.addTab("Details", new JPanel());
        tabbedPane.addTab("Events", new JPanel());
        tabbedPane.addTab("Messages", new JPanel());
        tabbedPane.addTab("Spectrum", new JPanel());

        JideSplitPane splitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
        splitPane.add(channelMetadataPanel);
        splitPane.add(tabbedPane);

        add(splitPane);
    }
}
