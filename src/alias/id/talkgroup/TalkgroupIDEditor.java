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

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import alias.AliasID;
import alias.ComponentEditor;

public class TalkgroupIDEditor extends ComponentEditor<AliasID>
{
    private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "Enter a formatted talkgroup identifier.\n"
    		+ "P25: 4 or 6 hex characters (e.g. AB12 or ABC123)\n"
    		+ "LTR: A-HH-TTT (A=Area H=Home T=Talkgroup)\n"
    		+ "Passport: xxxxx (up to 5-digit number)\n\n"
            + "Wildcard: use an asterisk (*) in place of each talkgroup digit";

    private JTextField mTextField;

	public TalkgroupIDEditor( AliasID aliasID )
	{
		super( aliasID );
		
		initGUI();
		
		setComponent( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][grow]" ) );

		add( new JLabel( "Talkgroup:" ) );
		mTextField = new JTextField();
		mTextField.getDocument().addDocumentListener( this );
		add( mTextField, "growx,push" );

		JTextArea helpText = new JTextArea( HELP_TEXT );
		helpText.setLineWrap( true );
		helpText.setBackground( getBackground() );
		add( helpText, "span,grow,push" );
	}
	
	public TalkgroupID getTalkgroupID()
	{
		if( getComponent() instanceof TalkgroupID )
		{
			return (TalkgroupID)getComponent();
		}
		
		return null;
	}

	@Override
	public void setComponent( AliasID aliasID )
	{
		mComponent = aliasID;
		
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
