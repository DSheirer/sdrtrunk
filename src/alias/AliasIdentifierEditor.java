package alias;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import net.miginfocom.swing.MigLayout;

public class AliasIdentifierEditor extends AliasConfigurationEditor
{
	private static final long serialVersionUID = 1L;

	private static ListModel<AliasID> EMPTY_MODEL = new DefaultListModel<>();
	private JList<AliasID> mAliasIDList = new JList<>( EMPTY_MODEL );

	private Alias mAlias;
	
	public AliasIdentifierEditor()
	{
		init();
	}

	private void init()
	{
		setLayout( new MigLayout( "fill,wrap 3", 
			"[grow,fill][grow,fill][grow,fill]", "[grow,fill][][grow,fill]" ) );

		mAliasIDList.setVisibleRowCount( 6 );
		mAliasIDList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		mAliasIDList.setLayoutOrientation( JList.VERTICAL );
		
		JScrollPane scroller = new JScrollPane( mAliasIDList );
		add( scroller, "span,grow" );

		add( new JButton( "New" ) );
		add( new JButton( "Copy" ) );
		add( new JButton( "Delete" ), "wrap" );
	}
	
	@Override
	public void setAlias( Alias alias )
	{
		mAlias = alias;
		
		if( mAlias == null || mAlias.getId().isEmpty() )
		{
			mAliasIDList.setModel( EMPTY_MODEL );
		}
		else
		{
			DefaultListModel<AliasID> model = new DefaultListModel<AliasID>();

			List<AliasID> ids = mAlias.getId();
			
			Collections.sort( ids, new Comparator<AliasID>()
			{
				@Override
				public int compare( AliasID o1, AliasID o2 )
				{
					return o1.toString().compareTo( o2.toString() );
				}
			} );
			
			for( AliasID id: ids )
			{
				model.addElement( id );
			}
			
			mAliasIDList.setModel( model );
		}
	}

	@Override
	public void save()
	{
		// TODO Auto-generated method stub
		
	}
}
