package filter.viewer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FilterViewer extends Application
{
	public FilterViewer()
	{
	}
	
	@Override
	public void start( Stage primaryStage ) throws Exception
	{
		Scene scene = new Scene( new FilterView() );
		
		primaryStage.setTitle( "Filter Viewer" );
		primaryStage.setScene( scene );
		primaryStage.show();
	}
	
	public static void main( String[] args )
	{
		launch( args );
	}
}
