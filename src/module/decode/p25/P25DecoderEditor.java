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
package module.decode.p25;

import gui.editor.Editor;
import gui.editor.EditorValidationException;
import gui.editor.ValidatingEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import module.decode.AuxDecodeConfigurationEditor;
import module.decode.config.AuxDecodeConfiguration;
import module.decode.config.DecodeConfiguration;
import module.decode.p25.P25Decoder.Modulation;
import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.SourceConfigurationEditor;
import source.config.SourceConfigTuner;
import source.config.SourceConfiguration;
import controller.channel.Channel;

public class P25DecoderEditor extends ValidatingEditor<Channel>
{
	private final static Logger mLog = LoggerFactory.getLogger( P25DecoderEditor.class );

	private static final long serialVersionUID = 1L;
    
    private JComboBox<P25_LSMDecoder.Modulation> mComboModulation;
    private JCheckBox mIgnoreDataCalls;
    private JLabel mTrafficChannelPoolSizeLabel;
    private JSlider mTrafficChannelPoolSize;

    public P25DecoderEditor()
	{
		init();
	}

	private void init()
	{
		setLayout( new MigLayout( "insets 0 0 0 0,wrap 3", "[right][grow,fill][]", "" ) );
		
		mComboModulation = new JComboBox<P25_LSMDecoder.Modulation>();
		mComboModulation.setModel( new DefaultComboBoxModel<P25_LSMDecoder.Modulation>( 
				P25_LSMDecoder.Modulation.values() ) );
		mComboModulation.setEnabled( false );
		mComboModulation.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				setModified( true );
			}
		} );
		
		add( new JLabel( "Modulation:" ) );
		add( mComboModulation );
		
		mIgnoreDataCalls = new JCheckBox( "Ignore Data Calls" );
		mIgnoreDataCalls.setEnabled( false );
		mIgnoreDataCalls.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				setModified( true );
			}
		} );
		
		add( mIgnoreDataCalls );

		mTrafficChannelPoolSize = new JSlider( JSlider.HORIZONTAL, DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_MINIMUM,
			DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_MAXIMUM, DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_DEFAULT );
		mTrafficChannelPoolSize.setEnabled( false );
		mTrafficChannelPoolSize.setMajorTickSpacing( 10 );
		mTrafficChannelPoolSize.setMinorTickSpacing( 5 );
		mTrafficChannelPoolSize.setPaintTicks( true );
		mTrafficChannelPoolSize.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				mTrafficChannelPoolSizeLabel.setText( "Traffic Pool: " + mTrafficChannelPoolSize.getValue() );
				setModified( true );
			}
		} );
		
		mTrafficChannelPoolSizeLabel = new JLabel( "Traffic Pool: " + mTrafficChannelPoolSize.getValue() + " " );

		add( mTrafficChannelPoolSizeLabel );
		add( mTrafficChannelPoolSize );
	}

	@Override
    public void save()
	{
		if( hasItem() && isModified() )
		{
			DecodeConfigP25Phase1 p25 = new DecodeConfigP25Phase1();

			p25.setModulation( (Modulation)mComboModulation.getSelectedItem() );
			p25.setIgnoreDataCalls( mIgnoreDataCalls.isSelected() );
			p25.setTrafficChannelPoolSize( mTrafficChannelPoolSize.getValue() );
			
			getItem().setDecodeConfiguration( p25 );
		}
		
		setModified( false );
    }

	@Override
	public void validate( Editor<Channel> editor ) throws EditorValidationException
	{
		/**
		 * Enforce CQPSK modulation uses a Tuner Source for I/Q sample data
		 */
		if( editor instanceof SourceConfigurationEditor && 
			((Modulation)mComboModulation.getSelectedItem()) == Modulation.CQPSK )
		{
			SourceConfiguration config = ((SourceConfigurationEditor)editor).getSourceConfiguration();

			if( config != null && !( config instanceof SourceConfigTuner ) )
			{
				throw new EditorValidationException( editor, 
						"<html><body width='175'><h3>LSM Simulcast</h3>"
						+ "<p>P25 LSM Simulcast decoder can only be used with "
						+ "a tuner source.  Please change the Source to use a tuner"
						+ " or change the P25 Decoder to C4FM modulation</p>" );
			}
		}
		
		if( editor instanceof AuxDecodeConfigurationEditor )
		{
			AuxDecodeConfiguration config = ((AuxDecodeConfigurationEditor)editor).getConfiguration();
			
			if( config != null && !config.getAuxDecoders().isEmpty() )
			{
				throw new EditorValidationException( editor, 
						"<html><body width='175'><h3>Auxilary Decoders</h3>"
						+ "<p>The auxiliary decoders work with analog audio and are not "
						+ "compatible with the P25 Decoder.  Please uncheck any auxiliary "
						+ "decoders that you have selected for this channel</p>" );
			}
		}
	}

	private void setControlsEnabled( boolean enabled )
	{
		if( mComboModulation.isEnabled() != enabled )
		{
			mComboModulation.setEnabled( enabled );
		}
		
		if( mTrafficChannelPoolSize.isEnabled() != enabled )
		{
			mTrafficChannelPoolSize.setEnabled( enabled );
		}

		if( mIgnoreDataCalls.isEnabled() != enabled )
		{
			mIgnoreDataCalls.setEnabled( enabled );
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
			
			if( config instanceof DecodeConfigP25Phase1 )
			{
				DecodeConfigP25Phase1 p25 = (DecodeConfigP25Phase1)config;
				
				mComboModulation.setSelectedItem( p25.getModulation() );
				mIgnoreDataCalls.setSelected( p25.getIgnoreDataCalls() );
				mTrafficChannelPoolSize.setValue( p25.getTrafficChannelPoolSize() );
				setModified( false );
			}
			else
			{
				mComboModulation.setSelectedItem( Modulation.C4FM );
				mIgnoreDataCalls.setSelected( false );
				mTrafficChannelPoolSize.setValue( DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_DEFAULT );
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
