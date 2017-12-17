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
package io.github.dsheirer.alias.id.priority;

import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.gui.editor.DocumentListenerEditor;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PriorityEditor extends DocumentListenerEditor<AliasID>
{
    private static final long serialVersionUID = 1L;

	public static final String DO_NOT_MONITOR = "Do Not Monitor";

	private static final String HELP_TEXT = "<html>"
			+ "<h3>Call Audio Priority</h3>"
			+ "Priority determines which calls have priority for playback<br>"
			+ "over your computer speakers, or designates an alias for<br>"
			+ "no-monitoring if you don't want to hear calls from an alias.<br><br>"
			+ "Lower values indicate higher priority levels.<br><br>"
			+ "<b>Do Not Monitor: </b> slide priority all the way to the right"
    		+ "</html>";

    private JSlider mPrioritySlider;
    private JLabel mPrioritySliderLabel;

	public PriorityEditor( AliasID aliasID )
	{
		initGUI();
		
		setItem( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][]" ) );

		mPrioritySlider = new JSlider( JSlider.HORIZONTAL,
										Priority.MIN_PRIORITY,
										Priority.MAX_PRIORITY + 1,
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
				int priority = mPrioritySlider.getValue();
				
				if( priority == Priority.MAX_PRIORITY + 1 )
				{
					mPrioritySliderLabel.setText( "Priority: Do Not Monitor" );
				}
				else
				{
					mPrioritySliderLabel.setText( "Priority: " + priority );
				}
				
				setModified( true );
			}
		} );
		mPrioritySlider.setToolTipText( HELP_TEXT );
		
		mPrioritySliderLabel = new JLabel( "Priority: " + mPrioritySlider.getValue() + " " );
		add( mPrioritySliderLabel, "span,align center" );
		add( mPrioritySlider, "span,grow" );
		
		JLabel help = new JLabel( "Help ..." );
		help.setForeground( Color.BLUE.brighter() );
		help.setCursor( new Cursor( Cursor.HAND_CURSOR ) );
		help.addMouseListener( new MouseAdapter() 
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				JOptionPane.showMessageDialog( PriorityEditor.this, 
					HELP_TEXT, "Help", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		add( help, "align left" );
	}
	
	public Priority getPriority()
	{
		if( getItem() instanceof Priority )
		{
			return (Priority)getItem();
		}
		
		return null;
	}

	@Override
	public void setItem( AliasID aliasID )
	{
		super.setItem( aliasID );

		Priority priority = getPriority();
		
		if( priority != null )
		{
			int value = priority.getPriority();
			
			if( value == Priority.DO_NOT_MONITOR )
			{
				value = Priority.MAX_PRIORITY + 1;
			}
			
			mPrioritySlider.setValue( value );
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
			int value = mPrioritySlider.getValue();

			if( value == Priority.MAX_PRIORITY + 1 )
			{
				value = Priority.DO_NOT_MONITOR;
			}

			priority.setPriority( value );
		}
		
		setModified( false );
	}
}
