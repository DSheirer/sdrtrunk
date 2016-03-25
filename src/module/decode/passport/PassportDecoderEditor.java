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
package module.decode.passport;

import gui.editor.Editor;
import gui.editor.EditorValidationException;
import gui.editor.ValidatingEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import module.decode.config.DecodeConfiguration;
import net.miginfocom.swing.MigLayout;
import controller.channel.Channel;

public class PassportDecoderEditor extends ValidatingEditor<Channel>
{
    private static final long serialVersionUID = 1L;

    private JCheckBox mAFC;
    private JSlider mAFCMaximumCorrection;

    public PassportDecoderEditor()
	{
    	init();
	}
    
    private void init()
    {
		setLayout( new MigLayout( "insets 0 0 0 0,wrap 2", "[right][grow,fill]", "" ) );
		
        mAFC = new JCheckBox( "AFC: 3000 Hz" );
        mAFC.setEnabled( false );
        mAFC.setToolTipText( "AFC automatically adjusts the center frequency of the channel to "
    		+ "correct/compensate for inaccuracies and frequency drift in the tuner" );
        mAFC.addActionListener( new ActionListener()
        {
            @Override
            public void actionPerformed( ActionEvent arg0 )
            {
            	setModified( true );
            	
            	if( mAFC.isSelected() && !mAFCMaximumCorrection.isEnabled() )
            	{
            		mAFCMaximumCorrection.setEnabled( true );
            	}
            	else if( !mAFC.isSelected() && mAFCMaximumCorrection.isEnabled() )
            	{
            		mAFCMaximumCorrection.setEnabled( false );
            	}
            }
        } );

        add( mAFC );
        
        mAFCMaximumCorrection = new JSlider( 0, 7000, 3000 );
        mAFCMaximumCorrection.setEnabled( false );
        mAFCMaximumCorrection.setToolTipText( "Maximum AFC frequency correction (0 - 15kHz)" );
        mAFCMaximumCorrection.setMajorTickSpacing( 2000 );
        mAFCMaximumCorrection.setMinorTickSpacing( 1000 );
        mAFCMaximumCorrection.setPaintTicks( true );

		mAFCMaximumCorrection.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				mAFC.setText( "AFC: " + mAFCMaximumCorrection.getValue() + " Hz" );
				setModified( true );
			}
		} );
        add( mAFCMaximumCorrection );
    }
    
	@Override
	public void validate( Editor<Channel> editor ) throws EditorValidationException
	{
		//No validation
	}

	@Override
	public void save()
	{
		if( hasItem() && isModified() )
		{
			DecodeConfigPassport passport = new DecodeConfigPassport();
			
			passport.setAFC( mAFC.isSelected() );                
			passport.setAFCMaximumCorrection( mAFCMaximumCorrection.getValue() );
			
			getItem().setDecodeConfiguration( passport );
		}
		
		setModified( false );
	}

	private void setControlsEnabled( boolean enabled )
	{
		if( mAFC.isEnabled() != enabled  )
		{
			mAFC.setEnabled( enabled );
		}
	}

	@Override
	public void setItem( Channel item )
	{
		super.setItem( item );
		
		if( hasItem() )
		{
			setControlsEnabled( true );
			
			DecodeConfiguration config = getItem().getDecodeConfiguration();
			
			if( config instanceof DecodeConfigPassport )
			{
				DecodeConfigPassport passport = (DecodeConfigPassport)config;
				
		        mAFC.setSelected( passport.isAFCEnabled() );
		        mAFCMaximumCorrection.setValue( passport.getAFCMaximumCorrection() );
		        mAFCMaximumCorrection.setEnabled( passport.isAFCEnabled() );
				setModified( false );
			}
			else
			{
		        mAFC.setSelected( false );
		        mAFCMaximumCorrection.setValue( DecodeConfiguration.DEFAULT_AFC_MAX_CORRECTION );
		        mAFCMaximumCorrection.setEnabled( false );
				setModified( true );
			}
		}
		else
		{
			setControlsEnabled( false );
			setModified( false );
		}
	}
}
