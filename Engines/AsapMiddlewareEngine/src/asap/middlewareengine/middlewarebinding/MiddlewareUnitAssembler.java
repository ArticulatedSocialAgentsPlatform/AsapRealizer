/*******************************************************************************
 * Copyright (C) 2009-2020 Human Media Interaction, University of Twente, the Netherlands
 *
 * This file is part of the Articulated Social Agents Platform BML realizer (ASAPRealizer).
 *
 * ASAPRealizer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (LGPL) as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ASAPRealizer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ASAPRealizer.  If not, see http://www.gnu.org/licenses/.
 ******************************************************************************/
package asap.middlewareengine.middlewarebinding;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asap.middlewareengine.planunit.InterpolatingJsonTemplateMessageMU;
import asap.middlewareengine.planunit.MiddlewareUnit;
import asap.middlewareengine.planunit.SendJsonDataMessageMU;
import asap.middlewareengine.planunit.SendJsonTemplateMessageMU;
import hmi.xml.XMLStructureAdapter;
import hmi.xml.XMLTokenizer;

public class MiddlewareUnitAssembler extends XMLStructureAdapter
{
    private static Logger logger = LoggerFactory
            .getLogger(MiddlewareUnitAssembler.class.getName());

    private MiddlewareUnit middlewareUnit;

    public MiddlewareUnitAssembler()
    {
    }

    @Override
    public void decodeAttributes(HashMap<String, String> attrMap, XMLTokenizer tokenizer)
    {
        String type = getRequiredAttribute("type", attrMap, tokenizer);
        if (type.equals("SendJsonDataMessageMU"))
        {
            middlewareUnit = new SendJsonDataMessageMU();
        }
        else if (type.equals("SendJsonTemplateMessageMU"))
        {
            middlewareUnit = new SendJsonTemplateMessageMU();
        }
        else if (type.equals("InterpolatingJsonTemplateMessageMU"))
        {
            middlewareUnit = new InterpolatingJsonTemplateMessageMU();
        }
        else
        {
          logger.warn("Cannot read MiddlewareUnit type \"{}\" in MiddlewareBinding; omitting this MiddlewareUnit", type);
        }
        
    }

    /**
     * @return the unit
     */
    public MiddlewareUnit getMiddlewareUnit()
    {
        return middlewareUnit;
    }

   /*
    * The XML Stag for XML encoding
    */
   private static final String XMLTAG = "MiddlewareUnit";
 
   /**
    * The XML Stag for XML encoding -- use this static method when you want to see if a given String equals
    * the xml tag for this class
    */
   public static String xmlTag() { return XMLTAG; }
 
   /**
    * The XML Stag for XML encoding -- use this method to find out the run-time xml tag of an object
    */
   @Override
   public String getXMLTag() {
      return XMLTAG;
   }
}
