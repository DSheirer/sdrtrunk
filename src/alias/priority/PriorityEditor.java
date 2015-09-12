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
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controller.ConfigurableNode;

public class PriorityEditor extends JPanel implements ActionListener
{
	private final static Logger mLog = LoggerFactory.getLogger( PriorityEditor.class );

	public static final String DO_NOT_MONITOR = "Do Not Monitor";
	public static final String SAVE = "Save";
	public static final String RESET = "Reset";
	
    private static final long serialVersionUID = 1L;
    private PriorityNode mPriorityNode;
    private JCheckBox mDoNotMonitorCheckBox = new JCheckBox( DO_NOT_MONITOR );
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

		boolean doNotMonitor = mPriorityNode.getPriority().isDoNotMonitor();

		mDoNotMonitorCheckBox.addActionListener( this );
		
		if( doNotMonitor )
		{
			mDoNotMonitorCheckBox.setSelected( true );
		}
		
		add( mDoNotMonitorCheckBox, "span,align center" );

		int priority = mPriorityNode.getPriority().getPriority();

		/* Adjust the displayable priority value to within the min/max bounds 
		 * so we don't get an error in the jslider control.  If the actual 
		 * priority is -1 (ie do not follow), the controls will be updated
		 * to reflect that state */
		if( priority < Priority.MIN_PRIORITY )
		{
			priority = Priority.DEFAULT_PRIORITY;
		}
		else if( priority > Priority.MAX_PRIORITY )
		{
			priority = Priority.DEFAULT_PRIORITY;
		}
		
		mPrioritySlider = new JSlider( JSlider.HORIZONTAL,
					Priority.MIN_PRIORITY,
					Priority.MAX_PRIORITY,
					priority );
		
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
		
		if( doNotMonitor )
		{
			mPrioritySlider.setEnabled( false );
			mPrioritySliderLabel.setEnabled( false );
		}
		
		add( mPrioritySliderLabel );
		add( mPrioritySlider, "wrap,grow" );
		
		JButton btnSave = new JButton( SAVE );
		btnSave.addActionListener( PriorityEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( RESET );
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
		
		if( command.contentEquals( SAVE ) )
		{
			if( mDoNotMonitorCheckBox.isSelected() )
			{
				mPriorityNode.getPriority().setPriority( Priority.DO_NOT_MONITOR );
			}
			else
			{
				mPriorityNode.getPriority().setPriority( mPrioritySlider.getValue() );
			}

			((ConfigurableNode)mPriorityNode.getParent()).sort();

			mPriorityNode.save();
			
			mPriorityNode.show();
		}
		else if( command.contentEquals( RESET ) )
		{
			mPrioritySlider.setValue( mPriorityNode.getPriority().getPriority() );
		}
		else if( command.contentEquals( DO_NOT_MONITOR ) )
		{
			if( mDoNotMonitorCheckBox.isSelected() )
			{
				mPriorityNode.getPriority().setPriority( Priority.DO_NOT_MONITOR );
				mPrioritySlider.setEnabled( false );
				mPrioritySliderLabel.setEnabled( false );
			}
			else
			{
				mPriorityNode.getPriority().setPriority( Priority.DEFAULT_PRIORITY );
				mPrioritySlider.setEnabled( true );
				mPrioritySliderLabel.setEnabled( true );
			}
			
			mPriorityNode.save();
			mPriorityNode.show();
		}
		
		mPriorityNode.refresh();
    }
}
