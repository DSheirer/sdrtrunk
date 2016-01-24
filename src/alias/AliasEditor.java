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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;
import sample.Listener;
import settings.SettingsManager;
import alias.AliasEvent.Event;

import com.jidesoft.swing.JideTabbedPane;

public class AliasEditor extends JPanel implements ActionListener, Listener<AliasEvent>
{
    private static final long serialVersionUID = 1L;
    
	private static final String DEFAULT_NAME = "select an alias";

	private AliasModel mAliasModel;
	
	private JLabel mAliasLabel;
    private AliasNameEditor mAliasNameEditor;
    
    private Alias mAlias;

    
    public AliasEditor( AliasModel aliasModel, SettingsManager settingsManager )
	{
    	mAliasModel = aliasModel;
    	mAliasNameEditor = new AliasNameEditor( mAliasModel, settingsManager );
		
		initGUI();
	}
    
    public void setAlias( Alias alias )
    {
    	mAlias = alias;

    	if( mAlias != null )
    	{
    		mAliasLabel.setText( alias.getName() );
    	}
    	else
    	{
    		mAliasLabel.setText( DEFAULT_NAME );
    	}
    	
		mAliasNameEditor.setAlias( alias );
    	
    }
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][grow,fill]", "[][][][][grow,fill]" ) );
		
		add( new JLabel( "Alias:" ) );
		mAliasLabel = new JLabel( DEFAULT_NAME );
		add( mAliasLabel );
		
		add( new JSeparator( JSeparator.HORIZONTAL ), "span,growx,push" );
		
		JideTabbedPane tabs = new JideTabbedPane();
		tabs.setFont( this.getFont() );
    	tabs.setForeground( Color.BLACK );

		tabs.addTab( "Alias", mAliasNameEditor );
		add( tabs, "span,grow" );		
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( AliasEditor.this );
		add( btnSave );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( AliasEditor.this );
		add( btnReset, "wrap" );
	}

	@Override
	public void receive( AliasEvent event )
	{
		//If this is the currently displayed alias, reload it
		if( mAlias != null && mAlias == event.getAlias() )
		{
			setAlias( event.getAlias() );
		}
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			mAliasNameEditor.save();
			
			//Broadcast an alias change event to save the updates
			mAliasModel.broadcast( new AliasEvent( mAlias, Event.CHANGE ) ); 
		}
		else if( command.contentEquals( "Reset" ) )
		{
			setAlias( mAlias );
		}
    }
}
