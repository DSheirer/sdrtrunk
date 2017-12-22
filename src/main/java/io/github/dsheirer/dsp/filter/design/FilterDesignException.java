package io.github.dsheirer.dsp.filter.design;

public class FilterDesignException extends Exception
{
    private static final long serialVersionUID = 1L;

    public FilterDesignException( String text )
    {
        super( text );
    }
    
    public FilterDesignException( String text, Throwable throwable )
    {
        super( text, throwable );
    }
}
