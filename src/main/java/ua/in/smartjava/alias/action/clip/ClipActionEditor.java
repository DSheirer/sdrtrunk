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
package ua.in.smartjava.alias.action.clip;

import ua.in.smartjava.gui.editor.DocumentListenerEditor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import ua.in.smartjava.alias.action.AliasAction;
import ua.in.smartjava.alias.action.RecurringAction.Interval;

public class ClipActionEditor extends DocumentListenerEditor<AliasAction>
{
    private static final long serialVersionUID = 1L;
    
    private static final String PLEASE_SELECT_A_FILE = "Please select a file";
	private static final String HELP_TEXT = 
		"<html><h3>Clip Action</h3>"
		+ "This action will play an ua.in.smartjava.audio clip according to the interval<br>"
		+ "that you select:<br><br>"
		+ "<b>Once:</b> play the first time the ua.in.smartjava.alias is active and<br>"
		+ "never again<br><br>"
		+ "<b>Once, Reset After Delay:</b> play once and suppress<br>"
		+ "subsequent runs for the specified period in seconds.<br>"
		+ "After the reset period, play again when the ua.in.smartjava.alias is active.<br><br>"
		+ "<b>Until Dismissed:</b> play every period seconds until you<br>"
		+ "click OK on the dialog that appears. Alerting is suppressed<br>"
		+ "for 15 seconds after you click OK.<br>"
		+ "</html>";

    private JComboBox<Interval> mComboInterval;
    private JSlider mPeriodSlider;
    private JLabel mPeriodSliderLabel;
    private JTextField mTextFilePath;

	public ClipActionEditor( AliasAction aliasAction )
	{
		init();

		setItem( aliasAction );
	}
	
	public ClipAction getClipAction()
	{
		if( getItem() instanceof ClipAction )
		{
			return (ClipAction)getItem();
		}
		
		return null;
	}
	
	private void init()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][][]" ) );

		add( new JLabel( "Play Interval:" ) );
		
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
					/* Enable/disable period slider based on selection */
					boolean enabled = selected != Interval.ONCE;
					
					mPeriodSlider.setEnabled( enabled );
					mPeriodSliderLabel.setEnabled( enabled );
				}
				
				setModified( true );
           }
		});
		add( mComboInterval );

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
		
		mTextFilePath = new JTextField( PLEASE_SELECT_A_FILE );
		add( mTextFilePath, "growx,span" );
		
		JButton fileButton = new JButton( "File" );
		
		fileButton.addActionListener( new ActionListener() 
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				final JFileChooser chooser = new JFileChooser();

				int value = chooser.showOpenDialog( ClipActionEditor.this );

				if( value == JFileChooser.APPROVE_OPTION )
					{
						File file = chooser.getSelectedFile();
						
						mTextFilePath.setText( file.getAbsolutePath() );
						
						setModified( true );
					}
				}
		} );
		
		add( fileButton, "grow" );
		
		JButton testButton = new JButton( "Test" );
		
		testButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				if( hasItem() )
				{
					String path = mTextFilePath.getText();
					
					if( path == null || path.trim().isEmpty() )
					{
						JOptionPane.showMessageDialog( ClipActionEditor.this, 
								"Please select an ua.in.smartjava.audio file", PLEASE_SELECT_A_FILE,
								JOptionPane.ERROR_MESSAGE );
					}
					else
					{
						if( isModified() )
						{
							int option = JOptionPane.showConfirmDialog( 
								ClipActionEditor.this, 
								"Settings have changed.  Do you want to save these changes?", 
								"Save Changes?",
								JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE );
							
							if( option == JOptionPane.YES_OPTION )
							{
								save();
							}
						}

						new Thread( new Runnable()
						{
							@Override
							public void run()
							{
								try
								{
									getClipAction().play();
								}
								catch( Exception e1 )
								{
									JOptionPane.showMessageDialog( ClipActionEditor.this, 
										"Couldn't play ua.in.smartjava.audio clip [" + e1.getMessage() + "]",
										"Error", JOptionPane.ERROR_MESSAGE );
								}
							}
						}).start();
					}
				}
			}
		} );

		add( testButton, "grow,wrap" );
		
		JLabel help = new JLabel( "Help ..." );
		help.setForeground( Color.BLUE.brighter() );
		help.setCursor( new Cursor( Cursor.HAND_CURSOR ) );
		help.addMouseListener( new MouseAdapter() 
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				JOptionPane.showMessageDialog( ClipActionEditor.this, 
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
			ClipAction clip = getClipAction();

			Interval interval = clip.getInterval();
			
			mComboInterval.setSelectedItem( interval );

			boolean enabled = interval != Interval.ONCE;
			
			mPeriodSliderLabel.setEnabled( enabled );
			mPeriodSlider.setEnabled( enabled );
			mPeriodSlider.setValue( clip.getPeriod() );
			
			String filepath = clip.getPath();
			
			if( filepath != null && !filepath.isEmpty() )
			{
				mTextFilePath.setText( filepath );
			}
			else
			{
				mTextFilePath.setText( PLEASE_SELECT_A_FILE );
			}
		}
		
		setModified( false );
	}

	@Override
	public void save()
	{
		if( hasItem() && isModified() )
		{
			ClipAction script = getClipAction();
			
			script.setInterval( (Interval)mComboInterval.getSelectedItem() );
			script.setPeriod( mPeriodSlider.getValue() );
			script.setPath( mTextFilePath.getText() );
		}
		
		setModified( false );
	}
}
