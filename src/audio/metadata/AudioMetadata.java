package audio.metadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import alias.Metadata;
import alias.MetadataType;

public class AudioMetadata implements Listener<Metadata>
{
	protected static final Logger mLog = LoggerFactory.getLogger( AudioMetadata.class );

	private int mSource;
	private int mPriority = 99;
	private boolean mSelected = false;
	private List<Metadata> mMetadata = new ArrayList<>();
	private boolean mUpdated = false;
	
	public AudioMetadata( int source )
	{
		mSource = source;
	}
	
	public AudioMetadata copyOf()
	{
		AudioMetadata copy = new AudioMetadata( mSource );
		
		copy.mPriority = mPriority;
		copy.mSelected = mSelected;
		copy.mMetadata.addAll( mMetadata );
		
		if( mUpdated )
		{
			copy.mUpdated = true;
			
			mUpdated = false;
		}
		
		return copy;
	}

	/**
	 * Removes any accumulated temporal metadata and returns to a default set.
	 */
	public void reset()
	{
		Iterator<Metadata> it = mMetadata.iterator();
		
		while( it.hasNext() )
		{
			if( it.next().isTemporal() )
			{
				it.remove();
			}
		}
		
		mUpdated = true;
	}

	/**
	 * Indicates if this audio metadata contains updated information.  The flag
	 * will only be set when new data is added and will be reset by a copyOf()
	 * method invocation.
	 */
	public boolean isUpdated()
	{
		return mUpdated;
	}

	/**
	 * Adds the metadata.  Any temporal metadata will be removed when the 
	 * dispose() method is invoked.
	 */
	@Override
	public void receive( Metadata metadata )
	{
		if( metadata.isReset() )
		{
			reset();
		}
		else
		{
			mMetadata.add( metadata );
			
			if( metadata.hasAlias() )
			{
				//TODO: assign priority from alias here
			}
		}
		
		mUpdated = true;
	}
	
	/**
	 * Returns the complete set of metadata
	 */
	public List<Metadata> getMetadata()
	{
		return mMetadata;
	}

	/**
	 * Returns the first metadata object matching the type, or null if the 
	 * metadata type is not found.
	 */
	public Metadata getMetadata( MetadataType type )
	{
		for( Metadata metadata: mMetadata )
		{
			if( metadata.getMetadataType() == type )
			{
				return metadata;
			}
		}
		
		return null;
	}

	/**
	 * Source channel ID
	 */
	public void setSource( int source )
	{
		mSource = source;
		
		mUpdated = true;
	}
	
	public int getSource()
	{
		return mSource;
	}

	/**
	 * Sets the priority of this audio packet in the range of 1 - Max Integer value
	 * 
	 * @param priority (1 - Max Int)
	 */
	public void setPriority( int priority )
	{
		assert( 1 <= priority && priority <= Integer.MAX_VALUE );
		
		mPriority = priority;
		
		mUpdated = true;
	}

	/**
	 * Returns the priority of this audio packet or a priority of -1 if the
	 * audio source is currently selected, so that the priority of this packet
	 * is higher (lower?) than any other audio source
	 * 
	 * @return - priority of audio packet where lower numbers are higher priority
	 */
	public int getPriority()
	{
		return mSelected ? -1 : mPriority;
	}

	/**
	 * Selected audio channel.  A selected channel will override the priority
	 * level so that the audio can be listened to immediately.
	 */
	public void setSelected( boolean selected )
	{
		mSelected = selected;
		
		mUpdated = true;
	}

	public boolean isSelected()
	{
		return mSelected;
	}
}
