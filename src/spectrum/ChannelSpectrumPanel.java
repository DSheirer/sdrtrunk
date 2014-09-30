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
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import sample.Listener;
import settings.ColorSetting.ColorSettingName;
import settings.ColorSettingMenuItem;
import settings.Setting;
import settings.SettingChangeListener;
import settings.SettingsManager;
import source.Source.SampleType;
import source.tuner.FrequencyChangeEvent;
import source.tuner.FrequencyChangeEvent.Attribute;
import spectrum.converter.DFTResultsConverter;
import spectrum.converter.RealDecibelConverter;
import spectrum.menu.AmplificationItem;
import spectrum.menu.AveragingItem;
import spectrum.menu.BaselineItem;
import spectrum.menu.FFTWidthItem;
import spectrum.menu.FFTWindowTypeItem;
import spectrum.menu.FrameRateItem;
import controller.ResourceManager;
import controller.channel.Channel;
import controller.channel.ChannelEvent;
import controller.channel.ChannelEventListener;
import dsp.filter.Filters;
import dsp.filter.FloatHalfBandFilter;
import dsp.filter.Window.WindowType;

public class ChannelSpectrumPanel extends JPanel 
								  implements ChannelEventListener,
								  			 Listener<Float>,
								  			 SettingChangeListener,
								  			 SpectralDisplayAdjuster
{
    private static final long serialVersionUID = 1L;
    private static final String CHANNEL_SPECTRUM_AVERAGING_SIZE = 
    		"channel_spectrum_averaging_size";

    private ResourceManager mResourceManager;
    private DFTProcessor mDFTProcessor = new DFTProcessor( SampleType.FLOAT );    
    private DFTResultsConverter mDFTConverter = new RealDecibelConverter();
    private JLayeredPane mLayeredPane;
    private SpectrumPanel mSpectrumPanel;
    private ChannelOverlayPanel mOverlayPanel;
    
    private Channel mCurrentChannel;
    
    private int mSpectrumAveragingSize;
    private int mSampleBufferSize = 2400;
    private Float[] mSamples = new Float[ mSampleBufferSize ];
    private int mSamplePointer = 0;

    private DecimatingSampleAssembler mDecimatingSampleAssembler;
   
    private AtomicBoolean mEnabled = new AtomicBoolean();

    public ChannelSpectrumPanel( ResourceManager resourceManager )
    {
    	mResourceManager = resourceManager;
    	mResourceManager.getSettingsManager().addListener( this );
    	mSpectrumPanel = new SpectrumPanel( mResourceManager );
    	mOverlayPanel = new ChannelOverlayPanel( mResourceManager );
    	
    	
    	mSpectrumAveragingSize = 10;
//    	mSpectrumAveragingSize = mResourceManager.getSettingsManager()
//			.getIntegerSetting( CHANNEL_SPECTRUM_AVERAGING_SIZE, 10 ).getValue();
    	
    	mSpectrumPanel.setAveraging( mSpectrumAveragingSize );
 
    	mDFTProcessor.addConverter( mDFTConverter );
    	mDFTConverter.addListener( mSpectrumPanel );

    	mDecimatingSampleAssembler = new DecimatingSampleAssembler( mDFTProcessor );
    	
    	/* Set the DFTProcessor to the decimated 24kHz sample rate */
    	mDFTProcessor.frequencyChanged( 
    			new FrequencyChangeEvent( Attribute.SAMPLE_RATE, 24000 ) );
    	
    	initGui();
    }
    
    public void dispose()
    {
    	setEnabled( false );
    	
    	mDFTProcessor.dispose();

    	mResourceManager = null;
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
			case CHANGE_SELECTED:
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
			case CHANNEL_DELETED:
				if( event.getChannel() == mCurrentChannel )
				{
					if( mEnabled.get() )
					{
						stop();
					}

					mCurrentChannel = null;
				}
				break;
			case CHANGE_ENABLED:
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
			mCurrentChannel.getProcessingChain().addFloatListener( this );
			mDFTProcessor.start();
		}
	}
	
	private void stop()
	{
		
		if( mCurrentChannel != null && mCurrentChannel.isProcessing() )
		{
			mCurrentChannel.getProcessingChain().removeFloatListener( this );
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
    public void receive( Float sample )
    {
		mDecimatingSampleAssembler.receive( sample );
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

				SettingsManager sm = mResourceManager.getSettingsManager();

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

				/**
				 * Amplification menu
				 */
				JMenu amplificationMenu = new JMenu( "Amplification" );
				amplificationMenu.add( 
						new AmplificationItem( ChannelSpectrumPanel.this, 50 ) );
				displayMenu.add( amplificationMenu );
				
				/**
				 * Averaging menu
				 */
				JMenu averagingMenu = new JMenu( "Averaging" );
				averagingMenu.add( 
						new AveragingItem( ChannelSpectrumPanel.this, 4 ) );
				displayMenu.add( averagingMenu );
				
				/**
				 * Baseline menu
				 */
				JMenu baselineMenu = new JMenu( "Baseline" );
				baselineMenu.add( 
						new BaselineItem( ChannelSpectrumPanel.this, 50 ) );
				displayMenu.add( baselineMenu );
				
				/**
				 * FFT width
				 */
				JMenu fftWidthMenu = new JMenu( "FFT Width" );
				displayMenu.add( fftWidthMenu );
				
				for( FFTWidth width: FFTWidth.values() )
				{
					fftWidthMenu.add( new FFTWidthItem( mDFTProcessor, width ) );
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
    public int getAmplification()
    {
	    return mSpectrumPanel.getAmplification();
    }

	@Override
    public void setAmplification( int amplification )
    {
		mSpectrumPanel.setAmplification( amplification );
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

	@Override
    public int getBaseline()
    {
	    return mSpectrumPanel.getBaseline();
    }

	@Override
    public void setBaseline( int baseline )
    {
		mSpectrumPanel.setBaseline( baseline );
    }
	
	public class DecimatingSampleAssembler implements Listener<Float>
	{
		private FloatHalfBandFilter mDecimationFilter = new FloatHalfBandFilter( 
				Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.0002 );
		
		private SampleAssembler mSampleAssembler;

		public DecimatingSampleAssembler( Listener<Float[]> listener )
		{
			mSampleAssembler = new SampleAssembler( listener );
			mDecimationFilter.setListener( mSampleAssembler );
		}

		@Override
        public void receive( Float sample )
        {
			mDecimationFilter.receive( sample );
        }
	}
	
	public class SampleAssembler implements Listener<Float>
	{
	    private Listener<Float[]> mListener;

	    public SampleAssembler( Listener<Float[]> listener )
		{
	    	mListener = listener;
		}

		@Override
	    public void receive( Float sample )
	    {
			mSamples[ mSamplePointer++ ] = sample;

			if( mSamplePointer >= mSamples.length )
			{
				if( mEnabled.get() )
				{
					mListener.receive( Arrays.copyOf( mSamples, 
														  mSamples.length ) );
				}
				
				mSamplePointer = 0;
				
				if( mSamples.length != mSampleBufferSize )
				{
					mSamples = new Float[ mSampleBufferSize ];
				}
			}
	    }
	}
}
