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
package module.decode.ltrnet;

import gui.editor.Editor;
import gui.editor.EditorValidationException;
import gui.editor.ValidatingEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import message.MessageDirection;
import module.decode.config.DecodeConfiguration;
import net.miginfocom.swing.MigLayout;
import controller.channel.Channel;

public class LTRNetDecoderEditor extends ValidatingEditor<Channel>
{
    private static final long serialVersionUID = 1L;

    protected JCheckBox mAFC;
    protected JSpinner mAFCMaximumCorrection;
    private JComboBox<MessageDirection> mComboDirection;
    
	public LTRNetDecoderEditor()
	{
		init();
	}
	
	private void init()
	{
		setLayout( new MigLayout( "fill,wrap 5", "[left][right][grow,fill][right][grow,fill]", 
				"[][grow]" ) );
		
        mAFC = new JCheckBox( "AFC" );
        mAFC.setEnabled( false );
        mAFC.setToolTipText( "AFC automatically adjusts the center frequency of the channel to "
    		+ "correct/compensate for inaccuracies and frequency drift in the tuner" );
        mAFC.addActionListener( new ActionListener()
        {
            @Override
            public void actionPerformed( ActionEvent arg0 )
            {
            	setModified( true );
            }
        } );

        add( mAFC );
        
        mAFCMaximumCorrection = new JSpinner( new SpinnerNumberModel( 3000, 0, 15000, 1 ) );
        mAFCMaximumCorrection.setEnabled( false );
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor)mAFCMaximumCorrection.getEditor();  
        editor.getTextField().setHorizontalAlignment( SwingConstants.CENTER );
        mAFCMaximumCorrection.setToolTipText( "Sets the maximum frequency correction value in hertz, 0 - 15kHz" );
        mAFCMaximumCorrection.addChangeListener( new ChangeListener() 
        {
			@Override
            public void stateChanged( ChangeEvent e )
            {
				setModified( true );
            }
        } );
        
        add( new JLabel( "Correction Limit Hz:" ) );
        add( mAFCMaximumCorrection );
		
		mComboDirection = new JComboBox<MessageDirection>();
		mComboDirection.setModel( new DefaultComboBoxModel<MessageDirection>( MessageDirection.values() ) );
		mComboDirection.setSelectedItem( MessageDirection.OSW );
		mComboDirection.setEnabled( false );
		mComboDirection.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				setModified( true );
            }
		} );

		add( new JLabel( "Format:" ) );
		add( mComboDirection );
	}

	@Override
	public void validate( Editor<Channel> editor ) throws EditorValidationException
	{
	}

	
	@Override
	public void setItem( Channel item )
	{
		super.setItem( item );
		
		if( hasItem() )
		{
			mAFC.setEnabled( true );
			mAFCMaximumCorrection.setEnabled( true );
			mComboDirection.setEnabled( true );
			
			DecodeConfiguration config = getItem().getDecodeConfiguration();
			
			if( config instanceof DecodeConfigLTRNet )
			{
				DecodeConfigLTRNet ltr = (DecodeConfigLTRNet)config;
				mComboDirection.setSelectedItem( ltr.getMessageDirection() );
		        mAFC.setSelected( ltr.isAFCEnabled() );
		        mAFCMaximumCorrection.setValue( ltr.getAFCMaximumCorrection() );
			}
		}
		else
		{
			mAFC.setEnabled( false );
			mAFCMaximumCorrection.setEnabled( false );
			mComboDirection.setEnabled( false );
		}
	}

	@Override
	public void save()
	{
		if( hasItem() && isModified() )
		{
			DecodeConfigLTRNet ltr = new DecodeConfigLTRNet();
			ltr.setAFC( mAFC.isSelected() );                
            
			int value = ((SpinnerNumberModel)mAFCMaximumCorrection.getModel()).getNumber().intValue();
			ltr.setAFCMaximumCorrection( value );
            
			ltr.setMessageDirection( (MessageDirection)mComboDirection.getSelectedItem() );
		}
		
		setModified( false );
	}
}
