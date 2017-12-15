package gui.editor;

public class EditorValidationException extends Exception
{
	private static final long serialVersionUID = 1L;

	private Editor<?> mEditor;
	private String mReason;
	
	public EditorValidationException( Editor<?> editor, String reason )
	{
		mEditor = editor;
		mReason = reason;
	}

	public Editor<?> getEditor()
	{
		return mEditor;
	}
	
	public String getReason()
	{
		return mReason;
	}
}
