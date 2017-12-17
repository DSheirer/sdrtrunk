package io.github.dsheirer.filter;

import java.util.List;

public abstract class Filter<T> implements IFilter<T>
{
	protected String mName;
	protected boolean mEnabled = true;
	
	public Filter( String name )
	{
		mName = name;
	}

	/**
	 * Indicates if this filter is enabled or disabled
	 */
	public boolean isEnabled()
	{
		return mEnabled;
	}

	/**
	 * Enables (true) or disables (false) this filter.  An enabled filter will
	 * evaluate messages and a disabled filter will return false on all message
	 * test methods.
	 */
	public void setEnabled( boolean enabled )
	{
		mEnabled = enabled;
	}
	
	/**
	 * List of filter elements managed by this filter
	 */
	public abstract List<FilterElement<?>> getFilterElements();

	/**
	 * Name of this filter
	 */
	public String getName()
	{
		return mName;
	}
	
	public void setName( String name )
	{
		mName = name;
	}
	
	public String toString()
	{
		return mName;
	}
}
