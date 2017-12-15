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
package ua.in.smartjava.alias.id.siteID;

import ua.in.smartjava.gui.editor.DocumentListenerEditor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import ua.in.smartjava.alias.id.AliasID;

public class SiteIDEditor extends DocumentListenerEditor<AliasID>
{
    private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "<html>"
    		+ "<h3>Site Identifier</h3>"
    		+ "<b>LTR-Net:</b> decimal (0-9) (e.g. <u>019</u>)<br>"
    		+ "<b>MPT-1327:</b> 5 digits (0-9) (e.g. <u>23619</u>)<br>"
    		+ "<b>Passport:</b> 3 digits (0-9) (e.g. <u>019</u>)<br>"
    		+ "<b>P25:</b> hex (0-9, A-F) format RR-SS where RR = RF Subsystem<br>"
    		+ "and SS = Site Number (e.g. RFSS 1 Site 1F: <u>01-1F</u>)"
    		+ "</html>";

    private JTextField mTextField;

	public SiteIDEditor( AliasID aliasID )
	{
		initGUI();
		
		setItem( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][]" ) );

		add( new JLabel( "Site ID:" ) );
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
				JOptionPane.showMessageDialog( SiteIDEditor.this, 
					HELP_TEXT, "Help", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		
		add( help, "align left" );
	}
	
	public SiteID getSiteID()
	{
		if( getItem() instanceof SiteID )
		{
			return (SiteID)getItem();
		}
		
		return null;
	}

	@Override
	public void setItem( AliasID aliasID )
	{
		super.setItem( aliasID );
		
		SiteID siteID = getSiteID();
		
		if( siteID != null )
		{
			mTextField.setText( siteID.getSite() );
		}
		
		setModified( false );
		
		repaint();
	}

	@Override
	public void save()
	{
		SiteID siteID = getSiteID();
		
		if( siteID != null )
		{
			siteID.setSite( mTextField.getText() );
		}
		
		setModified( false );
	}
}
