package io.github.dsheirer.spectrum.menu;

import io.github.dsheirer.spectrum.SpectralDisplayAdjuster;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class AveragingItem extends JSlider implements ChangeListener
{
    private static final long serialVersionUID = 1L;

    private SpectralDisplayAdjuster mAdjuster;
    private int mDefaultValue;
    
    public AveragingItem( SpectralDisplayAdjuster adjuster, int defaultValue )
    {
    	super( JSlider.HORIZONTAL, 1, 20, adjuster.getAveraging() );
    	mDefaultValue = defaultValue;
    	
    	mAdjuster = adjuster;
    	
    	setMajorTickSpacing( 5 );
    	setMinorTickSpacing( 1 );
    	setPaintTicks( true );
    	setPaintLabels( true );
    	
    	addChangeListener( this );

    	addMouseListener( new MouseListener()
		{
			@Override
			public void mouseClicked( MouseEvent event )
			{
				if( event.getClickCount() == 2 )
				{
					AveragingItem.this.setValue( mDefaultValue );
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
		
		mAdjuster.setAveraging( value );
    }
}
