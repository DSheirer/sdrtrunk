/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
package alias.id.broadcast;

import alias.id.AliasID;
import audio.broadcast.BroadcastModel;
import gui.editor.DocumentListenerEditor;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class BroadcastChannelEditor extends DocumentListenerEditor<AliasID>
{
    private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "<html>"
    		+ "<h3>Audio Broadcast Channel</h3>"
    		+ "Name of a audio streaming broadcast configuration<br>"
			+ "to be used for streaming audio to an external<br>"
			+ "streaming server";

	private JComboBox<String> mBroadcastConfigurations;

	public BroadcastChannelEditor(AliasID aliasID, BroadcastModel broadcastModel)
	{
        List<String> channelNames = broadcastModel.getBroadcastConfigurationNames();

        mBroadcastConfigurations = new JComboBox<String>(channelNames.toArray(new String[channelNames.size()]));

        setItem( aliasID );

		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 1", "[]", "[][]" ) );

		add( new JLabel( "Broadcast Channel" ), "grow" );

        add(mBroadcastConfigurations, "grow");

        BroadcastChannel channel = (BroadcastChannel)getItem();

        if(channel.isValid())
        {
            mBroadcastConfigurations.setSelectedItem(channel.getChannelName());
        }

		JLabel help = new JLabel( "Help ..." );
		help.setForeground( Color.BLUE.brighter() );
		help.setCursor( new Cursor( Cursor.HAND_CURSOR ) );
		help.addMouseListener( new MouseAdapter() 
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				JOptionPane.showMessageDialog( BroadcastChannelEditor.this,
					HELP_TEXT, "Help", JOptionPane.INFORMATION_MESSAGE );
			}
		} );

		add( help, "align left" );
	}
	
	@Override
	public void save()
	{
        ((BroadcastChannel)getItem()).setChannelName((String)mBroadcastConfigurations.getSelectedItem());
	}
}
