package gui.editor;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

public class EmptyEditor<T> extends Editor<T>
{
	private static final long serialVersionUID = 1L;

	public EmptyEditor()
	{
		setLayout( new MigLayout( "insets 0 0 0 0", "[center]", "[grow]" ) );
		add( new JLabel( "Please select an item" ), "wrap" );
	}

	@Override
	public void save()
	{
	}

	@Override
	public void reset()
	{
	}
}
