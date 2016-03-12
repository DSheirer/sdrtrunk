package source.tuner;

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

import com.jidesoft.swing.JideSplitPane;

public class TunerViewPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private final static Logger mLog = LoggerFactory.getLogger( TunerViewPanel.class );

	private TunerModel mTunerModel;
	private JTable mTunerTable;

	private JideSplitPane mSplitPane;
	private TunerEditor mTunerEditor;
	
	public TunerViewPanel( TunerModel tunerModel )
	{
		mTunerModel = tunerModel;
		mTunerEditor = new TunerEditor( mTunerModel.getTunerConfigurationModel() );
		
		init();
	}
	
	private void init()
	{
		setLayout( new MigLayout( "insets 0 0 0 0", "[fill,grow]", "[fill,grow]" ) );

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

					mTunerEditor.setItem( mTunerModel.getTuner( modelRow ) );
				}
			}
		} );
		
		JScrollPane listScroller = new JScrollPane( mTunerTable );
		listScroller.setPreferredSize( new Dimension( 400, 20 ) );

		JScrollPane editorScroller = new JScrollPane( mTunerEditor );
		editorScroller.setPreferredSize( new Dimension( 400, 80 ) );
		
		mSplitPane = new JideSplitPane();
		mSplitPane.setOrientation( JideSplitPane.VERTICAL_SPLIT );
		mSplitPane.add( listScroller );
		mSplitPane.add( editorScroller );
		
		add( mSplitPane );
	}
	
	
}
