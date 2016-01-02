package spectrum;

import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.miginfocom.swing.MigLayout;
import sample.Listener;
import sample.SampleType;
import sample.complex.ComplexBuffer;
import sample.real.RealBuffer;
import sample.real.RealSampleListener;
import settings.ColorSetting.ColorSettingName;
import settings.ColorSettingMenuItem;
import settings.Setting;
import settings.SettingChangeListener;
import settings.SettingsManager;
import source.tuner.frequency.FrequencyChangeEvent;
import source.tuner.frequency.FrequencyChangeEvent.Event;
import spectrum.converter.DFTResultsConverter;
import spectrum.converter.RealDecibelConverter;
import spectrum.menu.AveragingItem;
import spectrum.menu.DFTSizeItem;
import spectrum.menu.FFTWindowTypeItem;
import spectrum.menu.FrameRateItem;
import spectrum.menu.SmoothingItem;
import spectrum.menu.SmoothingTypeItem;
import controller.ResourceManager;
import controller.channel.Channel;
import controller.channel.ChannelEvent;
import controller.channel.ChannelEventListener;
import dsp.filter.Filters;
import dsp.filter.FloatHalfBandFilter;
import dsp.filter.Window.WindowType;
import dsp.filter.halfband.real.HalfBandFilter_RB_RB;
import dsp.filter.hilbert.HilbertTransform;
import dsp.filter.smoothing.SmoothingFilter.SmoothingType;

public class ChannelSpectrumPanel extends JPanel 
								  implements ChannelEventListener,
								  			 Listener<RealBuffer>,
								  			 SettingChangeListener,
								  			 SpectralDisplayAdjuster
{
	private static final Logger mLog = LoggerFactory.getLogger( ChannelSpectrumPanel.class );

	private static final long serialVersionUID = 1L;

//    private ResourceManager mResourceManager;
    private DFTProcessor mDFTProcessor = new DFTProcessor( SampleType.REAL );    
    private DFTResultsConverter mDFTConverter = new RealDecibelConverter();
    private JLayeredPane mLayeredPane;
    private SpectrumPanel mSpectrumPanel;
    private ChannelOverlayPanel mOverlayPanel;
    
    private Channel mCurrentChannel;
    
    private int mSampleBufferSize = 2400;
    private float[] mSamples = new float[ mSampleBufferSize ];
    private int mSamplePointer = 0;

    private HalfBandFilter_RB_RB mDecimatingFilter = new HalfBandFilter_RB_RB( 
		Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO.getCoefficients(), 1.0f, true );
    
    private AtomicBoolean mEnabled = new AtomicBoolean();
    
    private SettingsManager mSettingsManager;

    public ChannelSpectrumPanel( SettingsManager settingsManager )
    {
    	mSettingsManager = settingsManager;
    	
    	if( mSettingsManager != null )
    	{
    		mSettingsManager.addListener( this );
    	}
    	
    	mSpectrumPanel = new SpectrumPanel( mSettingsManager );
    	mSpectrumPanel.setAveraging( 1 );

    	mOverlayPanel = new ChannelOverlayPanel( mSettingsManager );
    	mDFTProcessor.addConverter( mDFTConverter );
    	mDFTConverter.addListener( mSpectrumPanel );

    	/* Set the DFTProcessor to the decimated 24kHz sample rate */
    	mDFTProcessor.frequencyChanged( 
    			new FrequencyChangeEvent( Event.SAMPLE_RATE_CHANGE_NOTIFICATION, 24000 ) );
    	
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
				if( !event.getChannel().isSelected() )
				{
					stop();
					
					mCurrentChannel = null;
				}
				else
				{
					mCurrentChannel = event.getChannel();
					
					if( mEnabled.get() )
					{
						start();
					}
				}
				if( event.getChannel().isSelected() && 
					mCurrentChannel != event.getChannel() )
				{
					stop();
					
					mCurrentChannel = event.getChannel();
					
					start();
				}
				break;
			case REQUEST_DISABLE:
				if( event.getChannel() == mCurrentChannel )
				{
					if( mEnabled.get() )
					{
						stop();
					}

					mCurrentChannel = null;
				}
				break;
			case REQUEST_ENABLE:
				if( event.getChannel() == mCurrentChannel &&
					!event.getChannel().isProcessing() )
				{
					stop();
					mCurrentChannel = null;
				}
				break;
		}
    }
	
	private void start()
	{
		if( mEnabled.get() && mCurrentChannel != null && mCurrentChannel.isProcessing() )
		{
			mCurrentChannel.getProcessingChain().addRealBufferListener( this );
			mDFTProcessor.start();
		}
	}
	
	private void stop()
	{
		
		if( mCurrentChannel != null && mCurrentChannel.isProcessing() )
		{
			mCurrentChannel.getProcessingChain().removeRealBufferListener( this );
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

		//Hack: we're placing real samples in a complex buffer that the DFT
		//processor is expecting.
		mDFTProcessor.receive( new ComplexBuffer( decimated.getSamples() ) );
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
