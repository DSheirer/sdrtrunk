package ua.in.smartjava.gui.editor;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

public class EmptyValidatingEditor<T> extends ValidatingEditor<T>
{
	private static final long serialVersionUID = 1L;

	public EmptyValidatingEditor( String descriptor )
	{
		setLayout( new MigLayout( "insets 0 0 0 0", "", "" ) );
		add( new JLabel( "Please select " + descriptor ), "wrap" );
	}
	
	public EmptyValidatingEditor()
	{
		this( "an item" );
	}

	@Override
	public void save()
	{
	}

	@Override
	public void reset()
	{
	}

	@Override
	public void validate( Editor<T> editor ) throws EditorValidationException
	{
	}
}
