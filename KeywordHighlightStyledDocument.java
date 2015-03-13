import javax.swing.text.*;
import java.util.*;

public class KeywordHighlightStyledDocument extends DefaultStyledDocument
{
	private List<String> keywords;
	private List<AttributeSet> attrs;
	private AttributeSet attrDefault;
	
	public KeywordHighlightStyledDocument( AttributeSet defaultAttr, List<List<String> > keywordSet, List<AttributeSet> attrSet )
	{
		attrDefault = defaultAttr;
		attrs = attrSet;
	
		// Build the keyword sets
		keywords = new ArrayList<String>();
		for( List<String> set : keywordSet )
		{
			String keyString = "";
			for( int i = 0; i < set.size(); ++i )
			{
				keyString += set.get(i);
				if( i < set.size() - 1 )
				{
					keyString += "|";
				}
			}
			keywords.add( "(\\W)*(" + keyString + ")" );
		}
	}
	
	public void insertString( int offset, String str, AttributeSet a ) throws BadLocationException
	{
		super.insertString( offset, str, a );

		String text = getText( 0, getLength() );
		int before = findLastNonWordChar( text, offset );
		if( before < 0 )
		{
			before = 0;
		}
		int after = findFirstNonWordChar( text, offset + str.length() );
		int wordL = before;
		int wordR = before;

		while( wordR <= after )
		{
			if( wordR == after || String.valueOf(text.charAt(wordR)).matches("\\W") )
			{
				for( int i = 0; i < keywords.size(); ++i )
				{
					if( text.substring(wordL, wordR).matches( keywords.get(i) ) )
					{
						setCharacterAttributes( wordL, wordR - wordL, attrs.get(i), false );
					}
					else
					{
						setCharacterAttributes( wordL, wordR - wordL, attrDefault, false );
					}
					wordL = wordR;
				}
			}
			wordR++;
		}
	}

	public void remove( int offs, int len ) throws BadLocationException
	{
		super.remove( offs, len );

		String text = getText( 0, getLength() );
		int before = findLastNonWordChar( text, offs );
		if( before < 0 )
		{
			before = 0;
		}
		int after = findFirstNonWordChar( text, offs );

		for( int i = 0; i < keywords.size(); ++i )
		{
			if( text.substring(before, after).matches( keywords.get(i) ) )
			{
				setCharacterAttributes( before, after - before, attrs.get(i), false );
			}
			else
			{
				setCharacterAttributes( before, after - before, attrDefault, false );
			}
		}
	}
	
	private int findLastNonWordChar( String text, int index )
	{
        while( --index >= 0 )
        {
            if( String.valueOf(text.charAt(index)).matches("\\W") )
            {
                break;
            }
        }
        return index;
    }

    private int findFirstNonWordChar( String text, int index )
    {
        while( index < text.length() )
        {
            if( String.valueOf(text.charAt(index)).matches("\\W") )
            {
                break;
            }
            index++;
        }
        return index;
    }
}