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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;

import javax.swing.JPanel;

import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingChangeListener;
import settings.SettingsManager;

public class ChannelOverlayPanel extends JPanel implements SettingChangeListener
{
	private static final long serialVersionUID = 1L;
	
	private static DecimalFormat sFORMAT = new DecimalFormat( "000.0" );
	private static DecimalFormat sCURSOR_FORMAT = new DecimalFormat( "000.0" );
	private double mBandwidth = 6000.0d;
	private Point mCursorLocation = new Point( 0, 0 );
	private boolean mCursorVisible = false;

	/**
	 * Colors used by this component
	 */
	private Color mColorSpectrumBackground;
	private Color mColorSpectrumCursor;
	private Color mColorSpectrumLine;

	//Defines the offset at the bottom of the spectral display to account for
	//the frequency labels
	private float mSpectrumInset = 20.0f;

	private SettingsManager mSettingsManager;
	
	/**
	 * Translucent overlay panel for displaying channel configurations,
	 * processing channels, selected channels, frequency labels and lines, and 
	 * a cursor with a frequency readout.
	 */
	public ChannelOverlayPanel( SettingsManager settingsManager )
    {
		mSettingsManager = settingsManager;
		
		if( mSettingsManager != null )
		{
			mSettingsManager.addListener( this );
		}
		
		//Set the background transparent, so the spectrum display can be seen
		setOpaque( false );

		//Fetch color settings from settings manager
		setColors();
    }
	
	public void dispose()
	{
		if( mSettingsManager != null )
		{
			mSettingsManager.removeListener( this );
		}
		
		mSettingsManager = null;
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
		ColorSetting setting = mSettingsManager.getColorSetting( name );
		
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

    		double value = mBandwidth * 
    				( mCursorLocation.getX() / getSize().getWidth() );
    		
    		String frequency = sCURSOR_FORMAT.format( value );

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
    	drawFrequencyLine( graphics, getAxisFromFrequency( 1000 ), Color.LIGHT_GRAY );
    	drawFrequencyLineAndLabel( graphics, 2000 );
    	drawFrequencyLine( graphics, getAxisFromFrequency( 3000 ), Color.LIGHT_GRAY );
		drawFrequencyLineAndLabel( graphics, 4000 );
    	drawFrequencyLine( graphics, getAxisFromFrequency( 5000 ), Color.LIGHT_GRAY );
//		drawFrequencyLineAndLabel( graphics, 6000 );
//    	drawFrequencyLine( graphics, getAxisFromFrequency( 7000 ), Color.LIGHT_GRAY );
//		drawFrequencyLineAndLabel( graphics, 8000 );
//    	drawFrequencyLine( graphics, getAxisFromFrequency( 9000 ), Color.LIGHT_GRAY );
//		drawFrequencyLineAndLabel( graphics, 10000 );
//    	drawFrequencyLine( graphics, getAxisFromFrequency( 11000 ), Color.LIGHT_GRAY );
    }
    
    private float getAxisFromFrequency( long frequency )
    {
    	return (float)( getSize().getWidth() * ( (double)frequency / mBandwidth ) );
    }
    
    /**
     * Draws a vertical line and a corresponding frequency label at the bottom
     */
    private void drawFrequencyLineAndLabel( Graphics2D graphics, long frequency )
    {
    	float xAxis = (float)( getSize().getWidth() * 
    			( (double)frequency / mBandwidth ) );

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
     * Draws a frequency label at the x-axis position, at the bottom of the panel
     */
    private void drawFrequencyLabel( Graphics2D graphics, 
    								 float xaxis,
    								 long frequency )
    {
    	String label = sFORMAT.format( (float)frequency );
    	
    	FontMetrics fontMetrics   = graphics.getFontMetrics( this.getFont() );

    	Rectangle2D rect = fontMetrics.getStringBounds( label, graphics );

    	float offset  = (float)rect.getWidth() / 2;

    	graphics.drawString( label, xaxis - offset, 
    			(float)getSize().getHeight() - ( mSpectrumInset * 0.2f ) );
    }

    public void settingDeleted( Setting setting ) {}
}
