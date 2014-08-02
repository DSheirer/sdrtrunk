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

import instrument.Instrumentable;
import instrument.tap.Tap;
import instrument.tap.TapViewPanel;
import instrument.tap.stream.BinaryTap;
import instrument.tap.stream.BinaryTapViewPanel;
import instrument.tap.stream.FloatTap;
import instrument.tap.stream.FloatTapViewPanel;
import instrument.tap.stream.SymbolEventTap;
import instrument.tap.stream.SymbolEventTapViewPanel;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import log.Log;
import message.Message;
import net.miginfocom.swing.MigLayout;
import sample.Listener;
import source.Source.SampleType;
import source.wave.FloatWaveSource;
import source.wave.WaveSource;
import source.wave.WaveSource.PositionListener;
import decode.Decoder;

public class DecoderViewFrame extends JInternalFrame 
							  implements PositionListener, Listener<Message>
{
    private static final long serialVersionUID = 1L;

    private Decoder mDecoder;
	private WaveSource mWaveSource;
	
	private HashMap<Tap,TapViewPanel> mPanelMap = 
				new HashMap<Tap,TapViewPanel>();
	
	public DecoderViewFrame( Decoder decoder, WaveSource source )
	{
		mDecoder = decoder;
		mWaveSource = source;

		if( source.getSampleType() == SampleType.COMPLEX )
		{
			Log.error( "Hey, we're not wired up yet for complex" );
		}
		else if( source.getSampleType() == SampleType.FLOAT )
		{
			FloatWaveSource fws = (FloatWaveSource)mWaveSource;
			fws.setListener( mDecoder.getFloatReceiver() );
		}
		
		mDecoder.addMessageListener( this );
		
		initGui();
	}
	
	private void initGui()
	{
        setLayout( new MigLayout( "insets 0 0 0 0 ", "[grow,fill]", "[grow,fill]" ) );

		setTitle( "Decoder [" + mDecoder.getType().getDisplayString() + "]" );
		setPreferredSize( new Dimension( 450, 250 ) );
		setSize( 450, 250 );

		setResizable( true );
		setClosable( true );
		setIconifiable( true );
		setMaximizable( false );

		addMouseListener( new MouseListener() 
		{
			@Override
            public void mouseClicked( MouseEvent me )
            {
				if( me.getButton() == MouseEvent.BUTTON3 )
				{
					JPopupMenu popup = new JPopupMenu();
					
					if( mDecoder instanceof Instrumentable )
					{
						JMenu tapMenu = new JMenu( "Taps" );
						popup.add( tapMenu );
						
						Instrumentable i = (Instrumentable)mDecoder;

						tapMenu.add( new AddAllTapsItem( i.getTaps() ) );
						
						tapMenu.add( new JSeparator() );
						
						for( Tap tap: i.getTaps() )
						{
							tapMenu.add( new TapSelectionItem( tap ) );
						}
					}

					if( !mPanelMap.values().isEmpty() )
					{
						popup.add( new JSeparator() );
						
						for( TapViewPanel panel: mPanelMap.values() )
						{
							popup.add( panel.getContextMenu() );
						}
					}
					
					popup.show( DecoderViewFrame.this, me.getX(), me.getY() );
				}
            }

			@Override
            public void mouseEntered( MouseEvent arg0 ) {}
			@Override
            public void mouseExited( MouseEvent arg0 ) {}
			@Override
            public void mousePressed( MouseEvent arg0 ) {}
			@Override
            public void mouseReleased( MouseEvent arg0 ) {}
			
		} );
	}
	
	public void add( Tap tap )
	{
		TapViewPanel panel = null;
		
		switch( tap.getType() )
		{
			case EVENT_SYNC_DETECT:
				break;
			case STREAM_BINARY:
				panel = new BinaryTapViewPanel( (BinaryTap)tap );
				break;
			case STREAM_COMPLEX:
				break;
			case STREAM_FLOAT:
				panel = new FloatTapViewPanel( (FloatTap)tap );
				break;
			case STREAM_SYMBOL:
				panel = new SymbolEventTapViewPanel( (SymbolEventTap)tap );
				break;
		}
		
		if( panel != null )
		{
			add( panel, "span" );
			
			validate();
			
			mPanelMap.put( tap, panel );

			((Instrumentable)mDecoder).addTap( tap );
		}
		else
		{
			Log.info( "Tap panel is null, couldn't add for tap " + 
					tap.getName() + "[" + tap.getType().toString() + "]" );
		}
	}
	
	public void remove( Tap tap )
	{
		((Instrumentable)mDecoder).removeTap( tap );
		
		TapViewPanel panel = mPanelMap.get( tap );
		
		remove( panel );
		
		validate();
		
		mPanelMap.remove( tap );
	}

	public class AddAllTapsItem extends JMenuItem
	{
		private final List<Tap> mTaps;
		
		public AddAllTapsItem( final List<Tap> taps )
		{
			super( "All Taps" );
			
			mTaps = taps;
			
			addActionListener( new ActionListener() 
			{
				@Override
                public void actionPerformed( ActionEvent e )
                {
					for( Tap tap: mTaps )
					{
						DecoderViewFrame.this.add( tap );
					}
                }
			} );
		}
	}
	
	public class TapSelectionItem extends JCheckBoxMenuItem
	{
        private static final long serialVersionUID = 1L;
		private Tap mTap;
		
		public TapSelectionItem( Tap tap )
		{
			super( tap.getName() );

			mTap = tap;
			
			if( mPanelMap.keySet().contains( tap ) )
			{
				setSelected( true );
			}
			
			addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( ActionEvent arg0 )
				{
					if( mPanelMap.keySet().contains( mTap ) )
					{
						DecoderViewFrame.this.remove( mTap );
					}
					else
					{
						DecoderViewFrame.this.add( mTap );
					}
				}
			} );
		}
	}

	@Override
    public void positionUpdated( long position, boolean reset )
    {
		if( reset )
		{
			for( TapViewPanel panel: mPanelMap.values() )
			{
				panel.reset();
			}
		}
    }

	@Override
    public void receive( Message message )
    {
		Log.info( "Decoder message: " + message.toString() );
    }
}
