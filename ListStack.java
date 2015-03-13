public class ListStack<T>
{
	public static void main( String[] args )
	{
		ListStack<Integer> s = new ListStack<Integer>();
		s.push( 1 );
		s.push( 2 );
		s.push( 3 );
		s.push( 4 );
		s.push( 5 );
		s.pop();
		s.push( 6 );
		
		while( !s.isEmpty() )
		{
			System.out.println( s.pop() );
		}
	}
	
	// fields
	private LinkedList<T> list;
	
	// constructor
	public ListStack()
	{
		list = new LinkedList<T>();
	}
	
	//methods
	public void push( T data )
	{
		list.insert( 0, data );
	}
	
	public T pop()
	{
		T data = list.get( 0 );
		list.remove( 0 );
		return data;
	}
	
	public boolean isEmpty()
	{
		return (list.length == 0);
	}
	
	public boolean isFull()
	{
		return false;
	}
}