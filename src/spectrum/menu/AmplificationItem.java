package spectrum.menu;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import spectrum.SpectralDisplayAdjuster;

public class AmplificationItem extends JSlider implements ChangeListener
{
    private static final long serialVersionUID = 1L;

    private SpectralDisplayAdjuster mAdjuster;
    private int mDefaultValue;
    
    public AmplificationItem( SpectralDisplayAdjuster adjuster, int defaultValue )
    {
    	super( JSlider.HORIZONTAL, 1, 100, adjuster.getAmplification() );
    	mDefaultValue = defaultValue;
    	
    	mAdjuster = adjuster;
    	
    	setMajorTickSpacing( 10 );
    	setMinorTickSpacing( 5 );
    	setPaintTicks( true );
    	
    	addChangeListener( this );

    	addMouseListener( new MouseListener()
		{
			@Override
			public void mouseClicked( MouseEvent event )
			{
				if( event.getClickCount() == 2 )
				{
					AmplificationItem.this.setValue( mDefaultValue );
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
		
		mAdjuster.setAmplification( value );
    }
}
