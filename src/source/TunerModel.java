package source;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import sample.Listener;
import source.tuner.Tuner;
import source.tuner.TunerChangeEvent;

public class TunerModel extends AbstractTableModel
					implements Listener<TunerChangeEvent>
{
	private static final long serialVersionUID = 1L;
	
	private static final int TUNER_CLASS = 0;
	private static final int TUNER_TYPE = 1;
	private static final int SAMPLE_RATE = 2;
	private static final int FREQUENCY = 3;
	private static final int CHANNEL_COUNT = 4;
	private static final int SPECTRAL_DISPLAY = 5;
	
	private static final String MHZ = " MHz";
	private static final String[] COLUMNS = 
		{ "Class", "Type", "Sample Rate", "Frequency", "Channels", "Spectrum" };
	
	private List<Tuner> mTuners = new ArrayList<>();
	private DecimalFormat mFrequencyFormat = new DecimalFormat( "0.00000" );
	private DecimalFormat mSampleRateFormat = new DecimalFormat( "0.000" );

	public TunerModel()
	{
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
			mTuners.add( tuner );
			
			int index = mTuners.indexOf( tuner );
			
			fireTableRowsInserted( index, index );
			
			tuner.addTunerChangeListener( this );
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

	
	@Override
	public void receive( TunerChangeEvent event )
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
					case SPECTRAL_DISPLAY:
						fireTableCellUpdated( index, SPECTRAL_DISPLAY );
					default:
						break;
				}
			}
		}
	}

	@Override
	public int getRowCount()
	{
		return mTuners.size();
	}

	@Override
	public int getColumnCount()
	{
		return 6;
	}

	@Override
	public Object getValueAt( int rowIndex, int columnIndex )
	{
		if( rowIndex < mTuners.size() )
		{
			Tuner tuner = mTuners.get( rowIndex );

			switch( columnIndex )
			{
				case TUNER_CLASS:
					return tuner.getTunerClass().getVendorDescription();
				case TUNER_TYPE:
					return tuner.getTunerType().getLabel();
				case SAMPLE_RATE:
					int sampleRate = tuner.getSampleRate();

					return mSampleRateFormat.format( sampleRate / 1E6D ) + MHZ;
				case FREQUENCY:
					try
					{
						long frequency = tuner.getFrequency();
						
						return mFrequencyFormat.format( frequency / 1E6D ) + MHZ;
					}
					catch( Exception e )
					{
						return 0;
					}
				case CHANNEL_COUNT:
					return tuner.getChannelCount();
				case SPECTRAL_DISPLAY:
					return "TBD";
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
}
