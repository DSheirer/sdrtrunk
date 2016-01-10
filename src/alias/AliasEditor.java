/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.tree.TreePath;

import map.IconManager;
import map.MapIcon;
import map.MapIconListCellRenderer;
import net.miginfocom.swing.MigLayout;
import settings.SettingsManager;
import controller.ConfigurableNode;

public class AliasEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    
    private AliasNode mAliasNode;
    private SettingsManager mSettingsManager;
    
    private JLabel mLabelName;
    private JTextField mTextAlias;

    private JLabel mLabelMapColor;
    private JButton mButtonColor;
    private JComboBox<MapIcon> mComboMapIcon;
    private JButton mBtnIconManager;
    
    public AliasEditor( AliasNode aliasNode, 
    					SettingsManager settingsManager )
	{
		mAliasNode = aliasNode;
		mSettingsManager = settingsManager;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "", "[right][grow,fill]", "[][][][][][grow][]" ) );
		
		setBorder( BorderFactory.createTitledBorder( "Alias" ) );

		mLabelName = new JLabel( "Name:" );
		add( mLabelName );
		
		mTextAlias = new JTextField( mAliasNode.getAlias().getName() );
		add( mTextAlias, "wrap" );

		mLabelMapColor = new JLabel( "Map Color:" );
		add( mLabelMapColor );

		mButtonColor = new JButton( "Map Color" );
		mButtonColor.setBackground( mAliasNode.getAlias().getMapColor() );
		mButtonColor.setForeground( mAliasNode.getAlias().getMapColor() );
		mButtonColor.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				Color newColor = JColorChooser.showDialog(
	                     AliasEditor.this,
	                     "Choose color for map labels and routes",
	                     mAliasNode.getAlias().getMapColor() );

				mButtonColor.setForeground( newColor );
				mButtonColor.setBackground( newColor );
            }
		} );
		add( mButtonColor, "wrap" );
		
		add( new JLabel( "Icon:" ) );

		mComboMapIcon = new JComboBox<MapIcon>( mSettingsManager.getMapIcons() );

		MapIconListCellRenderer renderer = new MapIconListCellRenderer();
		renderer.setPreferredSize( new Dimension( 200, 30 ) );
		mComboMapIcon.setRenderer( renderer );

		String iconName = mAliasNode.getAlias().getIconName();
		
		if( iconName == null )
		{
			iconName = SettingsManager.DEFAULT_ICON;
		}
		
		MapIcon savedIcon = mSettingsManager.getMapIcon( iconName );

		if( savedIcon != null )
		{
			mComboMapIcon.setSelectedItem( savedIcon );
		}

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
						new IconManager( mSettingsManager, AliasEditor.this );
				
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
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( AliasEditor.this );
		add( btnSave );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( AliasEditor.this );
		add( btnReset, "wrap" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			String alias = mTextAlias.getText();
			
			if( alias != null )
			{
				boolean expanded = mAliasNode.getModel().getTree()
						.isExpanded( new TreePath( mAliasNode ) );

				mAliasNode.getAlias().setName( alias );
				((ConfigurableNode)mAliasNode.getParent()).sort();
				
				mAliasNode.getAlias().setColor( 
						mButtonColor.getBackground().getRGB() );

				MapIcon selectedIcon = (MapIcon)mComboMapIcon.getSelectedItem();
				
				if( selectedIcon != null )
				{
					mAliasNode.getAlias().setIconName( selectedIcon.getName() );
				}
				
				mAliasNode.save();
				
				mAliasNode.show();

				if( expanded )
				{
					mAliasNode.getModel().getTree()
						.expandPath( new TreePath( mAliasNode ) );
				}
			}
			else
			{
				JOptionPane.showMessageDialog( AliasEditor.this, "Please enter an alias name" );
			}
		}
		else if( command.contentEquals( "Reset" ) )
		{
			mTextAlias.setText( mAliasNode.getAlias().getName() );
			
			mButtonColor.setBackground( mAliasNode.getAlias().getMapColor() );
			mButtonColor.setForeground( mAliasNode.getAlias().getMapColor() );

			MapIcon savedIcon = mSettingsManager
					.getMapIcon( mAliasNode.getAlias().getIconName() );
			
			mComboMapIcon.setSelectedItem( savedIcon );
		}
		
		mAliasNode.refresh();
    }
}
