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
package alias.action.script;

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

public class ScriptActionEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    
	private final static Logger mLog = 
							LoggerFactory.getLogger( ScriptActionEditor.class );
	
    private ScriptActionNode mScriptActionNode;
    private JComboBox<Interval> mComboInterval;
    private JSpinner mSpinnerPeriod;
    private JTextField mTextFilePath;

	public ScriptActionEditor( ScriptActionNode clipActionNode )
	{
		mScriptActionNode = clipActionNode;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][][][grow]" ) );

		add( new JLabel( "Action: Script" ), "span,align center" );
		
		add( new JLabel( "Interval:" ) );
		
		mComboInterval = new JComboBox<Interval>( Interval.values() );

		mComboInterval.setSelectedItem( mScriptActionNode.getScriptAction().getInterval() );

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
					
					mScriptActionNode.getScriptAction().setInterval( selected );
				}
           }
		});
		
		add( mComboInterval, "wrap" );

		final SpinnerModel model = new SpinnerNumberModel( 
				mScriptActionNode.getScriptAction().getPeriod(), 1, 30, 1 );
		
		model.addChangeListener( new ChangeListener() 
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				mScriptActionNode.getScriptAction().setPeriod( (int)model.getValue() );
			}
		} );
		
		mSpinnerPeriod = new JSpinner( model );

		if( mScriptActionNode.getScriptAction().getInterval() == Interval.ONCE )
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

				int value = chooser.showOpenDialog( ScriptActionEditor.this );

				if( value == JFileChooser.APPROVE_OPTION )
					{
						File file = chooser.getSelectedFile();
						
						mScriptActionNode.getScriptAction().setScript( file.getAbsolutePath() );
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
				String script = mScriptActionNode.getScriptAction().getScript();
				
				if( script == null || script.trim().isEmpty() )
				{
					JOptionPane.showMessageDialog( ScriptActionEditor.this, 
							"Please select a script file", "Please select file", 
							JOptionPane.ERROR_MESSAGE );
				}
				
				EventQueue.invokeLater( new Runnable() 
				{
					@Override
					public void run()
					{
						try
						{
							mScriptActionNode.getScriptAction().play();
						}
						catch( Exception e )
						{
							JOptionPane.showMessageDialog( ScriptActionEditor.this, 
									"Couldn't run script [" + e.getMessage() + "]", 
									"Error", JOptionPane.ERROR_MESSAGE );
						}
					}
				} );
			}
		} );
		
		add( testButton, "wrap" );
		
		mTextFilePath = new JTextField( mScriptActionNode.getScriptAction().getScript() );
		add( mTextFilePath, "growx,span" );

		StringBuilder sb = new StringBuilder();
		
		sb.append( "This action will run the script according to the interval that you select.\n\n" );
		sb.append( "Once - Run script the first time the alias is active and never again.\n\n" );
		sb.append( "Once, Reset After Delay - Run once and suppress subsequent runs for the specified period in seconds.  After the reset period, it will play again when the alias is active.\n\n" );
		sb.append( "Until Dismissed - Run script every period seconds until you click OK on the dialog that appears. Alerting is suppressed for 15 seconds after you click OK." );
		
		JTextArea description = new JTextArea( sb.toString() );
		
		description.setLineWrap( true );
		description.setBackground( getBackground() );
		
		add( description, "growx,span" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( ScriptActionEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( ScriptActionEditor.this );
		add( btnReset, "growx,push" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			mScriptActionNode.save();
			mScriptActionNode.show();
		}
		
		mScriptActionNode.refresh();
    }
}
