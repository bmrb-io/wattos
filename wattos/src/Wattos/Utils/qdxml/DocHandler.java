package Wattos.Utils.qdxml;

import java.util.*;

/**
 * {@link <A HREF="http://www.javaworld.com/javatips/jw-javatip128_p.html"  Here</A>}
 * @author Steven R. Brandt
 */
public interface DocHandler {
  public void startElement(String tag,Hashtable h) throws Exception;
  public void endElement(String tag) throws Exception;
  public void startDocument() throws Exception;
  public void endDocument() throws Exception;
  public void text(String str) throws Exception;
}
