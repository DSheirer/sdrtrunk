package ua.in.smartjava.spectrum;

import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import ua.in.smartjava.module.ProcessingChain;
import net.miginfocom.swing.MigLayout;
import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.SampleType;
import ua.in.smartjava.sample.complex.ComplexBuffer;
import ua.in.smartjava.sample.real.RealBuffer;
import settings.ColorSetting.ColorSettingName;
import settings.ColorSettingMenuItem;
import settings.Setting;
import settings.SettingChangeListener;
import settings.SettingsManager;
import ua.in.smartjava.source.tuner.frequency.FrequencyChangeEvent;
import ua.in.smartjava.source.tuner.frequency.FrequencyChangeEvent.Event;
import ua.in.smartjava.spectrum.converter.DFTResultsConverter;
import ua.in.smartjava.spectrum.converter.RealDecibelConverter;
import ua.in.smartjava.spectrum.menu.AveragingItem;
import ua.in.smartjava.spectrum.menu.DFTSizeItem;
import ua.in.smartjava.spectrum.menu.FFTWindowTypeItem;
import ua.in.smartjava.spectrum.menu.FrameRateItem;
import ua.in.smartjava.spectrum.menu.SmoothingItem;
import ua.in.smartjava.spectrum.menu.SmoothingTypeItem;
import ua.in.smartjava.controller.channel.Channel;
import ua.in.smartjava.controller.channel.ChannelEvent;
import ua.in.smartjava.controller.channel.ChannelEventListener;
import ua.in.smartjava.controller.channel.ChannelProcessingManager;
import ua.in.smartjava.dsp.filter.Filters;
import ua.in.smartjava.dsp.filter.Window.WindowType;
import ua.in.smartjava.dsp.filter.halfband.real.HalfBandFilter_RB_RB;
import ua.in.smartjava.dsp.filter.smoothing.SmoothingFilter.SmoothingType;

