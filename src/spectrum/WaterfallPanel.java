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
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Line2D;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingChangeListener;
import controller.ResourceManager;

/**
 * Produces a JPanel filled with a waterfall image derived from a continuous
 * stream of received Discrete Fourier Transform (DFT) results from the
 * DFTProcessor class.
 */
public class WaterfallPanel extends JPanel implements DFTResultsListener,
													  Pausable,
													  SettingChangeListener
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( WaterfallPanel.class );

	private static final long serialVersionUID = 1L;
	private static DecimalFormat CURSOR_FORMAT = new DecimalFormat( "0.00000" );
	private static final String SPECTRUM_CURSOR = "spectrum_cursor";
	private static final String PAUSED = "PAUSED";

	private byte[] mPixels;
    private int mDFTSize = 4096;
    private int mImageHeight = 700;
    private MemoryImageSource mMemoryImageSource;
    private ColorModel mColorModel = WaterfallColorModel.getDefaultColorModel();
	private Color mColorSpectrumCursor;
    private Image mWaterfallImage;

	private Point mCursorLocation = new Point( 0, 0 );
	private boolean mCursorVisible = false;
	private long mCursorFrequency = 0;
	private AtomicBoolean mPaused = new AtomicBoolean();
	private int mZoom = 0;
	private int mDFTZoomOffset = 0;
	
	private ResourceManager mResourceManager;
    
	public WaterfallPanel( ResourceManager resourceManager )
	{
		super();

		mResourceManager = resourceManager;
		
		mColorSpectrumCursor = getColor( ColorSettingName.SPECTRUM_CURSOR );

		reset();
	}
	
	public void dispose()
	{
		mResourceManager = null;
		mMemoryImageSource = null;
	}
	
	private void reset()
	{
		mPixels = new byte[ mDFTSize * mImageHeight ];

		mMemoryImageSource = new MemoryImageSource( mDFTSize, 
				mImageHeight,
				mColorModel,
				mPixels,
				0,
				mDFTSize );
		
        mMemoryImageSource.setAnimated( true );

        mWaterfallImage = createImage( mMemoryImageSource );
	}
	
	/**
	 * Pausable interface - pauses updates to the screen
	 */
	public void setPaused( boolean paused )
	{
		mPaused.set( paused );
		
		repaint();
	}
	
	public boolean isPaused()
	{
		return mPaused.get();
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
     * 
     * @param zoom level, 0 - 5.
     * @param offset into the DFT bins for display
     */
	public void setZoom( int zoom, int offset )
	{
		mZoom = zoom;
		mDFTZoomOffset = offset;
	}
	
	/**
	 * Fetches a named color setting from the settings manager.  If the setting
	 * doesn't exist, creates the setting using the defaultColor
	 */
	private Color getColor( ColorSettingName name )
	{
		ColorSetting setting = mResourceManager.getSettingsManager()
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

			if( ( (ColorSetting) setting ).getColorSettingName() == 
					ColorSettingName.SPECTRUM_CURSOR )
			{
				mColorSpectrumCursor = colorSetting.getColor();
			}
		}
    }

	public void setCursorLocation( Point point )
	{
		mCursorLocation = point;
		
		repaint();
	}
	
	public void setCursorFrequency( long frequency )
	{
		mCursorFrequency = frequency;
	}
	
	public void setCursorVisible( boolean visible )
	{
		mCursorVisible = visible;
		
		repaint();
	}
	
	private int getPixelOffset( int multiplier )
	{
		if( mZoom != 0 )
		{
			return -(int)( (double)getWidth() * multiplier / 
					   	   (double)mDFTSize * 
					   	   (double)( mDFTZoomOffset ) );
		}
		
		return 0;
	}

	public void paintComponent( Graphics g )
	{
		int multiplier = (int)Math.pow( 2.0, mZoom );

		int offset = getPixelOffset( multiplier );
		
		g.drawImage( mWaterfallImage, 
					 offset, 
					 0, 
					 getWidth() * multiplier, 
					 mImageHeight, 
					 this );
		
    	Graphics2D graphics = (Graphics2D) g;

    	if( mCursorVisible )
    	{
    		
        	graphics.setColor( mColorSpectrumCursor );
        	
        	graphics.draw( new Line2D.Float( mCursorLocation.x, 
        									 0, 
        									 mCursorLocation.x, 
        									 (float)(getSize().getHeight() ) ) );

    		String frequency = CURSOR_FORMAT.format( mCursorFrequency / 1000000.0D );

    		graphics.drawString( frequency , 
    							 mCursorLocation.x + 5, 
    							 mCursorLocation.y );
    		
        	if( mZoom != 0 )
        	{
        		graphics.drawString( "Zoom: " + (int)Math.pow( 2.0, mZoom ) + "x", 
    				mCursorLocation.x + 17, mCursorLocation.y + 11 ); 
        	}
    	}
    	
    	if( mPaused.get() )
    	{
    		graphics.drawString( PAUSED, 20, 20 ); 
    	}
    	
		graphics.dispose();
	}

	@Override
    public void receive( float[] update )
    {
		//If our FFT size changes, reset our pixel map and image source
		if( mDFTSize != update.length )
		{
			mDFTSize = update.length;

			reset();
		}

		//Move the pixels down a row to make room for the new results
		System.arraycopy( mPixels, 0, 
				mPixels, mDFTSize, mPixels.length - mDFTSize );
		
		/**
		 * Find the average value and scale the display to it
		 */
		double sum = 0.0d;
		
		for( int x = 0; x < update.length - 1; x++ )
		{
			sum += update[ x ];
		}
		
		float average = (float)( sum / (double)update.length - 1 );

		float scale = 256.0f / average;
		
		for( int x = 0; x < update.length - 1; x++ )
		{
			float value = ( average - update[ x ] ) * scale;

			if( value < 0 )
			{
				mPixels[ x ] = 0;
			}
			else if( value > 255 )
			{
				mPixels[ x ] = (byte)255;
			}
			else
			{
				mPixels[ x ] = (byte)value;
			}
		}
		
		if( !mPaused.get() )
		{
			EventQueue.invokeLater( new Runnable() 
			{
				@Override
	            public void run()
	            {
					if( mMemoryImageSource != null )
					{
						mMemoryImageSource.newPixels( mPixels, mColorModel, 0, mDFTSize );
					}
	            }
			} );
		}
    }
	
	public class Handler implements MouseListener
	{
		@Override
        public void mouseClicked( MouseEvent e )
        {
			//Display context menu to reset the display reset();
        }

        public void mousePressed( MouseEvent e ) {}
        public void mouseReleased( MouseEvent e ) {}
        public void mouseEntered( MouseEvent e ) {}
        public void mouseExited( MouseEvent e ){}
	}

	@Override
    public void settingDeleted( Setting setting ) {}
}
