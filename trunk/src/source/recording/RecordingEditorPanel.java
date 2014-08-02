package source.recording;

import gui.control.JFrequencyControl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import source.tuner.FrequencyChangeListener;
import net.miginfocom.swing.MigLayout;

public class RecordingEditorPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    private RecordingNode mRecordingNode;
    
	private JTextField mRecordingAlias;
    private JLabel mFilePathLabel;
	
	public RecordingEditorPanel( RecordingNode recordingNode )
	{
		mRecordingNode = recordingNode;
		
		initGUI();
	}

    private void initGUI()
    {
		setLayout( new MigLayout( "fill,wrap 2", "[right,grow][grow]", "[][][][][][grow]" ) );
        
		add( new JLabel( "I/Q Recording" ), "span,align left" );

		add( new JLabel( "Alias:" ) );

		mRecordingAlias = new JTextField( mRecordingNode.getRecording()
				.getRecordingConfiguration().getAlias() );
		
		mRecordingAlias.addFocusListener( new FocusListener()
		{
			@Override
			public void focusLost( FocusEvent arg0 )
			{
				String alias = mRecordingAlias.getText();
				
				if( alias != null )
				{
					mRecordingNode.getRecording().setAlias( alias );
				}
			}
			
			@Override
			public void focusGained( FocusEvent arg0 ) {}
		} );
		
		add( mRecordingAlias, "wrap" );
		
		JButton mFileSelectionButton = new JButton( "File ..." );
		mFileSelectionButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent arg0 )
			{
				if( mRecordingNode.getRecording().hasChannels() )
				{
					JOptionPane.showMessageDialog( RecordingEditorPanel.this, 
							"This recording is currently in replay.  Please "
							+ "disable any channels that are using this "
							+ "recording before changing the recording file." );					
				}
				else
				{
					final JFileChooser fc = new JFileChooser();
					
			        int returnVal = fc.showOpenDialog( RecordingEditorPanel.this );

			        if ( returnVal == JFileChooser.APPROVE_OPTION ) 
			        {
			            File file = fc.getSelectedFile();

			            if( file != null && file.exists() )
			            {
			            	mRecordingNode.getRecording()
			            		.getRecordingConfiguration()
			            				.setFilePath( file.getAbsolutePath() );
			            	
			            	if( mFilePathLabel != null )
			            	{
			            		mFilePathLabel.setText( file.getName() );
			            	}
			            }
			        } 
				}
			}
		} );
		add( mFileSelectionButton );

		String filePath = mRecordingNode.getRecording()
				.getRecordingConfiguration().getFilePath();
		
		if( filePath != null )
		{
			mFilePathLabel = new JLabel( filePath ); 
		}
		else
		{
			mFilePathLabel = new JLabel( "Please select a recording" );
		}
		
		add( mFilePathLabel, "wrap" );

		add( new JLabel( "Center Frequency:" ), "span,align left" );

		JFrequencyControl frequencyControl = new JFrequencyControl( 
				mRecordingNode.getRecording().getRecordingConfiguration()
				.getCenterFrequency() );

		frequencyControl.addListener( mRecordingNode.getRecording() );
		
		add( frequencyControl, "span,align left" );
    }
}
