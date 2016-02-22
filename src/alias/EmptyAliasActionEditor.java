package alias;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import alias.action.AliasAction;

public class EmptyAliasActionEditor extends ComponentEditor<AliasAction>
{
	private static final long serialVersionUID = 1L;

	public EmptyAliasActionEditor()
	{
		setLayout( new MigLayout( "", "[center]", "[][grow]" ) );
		add( new JLabel( "Please select an alias action" ), "wrap" );
	}
	
	@Override
	public void setComponent( AliasAction t )
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
