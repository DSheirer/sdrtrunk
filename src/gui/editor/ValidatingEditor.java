package gui.editor;


/**
 * Editor class that allows other editors to be validated for configuration with this editor
 */
public abstract class ValidatingEditor<T> extends Editor<T>
{
	private static final long serialVersionUID = 1L;

	/**
	 * Indicates if the editor argument is valid according to the settings of this editor
	 */
	public abstract boolean isValid( Editor<T> editor ) throws EditorValidationException;
}
