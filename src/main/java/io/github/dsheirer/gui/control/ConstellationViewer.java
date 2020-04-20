package io.github.dsheirer.gui.control;

import io.github.dsheirer.buffer.CircularBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import org.apache.commons.math3.util.FastMath;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.util.List;

public class ConstellationViewer extends JPanel implements Listener<Complex>
{
	private static final long serialVersionUID = 1L;
	
	private int mSampleRate;
	private int mSymbolRate;
	private float mSamplesPerSymbol;
	private float mCounter = 0;
	private float mOffset = 0;
	private CircularBuffer<Complex> mBuffer =
						new CircularBuffer<Complex>( 5000 );
	private Complex mPrevious = new Complex( 1, 1 );
	
	public ConstellationViewer( int sampleRate, int symbolRate )
	{
		mSampleRate = sampleRate;
		mSymbolRate = symbolRate;
		mSamplesPerSymbol = (float)mSampleRate / (float)mSymbolRate;
		
		initGui();
	}
	
	private void initGui()
	{
		setPreferredSize( new Dimension( 200,200 ) );
		
		addMouseListener( new MouseListener()
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				if( SwingUtilities.isRightMouseButton( e ) )
				{
					JPopupMenu menu = new JPopupMenu();
					
					menu.add( new TimingOffsetItem( (int)( mSamplesPerSymbol * 10 ), (int)( mOffset * 10 ) ) );
					
					menu.show( ConstellationViewer.this, e.getX(), e.getY() );
				}
			}
			
			public void mouseReleased( MouseEvent e ) {}
			public void mousePressed( MouseEvent e ) {}
			public void mouseExited( MouseEvent e ) {}
			public void mouseEntered( MouseEvent e ) {}
		} );
	}

	@Override
	public void receive( Complex sample )
	{
		mBuffer.receive( sample );
		
		Complex angle = Complex.multiply( sample, mPrevious.conjugate() );
		
		repaint();
	}
	
	/**
	 * Sets the timing offset ( 0 <> SamplesPerSymbol ) for selecting which
	 * sample to plot, within the symbol timeframe.  Values greater than the
	 * samples per symbol value will simply wrap or delay into the next symbol
	 * period.
	 * 
	 * @param offset
	 */
	public void setOffset( float offset )
	{
		mOffset = offset;
		
		repaint();
	}

    @Override
    public void paintComponent( Graphics g )
    {
    	super.paintComponent( g );
    	
    	Graphics2D graphics = (Graphics2D) g;
    	
    	graphics.setColor( Color.BLUE );

    	List<Complex> samples = mBuffer.getElements();
    	
    	double centerX = (double)getHeight() / 2.0d;
    	double centerY = (double)getWidth() / 2.0d;
    	
    	double scale = 0.5d;
    	
    	mCounter = 0;
    	
    	for( Complex sample: samples )
    	{
    		if( mCounter > ( mOffset + mSamplesPerSymbol ) )
    		{
        		/**
        		 * Multiply the current sample against the complex conjugate of the
        		 * previous sample to derive the phase delta between the two samples
        		 * 
        		 * Negating the previous sample quadrature produces the conjugate
        		 */
        		double i = ( sample.inphase() * mPrevious.inphase() ) - 
        				( sample.quadrature() * -mPrevious.quadrature() );
        		double q = ( sample.quadrature() * mPrevious.inphase() ) + 
        				( sample.inphase() * -mPrevious.quadrature() );

        		double angle;

        		//Check for divide by zero
        		if( i == 0 )
        		{
        			angle = 0.0;
        		}
        		else
        		{
        			/**
        			 * Use the arcus tangent of imaginary (q) divided by real (i) to
        			 * get the phase angle (+/-) which was directly manipulated by the
        			 * original message waveform during the modulation.  This value now
        			 * serves as the instantaneous amplitude of the demodulated signal
        			 */
        			double denominator = 1.0d / i;
        			angle = FastMath.atan( (double)q * denominator );
        		}
        		
        		Ellipse2D.Double ellipse = 
        				new Ellipse2D.Double( centerX - ( i * scale ), 
        						centerY - ( q * scale ), 4, 4 );
        		
        		graphics.draw( ellipse );
        		
        		mCounter -= mSamplesPerSymbol;
    		}

    		mCounter++;
    	}
    }
    
    public class TimingOffsetItem extends JSlider implements ChangeListener
    {
        private static final long serialVersionUID = 1L;

        public TimingOffsetItem( int maxValue, int currentValue )
        {
        	super( JSlider.HORIZONTAL, 0, maxValue, currentValue );

        	setMajorTickSpacing( 10 );
        	setMinorTickSpacing( 5 );
        	setPaintTicks( true );
        	setPaintLabels( true );
        	
        	addChangeListener( this );
        }

    	@Override
        public void stateChanged( ChangeEvent event )
        {
    		int value = ((JSlider)event.getSource()).getValue();
    		
    		setOffset( (float)value / 10.0f );
        }
    }
}
