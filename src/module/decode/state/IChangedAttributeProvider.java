package module.decode.state;

import sample.Listener;

public interface IChangedAttributeProvider
{
	public void setChangedAttributeListener( Listener<ChangedAttribute> listener );
	public void removeChangedAttributeListener();
}
