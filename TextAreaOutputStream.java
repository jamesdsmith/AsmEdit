import java.io.OutputStream;
import javax.swing.JTextArea;
import java.io.IOException;

class TextAreaOutputStream extends OutputStream
{
	private JTextArea textArea;

	public TextAreaOutputStream( JTextArea textArea )
	{
		this.textArea = textArea;
	}
	
	public void write( int b ) throws IOException
	{
		textArea.append( Character.toString( (char)b ) );
	}
}