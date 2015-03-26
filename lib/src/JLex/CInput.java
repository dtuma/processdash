
package JLex;

import java.io.IOException;


/***************************************************************
  Class: CInput
  Description: 
 **************************************************************/
class CInput
{
  /***************************************************************
    Member Variables
    **************************************************************/
  private java.io.BufferedReader m_input; /* JLex specification file. */

  boolean m_eof_reached; /* Whether EOF has been encountered. */
  boolean m_pushback_line; 

  char m_line[]; /* Line buffer. */
  int m_line_read; /* Number of bytes read into line buffer. */
  int m_line_index; /* Current index into line buffer. */

  int m_line_number; /* Current line number. */

  /***************************************************************
    Constants
    **************************************************************/
  static final boolean EOF = true;
  static final boolean NOT_EOF = false;
  
  /***************************************************************
    Function: CInput
    Description: 
    **************************************************************/
  CInput
    (
     java.io.Reader input
     )
      {
	if (CUtility.DEBUG)
	  {
	    CUtility.ASSERT(null != input);
	  }

	/* Initialize input stream. */
	m_input = new java.io.BufferedReader(input);

	/* Initialize buffers and index counters. */
	m_line = null;
	m_line_read = 0;
	m_line_index = 0;

	/* Initialize state variables. */
	m_eof_reached = false;
	m_line_number = 0;
	m_pushback_line = false;
      }

  /***************************************************************
    Function: getLine
    Description: Returns true on EOF, false otherwise.
    Guarantees not to return a blank line, or a line
    of zero length.
    **************************************************************/
  boolean getLine 
    (
     )
      throws java.io.IOException
      {
	String lineStr;
	int elem;
	
	/* Has EOF already been reached? */
	if (m_eof_reached)
	  {
	    return EOF;
	  }
	
	/* Pushback current line? */
	if (m_pushback_line)
	  {
	    m_pushback_line = false;

	    /* Check for empty line. */
	    for (elem = 0; elem < m_line_read; ++elem)
	      {
		if (false == CUtility.isspace(m_line[elem]))
		  {
		    break;
		  }
	      }

	    /* Nonempty? */
	    if (elem < m_line_read)
	      {
		m_line_index = 0;
		return NOT_EOF;
	      }
	  }

	while (true)
	  {
	    if (null == (lineStr = m_input.readLine()))
	      {
		m_eof_reached = true;
		m_line_index = 0;
		return EOF;
	      }
	    m_line = (lineStr + System.getProperty("line.separator"))
		.toCharArray();
	    m_line_read=m_line.length;
	    ++m_line_number;
	    
	    /* Check for empty lines and discard them. */
	    elem = 0;
	    while (CUtility.isspace(m_line[elem])) 
	      {
		++elem;
		if (elem == m_line_read)
		  {
		    break;
		  }
	      }
	    
	    if (elem < m_line_read)
	      {
		break;
	      }
	  }

	m_line_index = 0;
	return NOT_EOF;
      }

      public void close() throws IOException {
          m_input.close();
      }
}
