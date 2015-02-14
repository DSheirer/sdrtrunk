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
package alias.action.beep;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.action.RecurringAction.Interval;

public class BeepActionEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    
	private final static Logger mLog = 
							LoggerFactory.getLogger( BeepActionEditor.class );
	
    private BeepActionNode mBeepActionNode;
    private JTextField mTextSiteID;
    private JComboBox<Interval> mComboInterval;
    private JSpinner mSpinnerPeriod;

	public BeepActionEditor( BeepActionNode beepActionNode )
	{
		mBeepActionNode = beepActionNode;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][][][grow]" ) );

		add( new JLabel( "Action: Beep" ), "span,align center" );
		
		add( new JLabel( "Interval:" ) );
		
		mComboInterval = new JComboBox<Interval>( Interval.values() );

		mComboInterval.setSelectedItem( mBeepActionNode.getBeepAction().getInterval() );

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
					mSpinnerPeriod.setEnabled( selected != Interval.ONCE );
					
					mBeepActionNode.getBeepAction().setInterval( selected );
				}
           }
		});
		
		add( mComboInterval, "wrap" );

		final SpinnerModel model = new SpinnerNumberModel( 
				mBeepActionNode.getBeepAction().getPeriod(), 1, 30, 1 );
		
		model.addChangeListener( new ChangeListener() 
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				mBeepActionNode.getBeepAction().setPeriod( (int)model.getValue() );
			}
		} );
		
		mSpinnerPeriod = new JSpinner( model );

		if( mBeepActionNode.getBeepAction().getInterval() == Interval.ONCE )
		{
			mSpinnerPeriod.setEnabled( false );
		}
		
		add( new JLabel( "Period:" ) );
		add( mSpinnerPeriod, "wrap" );

		StringBuilder sb = new StringBuilder();
		
		sb.append( "This action will sound a beep according to the interval that you select.\n\n" );
		sb.append( "Once - Beep the first time the alias is active and never again.\n\n" );
		sb.append( "Once, Reset After Delay - Beep once and suppress subsequent beeps for the specified period in seconds.  After the reset period, it will beep again when the alias is active.\n\n" );
		sb.append( "Until Dismissed - Beep every period seconds until you click OK on the dialog that appears. Alerting is suppressed for 15 seconds after you click OK." );
		
		JTextArea description = new JTextArea( sb.toString() );
		
		description.setLineWrap( true );
		description.setBackground( getBackground() );
		
		add( description, "growx,span" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( BeepActionEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( BeepActionEditor.this );
		add( btnReset, "growx,push" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			mBeepActionNode.save();
			mBeepActionNode.show();
		}
		
		mBeepActionNode.refresh();
    }
}
