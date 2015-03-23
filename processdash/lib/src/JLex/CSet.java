
package JLex;

import java.util.Enumeration;


/********************************************************
  Class: CSet
  *******************************************************/
class CSet 
{
  /********************************************************
    Member Variables
    *******************************************************/
  private SparseBitSet m_set;
  private boolean m_complement;

  /********************************************************
    Function: CSet
    *******************************************************/
  CSet
    (
     )
    {
      m_set = new SparseBitSet();
      m_complement = false;
    }

  /********************************************************
    Function: complement
    *******************************************************/
  void complement
    (
     )
      {
	m_complement = true;
      }

  /********************************************************
    Function: add
    *******************************************************/
  void add
    (
     int i
     )
      {
	m_set.set(i);
      }

  /********************************************************
    Function: addncase
    *******************************************************/
  void addncase // add, ignoring case.
    (
     char c
     )
      {
	/* Do this in a Unicode-friendly way. */
	/* (note that duplicate adds have no effect) */
	add(c);
	add(Character.toLowerCase(c));
	add(Character.toTitleCase(c));
	add(Character.toUpperCase(c));
      }
  
  /********************************************************
    Function: contains
    *******************************************************/
  boolean contains
    (
     int i
     )
      {
	boolean result;
	
	result = m_set.get(i);
	
	if (m_complement)
	  {
	    return (false == result);
	  }
	
	return result;
      }

  /********************************************************
    Function: mimic
    *******************************************************/
  void mimic
    (
     CSet set
     )
      {
	m_complement = set.m_complement;
	m_set = (SparseBitSet) set.m_set.clone();
      } 

  /** Map set using character classes [CSA] */
  void map(CSet set, int[] mapping) {
    m_complement = set.m_complement;
    m_set.clearAll();
    for (Enumeration e=set.m_set.elements(); e.hasMoreElements(); ) {
      int old_value =((Integer)e.nextElement()).intValue();
      if (old_value<mapping.length) // skip unmapped characters
	m_set.set(mapping[old_value]);
    }
  }
}
