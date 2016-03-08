package source;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;

import source.tuner.Tuner;

public class TunerModel extends AbstractListModel<Tuner>
{
	private static final long serialVersionUID = 1L;
	
	private List<Tuner> mTuners = new ArrayList<>();

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
			
			fireIntervalAdded( this, index, index );
		}
	}

	/**
	 * Removes the Tuner from this model
	 */
	public void removeTuner( Tuner tuner )
	{
		if( mTuners.contains( tuner ) )
		{
			int index = mTuners.indexOf( tuner );
			
			mTuners.remove( tuner );
			
			fireIntervalRemoved( this, index, index );
		}
	}

	/**
	 * Number of Tuners in this model
	 */
	@Override
	public int getSize()
	{
		return mTuners.size();
	}

	/**
	 * Returns the Tuner at the specified index
	 */
	@Override
	public Tuner getElementAt( int index )
	{
		if( index <= mTuners.size() )
		{
			return mTuners.get( index );
		}
		
		return null;
	}
}
