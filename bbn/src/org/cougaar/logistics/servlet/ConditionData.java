package org.cougaar.logistics.servlet;

import org.cougaar.planning.servlet.data.xml.*;

import java.io.Writer;
import java.io.IOException;
import java.io.Serializable;

import org.xml.sax.Attributes;

/** simple piece of data returned from the ConditionServlet */
public class ConditionData implements XMLable, DeXMLable, Serializable{

  //Variables:
  ////////////

  public static final String NAME_TAG = "ConditionData";
  boolean wasSet = false;

  //Constructors:
  ///////////////

  public ConditionData(boolean val) { setWasSet(val);}

  //Members:
  //////////

  public void setWasSet (boolean val) { wasSet = val; }
  public boolean wasSet () { return wasSet; }
  
  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(NAME_TAG, "result", "true");
    w.cltagln(NAME_TAG);
  }

  //DeXMLable members:
  //------------------

  /**
   * Report a startElement that pertains to THIS object, not any
   * sub objects.  Call also provides the elements Attributes and data.  
   * Note, that  unlike in a SAX parser, data is guaranteed to contain 
   * ALL of this tag's data, not just a 'chunk' of it.
   * @param name startElement tag
   * @param attr attributes for this tag
   * @param data data for this tag
   **/
  public void openTag(String name, Attributes attr, String data)
    throws UnexpectedXMLException{
    if(name.equals(NAME_TAG)){
      wasSet=attr.getValue("result").equals("true");
    }else
      throw new UnexpectedXMLException("Unexpected tag: "+name);
  }

  /**
   * Report an endElement.
   * @param name endElement tag
   * @return true iff the object is DONE being DeXMLized
   **/
  public boolean closeTag(String name)
    throws UnexpectedXMLException{

    return name.equals(NAME_TAG);
  }

  /**
   * This function will be called whenever a subobject has
   * completed de-XMLizing and needs to be encorporated into
   * this object.
   * @param name the startElement tag that caused this subobject
   * to be created
   * @param obj the object itself
   **/
  public void completeSubObject(String name, DeXMLable obj)
    throws UnexpectedXMLException{}

  //Inner Classes:

  /** 
   * Set the serialVersionUID to keep the object serializer from seeing
   * xerces (org.xml.sax.Attributes).
   */
  //  private static final long serialVersionUID = 1389218378192871398L;
}

