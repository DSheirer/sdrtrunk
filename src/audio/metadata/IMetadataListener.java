package audio.metadata;

import sample.Listener;
import alias.Metadata;

public interface IMetadataListener
{
	public Listener<Metadata> getMetadataListener();
}
