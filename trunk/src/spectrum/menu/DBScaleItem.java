package spectrum.menu;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import spectrum.SpectralDisplayAdjuster;

public class DBScaleItem extends JSlider implements ChangeListener
{
    private static final long serialVersionUID = 1L;

    private SpectralDisplayAdjuster mAdjuster;
    private int mDefaultValue;
    
    public DBScaleItem( SpectralDisplayAdjuster adjuster, int defaultValue )
    {
    	super( JSlider.HORIZONTAL, -160, -30, adjuster.getDBScale() );
    	mDefaultValue = defaultValue;
    	
    	mAdjuster = adjuster;
    	
    	setMajorTickSpacing( 40 );
    	setMinorTickSpacing( 10 );
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
					DBScaleItem.this.setValue( mDefaultValue );
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
		
		mAdjuster.setDBScale( value );
    }
}
