package gui.editor;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.id.AliasIdentifierEditor;

/**
 * Generic editor with change detection, save and reset functions.
 */
public abstract class Editor<T> extends JPanel
{
	private static final long serialVersionUID = 1L;
	
	private final static Logger mLog = LoggerFactory.getLogger( Editor.class );

	protected T mItem;
	protected boolean mModified = false;

	public Editor()
	{
	}
	
	/**
	 * Sets the object to be edited
	 */
	public void setItem( T item )
	{
		if( isModified() )
		{
			int option = JOptionPane.showConfirmDialog( 
				Editor.this, 
				"This item has changed.  Do you want to save these changes?", 
				"Save Changes?",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE );
			
			if( option == JOptionPane.YES_OPTION )
			{
				save();
			}
			
			setModified( false );
		}
		
		mItem = item;
	}

	/**
	 * Current editing item
	 */
	public T getItem()
	{
		return mItem;
	}
	
	public boolean hasItem()
	{
		return mItem != null;
	}

	/**
	 * Sets the contents modified flag
	 */
	public void setModified( boolean modified )
	{
		mModified = modified;
	}

	/**
	 * Indicates if this editor contents have been modified
	 */
	public boolean isModified()
	{
		return mModified;
	}

	/**
	 * Save the contents of the editor
	 */
	public abstract void save();

	/**
	 * Reset the contents of the editor without saving any changes
	 */
	public void reset()
	{
		mModified = false;
		setItem( mItem );
	}
}
