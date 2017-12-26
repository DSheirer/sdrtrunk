package io.github.dsheirer.spectrum.menu;

import io.github.dsheirer.spectrum.DFTProcessor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

