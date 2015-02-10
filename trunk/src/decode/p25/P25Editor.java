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
package decode.p25;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.SourceEditor;
import source.tuner.TunerEditor;
import controller.Editor;
import controller.channel.ChannelValidationException;
import decode.DecodeEditor;
import decode.config.DecodeConfigP25Phase1;
import decode.config.DecodeConfiguration;
import decode.p25.P25Decoder.Modulation;

public class P25Editor extends DecodeEditor
{
	private final static Logger mLog = LoggerFactory.getLogger( P25Editor.class );

	private static final long serialVersionUID = 1L;
    
    private JComboBox<P25Decoder.Modulation> mComboModulation;

    public P25Editor( DecodeConfiguration config )
	{
		super( config );
		
		initGUI();
	}

	private void initGUI()
	{
		mComboModulation = new JComboBox<P25Decoder.Modulation>();

		mComboModulation.setModel( new DefaultComboBoxModel<P25Decoder.Modulation>( 
				P25Decoder.Modulation.values() ) );
		
		mComboModulation.setSelectedItem( ((DecodeConfigP25Phase1)mConfig).getModulation() );
		
		mComboModulation.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				P25Decoder.Modulation selected = mComboModulation
						.getItemAt( mComboModulation.getSelectedIndex() );
				
				if( selected != null )
				{
					((DecodeConfigP25Phase1)mConfig).setModulation( selected );
				}
            }
		} );
		
		add( mComboModulation, "span 2,wrap" );
	}

	/**
	 * Enforce source=tuner for CQPSK modulation
	 */
	@Override
    public void validate( Editor editor ) throws ChannelValidationException
    {
		if( editor instanceof SourceEditor && 
			((DecodeConfigP25Phase1)mConfig).getModulation() == Modulation.CQPSK )
		{
			if( !( editor instanceof TunerEditor ) )
			{
				throw new ChannelValidationException( 
						"<html><body width='175'><h1>LSM Simulcast</h1>"
						+ "<p>P25 LSM Simulcast decoder can only be used with "
						+ "a tuner source.  Please change the Source to use a tuner"
						+ " or change the P25 Decoder to C4FM modulation" );
				
			}
		}
    }
}
