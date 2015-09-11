package audio.metadata;

import sample.Listener;

public interface IMetadataProvider
{
	public void setMetadataListener( Listener<Metadata> listener );
	public void removeMetadataListener();
}
