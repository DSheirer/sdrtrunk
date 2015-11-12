/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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
package spectrum;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import module.decode.DecoderType;
import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.SampleType;
import sample.complex.ComplexBuffer;
import settings.ColorSetting.ColorSettingName;
import settings.ColorSettingMenuItem;
import settings.Setting;
import settings.SettingChangeListener;
import settings.SettingsManager;
import source.SourceException;
import source.tuner.Tuner;
import source.tuner.TunerSelectionListener;
import source.tuner.frequency.FrequencyChangeEvent;
import source.tuner.frequency.FrequencyChangeEvent.Attribute;
import source.tuner.frequency.FrequencyChangeListener;
import spectrum.OverlayPanel.ChannelDisplay;
import spectrum.converter.ComplexDecibelConverter;
import spectrum.converter.DFTResultsConverter;
import spectrum.menu.AveragingItem;
import spectrum.menu.DFTSizeItem;
import spectrum.menu.FFTWindowTypeItem;
import spectrum.menu.FrameRateItem;
import spectrum.menu.SmoothingItem;
import spectrum.menu.SmoothingTypeItem;

import com.jidesoft.swing.JideSplitPane;

import controller.ResourceManager;
import controller.channel.Channel;
import controller.channel.ChannelEvent;
import controller.channel.ChannelEventListener;
import controller.channel.ChannelUtils;
import dsp.filter.Window.WindowType;
import dsp.filter.smoothing.SmoothingFilter.SmoothingType;

/**
 * Spectral Display Panel - comprises a DFTProcessor coupled with a waterfall
 * display and a spectrum display, with an overlay of frequency data and channel
 * data.
 * 
 * Processes float or complex sample data and displays the frequency content
 * in the spectral and waterfall displays.
 */
