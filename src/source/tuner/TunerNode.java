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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.SourceException;
import source.tuner.frequency.FrequencyChangeEvent;
import source.tuner.frequency.FrequencyChangeListener;
import spectrum.SpectrumFrame;
import controller.BaseNode;
import controller.ResourceManager;
import controller.channel.ChannelModel;

public class TunerNode extends BaseNode implements FrequencyChangeListener
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( TunerNode.class );

	private static final long serialVersionUID = 1L;
	private static DecimalFormat FREQUENCY_FORMAT = new DecimalFormat( "0.00000" );
	private static DecimalFormat SAMPLE_RATE_FORMAT = new DecimalFormat( "0.000" );
    
    private long mFrequency;
    private int mSampleRate;
    private ChannelModel mChannelModel;

	public TunerNode( ChannelModel channelModel, Tuner tuner )
	{
		super( tuner );  //does this make it a super tuner?
		
		mChannelModel = channelModel;
		
		tuner.addListener( this );
		
		try
        {
	        mFrequency = tuner.getFrequency();
			mSampleRate = tuner.getSampleRate();
        }
        catch ( SourceException e )
        {
        	mLog.error( "TunerNode - error reading frequency and sample rate "
        			+ "from tuner", e );
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
		sb.append( SAMPLE_RATE_FORMAT.format( (double)mSampleRate / 1E6d ) );
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
				ResourceManager rm = getModel().getResourceManager();
				
				SpectrumFrame frame = new SpectrumFrame( mChannelModel, 
						rm.getController(), rm.getPlaylistManager(), 
						rm.getSettingsManager(), getTuner() );
            }
		} );
		
		retVal.add( newSpectrumItem );
		
		return retVal;
	}

	@Override
    public void frequencyChanged( FrequencyChangeEvent event )
    {
		switch( event.getEvent() )
		{
			case FREQUENCY_CHANGE_NOTIFICATION:
				long frequency = event.getValue().longValue();
				
				if( mFrequency != frequency )
				{
					mFrequency = frequency;
					getModel().nodeChanged( TunerNode.this );
				}
				break;
			case SAMPLE_RATE_CHANGE_NOTIFICATION:
				int sampleRate = event.getValue().intValue();
				
				if( mSampleRate != sampleRate )
				{
					mSampleRate = sampleRate;
					getModel().nodeChanged( TunerNode.this );
				}
				break;
			default:
				break;
		}
    }
}
