package ua.in.smartjava.filter;

public interface IFilter<T>
{
	/**
	 * Generic ua.in.smartjava.filter method.
	 * 
	 * @param message - ua.in.smartjava.message to ua.in.smartjava.filter
	 * @return - true if the ua.in.smartjava.message passes the ua.in.smartjava.filter
	 */
	public boolean passes( T t );

	/**
	 * Indicates if the ua.in.smartjava.filter can process (ua.in.smartjava.filter) the object
	 * 
	 * @param message - candidate ua.in.smartjava.message for filtering
	 * 
	 * @return - true if the ua.in.smartjava.filter is capable of filtering the ua.in.smartjava.message
	 */
	public boolean canProcess( T t );

	/**
	 * Indicates if this ua.in.smartjava.filter is enabled to evaluate messages
	 */
	public boolean isEnabled();

	/**
	 * Sets the enabled state of the ua.in.smartjava.filter
	 */
	public void setEnabled( boolean enabled );
	
	/**
	 * Display name for the ua.in.smartjava.filter
	 */
	public String getName();
}
