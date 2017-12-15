package ua.in.smartjava.spectrum.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;

import ua.in.smartjava.spectrum.DFTProcessor;

public class FrameRateItem extends JCheckBoxMenuItem
{
    private static final long serialVersionUID = 1L;

    private DFTProcessor mDFTProcessor;
    private int mFrameRate;
    
    public FrameRateItem( DFTProcessor processor, int frameRate )
    {
    	super( String.valueOf( frameRate ) );
    	
    	mDFTProcessor = processor;
    	mFrameRate = frameRate;

    	if( processor.getFrameRate() == mFrameRate )
    	{
    		setSelected( true );
    	}
    	
    	addActionListener( new ActionListener() 
    	{
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				mDFTProcessor.setFrameRate( mFrameRate );
            }
		} );
    }
}

