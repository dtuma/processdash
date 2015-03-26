
package JLex;


/********************************************************
  Class: Utility
  *******************************************************/
class CUtility 
{
  /********************************************************
    Constants
    *******************************************************/
  /*static final boolean DEBUG = true;
  static final boolean SLOW_DEBUG = true;
  static final boolean DUMP_DEBUG = true;*/
  static final boolean DEBUG = false;
  static final boolean SLOW_DEBUG = false;
  static final boolean DUMP_DEBUG = false;
  static final boolean DESCENT_DEBUG = false;
  static final boolean OLD_DEBUG = false;
  static final boolean OLD_DUMP_DEBUG = false;
  static final boolean FOODEBUG = false;
  static final boolean DO_DEBUG = false;
  
  /********************************************************
    Constants: Integer Bounds
    *******************************************************/
  static final int INT_MAX = 2147483647;

  static final int MAX_SEVEN_BIT = 127;
  static final int MAX_EIGHT_BIT = 255;
  static final int MAX_SIXTEEN_BIT=65535;

  /********************************************************
    Function: enter
    Description: Debugging routine.
    *******************************************************/
  static void enter
    (
     String descent,
     char lexeme,
     int token
     )
      {
	System.out.println("Entering " + descent 
			   + " [lexeme: " + lexeme 
			   + "] [token: " + token + "]");
      }

  /********************************************************
    Function: leave
    Description: Debugging routine.
    *******************************************************/
  static void leave
    (
     String descent,
     char lexeme,
     int token
     )
      {
	System.out.println("Leaving " + descent 
			   + " [lexeme:" + lexeme 
			   + "] [token:" + token + "]");
      }

  /********************************************************
    Function: ASSERT
    Description: Debugging routine.
    *******************************************************/
  static void ASSERT
    (
     boolean expr
     )
      {
	if (DEBUG && false == expr)
	  {
	    System.out.println("Assertion Failed");
	    throw new Error("Assertion Failed.");
	  }
      }

  /***************************************************************
    Function: doubleSize
    **************************************************************/
  static char[] doubleSize
    (
     char oldBuffer[]
     )
      {
	char newBuffer[] = new char[2 * oldBuffer.length];
	int elem;

	for (elem = 0; elem < oldBuffer.length; ++elem)
	  {
	    newBuffer[elem] = oldBuffer[elem];
	  }

	return newBuffer;
      }

  /***************************************************************
    Function: doubleSize
    **************************************************************/
  static byte[] doubleSize
    (
     byte oldBuffer[]
     )
      {
	byte newBuffer[] = new byte[2 * oldBuffer.length];
	int elem;

	for (elem = 0; elem < oldBuffer.length; ++elem)
	  {
	    newBuffer[elem] = oldBuffer[elem];
	  }

	return newBuffer;
      }

  /********************************************************
    Function: hex2bin
    *******************************************************/
  static char hex2bin
    (
     char c
     )
      {
	if ('0' <= c && '9' >= c)
	  {
	    return (char) (c - '0');
	  }
	else if ('a' <= c && 'f' >= c)
	  {
	    return (char) (c - 'a' + 10);
	  }	    
	else if ('A' <= c && 'F' >= c)
	  {
	    return (char) (c - 'A' + 10);
	  }
	
	CError.impos("Bad hexidecimal digit" + c);
	return 0;
      }

  /********************************************************
    Function: ishexdigit
    *******************************************************/
  static boolean ishexdigit
    (
     char c
     )
      {
	if (('0' <= c && '9' >= c)
	    || ('a' <= c && 'f' >= c)
	    || ('A' <= c && 'F' >= c))
	  {
	    return true;
	  }

	return false;
      }

  /********************************************************
    Function: oct2bin
    *******************************************************/
  static char oct2bin
    (
     char c
     )
      {
	if ('0' <= c && '7' >= c)
	  {
	    return (char) (c - '0');
	  }
	
	CError.impos("Bad octal digit " + c);
	return 0;
      }

  /********************************************************
    Function: isoctdigit
    *******************************************************/
  static boolean isoctdigit
    (
     char c
     )
      {
	if ('0' <= c && '7' >= c)
	  {
	    return true;
	  }

	return false;
      }
	
  /********************************************************
    Function: isspace
    *******************************************************/
  static boolean isspace
    (
     char c
     )
      {
	if ('\b' == c 
	    || '\t' == c
	    || '\n' == c
	    || '\f' == c
	    || '\r' == c
	    || ' ' == c)
	  {
	    return true;
	  }
	
	return false;
      }

  /********************************************************
    Function: isnewline
    *******************************************************/
  static boolean isnewline
    (
     char c
     )
      {
	if ('\n' == c
	    || '\r' == c)
	    {
	    return true;
	  }
	
	return false;
      }

  /********************************************************
    Function: bytencmp
    Description: Compares up to n elements of 
    byte array a[] against byte array b[].
    The first byte comparison is made between 
    a[a_first] and b[b_first].  Comparisons continue
    until the null terminating byte '\0' is reached
    or until n bytes are compared.
    Return Value: Returns 0 if arrays are the 
    same up to and including the null terminating byte 
    or up to and including the first n bytes,
    whichever comes first.
    *******************************************************/
  static int bytencmp
    (
     byte a[],
     int a_first,
     byte b[],
     int b_first,
     int n
     )
      {
	int elem;

	for (elem = 0; elem < n; ++elem)
	  {
	    /*System.out.print((char) a[a_first + elem]);
	    System.out.print((char) b[b_first + elem]);*/
			     
	    if ('\0' == a[a_first + elem] && '\0' == b[b_first + elem])
	      {
		/*System.out.println("return 0");*/
		return 0;
	      }
	    if (a[a_first + elem] < b[b_first + elem])
	      {
		/*System.out.println("return 1");*/
		return 1;
	      }
	    else if (a[a_first + elem] > b[b_first + elem])
	      {
		/*System.out.println("return -1");*/
		return -1;
	      }
	  }

	/*System.out.println("return 0");*/
	return 0;
      }

  /********************************************************
    Function: charncmp
    *******************************************************/
  static int charncmp
    (
     char a[],
     int a_first,
     char b[],
     int b_first,
     int n
     )
      {
	int elem;

	for (elem = 0; elem < n; ++elem)
	  {
	    if ('\0' == a[a_first + elem] && '\0' == b[b_first + elem])
	      {
		return 0;
	      }
	    if (a[a_first + elem] < b[b_first + elem])
	      {
		return 1;
	      }
	    else if (a[a_first + elem] > b[b_first + elem])
	      {
		return -1;
	      }
	  }

	return 0;
      }
}
