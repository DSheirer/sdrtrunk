/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package source.tuner;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import log.Log;
import source.SourceException;
import spectrum.SpectrumFrame;
import controller.BaseNode;

public class TunerNode extends BaseNode implements FrequencyChangeListener
{
    private static final long serialVersionUID = 1L;
	private static DecimalFormat FREQUENCY_FORMAT = new DecimalFormat( "0.00000" );
	private static DecimalFormat SAMPLE_RATE_FORMAT = new DecimalFormat( "0.000" );
    
    private long mFrequency;
    private int mBandwidth;

	public TunerNode( Tuner tuner )
	{
		super( tuner );  //does this make it a super tuner?
		
		tuner.addListener( this );
		
		try
        {
	        mFrequency = tuner.getFrequency();
			mBandwidth = tuner.getSampleRate();
        }
        catch ( SourceException e )
        {
        	Log.error( "TunerNode - error reading frequency and sample rate "
        			+ "from tuner - " + e.getLocalizedMessage() );
        }
	}

	@Override
	public JPanel getEditor()
	{
	    return getTuner().getEditor( getModel().getResourceManager() );
	}

	public Tuner getTuner()
	{
	    return (Tuner)getUserObject();
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getTuner().getName() );
		
		sb.append( "  " );
		sb.append( FREQUENCY_FORMAT.format( (double)mFrequency / 1E6d ) );
		sb.append( " [" );
		sb.append( SAMPLE_RATE_FORMAT.format( (double)mBandwidth / 1E6d ) );
		sb.append( "] MHz" );
		
		return sb.toString();
	}
	
	public JPopupMenu getContextMenu()
	{
		JPopupMenu retVal = new JPopupMenu();
		
		JMenuItem showSpectrumItem = new JMenuItem( "Show in Main Spectrum" );
		showSpectrumItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				getModel().fireTunerSelectedEvent( getTuner() );
            }
		} );
		
		retVal.add( showSpectrumItem );
		
		JMenuItem newSpectrumItem = new JMenuItem( "New Spectrum Window" );
		newSpectrumItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				SpectrumFrame frame = 
						new SpectrumFrame( getModel().getResourceManager(), 
										   getTuner() );
            }
		} );
		
		retVal.add( newSpectrumItem );
		
		return retVal;
	}

	@Override
    public void frequencyChanged( long frequency, int bandwidth )
    {
		mFrequency = frequency;
		mBandwidth = bandwidth;
		
		getModel().nodeChanged( TunerNode.this );
    }
}
