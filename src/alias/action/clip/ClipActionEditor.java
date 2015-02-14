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
package alias.action.clip;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
import alias.action.script.ScriptActionEditor;

public class ClipActionEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    
	private final static Logger mLog = 
							LoggerFactory.getLogger( ClipActionEditor.class );
	
    private ClipActionNode mClipActionNode;
    private JTextField mTextSiteID;
    private JComboBox<Interval> mComboInterval;
    private JSpinner mSpinnerPeriod;
    private JTextField mTextFilePath;

	public ClipActionEditor( ClipActionNode clipActionNode )
	{
		mClipActionNode = clipActionNode;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][][][grow]" ) );

		add( new JLabel( "Action: Audio Clip" ), "span,align center" );
		
		add( new JLabel( "Interval:" ) );
		
		mComboInterval = new JComboBox<Interval>( Interval.values() );

		mComboInterval.setSelectedItem( mClipActionNode.getClipAction().getInterval() );

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
					
					mClipActionNode.getClipAction().setInterval( selected );
				}
           }
		});
		
		add( mComboInterval, "wrap" );

		final SpinnerModel model = new SpinnerNumberModel( 
				mClipActionNode.getClipAction().getPeriod(), 1, 30, 1 );
		
		model.addChangeListener( new ChangeListener() 
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				mClipActionNode.getClipAction().setPeriod( (int)model.getValue() );
			}
		} );
		
		mSpinnerPeriod = new JSpinner( model );

		if( mClipActionNode.getClipAction().getInterval() == Interval.ONCE )
		{
			mSpinnerPeriod.setEnabled( false );
		}
		
		add( new JLabel( "Period:" ) );
		add( mSpinnerPeriod, "wrap" );
		
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
						
						mClipActionNode.getClipAction().setPath( file.getAbsolutePath() );
						mTextFilePath.setText( file.getAbsolutePath() );
					}
				}
		} );
		
		add( fileButton );
		
		JButton testButton = new JButton( "Test" );
		testButton.addActionListener( new ActionListener() 
		{
			@Override
			public void actionPerformed( ActionEvent actionEvent )
			{
				String path = mClipActionNode.getClipAction().getPath();
				
				if( path == null || path.trim().isEmpty() )
				{
					JOptionPane.showMessageDialog( ClipActionEditor.this, 
							"Please select an audio file", "Please select file", 
							JOptionPane.ERROR_MESSAGE );
				}
				
				EventQueue.invokeLater( new Runnable() 
				{
					@Override
					public void run()
					{
						try
						{
							mClipActionNode.getClipAction().play();
						}
						catch( Exception e )
						{
							JOptionPane.showMessageDialog( ClipActionEditor.this, 
									"Couldn't play audio clip [" + e.getMessage() + "]", 
									"Error", JOptionPane.ERROR_MESSAGE );
						}
					}
				} );
			}
		} );
		
		add( testButton, "wrap" );
		
		mTextFilePath = new JTextField( mClipActionNode.getClipAction().getPath() );
		add( mTextFilePath, "growx,span" );

		StringBuilder sb = new StringBuilder();
		
		sb.append( "This action will play the audio clip according to the interval that you select.\n\n" );
		sb.append( "Once - Play the first time the alias is active and never again.\n\n" );
		sb.append( "Once, Reset After Delay - Play once and suppress subsequent plays for the specified period in seconds.  After the reset period, it will play again when the alias is active.\n\n" );
		sb.append( "Until Dismissed - Play audio clip every period seconds until you click OK on the dialog that appears. Alerting is suppressed for 15 seconds after you click OK." );
		
		JTextArea description = new JTextArea( sb.toString() );
		
		description.setLineWrap( true );
		description.setBackground( getBackground() );
		
		add( description, "growx,span" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( ClipActionEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( ClipActionEditor.this );
		add( btnReset, "growx,push" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			mClipActionNode.save();
			mClipActionNode.show();
		}
		
		mClipActionNode.refresh();
    }
}
