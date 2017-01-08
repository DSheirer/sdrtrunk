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
package alias;

import audio.broadcast.BroadcastModel;
import gui.editor.Editor;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import icon.IconManager;
import net.miginfocom.swing.MigLayout;
import sample.Listener;
import settings.SettingsManager;
import alias.action.AliasActionEditor;
import alias.id.AliasIdentifierEditor;

import com.jidesoft.swing.JideTabbedPane;

public class AliasEditor extends Editor<Alias> 
			implements ActionListener, Listener<AliasEvent>
{
    private static final long serialVersionUID = 1L;

	private static final String DEFAULT_NAME = "select an alias";

	private AliasModel mAliasModel;
	
	private JLabel mAliasLabel;
    private AliasNameEditor mAliasNameEditor;
    private AliasIdentifierEditor mAliasIdentifierEditor;
    private AliasActionEditor mAliasActionEditor;
    
    public AliasEditor(AliasModel aliasModel, BroadcastModel broadcastModel, IconManager iconManager )
	{
    	mAliasModel = aliasModel;
    	mAliasNameEditor = new AliasNameEditor( mAliasModel, iconManager );
    	mAliasIdentifierEditor = new AliasIdentifierEditor( aliasModel, broadcastModel );
    	mAliasActionEditor = new AliasActionEditor( aliasModel );
    	
    	mAliasModel.addListener( this );
		
		init();
	}

    
    @Override
	public void setItem( Alias alias )
	{
		super.setItem( alias );
		
		if( hasItem() )
		{
    		mAliasLabel.setText( getItem().getName() );
		}
		else
		{
    		mAliasLabel.setText( DEFAULT_NAME );
		}
		
		mAliasNameEditor.setItem( alias );
		mAliasIdentifierEditor.setItem( alias );
		mAliasActionEditor.setItem( alias );
	}

	private void init()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[grow,fill][grow,fill]", 
				"[][][][][][grow,fill]" ) );
		
		add( new JLabel( "Alias:" ), "align right" );
		mAliasLabel = new JLabel( DEFAULT_NAME );
		add( mAliasLabel, "align left" );
		
		add( new JSeparator( JSeparator.HORIZONTAL ), "span,growx,push" );
		
		JideTabbedPane tabs = new JideTabbedPane();
		tabs.setFont( this.getFont() );
    	tabs.setForeground( Color.BLACK );

		tabs.addTab( "Alias", mAliasNameEditor );
		tabs.addTab( "Audio / Identifier", mAliasIdentifierEditor );
		tabs.addTab( "Action", mAliasActionEditor );
		add( tabs, "span,grow" );
		
		//Playlist management buttons
		JButton btnSave = new JButton( "Save" );
		btnSave.setToolTipText( "Save alias changes to the playlist" );
		btnSave.addActionListener( AliasEditor.this );
		add( btnSave );

		JButton btnReset = new JButton( "Reset" );
		btnReset.setToolTipText( "Reset alias changes since last save" );
		btnReset.addActionListener( AliasEditor.this );
		add( btnReset, "wrap" );
	}

	@Override
	public void receive( AliasEvent event )
	{
		//If this is the currently displayed alias, reload it
		if( hasItem() && getItem() == event.getAlias() )
		{
			switch( event.getEvent() )
			{
				case CHANGE:
					//Alias changed - reset the editor with changed alias
					setItem( event.getAlias() );
					break;
				case DELETE:
					setItem( null );
					break;
				default:
					break;
			}
		}
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		if( hasItem() )
		{
			String command = e.getActionCommand();
			
			if( command.contentEquals( "Save" ) )
			{
				save();
			}
			else if( command.contentEquals( "Reset" ) )
			{
				reset();
			}
		}
    }

	@Override
	public void save()
	{
		mAliasNameEditor.save();
		mAliasIdentifierEditor.save();
		mAliasActionEditor.save();
	}
}
