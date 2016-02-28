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
package alias.id.priority;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import alias.AliasID;
import alias.ComponentEditor;

public class PriorityEditor extends ComponentEditor<AliasID>
{
    private static final long serialVersionUID = 1L;

	public static final String DO_NOT_MONITOR = "Do Not Monitor";

	private static final String HELP_TEXT = "<html>"
			+ "<h3>Call Priority Example</h3>"
			+ "Priority determines which calls have priority for playback<br>"
			+ "over your computer speakers, or designates an alias for<br>"
			+ "no-monitoring if you don't want to hear calls from an alias.<br><br>"
			+ "Lower values indicate higher priority levels."
    		+ "</html>";

    private JCheckBox mDoNotMonitorCheckBox = new JCheckBox( DO_NOT_MONITOR );
    private JSlider mPrioritySlider;
    private JLabel mPrioritySliderLabel;

	public PriorityEditor( AliasID aliasID )
	{
		super( aliasID );
		
		initGUI();
		
		setComponent( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][]" ) );

		mDoNotMonitorCheckBox.setToolTipText( HELP_TEXT );
		add( mDoNotMonitorCheckBox, "span,align center" );

		mPrioritySlider = new JSlider( JSlider.HORIZONTAL,
										Priority.MIN_PRIORITY,
										Priority.MAX_PRIORITY,
										Priority.MIN_PRIORITY );
		
		mPrioritySlider.setMajorTickSpacing( 20 );
		mPrioritySlider.setMinorTickSpacing( 5 );
		mPrioritySlider.setPaintTicks( true );
		mPrioritySlider.setLabelTable( mPrioritySlider.createStandardLabels( 20, 20 ) );
		mPrioritySlider.setPaintLabels( true );
		mPrioritySlider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				mPrioritySliderLabel.setText( "Priority: " + mPrioritySlider.getValue() );
			}
		} );
		mPrioritySlider.setToolTipText( HELP_TEXT );
		
		mPrioritySliderLabel = new JLabel( "Priority: " + mPrioritySlider.getValue() + " " );
		add( mPrioritySliderLabel );
		add( mPrioritySlider, "wrap,grow" );
		
		JLabel example = new JLabel( "Example ..." );
		example.setForeground( Color.BLUE.brighter() );
		example.setCursor( new Cursor( Cursor.HAND_CURSOR ) );
		example.addMouseListener( new MouseAdapter() 
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				JOptionPane.showMessageDialog( PriorityEditor.this, 
					HELP_TEXT, "Example", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		add( example );
	}
	
	public Priority getPriority()
	{
		if( getComponent() instanceof Priority )
		{
			return (Priority)getComponent();
		}
		
		return null;
	}

	@Override
	public void setComponent( AliasID aliasID )
	{
		mComponent = aliasID;

		Priority priority = getPriority();
		
		if( priority != null )
		{
			mPrioritySlider.setValue( priority.getPriority() );
			mDoNotMonitorCheckBox.setSelected( priority.isDoNotMonitor() );
		}
		
		setModified( false );
		
		repaint();
	}

	@Override
	public void save()
	{
		Priority priority = getPriority();
		
		if( priority != null )
		{
			if( mDoNotMonitorCheckBox.isSelected() )
			{
				priority.setPriority( Priority.DO_NOT_MONITOR );
			}
			else
			{
				priority.setPriority( mPrioritySlider.getValue() );
			}
		}
		
		setModified( false );
	}
}
