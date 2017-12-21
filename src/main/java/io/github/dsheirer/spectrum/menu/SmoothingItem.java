package io.github.dsheirer.spectrum.menu;

import io.github.dsheirer.dsp.filter.smoothing.SmoothingFilter;
import io.github.dsheirer.spectrum.SpectralDisplayAdjuster;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Hashtable;

public class SmoothingItem extends JSlider implements ChangeListener
{
    private static final long serialVersionUID = 1L;

    private SpectralDisplayAdjuster mAdjuster;
    private int mDefaultValue;
    
    public SmoothingItem( SpectralDisplayAdjuster adjuster, int defaultValue )
    {
    	super( JSlider.HORIZONTAL, 
    		   SmoothingFilter.SMOOTHING_MINIMUM, 
    		   SmoothingFilter.SMOOTHING_MAXIMUM, 
    		   adjuster.getSmoothing() );
    	
    	mDefaultValue = defaultValue;
    	mAdjuster = adjuster;
    	
    	setSnapToTicks( true );
    	setMajorTickSpacing( 6 );
    	setMinorTickSpacing( 2 );
    	setPaintTicks( true );
    	setPaintLabels( true );
    	
    	Hashtable<Integer,JLabel> labels = new Hashtable<>();
    	labels.put( new Integer( 3 ), new JLabel( "3" ) );
    	labels.put( new Integer( 9 ), new JLabel( "9" ) );
    	labels.put( new Integer( 15 ), new JLabel( "15" ) );
    	labels.put( new Integer( 21 ), new JLabel( "21" ) );
    	labels.put( new Integer( 27 ), new JLabel( "27" ) );
    	
    	setLabelTable( labels );
    	
    	addChangeListener( this );

    	addMouseListener( new MouseListener()
		{
			@Override
			public void mouseClicked( MouseEvent event )
			{
				if( event.getClickCount() == 2 )
				{
					SmoothingItem.this.setValue( mDefaultValue );
				}
			}
			
			public void mouseReleased( MouseEvent arg0 ) {}
			public void mousePressed( MouseEvent arg0 ) {}
			public void mouseExited( MouseEvent arg0 ) {}
			public void mouseEntered( MouseEvent arg0 ) {}
		} );
    }

	@Override
    public void stateChanged( ChangeEvent event )
    {
		int value = ((JSlider)event.getSource()).getValue();

		if( value % 2 == 1 )
		{
			mAdjuster.setSmoothing( value );
		}
    }
}
