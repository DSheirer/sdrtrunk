package spectrum.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;

import spectrum.DFTProcessor;
import dsp.filter.Window.WindowType;

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