public class ChannelSpectrumPanel extends JPanel 
								  implements ChannelEventListener,
								  			 Listener<RealBuffer>,
								  			 SettingChangeListener,
								  			 SpectralDisplayAdjuster
{
	private static final long serialVersionUID = 1L;

    private DFTProcessor mDFTProcessor = new DFTProcessor( SampleType.REAL );    
    private DFTResultsConverter mDFTConverter = new RealDecibelConverter();
    private JLayeredPane mLayeredPane;
    private SpectrumPanel mSpectrumPanel;
    private ChannelOverlayPanel mOverlayPanel;
    
    private Channel mCurrentChannel;
    
    private int mSampleBufferSize = 2400;

    private HalfBandFilter_RB_RB mDecimatingFilter = new HalfBandFilter_RB_RB( 
		Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO.getCoefficients(), 1.0f, true );
    
    private AtomicBoolean mEnabled = new AtomicBoolean();
    
    private SettingsManager mSettingsManager;
    private ChannelProcessingManager mChannelProcessingManager;

    public ChannelSpectrumPanel( SettingsManager settingsManager, 
    							 ChannelProcessingManager channelProcessingManager )
    {
    	mSettingsManager = settingsManager;
    	mChannelProcessingManager = channelProcessingManager;
    	
    	if( mSettingsManager != null )
    	{
    		mSettingsManager.addListener( this );
    	}
    	
    	mSpectrumPanel = new SpectrumPanel( mSettingsManager );
    	mSpectrumPanel.setAveraging( 1 );

    	mOverlayPanel = new ChannelOverlayPanel( mSettingsManager );
    	mDFTProcessor.addConverter( mDFTConverter );
    	mDFTConverter.addListener( mSpectrumPanel );

    	/* Set the DFTProcessor to the decimated 24kHz ua.in.smartjava.sample rate */
    	mDFTProcessor.frequencyChanged( 
    			new FrequencyChangeEvent( Event.NOTIFICATION_SAMPLE_RATE_CHANGE, 24000 ) );
    	
    	initGui();
    }
    
    public void dispose()
    {
    	setEnabled( false );
    	
    	mDFTProcessor.dispose();

    	if( mSettingsManager != null )
    	{
    		mSettingsManager.removeListener( this );
    	}
    	
    	mSettingsManager = null;
    	mCurrentChannel = null;
    	mDFTProcessor = null;
    	mSpectrumPanel = null;
    }
    
    public void setFrameRate( int framesPerSecond )
    {
    	mSampleBufferSize = (int)( 48000 / framesPerSecond );
    	
    	mDFTProcessor.setFrameRate( framesPerSecond );
    }
    
    private void initGui()
    {
    	setLayout( new MigLayout( "insets 0 0 0 0 ", 
				  "[grow,fill]", 
				  "[grow,fill]") );
    	
    	mLayeredPane = new JLayeredPane();
    	mLayeredPane.addComponentListener( new ResizeListener() );
    	
    	MouseEventProcessor mouser = new MouseEventProcessor();
    	
    	mOverlayPanel.addMouseListener( mouser );
    	mOverlayPanel.addMouseMotionListener( mouser );
    	
    	mLayeredPane.add( mSpectrumPanel, new Integer( 0 ), 0 );
    	mLayeredPane.add( mOverlayPanel, new Integer( 1 ), 0 );

    	add( mLayeredPane );
    }
    
    public void setEnabled( boolean enabled )
    {
    	if( enabled && mEnabled.compareAndSet( false, true ) )
		{
    		start();
		}
    	else if( !enabled && mEnabled.compareAndSet( true, false ) )
    	{
    		stop();
    	}
    }

    @Override
	@SuppressWarnings( "incomplete-switch" )
    public void channelChanged( ChannelEvent event )
    {
		switch( event.getEvent() )
		{
			case NOTIFICATION_SELECTION_CHANGE:
				//ChannelSelectionManager ensures that only 1 ua.in.smartjava.channel can be
				//selected and any previously selected ua.in.smartjava.channel will be first
				//deselected before we get a new selection event
				if( event.getChannel().isSelected() )
				{
					if( mCurrentChannel != null )
					{
						stop();
						mCurrentChannel = null;
					}
					
					mCurrentChannel = event.getChannel();
					
					if( mEnabled.get() )
					{
						start();
					}
				}
				else
				{
					stop();
					mCurrentChannel = null;
				}
				break;
			case NOTIFICATION_PROCESSING_STOP:
				if( event.getChannel() == mCurrentChannel )
				{
					if( mEnabled.get() )
					{
						stop();
					}

					mCurrentChannel = null;
				}
				break;
		}
    }
	
	private void start()
	{
		if( mEnabled.get() && mCurrentChannel != null && mCurrentChannel.getEnabled() )
		{
			ProcessingChain processingChain = mChannelProcessingManager
					.getProcessingChain( mCurrentChannel );

			if( processingChain != null )
			{
				processingChain.addRealBufferListener( this );
				
				mDFTProcessor.start();
			}
		}
	}
	
	private void stop()
	{
		if( mCurrentChannel != null && mCurrentChannel.getEnabled() )
		{
			ProcessingChain processingChain = mChannelProcessingManager
					.getProcessingChain( mCurrentChannel );

			if( processingChain != null )
			{
				processingChain.removeRealBufferListener( this );
			}
		}

		mDFTProcessor.stop();
		
		mSpectrumPanel.clearSpectrum();
	}

	@Override
    public void settingChanged( Setting setting )
    {
		if( mSpectrumPanel != null )
		{
			mSpectrumPanel.settingChanged( setting );
		}
		if( mOverlayPanel != null )
		{
			mOverlayPanel.settingChanged( setting );
		}
    }

	@Override
    public void settingDeleted( Setting setting )
    {
		if( mSpectrumPanel != null )
		{
			mSpectrumPanel.settingDeleted( setting );
		}

		if( mOverlayPanel != null )
		{
			mOverlayPanel.settingDeleted( setting );
		}
    }

	@Override
    public void receive( RealBuffer buffer )
    {
		RealBuffer decimated = mDecimatingFilter.filter( buffer );

		//Hack: we're placing real samples in a complex ua.in.smartjava.buffer that the DFT
		//processor is expecting.
		mDFTProcessor.receive( new ComplexBuffer( decimated.getSamples() ) );
    }

	/**
	 * Monitors the sizing of the layered pane and resizes the ua.in.smartjava.spectrum and
	 * ua.in.smartjava.channel panels whenever the layered pane is resized
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
	 * Mouse event handler for the ua.in.smartjava.channel panel.
	 */
	public class MouseEventProcessor implements MouseMotionListener, MouseListener
	{
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
		}

		@Override
        public void mouseEntered( MouseEvent e ) 
		{
			if( e.getComponent() == mOverlayPanel )
			{
				mOverlayPanel.setCursorVisible( true );
			}
		}
		
		@Override
        public void mouseExited( MouseEvent e )
		{
			mOverlayPanel.setCursorVisible( false );
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
				
				/**
				 * Color Menus
				 */
				JMenu colorMenu = new JMenu( "Color" );

				colorMenu.add( new ColorSettingMenuItem( mSettingsManager, 
						ColorSettingName.SPECTRUM_CURSOR ) );

				colorMenu.add( new ColorSettingMenuItem( mSettingsManager, 
						ColorSettingName.SPECTRUM_LINE ) );

				colorMenu.add( new ColorSettingMenuItem( mSettingsManager, 
						ColorSettingName.SPECTRUM_BACKGROUND ) );

				colorMenu.add( new ColorSettingMenuItem( mSettingsManager, 
						ColorSettingName.SPECTRUM_GRADIENT_BOTTOM ) );

				colorMenu.add( new ColorSettingMenuItem( mSettingsManager, 
						ColorSettingName.SPECTRUM_GRADIENT_TOP ) );

				contextMenu.add( colorMenu );
				
				/**
				 * Display items: fft and frame rate
				 */
				JMenu displayMenu = new JMenu( "Display" );
				contextMenu.add(  displayMenu );

				/**
				 * Averaging menu
				 */
				JMenu averagingMenu = new JMenu( "Averaging" );
				averagingMenu.add( 
						new AveragingItem( ChannelSpectrumPanel.this, 2 ) );
				displayMenu.add( averagingMenu );
				
				/**
				 * FFT width
				 */
				JMenu fftWidthMenu = new JMenu( "FFT Width" );
				displayMenu.add( fftWidthMenu );
				
				for( DFTSize width: DFTSize.values() )
				{
					fftWidthMenu.add( new DFTSizeItem( mDFTProcessor, width ) );
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

				/**
				 * Smoothing menu
				 */
				JMenu smoothingMenu = new JMenu( "Smoothing" );

				if( mSpectrumPanel.getSmoothingType() != SmoothingType.NONE )
				{
					smoothingMenu.add( new SmoothingItem( ChannelSpectrumPanel.this, 5 ) );
					smoothingMenu.add( new JSeparator() );
				}
				
				smoothingMenu.add( new SmoothingTypeItem( ChannelSpectrumPanel.this, SmoothingType.GAUSSIAN ) );
				smoothingMenu.add( new SmoothingTypeItem( ChannelSpectrumPanel.this, SmoothingType.TRIANGLE ) );
				smoothingMenu.add( new SmoothingTypeItem( ChannelSpectrumPanel.this, SmoothingType.RECTANGLE ) );
				smoothingMenu.add( new SmoothingTypeItem( ChannelSpectrumPanel.this, SmoothingType.NONE ) );
				
				displayMenu.add( smoothingMenu );

				if( contextMenu != null )
				{
					contextMenu.show( mOverlayPanel, 
							  event.getX(), 
							  event.getY() );
				}				
			}
        }

		@Override
        public void mousePressed( MouseEvent e ) {}
		@Override
        public void mouseReleased( MouseEvent e ) {}
	}

	@Override
    public int getAveraging()
    {
	    return mSpectrumPanel.getAveraging();
    }

	@Override
    public void setAveraging( int averaging )
    {
		mSpectrumPanel.setAveraging( averaging );
    }

	public void setSampleSize( double sampleSize )
	{
		mSpectrumPanel.setSampleSize( sampleSize );
	}

	@Override
	public int getSmoothing()
	{
		return mSpectrumPanel.getSmoothing();
	}

	@Override
	public void setSmoothing( int smoothing )
	{
		mSpectrumPanel.setSmoothing( smoothing );
	}

	@Override
	public SmoothingType getSmoothingType()
	{
		return mSpectrumPanel.getSmoothingType();
	}

	@Override
	public void setSmoothingType( SmoothingType type )
	{
		mSpectrumPanel.setSmoothingType( type );
	}
}
