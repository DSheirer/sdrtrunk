package io.github.dsheirer.spectrum.menu;

import io.github.dsheirer.spectrum.DFTProcessor;
import io.github.dsheirer.dsp.filter.Window.WindowType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FFTWindowTypeItem extends JCheckBoxMenuItem
{
    private static final long serialVersionUID = 1L;

    private DFTProcessor mDFTProcessor;
    private WindowType mWindowType;
    
    public FFTWindowTypeItem( DFTProcessor processor, WindowType windowType )
    {
    	super( windowType.toString() );
    	
    	mDFTProcessor = processor;
    	mWindowType = windowType;

    	if( processor.getWindowType() == mWindowType )
    	{
    		setSelected( true );
    	}
    	
    	addActionListener( new ActionListener() 
    	{
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				mDFTProcessor.setWindowType( mWindowType );
            }
		} );
    }
}

