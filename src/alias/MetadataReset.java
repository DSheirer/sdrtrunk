package alias;

public class MetadataReset extends Metadata
{
	public MetadataReset()
	{
		super( MetadataType.TEMPORAL_RESET, "reset", true );
	}
	
	public boolean isReset()
	{
		return true;
	}
}
