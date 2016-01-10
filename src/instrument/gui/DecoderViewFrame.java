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
import instrument.InstrumentableProcessingChain;
import instrument.tap.Tap;
import instrument.tap.TapGroup;
import instrument.tap.TapViewPanel;
import instrument.tap.stream.BinaryTap;
import instrument.tap.stream.BinaryTapViewPanel;
import instrument.tap.stream.ComplexSampleTap;
import instrument.tap.stream.ComplexSampleTapViewPanel;
import instrument.tap.stream.ComplexTap;
import instrument.tap.stream.ComplexTapViewPanel;
import instrument.tap.stream.DibitTap;
import instrument.tap.stream.DibitTapViewPanel;
import instrument.tap.stream.EyeDiagramDataTap;
import instrument.tap.stream.EyeDiagramDataTapPanel;
import instrument.tap.stream.FloatTap;
import instrument.tap.stream.FloatTapViewPanel;
import instrument.tap.stream.QPSKTap;
import instrument.tap.stream.QPSKTapPanel;
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

import message.Message;
import module.Module;
import module.decode.DecoderFactory;
import module.decode.config.DecodeConfiguration;
import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import playlist.PlaylistManager;
import sample.Listener;
import source.IControllableFileSource;
import source.Source;
import controller.ThreadPoolManager;
import controller.channel.Channel;
import controller.channel.ChannelModel;
import controller.channel.ChannelProcessingManager;

public class DecoderViewFrame extends JInternalFrame 
							  implements Listener<Message>
{
    private static final long serialVersionUID = 1L;
	private final static Logger mLog = 
			LoggerFactory.getLogger( DecoderViewFrame.class );

	private DecodeConfiguration mDecodeConfig;
    private InstrumentableProcessingChain mProcessingChain;
	private IControllableFileSource mSource;
	
	private HashMap<Tap,TapViewPanel> mPanelMap = 
				new HashMap<Tap,TapViewPanel>();
	
	public DecoderViewFrame( PlaylistManager playlistManager,
							 Channel channel,
							 IControllableFileSource source )
	{
		mSource = source;

		mDecodeConfig = channel.getDecodeConfiguration();
		
		mProcessingChain = new InstrumentableProcessingChain();

		ChannelModel channelModel = new ChannelModel();
		ChannelProcessingManager channelProcessingManager = 
			new ChannelProcessingManager( channelModel, null, 
				playlistManager, null, null, new ThreadPoolManager() );
		
		List<Module> modules = DecoderFactory.getModules( channelModel, 
				channelProcessingManager, playlistManager, channel );

		mProcessingChain.addModules( modules );

		mProcessingChain.addMessageListener( this );
		
		mProcessingChain.setSource( (Source)mSource );
		
		mProcessingChain.start();
		
		initGui();
	}
	
	private void initGui()
	{
        setLayout( new MigLayout( "insets 0 0 0 0 ", "[grow,fill]", "[grow,fill]" ) );

		setTitle( "Decoder [" + mDecodeConfig.getDecoderType().getDisplayString() + "]" );
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

					popup.add( getTapContextMenu() );

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
	
	private JMenu getTapContextMenu()
	{
		JMenu menu = new JMenu( "Modules" );

		if( mProcessingChain != null )
		{
			for( Module module: mProcessingChain.getModules() )
			{
				if( module instanceof Instrumentable )
				{
					List<TapGroup> groups = ((Instrumentable)module).getTapGroups();
					
					for( TapGroup group: groups )
					{
						menu.add( new TapGroupMenu( group ) );
					}
				}
			}
		}

		return menu;
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
				panel = new ComplexTapViewPanel( (ComplexTap)tap );
				break;
			case STREAM_COMPLEX_SAMPLE:
				panel = new ComplexSampleTapViewPanel( (ComplexSampleTap)tap );
				break;
			case STREAM_DIBIT:
				panel = new DibitTapViewPanel( (DibitTap)tap );
				break;
			case STREAM_EYE_DIAGRAM:
				panel = new EyeDiagramDataTapPanel( (EyeDiagramDataTap)tap ) ;
				break;
			case STREAM_FLOAT:
				panel = new FloatTapViewPanel( (FloatTap)tap );
				break;
			case STREAM_QPSK:
				panel = new QPSKTapPanel( (QPSKTap)tap );
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

			for( Module module: mProcessingChain.getModules() )
			{
				if( module instanceof Instrumentable )
				{
					((Instrumentable)module).registerTap( tap );
				}
			}
		}
		else
		{
			mLog.info( "Tap panel is null, couldn't add for tap " + 
					tap.getName() + "[" + tap.getType().toString() + "]" );
		}
	}
	
	public void remove( Tap tap )
	{
		for( Module module: mProcessingChain.getModules() )
		{
			if( module instanceof Instrumentable )
			{
				((Instrumentable)module).unregisterTap( tap );
			}
		}
		
		TapViewPanel panel = mPanelMap.get( tap );
		
		remove( panel );
		
		validate();
		
		mPanelMap.remove( tap );
	}

	public class AddAllTapsItem extends JMenuItem
	{
		private static final long serialVersionUID = 1L;
		
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
    public void receive( Message message )
    {
		mLog.info( message.toString() );
    }
	
	public class TapGroupMenu extends JMenu
	{
		private static final long serialVersionUID = 1L;
		private TapGroup mTapGroup;
		
		public TapGroupMenu( TapGroup tapGroup )
		{
			super( tapGroup.getName() );
			
			mTapGroup = tapGroup;
		
			add( new AddAllTapsItem( mTapGroup.getTaps() ) );
			
			add( new JSeparator() );
			
			for( Tap tap: mTapGroup.getTaps() )
			{
				add( new TapSelectionItem( tap ) );
			}
		}
	}
}
