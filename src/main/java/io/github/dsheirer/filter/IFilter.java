package io.github.dsheirer.filter;

public interface IFilter<T>
{
	/**
	 * Generic filter method.
	 * 
	 * @param message - message to filter
	 * @return - true if the message passes the filter
	 */
	public boolean passes( T t );

	/**
	 * Indicates if the filter can process (filter) the object
	 * 
	 * @param message - candidate message for filtering
	 * 
	 * @return - true if the filter is capable of filtering the message
	 */
	public boolean canProcess( T t );

	/**
	 * Indicates if this filter is enabled to evaluate messages
	 */
	public boolean isEnabled();

	/**
	 * Sets the enabled state of the filter
	 */
	public void setEnabled( boolean enabled );
	
	/**
	 * Display name for the filter
	 */
	public String getName();
}
