package alias;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import map.IconManager;
import map.MapIcon;
import map.MapIconListCellRenderer;
import net.miginfocom.swing.MigLayout;
import settings.SettingsManager;

public class AliasNameEditor extends AliasConfigurationEditor
{
	private static final long serialVersionUID = 1L;
	
	private static ComboBoxModel<String> EMPTY_MODEL = new DefaultComboBoxModel<>();

	private JComboBox<String> mListCombo = new JComboBox<>( EMPTY_MODEL );
	private JComboBox<String> mGroupCombo = new JComboBox<>( EMPTY_MODEL );
    private JTextField mName;

    private JLabel mLabelMapColor;
    private JButton mButtonColor;
    private JComboBox<MapIcon> mComboMapIcon;
    private JButton mBtnIconManager;
    
    private Alias mAlias;
    private AliasModel mAliasModel;
    private SettingsManager mSettingsManager;
    
    public AliasNameEditor( AliasModel aliasModel, SettingsManager settingsManager )
	{
    	mAliasModel = aliasModel;
    	mSettingsManager = settingsManager;
		init();
	}
	
	private void init()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][grow,fill]", "[][][][][][grow][]" ) );

		add( new JLabel( "List:" ) );
		mListCombo.setEditable( true );
		add( mListCombo, "wrap" );

		add( new JLabel( "Group:" ) );
		mGroupCombo.setEditable( true );
    	add( mGroupCombo, "wrap" );

    	add( new JLabel( "Name:" ) );
    	mName = new JTextField();
    	add( mName, "wrap" );

    	add( new JLabel( "Color:" ) );

		mButtonColor = new JButton( "Select ..." );
		mButtonColor.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				Color newColor = JColorChooser.showDialog(
	                     AliasNameEditor.this,
	                     "Choose color for this alias",
	                     ( mAlias != null ? mAlias.getMapColor() : null ) );

				if( newColor != null )
				{
					mButtonColor.setForeground( newColor );
					mButtonColor.setBackground( newColor );
				}
            }
		} );
		add( mButtonColor, "wrap" );
		
		add( new JLabel( "Icon:" ) );
		
		mComboMapIcon = new JComboBox<MapIcon>( mSettingsManager.getMapIcons() );

		MapIconListCellRenderer renderer = new MapIconListCellRenderer();
		renderer.setPreferredSize( new Dimension( 200, 30 ) );
		mComboMapIcon.setRenderer( renderer );

		add( mComboMapIcon, "wrap" );

		//Dummy place holder
		add( new JLabel() );
		
		mBtnIconManager = new JButton( "Icon Manager" );
		mBtnIconManager.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				final IconManager iconManager = 
						new IconManager( mSettingsManager, AliasNameEditor.this );
				
				EventQueue.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						iconManager.setVisible( true );
					}
				} );
            }
		} );
		add( mBtnIconManager, "span 2,wrap" );
		
	}

	@Override
	public void setAlias( Alias alias )
	{
    	mAlias = alias;

    	if( mAlias != null )
    	{
    		mName.setText( alias.getName() );
    		
			List<String> listNames = mAliasModel.getListNames();
			
			if( listNames.isEmpty() )
			{
				mListCombo.setModel( EMPTY_MODEL );
			}
			else
			{
				mListCombo.setModel( new DefaultComboBoxModel<String>( 
						listNames.toArray( new String[ listNames.size() ] ) ) );;
			}
			
			mListCombo.setSelectedItem( mAlias.getList() );
			
			List<String> groupNames = mAliasModel.getGroupNames( mAlias.getList() );
			
			if( groupNames.isEmpty() )
			{
				mGroupCombo.setModel( EMPTY_MODEL );
			}
			else
			{
				mGroupCombo.setModel( new DefaultComboBoxModel<String>( 
						groupNames.toArray( new String[ groupNames.size() ] ) ) );;
			}
			
			mGroupCombo.setSelectedItem( mAlias.getGroup() );
			
			Color color = mAlias.getMapColor();
			
			mButtonColor.setBackground( color );
			mButtonColor.setForeground( color );
			
			String iconName = mAlias.getIconName();
			
			if( iconName == null )
			{
				iconName = SettingsManager.DEFAULT_ICON;
			}
			
			MapIcon savedIcon = mSettingsManager.getMapIcon( iconName );
	
			if( savedIcon != null )
			{
				mComboMapIcon.setSelectedItem( savedIcon );
			}
    	}
    	else
    	{
    		mListCombo.setModel( EMPTY_MODEL );
    		mGroupCombo.setModel( EMPTY_MODEL );
    		mName.setText( null );
    		
			mButtonColor.setBackground( getBackground() );
			mButtonColor.setForeground( getForeground() );
    	}
	}

	@Override
	public void save()
	{
		// TODO Auto-generated method stub
		String alias = mName.getText();
		
		if( alias != null )
		{
//			boolean expanded = mAliasNode.getModel().getTree()
//					.isExpanded( new TreePath( mAliasNode ) );
//
//			mAliasNode.getAlias().setName( alias );
//			((ConfigurableNode)mAliasNode.getParent()).sort();
//			
//			mAliasNode.getAlias().setColor( 
//					mButtonColor.getBackground().getRGB() );

			MapIcon selectedIcon = (MapIcon)mComboMapIcon.getSelectedItem();
			
//			if( selectedIcon != null )
//			{
//				mAliasNode.getAlias().setIconName( selectedIcon.getName() );
//			}
//			
//			mAliasNode.save();
//			
//			mAliasNode.show();
//
//			if( expanded )
//			{
//				mAliasNode.getModel().getTree()
//					.expandPath( new TreePath( mAliasNode ) );
//			}
		}
		else
		{
			JOptionPane.showMessageDialog( AliasNameEditor.this, "Please enter an alias name" );
		}
		
	}
}