public class SpectralDisplayPanel extends JPanel 
								  implements ChannelEventListener,
								  			 Listener<ComplexBuffer>,
								  			 FrequencyChangeListener,
								  			 IDFTWidthChangeProcessor,
								  			 SettingChangeListener,
								  			 TunerSelectionListener
 {
    private static final long serialVersionUID = 1L;
    
	private final static Logger mLog = 
			LoggerFactory.getLogger( SpectralDisplayPanel.class );

	public static final int NO_ZOOM = 0;
	public static final int MAX_ZOOM = 6;
	
	private static DecimalFormat sCURSOR_FORMAT = new DecimalFormat( "000.00000" );

	private DFTSize mDFTSize = DFTSize.FFT04096;
	private int mZoom = 0;
	private int mDFTZoomWindowOffset = 0;

	private JScrollPane mScrollPane;
    private JLayeredPane mLayeredPanel;
    private SpectrumPanel mSpectrumPanel;
    private WaterfallPanel mWaterfallPanel;
    private OverlayPanel mOverlayPanel;
    private DFTProcessor mDFTProcessor;
    private DFTResultsConverter mDFTConverter;
    private ResourceManager mResourceManager;
	private Tuner mTuner;
    
    public SpectralDisplayPanel( ResourceManager resourceManager )
    {
    	mResourceManager = resourceManager;

		init();
    }
    
    public void dispose()
    {
		/* De-register from receiving samples when the window closes */
    	clearTuner();
    	
    	mResourceManager.getChannelManager().removeListener( this );
    	
    	mDFTProcessor.dispose();
    	mDFTProcessor = null;
    	
    	mDFTConverter.dispose();
    	mDFTConverter = null;
    	
    	mSpectrumPanel.dispose();
    	mSpectrumPanel = null;
    	
    	mWaterfallPanel.dispose();
    	mWaterfallPanel = null;
    	
    	mOverlayPanel.dispose();
    	mOverlayPanel = null;
    	
    	mResourceManager = null;
    	
    	mTuner = null;
    }
    
	/**
	 * Queues an FFT size change request.  The scheduled executor will apply 
	 * the change when it runs.
	 */
	public void setDFTSize( DFTSize size )
	{
		mDFTProcessor.setDFTSize( size );
		mOverlayPanel.setDFTSize( size );
		mDFTSize = size;

		setZoom( 0, 0, 0 );
	}
	
    @Override
	public DFTSize getDFTSize()
	{
		return mDFTSize;
	}

	public int getZoom()
    {
    	return mZoom;
    }
    
    /**
     * Sets the current zoom level (2^zoom)
     * 
     * 0 	No Zoom
     * 1	2x Zoom
     * 2	4x Zoom
     * 3	8x Zoom
     * 4	16x Zoom
     * 5	32x Zoom
     * 6    64x Zoom
     * 
     * @param zoom level, 0 - 5.
     * @param frequency under the mouse to maintain while zooming
     * @param xAxisOffset where to maintain the frequency under the mouse
     */
    public void setZoom( int zoom, long frequency, double windowOffset )
    {
    	if( zoom != mZoom )
    	{
        	mZoom = zoom;
        	
        	double zoomTotal = Math.pow( 2.0, mZoom );

        	double overallOffset = getBinOffset( frequency );
        	double windowBinOffset = ( (double)mDFTSize.getSize() / 
        							   (double)zoomTotal ) * 
        							   windowOffset;
        	
        	mDFTZoomWindowOffset = (int)( overallOffset - windowBinOffset );

        	mSpectrumPanel.setZoom( mZoom, mDFTZoomWindowOffset );
        	mOverlayPanel.setZoom( mZoom, mDFTZoomWindowOffset );
        	mWaterfallPanel.setZoom( mZoom, mDFTZoomWindowOffset );
    	}
    }
    
    /**
     * Calculates the overall offset of the frequency from the current minimum
     * frequency in terms of total FFT width
     * @param frequency
     * @return
     */
    private double getBinOffset( long frequency )
    {
    	if( mOverlayPanel.containsFrequency( frequency ) )
    	{
    		return (double)mDFTSize.getSize() * 
				( (double)( frequency - mOverlayPanel.getMinFrequency() ) / 
					(double)mOverlayPanel.getBandwidth() );
    	}
    	
    	return 0.0;
    }

	/**
	 * Monitors for setting changes.  Colors can be changed by external actions
	 * and will automatically update in this class
	 */
	@Override
    public void settingChanged( Setting setting )
    {
		if( mOverlayPanel != null )
		{
			mOverlayPanel.settingChanged( setting );
		}
		
		if( mWaterfallPanel != null )
		{
			mWaterfallPanel.settingChanged( setting );
		}
		
		if( mSpectrumPanel != null )
		{
			mSpectrumPanel.settingChanged( setting );
		}
    }
	
    /**
     * Overrides JComponent method to return false, since we have overlapping
     * panels with the spectrum and channel panels
     */
    public boolean isOptimizedDrawingEnabled()
    {
    	return false;
    }
   
    private void init()
    {
    	setLayout( new MigLayout( "insets 0 0 0 0", "[grow]", "[grow]") );

		//Register for any future settings (ie color) changes
		mResourceManager.getSettingsManager().addListener( this );
		
    	/**
    	 * The layered pane holds the overlapping spectrum and channel panels
    	 * and manages the sizing of each panel with the resize listener
    	 */
    	mLayeredPanel = new JLayeredPane();
    	mLayeredPanel.addComponentListener( new ResizeListener() );

    	/**
    	 * Create a mouse adapter to handle mouse events over the spectrum
    	 * and waterfall panels
    	 */
    	MouseEventProcessor mouser = new MouseEventProcessor();
    	
    	//Create the spectrum panel
    	mSpectrumPanel = new SpectrumPanel( mResourceManager );

    	//Create the overlay panel
    	mOverlayPanel = new OverlayPanel( mResourceManager );
    	mOverlayPanel.addMouseListener( mouser );
    	mOverlayPanel.addMouseMotionListener( mouser );
    	mOverlayPanel.addMouseWheelListener( mouser );

    	//Add the spectrum and channel panels to the layered panel
    	mLayeredPanel.add( mSpectrumPanel, new Integer( 0 ), 0 );
    	mLayeredPanel.add( mOverlayPanel, new Integer( 1 ), 0 );

    	//Create the waterfall
    	mWaterfallPanel = new WaterfallPanel( mResourceManager );
    	mWaterfallPanel.addMouseListener( mouser );
    	mWaterfallPanel.addMouseMotionListener( mouser );
    	mWaterfallPanel.addMouseWheelListener( mouser );

    	/* Attempt to set a 50/50 split preferred size for the split pane */
    	double totalHeight = mLayeredPanel.getPreferredSize().getHeight() +
    					  mWaterfallPanel.getPreferredSize().getHeight();
    	
    	mLayeredPanel.setPreferredSize( new Dimension( (int)mLayeredPanel
    			.getPreferredSize().getWidth(), (int)( totalHeight / 2.0d ) ) );
    	
    	mWaterfallPanel.setPreferredSize( new Dimension( (int)mWaterfallPanel
    			.getPreferredSize().getWidth(), (int)( totalHeight / 2.0d ) ) );
    	
    	//Create the split pane to hold the layered pane and the waterfall
    	JideSplitPane splitPane = new JideSplitPane( JSplitPane.VERTICAL_SPLIT );
    	splitPane.setDividerSize( 5 );
    	splitPane.add( mLayeredPanel );
    	splitPane.add( mWaterfallPanel );
    	
    	mScrollPane = new JScrollPane( splitPane );
    	
        add( mScrollPane, "grow" );
        
    	/**
    	 * Setup DFTProcessor to process samples and register the waterfall and
    	 * spectrum panel to receive the processed dft results
    	 */
		mDFTProcessor = new DFTProcessor( SampleType.COMPLEX );
		mDFTConverter = new ComplexDecibelConverter();
		mDFTProcessor.addConverter( mDFTConverter );
		
    	mDFTConverter.addListener( (DFTResultsListener)mSpectrumPanel );
    	mDFTConverter.addListener( (DFTResultsListener)mWaterfallPanel );
    }

	/**
	 * Receives frequency change events -- primarily from tuner components.
	 */
	@Override
    public void frequencyChanged( FrequencyChangeEvent event )
    {
		mOverlayPanel.frequencyChanged( event );
		mDFTProcessor.frequencyChanged( event );
    }

	/**
	 * Channel change events
	 */
	@Override
    public void channelChanged( ChannelEvent event )
    {
		mOverlayPanel.channelChanged( event );
    }

	@Override
    public void receive( ComplexBuffer sampleBuffer )
    {
		mDFTProcessor.receive( sampleBuffer );
    }

	/**
	 * Tuner selected events
	 */
	@Override
	public void tunerSelected( Tuner tuner )
	{
		clearTuner();
		
		mDFTProcessor.clearBuffer();

		mTuner = tuner;

		if( mTuner != null )
		{
			//Register to receive frequency change events
			mTuner.addListener( (FrequencyChangeListener)this );

			//Register the dft processor to receive samples from the tuner
			mTuner.addListener( (Listener<ComplexBuffer>)mDFTProcessor );
			
			mSpectrumPanel.setSampleSize( mTuner.getSampleSize() );
			
			//Fire frequency and sample rate change events so that everyone 
			//can init
			try
            {
				frequencyChanged( new FrequencyChangeEvent( 
						Attribute.FREQUENCY, mTuner.getFrequency() ) );
				
				frequencyChanged( new FrequencyChangeEvent( 
						Attribute.SAMPLE_RATE, mTuner.getSampleRate() ) );
            }
            catch ( SourceException e )
            {
            	mLog.info( "DFTProcessor - exception during new tuner setup - "
            			+ "couldn't get frequency from the tuner", e );
            }
		}
	}

	/**
	 * Tuner de-selection cleanup method
	 */
	public void clearTuner()
	{
		if( mTuner != null )
		{
			//Deregister for frequency change events from the tuner
			mTuner.removeListener( (FrequencyChangeListener)this );
			
			//Deregister the dft processor from receiving samples
			mTuner.removeListener( (Listener<ComplexBuffer>)mDFTProcessor );
			mTuner = null;
		}
	}

	/**
	 * Monitors the sizing of the layered pane and resizes the spectrum and
	 * channel panels whenever the layered pane is resized
	 */
	public class ResizeListener implements ComponentListener
	{
		@Override
        public void componentResized( ComponentEvent e )
        {
			Component c = e.getComponent();
			
			mSpectrumPanel.setBounds( 0, 0, c.getWidth(), c.getHeight() );
			mOverlayPanel.setBounds( 0, 0, c.getWidth(), c.getHeight() );
        }

		@Override
        public void componentHidden( ComponentEvent arg0 ) {}
		@Override
        public void componentMoved( ComponentEvent arg0 ) {}
		@Override
        public void componentShown( ComponentEvent arg0 ) {}
	}
	
	/**
	 * Mouse event handler for the channel panel.
	 */
	public class MouseEventProcessor implements MouseMotionListener, 
												MouseListener, 
												MouseWheelListener
	{
		@Override
		public void mouseWheelMoved( MouseWheelEvent e )
		{
			int zoom = mZoom - e.getWheelRotation();

			if( zoom < NO_ZOOM )
			{
				zoom = NO_ZOOM;
			}
			else if( zoom > MAX_ZOOM )
			{
				zoom = MAX_ZOOM;
			}

			long frequency = mOverlayPanel.getFrequencyFromAxis( e.getX() );

			double windowOffset = (double)e.getX() / (double)getWidth();

			setZoom( zoom, frequency, windowOffset );
		}
		
		@Override
        public void mouseMoved( MouseEvent event )
        {
			update( event );
        }
		@Override
        public void mouseDragged( MouseEvent event ) 
		{
			update( event );
		}
		
		private void update( MouseEvent event )
		{
			if( event.getComponent() == mOverlayPanel )
			{
				mOverlayPanel.setCursorLocation( event.getPoint() );
			}
			else
			{
				mWaterfallPanel.setCursorLocation( event.getPoint() );
				mWaterfallPanel.setCursorFrequency( 
				mOverlayPanel.getFrequencyFromAxis( event.getPoint().x ) );
			}
		}

		@Override
        public void mouseEntered( MouseEvent e ) 
		{
			if( e.getComponent() == mOverlayPanel )
			{
				mOverlayPanel.setCursorVisible( true );
			}
			else
			{
				mWaterfallPanel.setCursorVisible( true );
			}
		}
		
		@Override
        public void mouseExited( MouseEvent e )
		{
			mOverlayPanel.setCursorVisible( false );
			mWaterfallPanel.setCursorVisible( false );
		}

		/**
		 * Displays the context menu.
		 */
		@Override
        public void mouseClicked( MouseEvent event )
        {
			if( SwingUtilities.isRightMouseButton( event ) )
			{
				JPopupMenu contextMenu = new JPopupMenu();
				
				if( event.getComponent() == mWaterfallPanel )
				{
					contextMenu.add( new PauseItem( mWaterfallPanel, "Pause" ) );
				}

				long frequency = 
						mOverlayPanel.getFrequencyFromAxis( event.getX() );
				
				if( event.getComponent() == mOverlayPanel )
				{
					ArrayList<Channel> channels = 
							mOverlayPanel.getChannelsAtFrequency( frequency );

					for( Channel channel: channels )
					{
						JMenu channelMenu = ChannelUtils.getContextMenu( 
							mResourceManager.getPlaylistManager(), 
								channel, SpectralDisplayPanel.this );

						if( channelMenu != null )
						{
							contextMenu.add( channelMenu );
						}
					}

					if( !channels.isEmpty() )
					{
						contextMenu.add( new JSeparator() );
					}
					
				}

				JMenu frequencyMenu = new JMenu( 
						sCURSOR_FORMAT.format( (float)frequency / 1000000.0f ) );

				JMenu decoderMenu = new JMenu( "Add Decoder" );

				for( DecoderType type: DecoderType.getPrimaryDecoders() )
				{
					decoderMenu.add( new DecoderItem( mResourceManager, 
													  frequency, 
													  type ) );
					
				}
				
				frequencyMenu.add( decoderMenu );
				
				contextMenu.add( frequencyMenu );

				contextMenu.add( new JSeparator() );

				/**
				 * Color Menus
				 */
				JMenu colorMenu = new JMenu( "Color" );

				SettingsManager sm = mResourceManager.getSettingsManager();

				colorMenu.add( new ColorSettingMenuItem( sm, 
						ColorSettingName.CHANNEL_CONFIG ) );

				colorMenu.add( new ColorSettingMenuItem( sm, 
						ColorSettingName.CHANNEL_CONFIG_PROCESSING ) );

				colorMenu.add( new ColorSettingMenuItem( sm, 
						ColorSettingName.CHANNEL_CONFIG_SELECTED ) );

				colorMenu.add( new ColorSettingMenuItem( sm, 
						ColorSettingName.SPECTRUM_CURSOR ) );

				colorMenu.add( new ColorSettingMenuItem( sm, 
						ColorSettingName.SPECTRUM_LINE ) );

				colorMenu.add( new ColorSettingMenuItem( sm, 
						ColorSettingName.SPECTRUM_BACKGROUND ) );

				colorMenu.add( new ColorSettingMenuItem( sm, 
						ColorSettingName.SPECTRUM_GRADIENT_BOTTOM ) );

				colorMenu.add( new ColorSettingMenuItem( sm, 
						ColorSettingName.SPECTRUM_GRADIENT_TOP ) );

				contextMenu.add( colorMenu );
				
				/**
				 * Display items: fft and frame rate
				 */
				JMenu displayMenu = new JMenu( "Display" );
				contextMenu.add(  displayMenu );

				if( event.getComponent() != mWaterfallPanel )
				{
					/**
					 * Averaging menu
					 */
					JMenu averagingMenu = new JMenu( "Averaging" );
					averagingMenu.add( 
							new AveragingItem( mSpectrumPanel, 4 ) );
					displayMenu.add( averagingMenu );
					
					/**
					 * Channel Display setting menu
					 */
					JMenu channelDisplayMenu = new JMenu( "Channel" );

					channelDisplayMenu.add( new ChannelDisplayItem( 
							mOverlayPanel, ChannelDisplay.ALL ) );
					channelDisplayMenu.add( new ChannelDisplayItem( 
							mOverlayPanel, ChannelDisplay.ENABLED ) );
					channelDisplayMenu.add( new ChannelDisplayItem( 
							mOverlayPanel, ChannelDisplay.NONE ) );

					displayMenu.add( channelDisplayMenu );
				}

				
				/**
				 * FFT width
				 */
				JMenu fftWidthMenu = new JMenu( "FFT Width" );
				displayMenu.add( fftWidthMenu );
				
				for( DFTSize width: DFTSize.values() )
				{
					fftWidthMenu.add( new DFTSizeItem( SpectralDisplayPanel.this, width ) );
				}

				/**
				 * DFT Processor Frame Rate
				 */
				JMenu frameRateMenu = new JMenu( "Frame Rate" );
				displayMenu.add(  frameRateMenu );
				
				frameRateMenu.add( new FrameRateItem( mDFTProcessor, 14 ) );
				frameRateMenu.add( new FrameRateItem( mDFTProcessor, 16 ) );
				frameRateMenu.add( new FrameRateItem( mDFTProcessor, 18 ) );
				frameRateMenu.add( new FrameRateItem( mDFTProcessor, 20 ) );
				frameRateMenu.add( new FrameRateItem( mDFTProcessor, 25 ) );
				frameRateMenu.add( new FrameRateItem( mDFTProcessor, 30 ) );
				frameRateMenu.add( new FrameRateItem( mDFTProcessor, 40 ) );
				frameRateMenu.add( new FrameRateItem( mDFTProcessor, 50 ) );

				/**
				 * FFT Window Type
				 */
				JMenu fftWindowType = new JMenu( "Window Type" );
				displayMenu.add( fftWindowType );
				
				for( WindowType type: WindowType.values() )
				{
					fftWindowType.add( 
							new FFTWindowTypeItem( mDFTProcessor, type ) );
				}
				
				if( event.getComponent() != mWaterfallPanel )
				{
					/**
					 * Smoothing menu
					 */
					JMenu smoothingMenu = new JMenu( "Smoothing" );

					if( mSpectrumPanel.getSmoothingType() != SmoothingType.NONE )
					{
						smoothingMenu.add( new SmoothingItem( mSpectrumPanel, 5 ) );
						smoothingMenu.add( new JSeparator() );
					}
					smoothingMenu.add( new SmoothingTypeItem( mSpectrumPanel, SmoothingType.GAUSSIAN ) );
					smoothingMenu.add( new SmoothingTypeItem( mSpectrumPanel, SmoothingType.TRIANGLE ) );
					smoothingMenu.add( new SmoothingTypeItem( mSpectrumPanel, SmoothingType.RECTANGLE ) );
					smoothingMenu.add( new SmoothingTypeItem( mSpectrumPanel, SmoothingType.NONE ) );
					
					displayMenu.add( smoothingMenu );
				}

				/*
				 * Zoom menu 
				 */
				JMenuItem zoomMenu = new JMenu( "Zoom" );

				double windowOffset = (double)event.getX() / (double)getWidth();
				
				zoomMenu.add( new ZoomItem( frequency, windowOffset ) );
				
				contextMenu.add( zoomMenu );
				

				if( contextMenu != null )
				{
					if( event.getComponent() == mOverlayPanel )
					{
						contextMenu.show( mOverlayPanel, 
										  event.getX(), 
										  event.getY() );
					}
					else
					{
						contextMenu.show( mWaterfallPanel, 
										  event.getX(), 
										  event.getY() );
					}
				}				
			}
        }

		@Override
        public void mousePressed( MouseEvent e ) {}
		@Override
        public void mouseReleased( MouseEvent e ) {}
	}
	
	public class PauseItem extends JCheckBoxMenuItem
	{
        private static final long serialVersionUID = 1L;

        private Pausable mPausable;
        
        public PauseItem( Pausable pausable, String label )
        {
        	super( label );
        	
        	final boolean paused = pausable.isPaused();
        	
        	setSelected( paused );
        	
        	mPausable = pausable;
        	
        	addActionListener( new ActionListener() 
        	{
				@Override
                public void actionPerformed( ActionEvent e )
                {
					EventQueue.invokeLater( new Runnable() 
					{
						@Override
                        public void run()
                        {
							mPausable.setPaused( !paused );
                        }
					} );
                }
        	} );
        }
	}
	
	public class ZoomItem extends JSlider
	{
		private static final long serialVersionUID = 1L;

		private long mFrequency;
		private double mWindowOffset;
		
		public ZoomItem( long frequency, double windowOffset )
		{
			super( NO_ZOOM, MAX_ZOOM, mZoom );
			
			mFrequency = frequency;
			mWindowOffset = windowOffset;

			Hashtable<Integer,JComponent> labels = new Hashtable<>();
			labels.put( new Integer( 0 ), new JLabel( "1x" ) );
			labels.put( new Integer( 1 ), new JLabel( "2x" ) );
			labels.put( new Integer( 2 ), new JLabel( "4x" ) );
			labels.put( new Integer( 3 ), new JLabel( "8x" ) );
			labels.put( new Integer( 4 ), new JLabel( "16x" ) );
			labels.put( new Integer( 5 ), new JLabel( "32x" ) );
			labels.put( new Integer( 6 ), new JLabel( "64x" ) );
			
			setLabelTable( labels );
			
			setMajorTickSpacing( 1 );
			setMinorTickSpacing( 1 );
			setPaintTicks( true );
			setPaintLabels( true );
			
			this.addChangeListener( new ChangeListener() 
			{
				@Override
				public void stateChanged( ChangeEvent e )
				{
					setZoom( getValue(), mFrequency, mWindowOffset );
				}
			} );
		}
	}
	
	public class ChannelDisplayItem extends JCheckBoxMenuItem
	{
        private static final long serialVersionUID = 1L;
        
        private OverlayPanel mOverlayPanel;
        private ChannelDisplay mChannelDisplay;

        public ChannelDisplayItem( OverlayPanel panel, ChannelDisplay display )
        {
        	super( display.name() );

        	mOverlayPanel = panel;
        	
        	mChannelDisplay = display;
        	
        	setSelected( mOverlayPanel.getChannelDisplay() == mChannelDisplay );
        	
        	addActionListener( new ActionListener() 
        	{
				@Override
                public void actionPerformed( ActionEvent e )
                {
					EventQueue.invokeLater( new Runnable() 
					{
						@Override
                        public void run()
                        {
							mOverlayPanel.setChannelDisplay( mChannelDisplay );
                        }
					} );
                }
        	} );
        }
	}
	
	/**
	 * Context menu item to provide one-click access to starting a channel
	 * processing with the selected decoder
	 */
	public class DecoderItem extends JMenuItem
	{
        private static final long serialVersionUID = 1L;

        private ResourceManager mResourceManager;
        private long mFrequency;
        private DecoderType mDecoder;
        
        public DecoderItem( ResourceManager manager, long frequency, DecoderType type )
        {
        	super( type.getDisplayString() );
        	
        	mResourceManager = manager;
        	mFrequency = frequency;
        	mDecoder = type;
        	
        	addActionListener( new ActionListener() 
        	{
				@Override
                public void actionPerformed( ActionEvent e )
                {
					mResourceManager.getController()
							.createChannel( mFrequency, mDecoder );
                }
        	} );
        }
	}

	@Override
    public void settingDeleted( Setting setting ) {}
}
