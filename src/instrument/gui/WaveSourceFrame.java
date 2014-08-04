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
package instrument.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import log.Log;
import message.MessageDirection;
import net.miginfocom.swing.MigLayout;
import source.Source.SampleType;
import source.wave.WaveSource;
import source.wave.WaveSource.PositionListener;
import decode.Decoder;
import decode.DecoderType;
import decode.fleetsync2.Fleetsync2Decoder;
import decode.ltrnet.LTRNetDecoder;
import decode.mdc1200.MDCDecoder;
import decode.mpt1327.MPT1327Decoder;
import decode.mpt1327.MPT1327Decoder.Sync;
import decode.p25.P25Decoder;
import decode.passport.PassportDecoder;

public class WaveSourceFrame extends JInternalFrame implements PositionListener
{
	private WaveSource mSource;
	private JDesktopPane mDesktop;
	
	private JLabel mCurrentPosition = new JLabel( "0" );
	private JComboBox<DecoderType> mComboDecoders;
	
	public WaveSourceFrame( WaveSource source, JDesktopPane desktop )
	{
		mSource = source;
		mSource.addListener( this );
		
		mDesktop = desktop;
		
		initGui();
	}
	
	private void initGui()
	{
		setTitle( "Wave Source [" + mSource.getSampleType().toString() + "]" );
		setPreferredSize( new Dimension( 600, 150 ) );
		setSize( 600, 150 );

		setResizable( true );
		setClosable( true );
		setIconifiable( true );
		setMaximizable( false );

		JPanel panel = new JPanel();
		panel.setLayout( new MigLayout() );

		JLabel fileLabel = new JLabel( "File: " + mSource.getFile().getName() );
		fileLabel.setToolTipText( mSource.getFile().getName() );
		panel.add(  fileLabel, "span,wrap" );

		panel.add( new JLabel( "Jump:" ) );
		panel.add( new JumpToField( mSource ) );
		panel.add( new NextSampleButton( mSource, ">", 1 ) );
		panel.add( new NextSampleButton( mSource, "> 100 >", 100 ) );
		panel.add( new NextSampleButton( mSource, "> 1000 >", 1000 ) );
		panel.add( new JLabel( "Posn:" ) );
		panel.add( mCurrentPosition, "wrap" );
		
		/* ComboBox: Decoders */
		mComboDecoders = new JComboBox<DecoderType>();

		DefaultComboBoxModel<DecoderType> model = 
							new DefaultComboBoxModel<DecoderType>();
		
		for( DecoderType type: DecoderType.getInstrumentableDecoders() )
		{
			model.addElement( type );
		}
		
		mComboDecoders.setModel( model );

		panel.add( new JLabel( "Decoders:" ) );
		panel.add( mComboDecoders, "span 3,grow" );
		panel.add( new AddDecoderButton(), "grow" );
		
		add( panel );
	}
	
	public class AddDecoderButton extends JButton
	{
		public AddDecoderButton()
		{
			super( "Add" );
			
			addActionListener( new ActionListener() 
			{
				@Override
				public void actionPerformed( ActionEvent arg0 )
				{
					DecoderType selected = 
							(DecoderType)mComboDecoders.getSelectedItem();
					
					Log.info( "Selected:" + selected.getDisplayString() );

					Decoder decoder = null;
					
					switch( selected )
					{
						case FLEETSYNC2:
							decoder = new Fleetsync2Decoder( null );
							break;
						case LTR_NET:
						    decoder = new LTRNetDecoder( SampleType.FLOAT, null, MessageDirection.OSW );
						    break;
						case MDC1200:
							decoder = new MDCDecoder( null );
							break;
						case MPT1327:
							decoder = new MPT1327Decoder( SampleType.FLOAT, null, Sync.NORMAL );
							break;
						case PASSPORT:
							decoder = new PassportDecoder( SampleType.FLOAT, null );
							break;
						case P25:
							decoder = new P25Decoder( SampleType.FLOAT, null );
							break;
					}

					if( decoder != null )
					{
						DecoderViewFrame decoderFrame = 
								new DecoderViewFrame( decoder, mSource );
						
						decoderFrame.setVisible( true );
						mDesktop.add( decoderFrame );
					}
				}
			} );
		}
	}
	
	public class NextSampleButton extends JButton
	{
		private WaveSource mSource;
		private int mCount;
		
		public NextSampleButton( WaveSource source, String label, int count )
		{
			super( label );
			mSource = source;
			mCount = count;
			
			addActionListener( new ActionListener() 
			{
				@Override
                public void actionPerformed( ActionEvent arg0 )
                {
					try
                    {
	                    mSource.next( mCount );
                    }
                    catch ( IOException e )
                    {
                    	Log.error( "Viewer - error trying to fetch next [" + 
                    			mCount + "] samples - " + e.getLocalizedMessage() );

                    	JOptionPane.showMessageDialog( WaveSourceFrame.this,
                    		    "Cannot read " + mCount + " more samples [" + 
                    		    		e.getLocalizedMessage() + "]",
                    		    "Wave File Error",
                    		    JOptionPane.ERROR_MESSAGE );                    	
                    }
                }
			} );
		}
	}

	public class JumpToField extends JTextField
	{
		private WaveSource mSource;
		
		public JumpToField( WaveSource source )
		{
			super( "0" );
			
			mSource = source;
			
			setMinimumSize( new Dimension( 100, getHeight() ) );

			addFocusListener( new FocusListener() 
			{
				@Override
                public void focusGained( FocusEvent arg0 ) {}

				@Override
                public void focusLost( FocusEvent arg0 )
                {
					try
					{
						long position = Long.parseLong( getText() );
						mSource.jumpTo( position );
					}
					catch( Exception e )
					{
						Log.error( "WaveSourceFrame - exception during jump "
								+ "to - " + e.getLocalizedMessage() );
						
                    	JOptionPane.showMessageDialog( WaveSourceFrame.this,
                    		    "Can't jump to position [" + getText() + "]",
                    		    "Error",
                    		    JOptionPane.ERROR_MESSAGE );                    	
					}
                }
			} );
		}
	}

	@Override
    public void positionUpdated( long position, boolean reset )
    {
		mCurrentPosition.setText( String.valueOf( position ) );
		
		if( reset )
		{
			
		}
    }
}
