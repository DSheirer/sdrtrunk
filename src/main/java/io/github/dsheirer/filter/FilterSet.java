package io.github.dsheirer.filter;

import java.util.ArrayList;
import java.util.List;

public class FilterSet<T> implements IFilter<T>
{
    protected List<IFilter<T>> mFilters = new ArrayList<IFilter<T>>();
    protected boolean mEnabled = true;
    protected String mName;

    public FilterSet( String name )
    {
    	mName = name;
    }
    
    public FilterSet()
    {
    	mName = "Message Filter";
    }

    public FilterSet(IFilter<T> filter)
	{
		addFilter(filter);
	}
    
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
    	return getName();
    }
    
	@Override
    public boolean passes( T t )
    {
		if( mEnabled )
		{
			for( IFilter<T> filter: mFilters )
			{
				if( filter.canProcess( t ) )
				{
					return filter.passes( t );
				}
			}
		}
		
		return false;
    }

	@Override
    public boolean canProcess( T t )
    {
		if( mEnabled )
		{
			for( IFilter<T> filter: mFilters )
			{
				if( filter.canProcess( t ) )
				{
					return true;
				}
			}
		}
		
		return false;
    }
	
	public List<IFilter<T>> getFilters()
	{
		return mFilters;
	}
	
	public void addFilters( List<IFilter<T>> filters )
	{
		mFilters.addAll( filters );
	}
	
	public void addFilter( IFilter<T> filter )
	{
		mFilters.add( filter );
	}
	
	public void removeFilter( IFilter<T> filter )
	{
		mFilters.remove( filter );
	}

	@Override
    public boolean isEnabled()
    {
	    return mEnabled;
    }

	@Override
    public void setEnabled( boolean enabled )
    {
		mEnabled = enabled;
    }
}
