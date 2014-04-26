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
package alias.siteID;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import log.Log;
import net.miginfocom.swing.MigLayout;
import controller.ConfigurableNode;

public class SiteIDEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private SiteIDNode mSiteIDNode;
    
    private JLabel mLabelName;
    private JTextField mTextSiteID;

	public SiteIDEditor( SiteIDNode siteIDNode )
	{
		mSiteIDNode = siteIDNode;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][grow]" ) );

		add( new JLabel( "Site ID" ), "span,align center" );

		add( new JLabel( "Site:" ) );
		
		mTextSiteID = new JTextField( 
				String.valueOf( mSiteIDNode.getSiteID().getSite() ) );
		add( mTextSiteID, "growx,push" );
		
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
				try
				{
					int id = Integer.parseInt( siteIDString );
					
					mSiteIDNode.getSiteID().setSite( id );
					
					((ConfigurableNode)mSiteIDNode.getParent()).sort();

					mSiteIDNode.save();
					
					mSiteIDNode.show();
				}
				catch( Exception ex )
				{
					Log.error( "SiteIDEditor - error parsing int site " +
							"id from [" + siteIDString + "]" );
				}
			}
			else
			{
				JOptionPane.showMessageDialog( SiteIDEditor.this, 
						"Please enter a site ID" );
			}
		}
		else if( command.contentEquals( "Reset" ) )
		{
			mTextSiteID.setText( 
					String.valueOf( mSiteIDNode.getSiteID().getSite() ) );
		}
		
		mSiteIDNode.refresh();
    }
}
