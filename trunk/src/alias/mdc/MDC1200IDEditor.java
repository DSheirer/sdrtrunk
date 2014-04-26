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
package alias.mdc;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import controller.ConfigurableNode;

public class MDC1200IDEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private MDC1200IDNode mMDC1200IDNode;
    
    private JTextField mTextIdent;

	public MDC1200IDEditor( MDC1200IDNode fsNode )
	{
		mMDC1200IDNode = fsNode;
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][grow]" ) );

		add( new JLabel( "MDC-1200 " ), "span,align center" );

		add( new JLabel( "Unit ID:" ) );
		mTextIdent = new JTextField( mMDC1200IDNode.getMDC1200ID().getIdent() );
		add( mTextIdent, "growx,push" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( MDC1200IDEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( MDC1200IDEditor.this );
		add( btnReset, "growx,push" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			String ident = mTextIdent.getText();
			
			if( ident != null )
			{
				mMDC1200IDNode.getMDC1200ID().setIdent( ident );

				((ConfigurableNode)mMDC1200IDNode.getParent()).sort();
				
				mMDC1200IDNode.save();
				
				mMDC1200IDNode.show();
			}
			else
			{
				JOptionPane.showMessageDialog( MDC1200IDEditor.this, "Please enter a unit ID" );
			}
		}
		else if( command.contentEquals( "Reset" ) )
		{
			mTextIdent.setText( mMDC1200IDNode.getMDC1200ID().getIdent() );
		}
		
		mMDC1200IDNode.refresh();
    }
}
