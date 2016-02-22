package alias;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

public class EmptyAliasIDEditor extends ComponentEditor<AliasID>
{
	private static final long serialVersionUID = 1L;

	public EmptyAliasIDEditor()
	{
		setLayout( new MigLayout( "", "[center]", "[][grow]" ) );
		add( new JLabel( "Please select an identifier" ), "wrap" );
	}
	
	@Override
	public void setComponent( AliasID t )
	{
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
