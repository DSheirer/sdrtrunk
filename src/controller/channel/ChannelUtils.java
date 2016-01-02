package controller.channel;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

import module.decode.event.ActivitySummaryFrame;
import module.decode.state.DecoderState;
import playlist.PlaylistManager;
import controller.channel.ChannelEvent.Event;

public class ChannelUtils
{
	private static final boolean ENABLED = true;
	private static final boolean DISABLED = false;
	private static final boolean BROADCAST_CHANGE = true;

	/**
	 * Creates a context menu for the channel argument
	 */
	public static JMenu getContextMenu( final PlaylistManager playlistManager, 
										final Channel channel,
										final Component anchor )
	{
		if( channel != null )
		{
			JMenu menu = new JMenu( "Channel: " + channel.getName() );
			
			if( channel.getEnabled() )
			{
				JMenuItem disable = new JMenuItem( "Disable" );
				disable.addActionListener( new ActionListener() 
				{
					@Override
	                public void actionPerformed( ActionEvent e )
	                {
						channel.setEnabled( DISABLED );
						
						if( playlistManager != null )
						{
							playlistManager.save();					
						}
	                }
				} );
				
				menu.add( disable );
				
				menu.add( new JSeparator() );

				JMenuItem actySummaryItem = 
						new JMenuItem( "Activity Summary" );

				actySummaryItem.addActionListener( new ActionListener() 
				{
					@Override
		            public void actionPerformed( ActionEvent e )
		            {
						StringBuilder sb = new StringBuilder();
						
						for( DecoderState decoderState: channel
								.getProcessingChain().getDecoderStates() )
						{
							sb.append( decoderState.getActivitySummary() );
						}
						
						new ActivitySummaryFrame( sb.toString(), anchor );
		            }
				} );
					
				menu.add( actySummaryItem );
			}
			else
			{
				JMenuItem enable = new JMenuItem( "Enable" );
				enable.addActionListener( new ActionListener() 
				{
					@Override
	                public void actionPerformed( ActionEvent e )
	                {
						channel.setEnabled( ENABLED );
						
						if( playlistManager != null )
						{
							playlistManager.save();
						}
	    			}	
				} );
				
				menu.add( enable );
			}
			
			menu.add( new JSeparator() );
			
			JMenuItem deleteItem = new JMenuItem( "Delete" );
			deleteItem.addActionListener( new ActionListener() 
			{
				@Override
	            public void actionPerformed( ActionEvent e )
	            {
					int response = JOptionPane.showConfirmDialog( anchor, 
						"Do you want to delete channel " + channel.getName() + "?",
						"Are you sure?", JOptionPane.YES_NO_CANCEL_OPTION );
					
					if( response == JOptionPane.YES_OPTION )
					{
						/* Disable the channel */
						channel.setEnabled( DISABLED );	

		                /* Broadcast channel deleted event */
//						channel.fireChannelEvent( Event.NOTIFICATION_DELETE );					
		                
						if( playlistManager != null )
						{
							playlistManager.save();           
						}
					}
				}
			} );
			
			menu.add( deleteItem );
			
			return menu;
			
		}
		
		return null;
	}
	
}
