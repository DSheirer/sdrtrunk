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
import controller.ResourceManager;
import dsp.filter.smoothing.GaussianSmoothingFilter;
import dsp.filter.smoothing.NoSmoothingFilter;
import dsp.filter.smoothing.RectangularSmoothingFilter;
import dsp.filter.smoothing.SmoothingFilter;
import dsp.filter.smoothing.SmoothingFilter.SmoothingType;
import dsp.filter.smoothing.TriangularSmoothingFilter;

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
	private int mAveraging = 4;
	
	private float mDBScale; 

	/**
	 * Spectral Display Color Settings
	 */
	private static final String SPECTRUM_BACKGROUND = "spectrum_background";
	private static final String SPECTRUM_GRADIENT_TOP = "spectrum_gradient_top";
	private static final String SPECTRUM_GRADIENT_BOTTOM = "spectrum_gradient_bottom";
	private static final String SPECTRUM_LINE = "spectrum_line";
	private static final String SPECTRUM_AVERAGING_SIZE = "spectrum_averaging_size";
	private static final int SPECTRUM_TRANSLUCENCY = 128;
	
	private static final RenderingHints RENDERING_HINTS = 
    		new RenderingHints( RenderingHints.KEY_ANTIALIASING, 
    							RenderingHints.VALUE_ANTIALIAS_ON );



	private Color mColorSpectrumBackground;
	private Color mColorSpectrumGradientTop;
	private Color mColorSpectrumGradientBottom;
	private Color mColorSpectrumLine;

	private float mSpectrumInset = 20.0f;
	
	private SmoothingFilter mSmoothingFilter = new GaussianSmoothingFilter();
	
	private ResourceManager mResourceManager;
	
	public SpectrumPanel( ResourceManager resourceManager )
    {
		mResourceManager = resourceManager;
		
		RENDERING_HINTS.put( RenderingHints.KEY_RENDERING, 
							 RenderingHints.VALUE_RENDER_QUALITY );
		
		setSampleSize( 16.0 );
		
		mSmoothingFilter.setPointSize( SmoothingFilter.SMOOTHING_DEFAULT );

		getSettings();

		setAveraging( mAveraging );
    }
	
	public void dispose()
	{
		mResourceManager = null;
	}
	
	public void setSampleSize( double sampleSize )
	{
		assert( 2.0 <= sampleSize && sampleSize <= 32.0 );
		
		mDBScale = -(float)( 20.0 * Math.log10( Math.pow( 2.0, sampleSize ) ) );
	}
	
	public void setAveraging( int size )
	{
		mAveraging = size;
	}
	
	public int getAveraging()
	{
		return mAveraging;
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
	}
	
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
				default:
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
    	if( Float.isInfinite( currentFFTBins[ 0 ] ) || Float.isNaN( currentFFTBins[ 0 ] ) )
		{
			currentFFTBins = new float[ currentFFTBins.length ];
		}

    	//Construct and/or resize our fft results variables
    	if( mDisplayFFTBins == null || 
    		mDisplayFFTBins.length != currentFFTBins.length )
    	{
    		mDisplayFFTBins = currentFFTBins;
    	}

    	//Smooth across the bins
    	float[] smoothedBins = mSmoothingFilter.filter( currentFFTBins );

    	//Average bins over multiple frames
    	if( mAveraging > 1 )
    	{
    		float gain = 1.0f / (float)mAveraging;
    		
    		for( int x = 0; x < mDisplayFFTBins.length; x++ )
    		{
    			mDisplayFFTBins[ x ] += ( smoothedBins[ x ] - mDisplayFFTBins[ x ] ) * gain;
    		}
    	}
    	else
    	{
    		mDisplayFFTBins = smoothedBins;
    	}
    	
    	
		repaint();
    }

    @Override
    public void paintComponent( Graphics g )
    {
    	super.paintComponent( g );
    	
    	Graphics2D graphics = (Graphics2D) g;
    	graphics.setBackground( mColorSpectrumBackground );


        graphics.setRenderingHints( RENDERING_HINTS );
        
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
    												getSize().height / 4, 
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
    		float insideHeight = size.height - mSpectrumInset;

    		float scalor = insideHeight / mDBScale;
			
    		float insideWidth = size.width;

    		int binCount = mDisplayFFTBins.length;

    		float binSize = insideWidth / ( binCount );
    		
    		for( int x = 0; x < binCount; x++ )
    		{
    			float height;
    			
    			if( mDisplayFFTBins[ x ] > 0 )
    			{
    				height = 0;
    			}
    			else
    			{
    				height = mDisplayFFTBins[ x ] * scalor;
    				
        			if( height > insideHeight )
        			{
        				height = insideHeight;
        			}
        			
        			if( height < 0 )
        			{
        				height = 0;
        			}
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
    public void settingDeleted( Setting setting ) {  /* not implemented */ }

	@Override
	public int getSmoothing()
	{
		return mSmoothingFilter.getPointSize();
	}

	@Override
	public void setSmoothing( int smoothing )
	{
		mSmoothingFilter.setPointSize( smoothing );
	}

	@Override
	public SmoothingType getSmoothingType()
	{
		return mSmoothingFilter.getSmoothingType();
	}

	@Override
	public void setSmoothingType( SmoothingType type )
	{
		if( mSmoothingFilter.getSmoothingType() != type )
		{
			int pointSize = getSmoothing();

			synchronized ( mSmoothingFilter )
			{
				switch( type )
				{
					case GAUSSIAN:
						mSmoothingFilter = new GaussianSmoothingFilter();
						break;
					case RECTANGLE:
						mSmoothingFilter = new RectangularSmoothingFilter();
						break;
					case TRIANGLE:
						mSmoothingFilter = new TriangularSmoothingFilter();
						break;
					case NONE:
					default:
						mSmoothingFilter = new NoSmoothingFilter();
						break;
				}
			}
			
			setSmoothing( pointSize );
		}
	}
}
