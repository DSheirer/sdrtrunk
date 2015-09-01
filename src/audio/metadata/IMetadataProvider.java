package audio.metadata;

import sample.Listener;
import alias.Metadata;

public interface IMetadataProvider
{
	public void setMetadataListener( Listener<Metadata> listener );
	public void removeMetadataListener();
}
