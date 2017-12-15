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
package controller.channel.map;

import java.awt.EventQueue;

import javax.swing.JFrame;

import net.miginfocom.swing.MigLayout;

public class ChannelMapManagerFrame extends JFrame
{
	private static final long serialVersionUID = 1L;

	public ChannelMapManagerFrame( ChannelMapModel channelMapModel )
	{
		init( new ChannelMapManager( channelMapModel ) );
	}
	
	private void init( ChannelMapManager channelMapManager )
	{
		setTitle( "Channel Map Manager" );
		setSize( 800, 400 );
    	setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
    	setLayout( new MigLayout( "", "[grow,fill]", "[grow,fill]" ) );
    	add( channelMapManager );
		setLocationRelativeTo( null );
	}
	
	public static void main( String[] args )
	{
		ChannelMapModel channelMapModel = new ChannelMapModel();
		
		final ChannelMapManagerFrame frame = 
				new ChannelMapManagerFrame( channelMapModel );
		
		EventQueue.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				frame.setVisible( true );
			}
		} );
	}
}
