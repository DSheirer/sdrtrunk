package channel.state;

import channel.metadata.Attribute;
import sample.Listener;

public interface IChangedAttributeProvider
{
	public void setChangedAttributeListener( Listener<Attribute> listener );
	public void removeChangedAttributeListener();
}
