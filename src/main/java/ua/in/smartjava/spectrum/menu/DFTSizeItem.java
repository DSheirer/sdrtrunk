package ua.in.smartjava.spectrum.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;

import ua.in.smartjava.spectrum.DFTSize;
import ua.in.smartjava.spectrum.IDFTWidthChangeProcessor;

public class DFTSizeItem extends JCheckBoxMenuItem
{
    private static final long serialVersionUID = 1L;

    private IDFTWidthChangeProcessor mDFTProcessor;
    private DFTSize mDFTSize;
    
    public DFTSizeItem( IDFTWidthChangeProcessor processor, DFTSize size )
    {
    	super( size.getLabel() );
    	
    	mDFTProcessor = processor;
    	mDFTSize = size;

    	if( processor.getDFTSize() == mDFTSize )
    	{
    		setSelected( true );
    	}
    	
    	addActionListener( new ActionListener() 
    	{
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				mDFTProcessor.setDFTSize( mDFTSize );
            }
		} );
    }
}
