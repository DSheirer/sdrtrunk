package spectrum.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;

import spectrum.DFTProcessor;
import spectrum.FFTWidth;

public class FFTWidthItem extends JCheckBoxMenuItem
{
    private static final long serialVersionUID = 1L;

    private DFTProcessor mDFTProcessor;
    private FFTWidth mFFTWidth;
    
    public FFTWidthItem( DFTProcessor processor, FFTWidth width )
    {
    	super( width.getLabel() );
    	
    	mDFTProcessor = processor;
    	mFFTWidth = width;

    	if( processor.getFFTWidth() == mFFTWidth )
    	{
    		setSelected( true );
    	}
    	
    	addActionListener( new ActionListener() 
    	{
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				mDFTProcessor.setFFTSize( mFFTWidth );
            }
		} );
    }
}
