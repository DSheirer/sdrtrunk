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
import controller.channel.ChannelEvent;
import controller.channel.ChannelEvent.Event;
import controller.channel.ChannelEventListener;

public class MessageActivityPanel extends JPanel implements ChannelEventListener
{
    private static final long serialVersionUID = 1L;
    
    private JScrollPane mEmptyScrollPane;
    private Channel mDisplayedChannel;

	public MessageActivityPanel()
	{
    	setLayout( new MigLayout("insets 0 0 0 0 ", "[grow,fill]", "[grow,fill]") );

    	mEmptyScrollPane = 
    			new JScrollPane( new JTable( new MessageActivityModel() ) );
    	
    	add( mEmptyScrollPane );
	}

	@Override
    public void channelChanged( ChannelEvent event )
    {
		if( event.getEvent() == Event.CHANGE_SELECTED && 
			event.getChannel().isSelected() )
		{
			if( mDisplayedChannel == null || 
				( mDisplayedChannel != null && 
				  mDisplayedChannel != event.getChannel() ) )
			{
				removeAll();
				
				JScrollPane scroll = 
						new JScrollPane( event.getChannel().getProcessingChain()
								.getChannelState().getMessageActivityTable() );

				add( scroll );
				
				mDisplayedChannel = event.getChannel();
				
				revalidate();
				repaint();
			}
		}
		else if( event.getEvent() == Event.PROCESSING_STOPPED ||
				 event.getEvent() == Event.CHANNEL_DELETED )
		{
			if( mDisplayedChannel != null && 
				mDisplayedChannel == event.getChannel() )
			{
				mDisplayedChannel = null;
				removeAll();
				add( mEmptyScrollPane );

				revalidate();
				repaint();
			}
		}
    }
}
