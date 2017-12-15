package ua.in.smartjava.gui.editor;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class DocumentListenerEditor<T> extends Editor<T> 
				implements DocumentListener
{
	private static final long serialVersionUID = 1L;

	public DocumentListenerEditor()
	{
	}

    @Override
	public void insertUpdate( DocumentEvent e )
	{
		setModified( true );
	}

	@Override
	public void removeUpdate( DocumentEvent e )
	{
		setModified( true );
	}

	@Override
	public void changedUpdate( DocumentEvent e ) { }
}
