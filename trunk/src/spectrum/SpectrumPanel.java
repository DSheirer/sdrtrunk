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
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingChangeListener;
import buffer.FloatArrayCircularAveragingBuffer;
import buffer.FloatAveragingBuffer;
import controller.ResourceManager;

public class SpectrumPanel extends JPanel
							implements DFTResultsListener,
									   SettingChangeListener,
									   SpectralDisplayAdjuster
{
	private static final long serialVersionUID = 1L;
	private final static Logger mLog = 
			LoggerFactory.getLogger( SpectrumPanel.class );
	
	/* Set display bins size to 1, so that we're guaranteed a reset to the 
	 * correct width once the first sample set arrives */
	private float[] mDisplayFFTBins = new float[ 1 ];
	private FloatArrayCircularAveragingBuffer mFFTAveragingBuffer;
	private FloatAveragingBuffer mScaleBuffer = new FloatAveragingBuffer( 20 );
	private int mAmplification = 50;
	private int mAveraging = 4;
	private int mBaseline = 50;

	/**
	 * Spectral Display Color Settings
	 */
	private static final String SPECTRUM_BACKGROUND = "spectrum_background";
	private static final String SPECTRUM_GRADIENT_TOP = "spectrum_gradient_top";
	private static final String SPECTRUM_GRADIENT_BOTTOM = "spectrum_gradient_bottom";
	private static final String SPECTRUM_LINE = "spectrum_line";
	private static final String SPECTRUM_AVERAGING_SIZE = "spectrum_averaging_size";
	private static final int SPECTRUM_TRANSLUCENCY = 128;
	

	private Color mColorSpectrumBackground;
	private Color mColorSpectrumGradientTop;
	private Color mColorSpectrumGradientBottom;
	private Color mColorSpectrumLine;

	private float mSpectrumInset = 20.0f;
	
	private ResourceManager mResourceManager;
	
	public SpectrumPanel( ResourceManager resourceManager )
    {
		mResourceManager = resourceManager;

		getSettings();

		setAveraging( mAveraging );
    }
	
	public void dispose()
	{
		mResourceManager = null;
	}
	
	public void setAveraging( int size )
	{
		mAveraging = size;
		
//		mResourceManager.getSettingsManager().setIntegerSetting( 
//				SPECTRUM_AVERAGING_SIZE, mAveraging );
		
		mFFTAveragingBuffer = 
				new FloatArrayCircularAveragingBuffer( mAveraging );		
	}
	
	public int getAveraging()
	{
		return mAveraging;
	}
	
	public void setAmplification( int amplification )
	{
		mAmplification = amplification;
	}
	
	public int getAmplification()
	{
		return mAmplification;
	}
	
	public void setBaseline( int baseline )
	{
		mBaseline = baseline;
	}
	
	public int getBaseline()
	{
		return mBaseline;
	}
	
	public void clearSpectrum()
	{
		mDisplayFFTBins = null;
		
		repaint();
	}
	
	private void getSettings()
	{
		mColorSpectrumBackground = 
				getColor( ColorSettingName.SPECTRUM_BACKGROUND );

		mColorSpectrumGradientBottom = 
				getColor( ColorSettingName.SPECTRUM_GRADIENT_BOTTOM );

		mColorSpectrumGradientTop = 
				getColor( ColorSettingName.SPECTRUM_GRADIENT_TOP );

		mColorSpectrumLine = getColor( ColorSettingName.SPECTRUM_LINE );

		mAveraging = 4;
//		mAveraging = getIntegerSetting( "spectrum_averaging_size", 4 );
	}
	
//	private int getIntegerSetting( String name, int defaultValue )
//	{
//		IntegerSetting setting = mResourceManager.getSettingsManager()
//				.getIntegerSetting( name, defaultValue );
//		
//		return setting.getValue();
//	}
	
	private Color getColor( ColorSettingName name )
	{
		ColorSetting setting = 
				mResourceManager.getSettingsManager().getColorSetting( name );
		
		return setting.getColor();
	}
	
	@Override
    public void settingChanged( Setting setting )
    {
		if( setting instanceof ColorSetting )
		{
			ColorSetting colorSetting = (ColorSetting)setting;
			
			switch( ( (ColorSetting) setting ).getColorSettingName() )
			{
				case SPECTRUM_BACKGROUND:
					mColorSpectrumBackground = colorSetting.getColor();
					break;
				case SPECTRUM_GRADIENT_BOTTOM:
					mColorSpectrumGradientBottom = colorSetting.getColor();
					break;
				case SPECTRUM_GRADIENT_TOP:
					mColorSpectrumGradientTop = colorSetting.getColor();
					break;
				case SPECTRUM_LINE:
					mColorSpectrumLine = colorSetting.getColor();
					break;
			}
		}
    }
	
	
    /**
     * DFTResultsListener interface method - for receiving the processed data
     * to display
     */
    public void receive( float[] currentFFTBins )
    {
    	//Construct and/or resize our fft results variables
    	if( mDisplayFFTBins == null || 
    		mDisplayFFTBins.length != currentFFTBins.length )
    	{
    		mDisplayFFTBins = new float[ currentFFTBins.length ];
    	}

    	/**
    	 * Store the new FFT bins in the buffer and get the average of the FFT
    	 * bin buffer contents into the display fft bins variable
    	 */
    	mDisplayFFTBins = mFFTAveragingBuffer.get( currentFFTBins );
    	
		repaint();
    }

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
        
    	drawSpectrum( graphics );
    }
    
    /**
     * Draws the current fft spectrum with a line and a gradient fill.
     */
    private void drawSpectrum( Graphics2D graphics )
    {
    	Dimension size = getSize();

    	//Draw the background
    	Rectangle background = new Rectangle( 0, 0, size.width, size.height );
    	graphics.setColor( mColorSpectrumBackground );
    	graphics.draw( background );
    	graphics.fill( background );

    	//Define the gradient
    	GradientPaint gradient = new GradientPaint( 0, 
    												0, 
    												mColorSpectrumGradientTop,
    												0,
    												getSize().height, 
    												mColorSpectrumGradientBottom );
    	
    	graphics.setBackground( mColorSpectrumBackground );
    	
    	GeneralPath spectrumShape = new GeneralPath();

    	//Start at the lower right inset point
    	spectrumShape.moveTo( size.getWidth(), 
    						  size.getHeight() - mSpectrumInset );
    	
    	//Draw to the lower left
    	spectrumShape.lineTo( 0, size.getHeight() - mSpectrumInset );
    	
    	//If we have FFT data to display ...
    	if( mDisplayFFTBins != null )
    	{
    		int binCount = mDisplayFFTBins.length - 1;
    		
    		float amplify = mAmplification / 50.0f;
    		
    		float insideHeight = size.height - mSpectrumInset;

			float baseline = ( insideHeight * ( 50.0f / (float)mBaseline ) ) - 20.0f;

			/* Use the DC component as a reference to scale values to panel height */
			float scale = mScaleBuffer.get( ( insideHeight * 0.75f ) / 
					mDisplayFFTBins[ binCount ] );
			
    		float insideWidth = size.width;

    		float binSize = insideWidth / ( binCount );
    		
    		for( int x = 0; x < binCount; x++ )
    		{
    			/**
    			 * Height is an inverse value reflecting the drawable spectral 
    			 * display vertical axis between the inset value at the bottom 
    			 * and the top of the display.  Values range from:
    			 * 
    			 *    top = 0
    			 * bottom = insideHeight = panel height - mSpectrumInset
    			 */
    			float height = baseline - ( mDisplayFFTBins[ x ] * scale * amplify );

    			if( height > insideHeight )
    			{
    				height = insideHeight;
    			}
    			
    			if( height < 0 )
    			{
    				height = 0;
    			}

        		spectrumShape.lineTo( ( x * binSize ), height );
    		}
    	}
    	//Otherwise show an empty spectrum
    	else
    	{
    		//Draw Left Size
        	graphics.setPaint( gradient );
    		spectrumShape.lineTo( 0, size.getHeight() - mSpectrumInset );
    		//Draw Middle
        	spectrumShape.lineTo( size.getWidth(), 
        						  size.getHeight() - mSpectrumInset );
    	}
    	
    	//Draw Right Side
    	spectrumShape.lineTo( size.getWidth(), 
    						  size.getHeight() - mSpectrumInset );
    	
    	graphics.setPaint( gradient );
    	graphics.draw( spectrumShape );
    	graphics.fill( spectrumShape );
    	
    	graphics.setPaint( mColorSpectrumLine );

    	//Draw the bottom line under the spectrum
    	graphics.draw( new Line2D.Float( 0, 
    					   size.height - mSpectrumInset, 
    					   size.width,
    					   size.height - mSpectrumInset ) );
    }

	@Override
    public void settingDeleted( Setting setting ) {}
}
