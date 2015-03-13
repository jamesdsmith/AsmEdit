import javax.swing.text.*;
import java.util.*;

public class AsmDocument extends KeywordHighlightStyledDocument
{
	private List<Integer> breakpoints;
	protected AttributeSet attrDebugBackground;
	protected AttributeSet attrDefaultBg;
		
	public AsmDocument( AttributeSet defaultAttr, List<List<String> > keywordSet, List<AttributeSet> attrSet, AttributeSet attrDebugBackground, AttributeSet attrDefaultBg )
	{
		super( defaultAttr, keywordSet, attrSet );
		this.attrDebugBackground = attrDebugBackground;
		this.attrDefaultBg = attrDefaultBg;
		breakpoints = new ArrayList<Integer>();
	}
	
	private void applyBreakpointStyle() throws BadLocationException
	{
		String text = getText( 0, getLength() );
		int lineNumber = 0;
		int startLine = 0;
		for( int i = 0; i < text.length(); ++i )
		{
			if( text.charAt( i ) == '\n' )
			{
				if( breakpoints.contains( lineNumber ) )
				{
					setCharacterAttributes( startLine, i - startLine, attrDebugBackground, false );
				}
				else
				{
					setCharacterAttributes( startLine, i - startLine, attrDefaultBg, false );
				}
				lineNumber++;
				startLine = i;
			}
		}
	}
	
	public void insertString( int offset, String str, AttributeSet a ) throws BadLocationException
	{
		super.insertString( offset, str, a );
		
		int shift = 0;
		for( int i = 0; i < str.length(); ++i )
		{
			if( str.charAt(i) == '\n' )
			{
				shift++;
			}
		}
		
		String text = getText( 0, getLength() );
		int lineNumber = 0;
		int startLine = 0;
		for( int i = 0; i < text.length(); ++i )
		{
			
		}
		
		applyBreakpointStyle();
		
		String txt = getText( 0, getLength() );
		int startIndex = 0;
		int lineNum = 0;
		for( int i = 0; i < txt.length(); ++i )
		{
			if( txt.charAt(i) == '\n' )
			{
				//System.out.println( "[" + lineNum + "]: (" + startIndex + ", " + i + ")" );
				startIndex = i+1;
				lineNum++;
			}
		}
	}
	
	public void remove( int offset, int len ) throws BadLocationException
	{
		String str = getText( offset, len );
		for( int i = offset; i < str.length(); ++i )
		{
			if( str.charAt(i) == '\n' )
			{
				for( int j = 0; j < breakpoints.size(); ++j )
				{
					breakpoints.set( j, breakpoints.get(j)-1 );
				}
			}
		}
		
		super.remove( offset, len );
		
		applyBreakpointStyle();
	}
	
	public void addBreakpoint( int index )
	{
		breakpoints.add( index );
		try
		{
			applyBreakpointStyle();
		}
		catch( BadLocationException ex )
		{
		}
	}
}