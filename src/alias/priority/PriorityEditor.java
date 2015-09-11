/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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
package alias.priority;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import module.decode.config.DecodeConfiguration;
import net.miginfocom.swing.MigLayout;
import controller.ConfigurableNode;

public class PriorityEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private PriorityNode mPriorityNode;
    private JSlider mPrioritySlider;
    private JLabel mPrioritySliderLabel;

	public PriorityEditor( PriorityNode priorityNode )
	{
		mPriorityNode = priorityNode;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][grow]" ) );

		add( new JLabel( "Priority" ), "span,align center" );

		mPrioritySlider = new JSlider( JSlider.HORIZONTAL,
										Priority.MIN_PRIORITY,
										Priority.MAX_PRIORITY,
										Priority.DEFAULT_PRIORITY );

		mPrioritySlider.setMajorTickSpacing( 20 );
		mPrioritySlider.setMinorTickSpacing( 5 );
		mPrioritySlider.setPaintTicks( true );
		
		mPrioritySlider.setLabelTable( mPrioritySlider.createStandardLabels( 20, 20 ) );
		mPrioritySlider.setPaintLabels( true );
		
		mPrioritySliderLabel = new JLabel( "Priority: " + mPrioritySlider.getValue() + " " );
		
		mPrioritySlider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				mPrioritySliderLabel.setText( "Priority: " + mPrioritySlider.getValue() );
			}
		} );
		
		add( mPrioritySliderLabel );
		add( mPrioritySlider, "wrap,grow" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( PriorityEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( PriorityEditor.this );
		add( btnReset, "growx,push" );
		
		StringBuilder sb = new StringBuilder();
		sb.append( "Select a value for audio priority within the range " );
		sb.append( Priority.MIN_PRIORITY );
		sb.append( " and " );
		sb.append( Priority.MAX_PRIORITY );
		sb.append( ".  Lower values indicate higher priority levels." );

		JTextArea helpText = new JTextArea( sb.toString() );
		helpText.setLineWrap( true );
		add( helpText, "span,grow,push" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			mPriorityNode.getPriority().setPriority( mPrioritySlider.getValue() );

			((ConfigurableNode)mPriorityNode.getParent()).sort();

			mPriorityNode.save();
			
			mPriorityNode.show();
		}
		else if( command.contentEquals( "Reset" ) )
		{
			mPrioritySlider.setValue( mPriorityNode.getPriority().getPriority() );
		}
		
		mPriorityNode.refresh();
    }
}
