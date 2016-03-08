package source;

import gui.editor.EmptyEditor;

import java.awt.Component;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
	private JList<Tuner> mTunerList;
	
	public TunerViewPanel( TunerModel tunerModel, SettingsManager settingsManager )
	{
		mTunerModel = tunerModel;
		mSettingsManager = settingsManager;
		
		init();
	}
	
	private void init()
	{
		setLayout( new MigLayout( "insets 1 1 1 1", "[fill,grow]", "[fill,grow]" ) );

		mTunerList = new JList<>( mTunerModel );
		mTunerList.setLayoutOrientation( JList.VERTICAL );
		mTunerList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		mTunerList.addListSelectionListener( new ListSelectionListener()
		{
			@Override
			public void valueChanged( ListSelectionEvent event )
			{
				if( !event.getValueIsAdjusting() )
				{
					Tuner selected = mTunerList.getSelectedValue();
					
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
		
		JScrollPane listScroller = new JScrollPane( mTunerList );

		JScrollPane editorScroller = new JScrollPane( new EmptyEditor<Tuner>() );
		
		mSplitPane = new JideSplitPane();
		mSplitPane.setOrientation( JideSplitPane.HORIZONTAL_SPLIT );
		mSplitPane.add( listScroller );
		mSplitPane.add( editorScroller );
		
		add( mSplitPane );
	}
}
