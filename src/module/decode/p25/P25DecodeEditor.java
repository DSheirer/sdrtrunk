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
package module.decode.p25;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import module.decode.DecodeEditor;
import module.decode.config.DecodeConfiguration;
import module.decode.p25.P25Decoder.Modulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.SourceEditor;
import source.tuner.TunerEditor;
import controller.channel.ChannelConfigurationEditor;
import controller.channel.ConfigurationValidationException;

public class P25DecodeEditor extends DecodeEditor
{
	private final static Logger mLog = LoggerFactory.getLogger( P25DecodeEditor.class );

	private static final long serialVersionUID = 1L;
    
    private JComboBox<P25_LSMDecoder.Modulation> mComboModulation;
    private JCheckBox mIgnoreDataCalls;
    private JLabel mTrafficChannelPoolSizeLabel;
    private JSlider mTrafficChannelPoolSize;

    public P25DecodeEditor( DecodeConfiguration config )
	{
		super( config );
		
		initGUI();
	}

	private void initGUI()
	{
		final DecodeConfigP25Phase1 config = (DecodeConfigP25Phase1)mConfig;
		
		mComboModulation = new JComboBox<P25_LSMDecoder.Modulation>();

		mComboModulation.setModel( new DefaultComboBoxModel<P25_LSMDecoder.Modulation>( 
				P25_LSMDecoder.Modulation.values() ) );
		
		mComboModulation.setSelectedItem( config.getModulation() );
		
		add( new JLabel( "Modulation:" ) );
		add( mComboModulation, "wrap" );
		
		mTrafficChannelPoolSize = new JSlider( JSlider.HORIZONTAL,
				DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_MINIMUM,
				DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_MAXIMUM,
				config.getTrafficChannelPoolSize() );
		
		mTrafficChannelPoolSize.setMajorTickSpacing( 10 );
		mTrafficChannelPoolSize.setMinorTickSpacing( 1 );
		mTrafficChannelPoolSize.setPaintTicks( true );
		
		mTrafficChannelPoolSize.setLabelTable( mTrafficChannelPoolSize.createStandardLabels( 10, 10 ) );
		mTrafficChannelPoolSize.setPaintLabels( true );
		
		mTrafficChannelPoolSizeLabel = new JLabel( "Traffic Channel Pool: " + mTrafficChannelPoolSize.getValue() + " " );
		
		mTrafficChannelPoolSize.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				mTrafficChannelPoolSizeLabel.setText( "Traffic Channel Pool: " + mTrafficChannelPoolSize.getValue() );
			}
		} );
		
		add( mTrafficChannelPoolSizeLabel );
		add( mTrafficChannelPoolSize, "wrap,grow" );

		mIgnoreDataCalls = new JCheckBox();
		mIgnoreDataCalls.setSelected( config.getIgnoreDataCalls() );
		
		add( new JLabel( "Ignore Data Calls" ) );
		add( mIgnoreDataCalls, "wrap,grow" );
		
//		reset();
	}

//	/**
//	 * Enforce source=tuner for CQPSK modulation
//	 */
//	@Override
//    public void validate( ChannelConfigurationEditor editor ) throws ConfigurationValidationException
//    {
//		if( editor instanceof SourceEditor && 
//			((DecodeConfigP25Phase1)mConfig).getModulation() == Modulation.CQPSK )
//		{
//			if( !( editor instanceof TunerEditor ) )
//			{
//				throw new ConfigurationValidationException( 
//						"<html><body width='175'><h1>LSM Simulcast</h1>"
//						+ "<p>P25 LSM Simulcast decoder can only be used with "
//						+ "a tuner source.  Please change the Source to use a tuner"
//						+ " or change the P25 Decoder to C4FM modulation" );
//				
//			}
//		}
//    }
	
	@Override
    public void save()
	{
		DecodeConfigP25Phase1 config = (DecodeConfigP25Phase1)mConfig;

		config.setModulation( (Modulation)mComboModulation.getSelectedItem() );
		config.setIgnoreDataCalls( mIgnoreDataCalls.isSelected() );
		config.setTrafficChannelPoolSize( mTrafficChannelPoolSize.getValue() );
    }

//	@Override
//    public void reset()
//    {
//		DecodeConfigP25Phase1 config = (DecodeConfigP25Phase1)mConfig;
//
//		mComboModulation.setSelectedItem( config.getModulation() );
//		mIgnoreDataCalls.setSelected( config.getIgnoreDataCalls() );
//		mTrafficChannelPoolSize.setValue( config.getTrafficChannelPoolSize() );
//    }
}
