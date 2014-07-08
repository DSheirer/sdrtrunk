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
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JPanel;

import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingChangeListener;
import source.tuner.FrequencyChangeListener;
import source.tuner.TunerChannel;
import controller.ResourceManager;
import controller.channel.Channel;
import controller.channel.Channel.ChannelEvent;
import controller.channel.Channel.ChannelType;
import controller.channel.ChannelListener;

public class OverlayPanel extends JPanel 
						   implements ChannelListener,
						   			  FrequencyChangeListener,
						   			  SettingChangeListener
{
	private static final long serialVersionUID = 1L;
	
	private static DecimalFormat sFORMAT = new DecimalFormat( "000.000" );
	private static DecimalFormat sCURSOR_FORMAT = new DecimalFormat( "000.00000" );
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

	//Defines the offset at the bottom of the spectral display to account for
	//the frequency labels
	private float mSpectrumInset = 20.0f;

	private ResourceManager mResourceManager;
	
	/**
	 * Translucent overlay panel for displaying channel configurations,
	 * processing channels, selected channels, frequency labels and lines, and 
	 * a cursor with a frequency readout.
	 */
	public OverlayPanel( ResourceManager resourceManager )
    {
		mResourceManager = resourceManager;
		
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

    		String frequency = sCURSOR_FORMAT.format( 
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

//TODO: the increment should be changeable according to the overall bandwidth
//   	so that when we move to higher sample rates, the display reacts accordingly

    	long frequency;
    	
    	if( mBandwidth < 200000 )
    	{
        	//Start with the first diplayable frequency (ie whole 10kHz increment)
    		frequency = minFrequency - ( minFrequency % 10000 );
        	frequency += 10000;

        	//Draw a line and label at each 10kHz interval
        	while( frequency < maxFrequency )
        	{
        		drawFrequencyLineAndLabel( graphics, frequency );
        		
        		frequency += 10000;
        	}
    	}
    	else
    	{
        	//Start with the first diplayable frequency (ie whole 100kHz increment)
    		frequency = minFrequency - ( minFrequency % 100000 );
        	frequency += 100000;

        	//Draw a line and label at each 10kHz interval
        	while( frequency < maxFrequency )
        	{
        		drawFrequencyLineAndLabel( graphics, frequency );
        		
        		frequency += 100000;
        	}
    	}
    }
    
    /**
     * Draws a vertical line and a corresponding frequency label at the bottom
     */
    private void drawFrequencyLineAndLabel( Graphics2D graphics, long frequency )
    {
    	float xAxis = getAxisFromFrequency( frequency );
    	
    	drawFrequencyLine( graphics, xAxis, mColorSpectrumLine );

    	graphics.setColor( mColorSpectrumLine );

    	drawFrequencyLabel( graphics, xAxis, frequency );
    }

    /**
     * Draws a vertical line at the xaxis
     */
    private void drawFrequencyLine( Graphics2D graphics, float xaxis, Color color )
    {
    	graphics.setColor( color );
    	
    	graphics.draw( new Line2D.Float( xaxis, 0, 
					 xaxis, (float)(getSize().getHeight()) - mSpectrumInset ) );
    }

    /**
     * Returns the x-axis value corresponding to the frequency
     */
    public float getAxisFromFrequency( long frequency )
    {
    	float canvasMiddle = (float)getSize().getWidth() / 2;

    	//Determine frequency offset from middle
    	long frequencyOffset = mFrequency - frequency;

    	//Determine ratio of offset to bandwidth
    	float ratio = (float)frequencyOffset / (float)mBandwidth;

    	//Calculate offset against the total width
    	float xOffset = (float)getSize().getWidth() * ratio;

    	//Apply the offset against the canvas middle
    	return canvasMiddle - xOffset;
    }

    /**
     * Returns the frequency corresponding to the x-axis value
     */
    public long getFrequencyFromAxis( double xAxis )
    {
    	float width = (float)getSize().getWidth();
    	
    	double offset = xAxis / width;
    	
    	return getMinFrequency() + (int)( mBandwidth * offset ); 
    }

    /**
     * Draws a frequency label at the x-axis position, at the bottom of the panel
     */
    private void drawFrequencyLabel( Graphics2D graphics, 
    								 float xaxis,
    								 long frequency )
    {
    	String label = sFORMAT.format( (float)frequency / 1000000.0f );
    	
    	FontMetrics fontMetrics   = graphics.getFontMetrics( this.getFont() );

    	Rectangle2D rect = fontMetrics.getStringBounds( label, graphics );

    	float offset  = (float)rect.getWidth() / 2;

    	graphics.drawString( label, xaxis - offset, 
    			(float)getSize().getHeight() - ( mSpectrumInset * 0.2f ) );
    }
    

    /**
     * Draws visible channel configs as translucent shaded frequency regions
     */
    private void drawChannels( Graphics2D graphics )
    {
    	for( Channel channel: mVisibleChannels )
    	{
    		//Choose the correct background color to use
    		if( channel.getSelected() )
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
                float xAxis = getAxisFromFrequency( 
                        (int)( tunerChannel.getFrequency() ) );

                float width = (float)( tunerChannel.getBandwidth() ) / 
                            (float)mBandwidth * (float)getSize().getWidth(); 
                
                Rectangle2D.Float box = 
                    new Rectangle2D.Float( xAxis - ( width / 2 ),
                                           0,
                                           width,
                                           (float)( getSize().getHeight() - 
                                                                mSpectrumInset ) );

                //Fill the box with the correct color
                graphics.fill( box );

                graphics.draw( box );

                //Change to the line color to render the channel name, etc.
                graphics.setColor( mColorSpectrumLine );

                //Draw the labels starting at yAxis position 0
                float yAxis = 0;
                
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
    	}
    }
    /**
     * Draws a textual label at the x/y position, clipping the end of the text
     * to fit within the maxwidth value.
     * 
     * @return height of the drawn label
     */
    private float drawLabel( Graphics2D graphics, String text, Font font, 
    						 float x, float baseY, float maxWidth )
    {
        FontMetrics fontMetrics = graphics.getFontMetrics( font );
        
        Rectangle2D label = fontMetrics.getStringBounds( text, graphics );
        
        float offset = (float)label.getWidth() / 2;
        float y = baseY + (float)label.getHeight();
        
        /**
         * If the label is wider than the max width, left justify the text and
         * clip the end of it
         */
        if( offset > ( maxWidth / 2 ) )
        {
        	label.setRect( x - ( maxWidth / 2 ), 		
        				   y - label.getHeight(), 
		   				   maxWidth, 
		   				   label.getHeight() );  //* 2
        	
        	graphics.setClip( label );		

            graphics.drawString( text, x - ( maxWidth / 2 ), y );
        	
        	graphics.setClip( null );
        }
        else
        {
            graphics.drawString( text, x - offset, y );
        }
        
        return (float)label.getHeight();
    }

    /**
     * Frequency change event handler
     */
	@Override
    public void frequencyChanged( long frequency, int bandwidth )
    {
		mBandwidth = bandwidth;
		mFrequency = frequency;
		
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
	public void occurred( Channel channel, ChannelEvent component )
	{
		switch( component )
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
}
