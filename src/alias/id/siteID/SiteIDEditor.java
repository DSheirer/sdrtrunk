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
package alias.id.siteID;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiteIDEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
	private final static Logger mLog = 
							LoggerFactory.getLogger( SiteIDEditor.class );
    private SiteIDNode mSiteIDNode;
    private JTextField mTextSiteID;

	public SiteIDEditor( SiteIDNode siteIDNode )
	{
		mSiteIDNode = siteIDNode;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][][grow]" ) );

		add( new JLabel( "Site ID" ), "span,align center" );

		add( new JLabel( "Site:" ) );
		
		mTextSiteID = new JTextField( 
				String.valueOf( mSiteIDNode.getSiteID().getSite() ) );
		add( mTextSiteID, "growx,push" );

		JTextArea description = new JTextArea( "Enter a site number.  "
				+ "For P25 systems, use the format RR-SS, where RR = RF "
				+ "Subsystem and SS = Site Number.  For example, RFSS 1 and "
				+ "Site 3 would be: 01-03");
		
		description.setLineWrap( true );
		description.setBackground( getBackground() );
		
		add( description, "growx,span" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( SiteIDEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( SiteIDEditor.this );
		add( btnReset, "growx,push" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			String siteIDString = mTextSiteID.getText();
			
			if( siteIDString != null )
			{
				mSiteIDNode.getSiteID().setSite( siteIDString );

				mSiteIDNode.save();
				
				mSiteIDNode.show();
			}
			else
			{
				JOptionPane.showMessageDialog( SiteIDEditor.this, 
						"Please enter a site ID" );
			}
		}
		else if( command.contentEquals( "Reset" ) )
		{
			mTextSiteID.setText( mSiteIDNode.getSiteID().getSite() );
		}
		
		mSiteIDNode.refresh();
    }
}
