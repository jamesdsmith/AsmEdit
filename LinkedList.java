import java.lang.*;

public class LinkedList<T>
{
	public static void main( String[] args )
	{
		LinkedList<Integer> l = new LinkedList<Integer>();
		//LinkedList l = new LinkedList();
		l.insert( 0, 1 );
		l.insert( 0, 2 );
		l.insert( 1, 3 );
		l.insert( 2, 4 );
		l.insert( 2, 5 );
		l.insert( 4, 6 );
		
		for( int i = 0; i < l.length; ++i )
		{
			System.out.println( l.get( i ) );
		}
	}
	
	// fields
	public int length;
	protected Node first;
	
	// methods
	public void insert( int index, T data )
	{
		if( index >= 0 && index <= length )
		{
			Node newNode = new Node( data );

			if( first != null )
			{
				// List is not empty
				if( index == 0 )
				{
					newNode.next = first;
					first = newNode;
				}
				else
				{
					Node n = getNodeAt( index - 1 );
					newNode.next = n.next;
					n.next = newNode;
				}
			}
			else
			{
				// List is empty
				first = newNode;
			}
			
			length++;
		}
		else
		{
			//System.out.println( "Error: Index out of bounds!" );
			throw new IndexOutOfBoundsException( "Index " + index + " is out of bounds. List size " + length + "." );
		}
	}
	
	public void remove( int index )
	{
		if( index >= 0 && index < length )
		{
			if( index == 0 )
			{
				Node deadNode = first;
				first = deadNode.next;
				deadNode.next = null;
			}
			else
			{
				Node n = getNodeAt( index - 1 );
				Node deadNode = n.next;
				n.next = deadNode.next;
				deadNode.next = null;
			}
			
			
			length--;
		}
		else
		{
			//System.out.println( "Error: Index out of bounds!" );
			throw new IndexOutOfBoundsException( "Index " + index + " is out of bounds. List size " + length + "." );
		}
	}
	
	public T get( int index )
	{
		if( index >= 0 && index < length )
		{
			Node n = getNodeAt( index );
			return n.data;
		}
		else
		{
			//System.out.println( "Error: Index out of bounds!" );
			
			// TODO: throw exception here
			throw new IndexOutOfBoundsException( "LinkedList class detected index out of bounds on get method. Index " + index + " with length " + length );
		}
	}
	
	public void set( int index, T data )
	{
		if( index >= 0 && index < length )
		{
			Node n = getNodeAt( index );
			n.data = data;
		}
		else
		{
			//System.out.println( "Error: Index out of bounds!" );
			throw new IndexOutOfBoundsException( "Index " + index + " is out of bounds. List size " + length + "." );
		}
	}
	
	protected Node getNodeAt( int index )
	{
		Node n = first;
		for( int i = 0; i < index; ++i )
		{
			n = n.next;
		}
		return n;
	}
	
	protected class Node
	{
		public T data;
		public Node next;
		
		public Node( T data )
		{
			this.data = data;
		}
	}
}