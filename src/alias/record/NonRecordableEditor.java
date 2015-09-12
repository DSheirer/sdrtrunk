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
package alias.record;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

public class NonRecordableEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    
    private NonRecordableNode mNode;

	public NonRecordableEditor( NonRecordableNode node )
	{
		mNode = node;
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill", "[left]", "[grow]" ) );

		JTextArea description = new JTextArea( "Non-Recordable.  Any audio where"
			+ " this alias is a participant in the call will not be recorded." );
		
		description.setLineWrap( true );
		description.setBackground( getBackground() );
		
		add( description, "growx,span" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( NonRecordableEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( NonRecordableEditor.this );
		add( btnReset, "growx,push" );
	}
	
	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
				mNode.save();
				mNode.show();
		}
		
		mNode.refresh();
    }
}
