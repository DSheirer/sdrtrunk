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
package source.recording;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import settings.SettingsManager;
import source.SourceManager;
import controller.BaseNode;

public class RecordingGroupNode extends BaseNode
{
    private static final long serialVersionUID = 1L;
    
    private SettingsManager mSettingsManager;
    
    public RecordingGroupNode( SourceManager sourceManager, SettingsManager settingsManager )
	{
        super( sourceManager );
        
        mSettingsManager = settingsManager;
	}
    
    private SourceManager getSourceManager()
    {
    	return (SourceManager)getUserObject();
    }
    
    public String toString()
    {
    	return "Recordings [" + this.getChildCount() + "]";
    }

	public JPopupMenu getContextMenu()
	{
		JPopupMenu menu = new JPopupMenu();
		
		JMenuItem addRecordingItem = new JMenuItem( "Add Recording" );
		
		addRecordingItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				RecordingConfiguration config = new RecordingConfiguration();
				
				mSettingsManager.addRecordingConfiguration( config );
				
				Recording recording = getSourceManager()
						.getRecordingSourceManager().addRecording( config );
				
				RecordingNode node = new RecordingNode( getSourceManager(), recording );
				
				getModel().addNode( node, RecordingGroupNode.this, 0 );
				
				sort();
				
				node.show();
            }
		} );
		
		menu.add( addRecordingItem );
		
		return menu;
	}
	
	public void loadRecordings()
	{
		List<Recording> recordings = getSourceManager()
				.getRecordingSourceManager().getRecordings();
		
		for( Recording recording: recordings )
		{
			RecordingNode node = new RecordingNode( getSourceManager(), recording );
			
			getModel().addNode( node, this, 0 );
		}
		
		sort();
	}
}
