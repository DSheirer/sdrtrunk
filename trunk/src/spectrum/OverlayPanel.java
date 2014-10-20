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
package spectrum;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingChangeListener;
import source.tuner.FrequencyChangeEvent;
import source.tuner.FrequencyChangeListener;
import source.tuner.TunerChannel;
import controller.ResourceManager;
import controller.channel.Channel;
import controller.channel.Channel.ChannelType;
import controller.channel.ChannelEvent;
import controller.channel.ChannelEventListener;

public class OverlayPanel extends JPanel 
						   implements ChannelEventListener,
						   			  FrequencyChangeListener,
						   			  SettingChangeListener
{
	private static final long serialVersionUID = 1L;
	
	private final static Logger mLog = 
						LoggerFactory.getLogger( OverlayPanel.class );

	private static DecimalFormat CURSOR_FORMAT = new DecimalFormat( "000.00000" );
	private DecimalFormat mFrequencyFormat = new DecimalFormat( "0.0" );
	private long mFrequency = 0;
	private int mBandwidth = 0;
	private Point mCursorLocation = new Point( 0, 0 );
	private boolean mCursorVisible = false;

	/**
	 * Colors used by this component
	 */
	private Color mColorChannelConfig;
	private Color mColorChannelConfigProcessing;
	private Color mColorChannelConfigSelected;
	private Color mColorSpectrumBackground;
	private Color mColorSpectrumCursor;
	private Color mColorSpectrumLine;

	//All channels
	private ArrayList<Channel> mChannels = new ArrayList<Channel>();

	//Currently visible/displayable channels
	private CopyOnWriteArrayList<Channel> mVisibleChannels = 
								new CopyOnWriteArrayList<Channel>();
	private ChannelDisplay mChannelDisplay = ChannelDisplay.ALL;

	//Defines the offset at the bottom of the spectral display to account for
	//the frequency labels
	private double mSpectrumInset = 20.0d;
	private LabelSizeMonitor mLabelSizeMonitor = new LabelSizeMonitor();

	private ResourceManager mResourceManager;
	
	/**
	 * Translucent overlay panel for displaying channel configurations,
	 * processing channels, selected channels, frequency labels and lines, and 
	 * a cursor with a frequency readout.
	 */
	public OverlayPanel( ResourceManager resourceManager )
    {
		mResourceManager = resourceManager;
		
		addComponentListener( mLabelSizeMonitor );
		
		//Set the background transparent, so the spectrum display can be seen
		setOpaque( false );

		//Fetch color settings from settings manager
		setColors();
    }
	
	public void dispose()
	{
		mChannels.clear();
		mVisibleChannels.clear();
		
		mResourceManager = null;
	}
	
	public ChannelDisplay getChannelDisplay()
	{
		return mChannelDisplay;
	}

	public void setChannelDisplay( ChannelDisplay display )
	{
		mChannelDisplay = display;
	}
	
	public void setCursorLocation( Point point )
	{
		mCursorLocation = point;
		
		repaint();
	}
	
	public void setCursorVisible( boolean visible )
	{
		mCursorVisible = visible;
		
		repaint();
	}
	
	/**
	 * Fetches the color settings from the settings manager
	 */
	private void setColors()
	{
		mColorChannelConfig = getColor( ColorSettingName.CHANNEL_CONFIG );

		mColorChannelConfigProcessing = 
				getColor( ColorSettingName.CHANNEL_CONFIG_PROCESSING ); 

		mColorChannelConfigSelected = 
				getColor( ColorSettingName.CHANNEL_CONFIG_SELECTED );

		mColorSpectrumCursor = getColor( ColorSettingName.SPECTRUM_CURSOR );

		mColorSpectrumLine = getColor( ColorSettingName.SPECTRUM_LINE );
		
		mColorSpectrumBackground = 
				getColor( ColorSettingName.SPECTRUM_BACKGROUND );
	}

	/**
	 * Fetches a named color setting from the settings manager.  If the setting
	 * doesn't exist, creates the setting using the defaultColor
	 */
	private Color getColor( ColorSettingName name )
	{
		ColorSetting setting = 
				mResourceManager.getSettingsManager()
				.getColorSetting( name );
		
		return setting.getColor();
	}

	/**
	 * Monitors for setting changes.  Colors can be changed by external actions
	 * and will automatically update in this class
	 */
	@Override
    public void settingChanged( Setting setting )
    {
		if( setting instanceof ColorSetting )
		{
			ColorSetting colorSetting = (ColorSetting)setting;
			
			switch( colorSetting.getColorSettingName() )
			{
				case CHANNEL_CONFIG:
					mColorChannelConfig = colorSetting.getColor();
					break;
				case CHANNEL_CONFIG_PROCESSING:
					mColorChannelConfigProcessing = colorSetting.getColor();
					break;
				case CHANNEL_CONFIG_SELECTED:
					mColorChannelConfigSelected = colorSetting.getColor();
					break;
				case SPECTRUM_BACKGROUND:
					mColorSpectrumBackground = colorSetting.getColor();
					break;
				case SPECTRUM_CURSOR:
					mColorSpectrumCursor = colorSetting.getColor();
					break;
				case SPECTRUM_LINE:
					mColorSpectrumLine = colorSetting.getColor();
					break;
				default:
					break;
			}
		}
    }
	
	/**
	 * Renders the channel configs, lines, labels, and cursor
	 */
    @Override
    public void paintComponent( Graphics g )
    {
    	super.paintComponent( g );
    	
    	Graphics2D graphics = (Graphics2D) g;
    	graphics.setBackground( mColorSpectrumBackground );

        RenderingHints renderHints = 
        		new RenderingHints( RenderingHints.KEY_ANTIALIASING, 
        							RenderingHints.VALUE_ANTIALIAS_ON );

        renderHints.put( RenderingHints.KEY_RENDERING, 
        				 RenderingHints.VALUE_RENDER_QUALITY );

        graphics.setRenderingHints( renderHints );
        
    	drawFrequencies( graphics );
    	drawChannels( graphics );
    	drawCursor( graphics );
    }
    
    /**
     * Draws a cursor on the panel, whenever the mouse is hovering over the 
     * panel
     */
    private void drawCursor( Graphics2D graphics )
    {
    	if( mCursorVisible )
    	{
    		drawFrequencyLine( graphics, 
    						   mCursorLocation.x, 
    						   mColorSpectrumCursor );

    		String frequency = CURSOR_FORMAT.format( 
				getFrequencyFromAxis( mCursorLocation.getX() ) / 1000000.0D );

    		graphics.drawString( frequency , 
    							 mCursorLocation.x + 5, 
    							 mCursorLocation.y );
    	}
    }
    
    /**
     * Draws the frequency lines and labels every 10kHz
     */
    private void drawFrequencies( Graphics2D graphics )
    {
    	long minFrequency = getMinFrequency();
    	long maxFrequency = getMaxFrequency();

    	int major = mLabelSizeMonitor.getMajorTickIncrement( graphics );
    	int minor = mLabelSizeMonitor.getMinorTickIncrement( graphics );
    	int label = mLabelSizeMonitor.getLabelIncrement( graphics );
    	
    	long start = minFrequency - ( minFrequency % label );
    	
    	long frequency = start;
    	
    	while( frequency < maxFrequency )
    	{
    		int offset = (int)( frequency - start );
    		
    		if( offset % label == 0  )
    		{
        		drawFrequencyLineAndLabel( graphics, frequency );
    		}
    		else if( offset % major == 0 )
    		{
    			drawTickLine( graphics, frequency, true );
    		}
    		else
    		{
    			drawTickLine( graphics, frequency, false );
    		}
    		
    		frequency += minor;
    	}
    }
    
    /**
     * Draws a vertical line and a corresponding frequency label at the bottom
     */
    private void drawFrequencyLineAndLabel( Graphics2D graphics, long frequency )
    {
    	double xAxis = getAxisFromFrequency( frequency );
    	
    	drawFrequencyLine( graphics, xAxis, mColorSpectrumLine );
    	
    	drawTickLine( graphics, frequency, false );

    	graphics.setColor( mColorSpectrumLine );

    	drawFrequencyLabel( graphics, xAxis, frequency );
    }

    /**
     * Draws a vertical line at the xaxis
     */
    private void drawTickLine( Graphics2D graphics, long frequency, boolean major )
    {
    	graphics.setColor( mColorSpectrumLine );
    	
    	double xAxis = getAxisFromFrequency( frequency );

    	double start = getSize().getHeight() - mSpectrumInset;
    	double end = start + ( major ? 6.0d : 3.0d );
    	
    	graphics.draw( new Line2D.Double( xAxis, start, xAxis, end ) ); 
    }


    /**
     * Draws a vertical line at the xaxis
     */
    private void drawFrequencyLine( Graphics2D graphics, double xaxis, Color color )
    {
    	graphics.setColor( color );
    	
    	graphics.draw( new Line2D.Double( xaxis, 0.0d, 
					 xaxis, getSize().getHeight() - mSpectrumInset ) );
    }

    /**
     * Draws a vertical line at the xaxis
     */
    private void drawAFC( Graphics2D graphics, double xaxis, boolean isError )
    {
    	double height = getSize().getHeight() - mSpectrumInset;

    	if( isError )
    	{
        	graphics.setColor( Color.YELLOW );

        	graphics.draw( new Line2D.Double( xaxis, height * 0.75d, 
					 xaxis, height - 1.0d ) );
    	}
    	else
    	{
        	graphics.setColor( Color.LIGHT_GRAY );

        	graphics.draw( new Line2D.Double( xaxis, height * 0.65d, 
					 xaxis, height - 1.0d ) );
    	}
    }
    
    /**
     * Returns the x-axis value corresponding to the frequency
     */
    public double getAxisFromFrequency( long frequency )
    {
    	double canvasMiddle = getSize().getWidth() / 2.0d;

    	//Determine frequency offset from middle
    	double frequencyOffset = mFrequency - frequency;

    	//Determine ratio of offset to bandwidth
    	double ratio = frequencyOffset / (double)mBandwidth;

    	//Calculate offset against the total width
    	double xOffset = getSize().getWidth() * ratio;

    	//Apply the offset against the canvas middle
    	return canvasMiddle - xOffset;
    }

    /**
     * Returns the frequency corresponding to the x-axis value
     */
    public long getFrequencyFromAxis( double xAxis )
    {
    	double width = getSize().getWidth();
    	
    	double offset = xAxis / width;
    	
    	return getMinFrequency() + Math.round( (double)mBandwidth * offset ); 
    }

    /**
     * Draws a frequency label at the x-axis position, at the bottom of the panel
     */
    private void drawFrequencyLabel( Graphics2D graphics, 
    								 double xaxis,
    								 long frequency )
    {
    	String label = mFrequencyFormat.format( (float)frequency / 1000000.0f );
    	
    	FontMetrics fontMetrics   = graphics.getFontMetrics( this.getFont() );

    	Rectangle2D rect = fontMetrics.getStringBounds( label, graphics );

    	float xOffset  = (float)rect.getWidth() / 2;

//    	graphics.drawString( label, (float)( xaxis - xOffset ), 
//			(float)( getSize().getHeight() - ( mSpectrumInset * 0.2d ) ) );
    	graphics.drawString( label, (float)( xaxis - xOffset ), 
			(float)( getSize().getHeight() - 2.0f ) );
    }
    

    /**
     * Draws visible channel configs as translucent shaded frequency regions
     */
    private void drawChannels( Graphics2D graphics )
    {
    	for( Channel channel: mVisibleChannels )
    	{
    		if( mChannelDisplay == ChannelDisplay.ALL ||
    			( mChannelDisplay == ChannelDisplay.ENABLED && 
    			  channel.isProcessing() ) )
    		{
        		//Choose the correct background color to use
        		if( channel.isSelected() )
        		{
                	graphics.setColor( mColorChannelConfigSelected );
        		}
        		else if( channel.isProcessing() )
        		{
                	graphics.setColor( mColorChannelConfigProcessing );
        		}
        		else
        		{
                	graphics.setColor( mColorChannelConfig );
        		}
            	
        	    TunerChannel tunerChannel = channel.getTunerChannel();
        	    
                if( tunerChannel != null )
                {
                	double xAxis = getAxisFromFrequency( tunerChannel.getFrequency() );

                	double width = (double)( tunerChannel.getBandwidth() ) / 
                                (double)mBandwidth * getSize().getWidth(); 
                    
                    Rectangle2D.Double box = 
                        new Rectangle2D.Double( xAxis - ( width / 2.0d ),
                    		0.0d, width, getSize().getHeight() - mSpectrumInset );

                    //Fill the box with the correct color
                    graphics.fill( box );

                    graphics.draw( box );

                    //Change to the line color to render the channel name, etc.
                    graphics.setColor( mColorSpectrumLine );

                    //Draw the labels starting at yAxis position 0
                    double yAxis = 0;
                    
                    //Draw the system label and adjust the y-axis position
                    yAxis += drawLabel( graphics, 
                    				   	channel.getSystem().getName(),
                    				   	this.getFont(),
                    				   	xAxis,
                    				   	yAxis,
                    				   	width );

                    //Draw the site label and adjust the y-axis position
                    yAxis += drawLabel( graphics, 
                    					channel.getSite().getName(),
                    					this.getFont(),
                    					xAxis,
                    					yAxis,
                    					width );

                    //Draw the channel label and adjust the y-axis position
                    yAxis += drawLabel( graphics, 
                    					channel.getName(),
                    					this.getFont(),
                    					xAxis,
                    					yAxis,
                    					width );
                    
                    //Draw the decoder label
                    drawLabel( graphics, 
                    		channel.getDecodeConfiguration().getDecoderType()
                    			.getShortDisplayString(),
                    		   this.getFont(),
                    		   xAxis,
                    		   yAxis,
                    		   width );
                }
                
                /* Draw Automatic Frequency Control line */
                if( channel.hasAFC() )
                {
                	int frequency = (int)tunerChannel.getFrequency();
                	
                	int error = frequency + channel.getAFC().getErrorCorrection();
                	
                    drawAFC( graphics, getAxisFromFrequency( frequency ), false );

                    drawAFC( graphics, getAxisFromFrequency( error ), true );
                }
    		}
    	}
    }
    
    
    /**
     * Draws a textual label at the x/y position, clipping the end of the text
     * to fit within the maxwidth value.
     * 
     * @return height of the drawn label
     */
    private double drawLabel( Graphics2D graphics, String text, Font font, 
    						 double x, double baseY, double maxWidth )
    {
        FontMetrics fontMetrics = graphics.getFontMetrics( font );
        
        Rectangle2D label = fontMetrics.getStringBounds( text, graphics );
        
        double offset = label.getWidth() / 2.0d;
        double y = baseY + label.getHeight();
        
        /**
         * If the label is wider than the max width, left justify the text and
         * clip the end of it
         */
        if( offset > ( maxWidth / 2.0d ) )
        {
        	label.setRect( x - ( maxWidth / 2.0d ), 		
        				   y - label.getHeight(), 
		   				   maxWidth, 
		   				   label.getHeight() );
        	
        	graphics.setClip( label );		

            graphics.drawString( text, 
            		(float)( x - ( maxWidth / 2.0d ) ), (float)y );
        	
        	graphics.setClip( null );
        }
        else
        {
            graphics.drawString( text, (float)( x - offset ), (float)y );
        }
        
        return label.getHeight();
    }

    /**
     * Frequency change event handler
     */
	@Override
    public void frequencyChanged( FrequencyChangeEvent event )
    {
		mLabelSizeMonitor.frequencyChanged( event );
		
		switch( event.getAttribute() )
		{
			case SAMPLE_RATE:
				mBandwidth = (int)event.getValue();
				
				if( mBandwidth < 200000 )
				{
					mFrequencyFormat = new DecimalFormat( "0.00" );					
				}
				else
				{
					mFrequencyFormat = new DecimalFormat( "0.0" );
				}
				break;
			case FREQUENCY:
				mFrequency = event.getValue();
				break;
			default:
				break;
		}
		
		/**
		 * Reset the visible channel configs list
		 */
		mVisibleChannels.clear();

		long minimum = getMinFrequency();
		long maximum = getMaxFrequency();
		
		for( Channel channel: mChannels )
		{
			if( channel.isWithin( minimum, maximum ) )
			{
				mVisibleChannels.add( channel );
			}
		}
    }

	/**
	 * Channel change event handler
	 */
    @Override
	@SuppressWarnings( "incomplete-switch" )
	public void channelChanged( ChannelEvent event )
	{
    	Channel channel = event.getChannel();
    	
		switch( event.getEvent() )
		{
			case CHANNEL_ADDED:
				if( !mChannels.contains( channel ) )
				{
					mChannels.add( channel );
				}
				
				if( channel.isWithin( getMinFrequency(), getMaxFrequency() ) && 
					!mVisibleChannels.contains( channel ) )
				{
					mVisibleChannels.add( channel );
				}
				break;
			case CHANNEL_DELETED:
				mChannels.remove( channel );
				mVisibleChannels.remove( channel );
				break;
			case PROCESSING_STOPPED:
				if( channel.getChannelType() == ChannelType.TRAFFIC )
				{
					mChannels.remove( channel );
					mVisibleChannels.remove( channel );
				}
				break;
		}
		
		repaint();
	}

	/**
	 * Currently displayed minimum frequency
	 */
	private long getMinFrequency()
	{
		return mFrequency - ( mBandwidth / 2 );
	}

	/**
	 * Currently displayed maximum frequency
	 */
	private long getMaxFrequency()
	{
		return mFrequency + ( mBandwidth / 2 );
	}

	/**
	 * Returns a list of channel configs that contain the frequency within their
	 * min/max frequency settings.
	 */
	public ArrayList<Channel> getChannelsAtFrequency( long frequency )
	{
		ArrayList<Channel> configs = new ArrayList<Channel>();
		
		for( Channel config: mVisibleChannels )
		{
			TunerChannel channel = config.getTunerChannel();
			
			if( channel != null &&
				channel.getMinFrequency() <= frequency &&
				channel.getMaxFrequency() >= frequency )
			{
				configs.add( config );
			}
		}
		
		return configs;
	}

	@Override
    public void settingDeleted( Setting setting )
    {
	    // TODO Auto-generated method stub
    }

	/**
	 * Monitors the display for resize events so that we can calculate how many
	 * frequency labels will fit within the current screen real estate
	 */
	public class LabelSizeMonitor implements ComponentListener, 
											 FrequencyChangeListener
	{
		private static final int MAJOR_TICK_MINIMUM = 10000; //10 kHz
		private static final int MINOR_TICK_MINIMUM = 1000; //1 kHz
		private static final int TICK_SPACING_MINIMUM = 10; //pixels
		
		private boolean mUpdateRequired = true;
		private LabelDisplay mLabelDisplay = LabelDisplay.DIGIT_3;
		private int mMajorTickIncrement;
		private int mMinorTickIncrement;
		private int mLabelIncrement;

		private void update( Graphics2D graphics )
		{
			if( mUpdateRequired )
			{
				double width = OverlayPanel.this.getSize().getWidth();

				int major = MAJOR_TICK_MINIMUM;

				while( width / ( (double)mBandwidth / 
						(double)major ) < TICK_SPACING_MINIMUM )
				{
					major *= 10;
				}
				
				mMajorTickIncrement = major;
				
				int minor = MINOR_TICK_MINIMUM;

				while( width / ( (double)mBandwidth / 
						(double)minor ) < TICK_SPACING_MINIMUM )
				{
					minor *= 10;
				}
				
				if( minor == major )
				{
					minor = (int)( major / 2 );
				}
				
				mMinorTickIncrement = minor;
				
		        FontMetrics fontMetrics = 
		        		graphics.getFontMetrics( OverlayPanel.this.getFont() );
		        
		        Rectangle2D labelDimension = fontMetrics.getStringBounds( 
		        		mLabelDisplay.getExample(), graphics );
				
		        int maxLabelCount = (int)( width / labelDimension.getWidth() );
		        
		        int label = major;
		        
				while( ( (double)mBandwidth / (double)label ) > maxLabelCount )
				{
					label += major;
				}
		        
				mLabelIncrement = label;
				
		        mUpdateRequired = false;
			}
		}

		public int getMajorTickIncrement( Graphics2D graphics )
		{
			update( graphics );
			
			return mMajorTickIncrement;
		}
		
		public int getMinorTickIncrement( Graphics2D graphics )
		{
			update( graphics );
			
			return mMinorTickIncrement;
		}
		
		public int getLabelIncrement( Graphics2D graphics )
		{
			update( graphics );
			
			return mLabelIncrement;
		}
		
		public LabelDisplay getLabelDisplay()
		{
			return mLabelDisplay;
		}
		
		@Override
        public void componentResized( ComponentEvent arg0 )
        {
			mUpdateRequired = true;
        }

		public void componentHidden( ComponentEvent arg0 ) {}
        public void componentMoved( ComponentEvent arg0 ) {}
        public void componentShown( ComponentEvent arg0 ) {}

		@Override
        public void frequencyChanged( FrequencyChangeEvent event )
        {
			switch( event.getAttribute() )
			{
				case FREQUENCY:
					LabelDisplay display = 
								LabelDisplay.fromFrequency( event.getValue() );
					
					if( mLabelDisplay != display )
					{
						mLabelDisplay = display;
						mUpdateRequired = true;
					}
					break;
				case SAMPLE_RATE:
					mUpdateRequired = true;
					break;
				default:
					break;
			}
        }
	}

	/**
	 * Frequency display formats for determining label sizing and value formatting
	 */
	public enum LabelDisplay
	{
		DIGIT_1( " 9.9 " ),
		DIGIT_2( " 99.9 " ),
		DIGIT_3( " 999.9 " ),
		DIGIT_4( " 9999.9 " ),
		DIGIT_5( " 99999.9 " );
		
		private String mExample;
		
		private LabelDisplay( String example )
		{
			mExample = example;
		}
		
		public String getExample()
		{
			return mExample;
		}
		
		public static LabelDisplay fromFrequency( long frequency )
		{
			if( frequency < 10000000l ) //10 MHz
			{
				return LabelDisplay.DIGIT_1;
			}
			else if( frequency < 100000000l ) //100 MHz
			{
				return LabelDisplay.DIGIT_2;
			}
			else if( frequency < 1000000000l ) //1,000 MHz
			{
				return LabelDisplay.DIGIT_3;
			}
			else if( frequency < 10000000000l ) //10,000 MHz
			{
				return LabelDisplay.DIGIT_4;
			}

			return LabelDisplay.DIGIT_5;
		}
	}
	
	public enum ChannelDisplay
	{
		ALL, ENABLED, NONE;
	}
}
