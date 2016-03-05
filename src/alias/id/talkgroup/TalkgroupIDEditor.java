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
package alias.id.talkgroup;

import gui.editor.DocumentListenerEditor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import alias.id.AliasID;

public class TalkgroupIDEditor extends DocumentListenerEditor<AliasID>
{
    private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = 
    		"<html><h3>Talkgroup Identifier</h3>"
    		+ "<b>P25:</b> 4 or 6 hex characters (e.g. <u>AB12</u> or <u>ABC123</u>)<br>"
    		+ "<b>LTR:</b> A-HH-TTT where A=Area H=Home T=Talkgroup (<u>0-01-128</u>)<br>"
    		+ "<b>Passport</b>: 5-digit number (<u>12345</u> or <u>00023</u>)<br>"
    		+ "<br>"
            + "<b>Wildcard:</b> use an asterisk (*) in place of each talkgroup digit (e.g. <u>0*1*5</u>)"
    		+ "</html>";

    private JTextField mTextField;

	public TalkgroupIDEditor( AliasID aliasID )
	{
		initGUI();
		
		setItem( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][]" ) );

		add( new JLabel( "Talkgroup:" ) );
		
		mTextField = new JTextField();
		mTextField.getDocument().addDocumentListener( this );
		mTextField.setToolTipText( HELP_TEXT );
		add( mTextField, "growx,push" );

		JLabel help = new JLabel( "Help ..." );
		help.setForeground( Color.BLUE.brighter() );
		help.setCursor( new Cursor( Cursor.HAND_CURSOR ) );
		help.addMouseListener( new MouseAdapter() 
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				JOptionPane.showMessageDialog( TalkgroupIDEditor.this, 
					HELP_TEXT, "Help", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		add( help, "align left" );
	}
	
	public TalkgroupID getTalkgroupID()
	{
		if( getItem() instanceof TalkgroupID )
		{
			return (TalkgroupID)getItem();
		}
		
		return null;
	}

	@Override
	public void setItem( AliasID aliasID )
	{
		super.setItem( aliasID );
		
		TalkgroupID talkgroup = getTalkgroupID();
		
		if( talkgroup != null )
		{
			mTextField.setText( talkgroup.getTalkgroup() );
		}
		
		setModified( false );
		
		repaint();
	}

	@Override
	public void save()
	{
		TalkgroupID talkgroup = getTalkgroupID();
		
		if( talkgroup != null )
		{
			talkgroup.setTalkgroup( mTextField.getText() );
		}
		
		setModified( false );
	}
}
