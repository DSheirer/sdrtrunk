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
package decode;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import controller.Editor;
import controller.channel.Channel;
import controller.channel.ChannelValidationException;
import decode.config.DecodeConfiguration;

public class DecodeEditor extends Editor
{
    private static final long serialVersionUID = 1L;

    protected DecodeConfiguration mConfig;
    
    protected JCheckBox mAFC;
    protected JSpinner mAFCMaximumCorrection;

	public DecodeEditor( DecodeConfiguration config )
	{
		mConfig = config;

		if( config.supportsAFC() )
		{
	        mAFC = new JCheckBox( "Automatic Frequency Control" );
	        mAFC.setSelected( mConfig.isAFCEnabled() );
	        mAFC.addActionListener( new ActionListener()
	        {
	            @Override
	            public void actionPerformed( ActionEvent arg0 )
	            {
	                mConfig.setAFC( mAFC.isSelected() );                
	            }
	        } );
	        
	        mAFC.setToolTipText( "AFC automatically adjusts the center "
        		+ "frequency of the channel to correct/compensate for "
        		+ "inaccuracies and frequency drift in the tuner" );

	        add( mAFC, "span" );
	        
	        int initial = mConfig.getAFCMaximumCorrection();
	        
	        SpinnerModel model = new SpinnerNumberModel( initial, 0, 15000, 1 );

	        mAFCMaximumCorrection = new JSpinner( model );

	        JSpinner.NumberEditor editor = 
	        		(JSpinner.NumberEditor)mAFCMaximumCorrection.getEditor();  
	        editor.getTextField().setHorizontalAlignment( SwingConstants.CENTER );
	        
	        mAFCMaximumCorrection.setToolTipText( "Sets the maximum frequency "
	        		+ "correction value in hertz, 0 - 15kHz" );

	        mAFCMaximumCorrection.addChangeListener( new ChangeListener() 
	        {
				@Override
	            public void stateChanged( ChangeEvent e )
	            {
					int value = ((SpinnerNumberModel)mAFCMaximumCorrection
							.getModel()).getNumber().intValue();

	                mConfig.setAFCMaximumCorrection( value );
	                
	                save();
	            }
	        } );
	        
	        add( mAFCMaximumCorrection );
	        add( new JLabel( "Correction Limit Hz" ), "growx, push" );
		}
	}

	public DecodeConfiguration getConfig()
	{
		return mConfig;
	}

	@Override
    public void save()
    {
    }

	@Override
    public void reset()
    {
    }
}
