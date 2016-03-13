package source.tuner;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.AbstractTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import source.Source;
import source.SourceException;
import source.config.SourceConfigTuner;
import source.tuner.TunerEvent.Event;
import source.tuner.configuration.TunerConfiguration;
import source.tuner.configuration.TunerConfigurationModel;

public class TunerModel extends AbstractTableModel implements Listener<TunerEvent>
{
	private static final long serialVersionUID = 1L;
	
	private final static Logger mLog = LoggerFactory.getLogger( TunerModel.class );

	public static final int TUNER_TYPE = 0;
	public static final int TUNER_ID = 1;
	public static final int SAMPLE_RATE = 2;
	public static final int FREQUENCY = 3;
	public static final int CHANNEL_COUNT = 4;
	public static final int SPECTRAL_DISPLAY_MAIN = 5;
	public static final int SPECTRAL_DISPLAY_NEW = 6;
	
	private static final String MHZ = " MHz";
	private static final String[] COLUMNS = 
		{ "Tuner", "ID", "Sample Rate", "Frequency", "Channels", "Spectral", "Display" };
	
	private List<Tuner> mTuners = new ArrayList<>();
	private List<Listener<TunerEvent>> mTunerEventListeners = new ArrayList<>();
	private DecimalFormat mFrequencyFormat = new DecimalFormat( "0.00000" );
	private DecimalFormat mSampleRateFormat = new DecimalFormat( "0.000" );
	
	private TunerConfigurationModel mTunerConfigurationModel;

	public TunerModel( TunerConfigurationModel tunerConfigurationModel )
	{
		mTunerConfigurationModel = tunerConfigurationModel;
	}
	
	public TunerConfigurationModel getTunerConfigurationModel()
	{
		return mTunerConfigurationModel;
	}

	/**
	 * List of Tuners currently in the model
	 */
	public List<Tuner> getTuners()
	{
		return mTuners;
	}
	
	public Tuner getTuner( int index )
	{
		if( index < mTuners.size() )
		{
			return mTuners.get( index );
		}
		
		return null;
	}

	public void addTuners( List<Tuner> Tuners )
	{
		for( Tuner tuner: Tuners )
		{
			addTuner( tuner );
		}
	}

	/**
	 * Adds the Tuner to this model 
	 */
	public void addTuner( Tuner tuner )
	{
		if( !mTuners.contains( tuner ) )
		{
			//Get the tuner configuration and apply it to the tuner - this 
			//call should always produce a tuner configuration
			TunerConfiguration config = mTunerConfigurationModel
				.getTunerConfiguration( tuner.getTunerType(), tuner.getUniqueID() );
			
			try
			{
				tuner.getTunerController().apply( config );
				
				mTuners.add( tuner );
				
				int index = mTuners.indexOf( tuner );
				
				fireTableRowsInserted( index, index );
				
				tuner.addTunerChangeListener( this );
			}
			catch( SourceException se )
			{
				mLog.error( "Couldn't apply tuner configuration to tuner - ["
					+ tuner.getTunerType().name() + "] with id ["
					+ tuner.getUniqueID() + "] - tuner will not be included" );
			}
			
		}
	}

	/**
	 * Removes the Tuner from this model
	 */
	public void removeTuner( Tuner tuner )
	{
		if( mTuners.contains( tuner ) )
		{
			tuner.removeTunerChangeListener( this );
			
			int index = mTuners.indexOf( tuner );
			
			mTuners.remove( tuner );
			
			fireTableRowsDeleted( index, index );
		}
	}
	
	public void addListener( Listener<TunerEvent> listener )
	{
		mTunerEventListeners.add( listener );
	}

	public void removeListener( Listener<TunerEvent> listener )
	{
		mTunerEventListeners.remove( listener );
	}

	public void broadcast( TunerEvent event )
	{
		for( Listener<TunerEvent> listener: mTunerEventListeners )
		{
			listener.receive( event );
		}
	}

	/**
	 * Requests to display the first tuner in this model.  Invoke this method
	 * after all listeners have registered and tuners have been added to this
	 * model, in order to inform the primary display to use the first tuner.
	 */
	public void requestFirstTunerDisplay()
	{
		if( mTuners.size() > 0 )
		{
			broadcast( new TunerEvent( mTuners.get( 0 ),
					Event.REQUEST_MAIN_SPECTRAL_DISPLAY ) );
		}
	}
	
	@Override
	public void receive( TunerEvent event )
	{
		if( event.getTuner() != null )
		{
			int index = mTuners.indexOf( event.getTuner() );
			
			if( index >= 0 )
			{
				switch( event.getEvent() )
				{
					case CHANNEL_COUNT:
						fireTableCellUpdated( index, CHANNEL_COUNT );
						break;
					case FREQUENCY:
						fireTableCellUpdated( index, FREQUENCY );
						break;
					case SAMPLE_RATE:
						fireTableCellUpdated( index, SAMPLE_RATE );
						break;
					case REQUEST_MAIN_SPECTRAL_DISPLAY:
						fireTableCellUpdated( index, SPECTRAL_DISPLAY_MAIN );
					default:
						break;
				}
			}
		}
		
		broadcast( event );
	}

	@Override
	public int getRowCount()
	{
		return mTuners.size();
	}

	@Override
	public int getColumnCount()
	{
		return COLUMNS.length;
	}

	@Override
	public Object getValueAt( int rowIndex, int columnIndex )
	{
		if( rowIndex < mTuners.size() )
		{
			Tuner tuner = mTuners.get( rowIndex );

			switch( columnIndex )
			{
				case TUNER_TYPE:
					return tuner.getTunerType().getLabel();
				case TUNER_ID:
					return tuner.getUniqueID();
				case SAMPLE_RATE:
					int sampleRate = tuner.getTunerController().getSampleRate();

					return mSampleRateFormat.format( sampleRate / 1E6D ) + MHZ;
				case FREQUENCY:
					try
					{
						long frequency = tuner.getTunerController().getFrequency();
						
						return mFrequencyFormat.format( frequency / 1E6D ) + MHZ;
					}
					catch( Exception e )
					{
						return 0;
					}
				case CHANNEL_COUNT:
					return tuner.getTunerController().getChannelCount();
				case SPECTRAL_DISPLAY_MAIN:
					return "Main";
				case SPECTRAL_DISPLAY_NEW:
					return "New";
				default:
					break;
			}
		}

		return null;
	}
	
	@Override
	public String getColumnName( int columnIndex )
	{
		return COLUMNS[ columnIndex ];
	}
	
    /**
     * Iterates current tuners to get a tuner channel source for the frequency
     * specified in the channel config's source config object.
     * 
     * Returns null if no tuner can source the channel
     */
    public Source getSource( SourceConfigTuner config, int bandwidth )
    {
    	TunerChannelSource retVal = null;
    	
		TunerChannel tunerChannel = config.getTunerChannel();
		
		tunerChannel.setBandwidth( bandwidth );
		
		Iterator<Tuner> it = mTuners.iterator();
		
		Tuner tuner;
		
		while( it.hasNext() && retVal == null )
		{
			tuner = it.next();
			
			try
            {
                retVal = tuner.getChannel( tunerChannel );
            }
			catch ( RejectedExecutionException ree )
			{
				mLog.error( "couldn't provide tuner channel source", ree );
			}
            catch ( SourceException e )
            {
            	mLog.error( "error obtaining channel from tuner [" + 
            			tuner.getName() + "]", e );
            }
		}
    	
    	return retVal;
    }
	
}
