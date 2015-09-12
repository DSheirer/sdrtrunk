package audio.metadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import alias.Alias;
import alias.priority.Priority;

/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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
public class AudioMetadata implements Listener<Metadata>
{
	protected static final Logger mLog = LoggerFactory.getLogger( AudioMetadata.class );

	private int mSource;
	private List<Metadata> mMetadata = new ArrayList<>();
	private boolean mUpdated = false;

	/* Channel Selectect == highest priority */
	private boolean mSelected = false;

	/* Call Priority */
	private int mPriority = Priority.DEFAULT_PRIORITY;

	/* Indicates if the overall source is recordable */
	private boolean mSourceRecordable = false;
	
	/* Indicates the current recordable state */
	private boolean mRecordable = false;
	
	/* A unique identifier for this metadata */
	private String mIdentifier;
	
	public AudioMetadata( int source, boolean recordable )
	{
		mSource = source;
		
		setIdentifier( mSource );
		
		mSourceRecordable = recordable;
		
		setRecordable( mSourceRecordable );
	}

	/**
	 * Returns a unique identifier for this audio metadata that contains the 
	 * source id and optionally the TO metadata string.
	 */
	public String getIdentifier()
	{
		return mIdentifier;
	}
	
	public void setIdentifier( int source )
	{
		setIdentifier( source, null );
	}
	
	public void setIdentifier( int source, String id )
	{
		mIdentifier = "SRC:" + source + " ID:" + ( id == null ? "UNKNOWN" : id );
		mUpdated = true;
	}
	
	/**
	 * Returns a copy of the current audio metadata.
	 */
	public AudioMetadata copyOf()
	{
		AudioMetadata copy = new AudioMetadata( mSource, mSourceRecordable );
		
		copy.mPriority = mPriority;
		copy.mSelected = mSelected;
		copy.mRecordable = mRecordable;
		copy.mMetadata.addAll( mMetadata );
		copy.mUpdated = mUpdated;
		
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

		/* Reset recordable state, identifier and audio call priority levels
		 * and reprocess the metadata */
		mRecordable = mSourceRecordable;
		setIdentifier( mSource );
		mPriority = Priority.DEFAULT_PRIORITY;
		
		for( Metadata metadata: mMetadata )
		{
			processMetadata( metadata );
		}
		
		mUpdated = true;
	}
	
	private void processMetadata( Metadata metadata )
	{
		if( metadata.hasAlias() )
		{
			Alias alias = metadata.getAlias();
			
			if( metadata.getMetadataType() == MetadataType.FROM ||
				metadata.getMetadataType() == MetadataType.TO )
			{
				if( !alias.isRecordable() )
				{
					mRecordable = false;
				}
			}

			if( metadata.getMetadataType() == MetadataType.TO )
			{
				setIdentifier( mSource, metadata.getValue() );
			}

			if( alias != null && alias.getCallPriority() < mPriority )
			{
				mPriority = alias.getCallPriority();
			}
		}
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
	
	public void setUpdated( boolean updated )
	{
		mUpdated = updated;
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
			processMetadata( metadata );
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
	 * Sets the priority of this audio packet within the defined min/max priority range */
	public void setPriority( int priority )
	{
		mPriority = priority;
		
		mUpdated = true;
	}

	/**
	 * Returns the priority of this audio metadata
	 * 
	 * @return - priority of audio packet where lower numbers are higher priority
	 */
	public int getPriority()
	{
		return mPriority;
	}
	
	public boolean isDoNotMonitor()
	{
		return mPriority == Priority.DO_NOT_MONITOR;
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
	
	/**
	 * Indicates if audio associated with this metadata is recordable.
	 */
	public boolean isRecordable()
	{
		return mRecordable;
	}
	
	public void setRecordable( boolean recordable )
	{
		mRecordable = recordable;
		mUpdated = true;
	}
}
