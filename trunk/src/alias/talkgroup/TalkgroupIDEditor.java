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
package alias.talkgroup;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import audio.AudioType;
import controller.ConfigurableNode;

public class TalkgroupIDEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private TalkgroupIDNode mTalkgroupIDNode;
    
    private JLabel mLabelName;
    private JTextField mTextTalkgroupID;
    private JLabel mLabelAudio;
    private JComboBox<AudioType> mComboAudioTypes;
    
    private String mHelpText = "Enter a formatted talkgroup identifier.\n\n"
    		+ "LTR Talkgroup Format: A-HH-TTT (A=Area, H=Home Repeater, "
    		+ "T=Talkgroup)\n\n"
    		+ "Passport Talkgroup Format: xxxxx (up to 5-digit number)\n\n"
            + "Wildcard: use one or more asterisks (*) for any talkgroup digits.\n\n"
            + "Audio: if the talkgroup uses simple frequency inversion, specify"
            + " the inversion frequency from the dropdown list to automatically"
            + " un-invert audio for this specific talkgroup";

	public TalkgroupIDEditor( TalkgroupIDNode talkgroupIDNode )
	{
		mTalkgroupIDNode = talkgroupIDNode;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][][grow]" ) );
		
		add( new JLabel( "Talkgroup ID" ), "span,align center" );
		
		add( new JLabel( "TGID:" ) );
        mTextTalkgroupID = new JTextField();

		mTextTalkgroupID.setText( mTalkgroupIDNode.getTalkgroupID().getTalkgroup() );

		add( mTextTalkgroupID, "growx,push" );

		add( new JLabel( "Audio:" ) );

		mComboAudioTypes = new JComboBox<AudioType>();

		mComboAudioTypes.setModel( 
				new DefaultComboBoxModel<AudioType>( AudioType.values() ) );
		
		AudioType audioType = mTalkgroupIDNode.getTalkgroupID().getAudioType();
		
		if( audioType != null )
		{
			mComboAudioTypes.setSelectedItem( audioType );
		}
		
		add( mComboAudioTypes, "growx,push" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( TalkgroupIDEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( TalkgroupIDEditor.this );
		add( btnReset, "growx,push" );
		
		JTextArea helpText = new JTextArea( mHelpText );
		helpText.setLineWrap( true );
		add( helpText, "span,grow,push" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			String tgid = mTextTalkgroupID.getText();
			
			if( tgid != null )
			{
				mTalkgroupIDNode.getTalkgroupID().setTalkgroup( tgid );
				
				((ConfigurableNode)mTalkgroupIDNode.getParent()).sort();

				AudioType selected = mComboAudioTypes
						.getItemAt( mComboAudioTypes.getSelectedIndex() );
				
				if( selected != null )
				{
					mTalkgroupIDNode.getTalkgroupID().setAudioType( selected );
				}

				mTalkgroupIDNode.save();
				
				mTalkgroupIDNode.show();
			}
			else
			{
				JOptionPane.showMessageDialog( TalkgroupIDEditor.this, 
						"Please enter a talkgroup ID" );
			}
		}
		else if( command.contentEquals( "Reset" ) )
		{
			mTextTalkgroupID.setText( 
					mTalkgroupIDNode.getTalkgroupID().getTalkgroup() );

			mComboAudioTypes.setSelectedItem( 
					mTalkgroupIDNode.getTalkgroupID().getAudioType() );
		}
		
		mTalkgroupIDNode.refresh();
    }
}
