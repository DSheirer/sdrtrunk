/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
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
package controller.activity;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;
import controller.channel.Channel;
import controller.channel.Channel.ChannelEvent;
import controller.channel.ChannelListener;

public class CallEventPanel extends JPanel implements ChannelListener
{
    private static final long serialVersionUID = 1L;

    private JScrollPane mEmptyScroller;
    private Channel mDisplayedChannel;

	public CallEventPanel()
	{
    	setLayout( new MigLayout("insets 0 0 0 0 ", "[grow,fill]", "[grow,fill]") );

    	JTable table = new JTable( new CallEventModel() );
    	table.setAutoCreateRowSorter( true );
    	table.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
    	mEmptyScroller = new JScrollPane( table );
    	
    	add( mEmptyScroller );
	}

	@Override
    public void occurred( Channel channel, ChannelEvent event )
    {
		if( event == ChannelEvent.CHANGE_SELECTED && channel.getSelected() )
		{
			if( mDisplayedChannel == null || 
				( mDisplayedChannel != null && mDisplayedChannel != channel ) )
			{
				removeAll();
				
				JScrollPane scroller = new JScrollPane(
						channel.getProcessingChain().getChannelState()
						.getCallEventTable() );

				add( scroller );
				
				mDisplayedChannel = channel;
				
				revalidate();
				repaint();
			}
		}
		else if( event == ChannelEvent.PROCESSING_STOPPED ||
				 event == ChannelEvent.CHANNEL_DELETED )
		{
			if( mDisplayedChannel != null && mDisplayedChannel == channel )
			{
				mDisplayedChannel = null;
				removeAll();
				add( mEmptyScroller );

				revalidate();
				repaint();
			}
		}
    }
}
