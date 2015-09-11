package audio.metadata;

import alias.Alias;

public class Metadata
{
	private MetadataType mMetadataType;
	private String mValue;
	private Alias mValueAlias;
	private boolean mTemporal;

	/**
	 * Constucts a metadata object with the temporal settings.  Temporal 
	 * indicates that this metadata will be removed at the end of a reset 
	 * event, or call end event.
	 */
	public Metadata( MetadataType metadataType, String value, Alias alias, boolean temporal )
	{
		mMetadataType = metadataType;
		mValue = value;
		mValueAlias = alias;
		mTemporal = temporal;
	}

	/**
	 * Constucts a metadata object with the temporal settings.  Temporal 
	 * indicates that this metadata will be removed at the end of a reset 
	 * event, or call end event.
	 */
	public Metadata( MetadataType metadataType, String value, boolean temporal )
	{
		this( metadataType, value, null, temporal );
	}

	/**
	 * Constucts a non-temporal metadata object
	 */
	public Metadata( MetadataType metadataType, String value, Alias alias )
	{
		this( metadataType, value, alias, false );
	}

	/**
	 * Constucts a non-temporal metadata object
	 */
	public Metadata( MetadataType metadataType, String value )
	{
		this( metadataType, value, null, false );
	}
	
	public MetadataType getMetadataType()
	{
		return mMetadataType;
	}
	
	public String getKey()
	{
		return mMetadataType.getLabel();
	}
	
	public String getValue()
	{
		return mValue;
	}
	
	public Alias getAlias()
	{
		return mValueAlias;
	}
	
	public boolean hasAlias()
	{
		return mValueAlias != null;
	}

	/**
	 * Indicates that this piece of metadata is only valid for the duration of 
	 * a call event and should be removed during a reset
	 */
	public boolean isTemporal()
	{
		return mTemporal;
	}
	
	public boolean isReset()
	{
		return false;
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( mMetadataType.getLabel() );
		sb.append( ":" );
		sb.append( mValue );
		sb.append( " [" );
		sb.append( mMetadataType.name() );
		sb.append( "] temporal:" );
		sb.append( mTemporal );
		
		return sb.toString();
	}
}
