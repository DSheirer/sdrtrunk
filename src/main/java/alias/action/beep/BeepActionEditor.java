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
package alias.action.beep;

import gui.editor.DocumentListenerEditor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import alias.action.AliasAction;
import alias.action.RecurringAction.Interval;

public class BeepActionEditor extends DocumentListenerEditor<AliasAction>
{
    private static final long serialVersionUID = 1L;
    
	private static final String HELP_TEXT = 
		"<html><h3>Beep Action</h3>"
		+ "This action will sound a beep according to the interval<br>"
		+ "that you select:<br><br>"
		+ "<b>Once:</b> Beep the first time the alias is active and<br>"
		+ "never again<br><br>"
		+ "<b>Once, Reset After Delay:</b> Beep once and suppress<br>"
		+ "subsequent beeps for the specified period in seconds.<br>"
		+ "After the reset period, beep again when the alias is active.<br><br>"
		+ "<b>Until Dismissed:</b> Beep every period seconds until you<br>"
		+ "click OK on the dialog that appears. Alerting is suppressed<br>"
		+ "for 15 seconds after you click OK.<br>"
		+ "</html>";

    private JComboBox<Interval> mComboInterval;
    private JSlider mPeriodSlider;
    private JLabel mPeriodSliderLabel;

	public BeepActionEditor( AliasAction aliasAction )
	{
		init();

		setItem( aliasAction );
	}
	
	public BeepAction getBeepAction()
	{
		if( getItem() instanceof BeepAction )
		{
			return (BeepAction)getItem();
		}
		
		return null;
	}
	
	private void init()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][]" ) );

		add( new JLabel( "Beep Interval:" ) );
		
		mComboInterval = new JComboBox<Interval>( Interval.values() );
		mComboInterval.setToolTipText( HELP_TEXT );
		mComboInterval.addActionListener( new ActionListener()
		{
			@Override
           public void actionPerformed( ActionEvent e )
           {
				Interval selected = mComboInterval
						.getItemAt( mComboInterval.getSelectedIndex() );
				
				if( selected != null )
				{
					/* Enable/disable period spinner based on selection */
					boolean enabled = selected != Interval.ONCE;
					
					mPeriodSlider.setEnabled( enabled );
					mPeriodSliderLabel.setEnabled( enabled );
				}
				
				setModified( true );
           }
		});
		add( mComboInterval, "wrap" );

		final SpinnerModel model = new SpinnerNumberModel( 1, 1, 30, 1 );
		model.addChangeListener( new ChangeListener() 
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				setModified( true );
			}
		} );
		
		mPeriodSlider = new JSlider( JSlider.HORIZONTAL, 1, 60, 1 );
		mPeriodSlider.setMajorTickSpacing( 10 );
		mPeriodSlider.setMinorTickSpacing( 2 );
		mPeriodSlider.setPaintTicks( true );
		mPeriodSlider.setLabelTable( mPeriodSlider.createStandardLabels( 10, 10 ) );
		mPeriodSlider.setPaintLabels( true );
		mPeriodSlider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				mPeriodSliderLabel.setText( "Period: " + mPeriodSlider.getValue() );
				setModified( true );
			}
		} );
		mPeriodSlider.setToolTipText( HELP_TEXT );
		
		mPeriodSliderLabel = new JLabel( "Period: " + mPeriodSlider.getValue() + " " );
		add( mPeriodSliderLabel );
		add( mPeriodSlider, "wrap,grow" );
		
		JLabel help = new JLabel( "Help ..." );
		help.setForeground( Color.BLUE.brighter() );
		help.setCursor( new Cursor( Cursor.HAND_CURSOR ) );
		help.addMouseListener( new MouseAdapter() 
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				JOptionPane.showMessageDialog( BeepActionEditor.this, 
					HELP_TEXT, "Help", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		add( help, "align left" );
	}
	
	@Override
	public void setItem( AliasAction item )
	{
		super.setItem( item );
		
		if( hasItem() )
		{
			BeepAction beep = getBeepAction();

			Interval interval = beep.getInterval();
			
			mComboInterval.setSelectedItem( interval );

			boolean enabled = interval != Interval.ONCE;
			
			mPeriodSliderLabel.setEnabled( enabled );
			mPeriodSlider.setEnabled( enabled );
			mPeriodSlider.setValue( beep.getPeriod() );
		}
		
		setModified( false );
	}

	@Override
	public void save()
	{
		if( hasItem() && isModified() )
		{
			BeepAction beep = getBeepAction();
			
			beep.setInterval( (Interval)mComboInterval.getSelectedItem() );
			beep.setPeriod( mPeriodSlider.getValue() );
		}
		
		setModified( false );
	}
}
