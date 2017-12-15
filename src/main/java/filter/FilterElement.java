package filter;

public class FilterElement<T> implements Comparable<FilterElement<T>>
{
	private boolean mEnabled;
	private T mElement;
	
	public FilterElement( T element, boolean enabled )
	{
		mElement = element;
		mEnabled = enabled;
	}
	
	public FilterElement( T t )
	{
		this( t, true );
	}
	
	public T getElement()
	{
		return mElement;
	}
	
	public String toString()
	{
		return getName();
	}
	
	public boolean isEnabled()
	{
		return mEnabled;
	}
	
	public void setEnabled( boolean enabled )
	{
		mEnabled = enabled;
	}
	
	public String getName()
	{
		return mElement.toString();
	}

	@Override
    public int compareTo( FilterElement<T> other )
    {
	    return toString().compareTo( other.toString() );
    }
}
