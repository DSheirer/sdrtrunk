/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package alias;

import javax.swing.JPanel;
import javax.swing.event.DocumentListener;

public abstract class ComponentEditor<T> extends JPanel implements DocumentListener
{
    private static final long serialVersionUID = 1L;
    private boolean mModified;
    private ComponentModificationListener mListener;
    protected T mComponent;
    
    /**
     * Abstract class for editor with notifications when contents are modified
     * and methods for saving or resetting the component values with the
     * contents of the editor.
     */
    public ComponentEditor( T component )
    {
    	mComponent = component;
    }
    
    public ComponentEditor()
    {
    }
    
    public void dispose()
    {
    	mComponent = null;
    	mListener = null;
    }
    
    public T getComponent()
    {
    	return mComponent;
    }
    
    protected void setModified( boolean modified )
    {
    	mModified = modified;
    	
    	if( modified && mListener != null )
    	{
    		mListener.componentModified();
    	}
    }
    
    /**
     * Indicates if the contents of the editor have been modified
     */
    public boolean isModified()
    {
    	return mModified;
    }
    
	public void setComponentModificationListener( ComponentModificationListener listener )
    {
    	mListener = listener;
    }
    
    /**
     * Sets or resets the editable component
     */
    public abstract void setComponent( T t );
    
    /**
     * Saves changed editor values to the component
     */
    public abstract void save();

    /**
     * Reloads the editor contents from the source component and resets the 
     * modified flag
     */
    public void reset()
    {
    	setComponent( getComponent() );
    }
    
    /**
     * Validate contents of other editors against this component editor before 
     * saving.  Override this method to allow controller to pass other editors
     * to this editor for content inspection/validation before indicating that
     * this editor passes validation.  This allows for validating editor contents
     * that are contingent on settings in other editors.
     * 
     * @throws ComponentValidationException if the contents of the editor 
     * argument cannot be validated, with the exception containing a detailed
     * description of the issue.
     */
    public void validate( ComponentEditor<?> editor ) throws ComponentValidationException
    {
    }
    
    /**
     * Listener interface to receive notification that the contents of the
     * editor have been modified from the original component value and either 
     * need to be saved or reset
     */
    public interface ComponentModificationListener
    {
    	public void componentModified();
    }
}
