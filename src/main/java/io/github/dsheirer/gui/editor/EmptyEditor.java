package io.github.dsheirer.gui.editor;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class EmptyEditor<T> extends Editor<T>
{
	private static final long serialVersionUID = 1L;

	public EmptyEditor( String descriptor )
	{
		setLayout( new MigLayout( "insets 0 0 0 0", "[center]", "[grow]" ) );
		add( new JLabel( "Please select " + descriptor ), "wrap" );
	}
	
	public EmptyEditor()
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
}
