package source;

import gui.editor.EmptyEditor;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import settings.SettingsManager;
import source.tuner.Tuner;
import source.tuner.TunerManager;

import com.jidesoft.swing.JideSplitPane;

public class TunerViewPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private final static Logger mLog = LoggerFactory.getLogger( TunerViewPanel.class );

	private TunerModel mTunerModel;
	private SettingsManager mSettingsManager;
	private JideSplitPane mSplitPane;
	private JTable mTunerTable;
	
	public TunerViewPanel( TunerModel tunerModel, SettingsManager settingsManager )
	{
		mTunerModel = tunerModel;
		mSettingsManager = settingsManager;
		
		init();
	}
	
	private void init()
	{
		setLayout( new MigLayout( "insets 1 1 1 1", "[fill,grow]", "[fill,grow]" ) );

		mTunerTable = new JTable( mTunerModel );
		mTunerTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		mTunerTable.getSelectionModel().addListSelectionListener( new ListSelectionListener()
		{
			@Override
			public void valueChanged( ListSelectionEvent event )
			{
				if( !event.getValueIsAdjusting() )
				{
					int row = mTunerTable.getSelectedRow();
					int modelRow = mTunerTable.convertRowIndexToModel( row );
					
					Tuner selected = mTunerModel.getTuner( modelRow );
					
					Component component = null;
					
					if( selected != null )
					{
						JPanel tunerEditor = TunerManager
								.getEditor( mSettingsManager, selected );
						
						component = new JScrollPane( tunerEditor );
					}
					else
					{
						component = new EmptyEditor<Tuner>();
					}

					int split = mSplitPane.getDividerLocation( 0 );
					
					mSplitPane.remove( 2 );
					mSplitPane.add( component );
					mSplitPane.setDividerLocation( 0, split );
					validate();
				}
			}
		} );
		
		JScrollPane listScroller = new JScrollPane( mTunerTable );
		listScroller.setPreferredSize( new Dimension( 400, 20 ) );

		JScrollPane editorScroller = new JScrollPane( new EmptyEditor<Tuner>() );
		editorScroller.setPreferredSize( new Dimension( 400, 80 ) );
		
		mSplitPane = new JideSplitPane();
		mSplitPane.setOrientation( JideSplitPane.VERTICAL_SPLIT );
		mSplitPane.add( listScroller );
		mSplitPane.add( editorScroller );
		
		add( mSplitPane );
	}
}
