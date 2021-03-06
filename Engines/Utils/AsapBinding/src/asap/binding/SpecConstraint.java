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
package asap.binding;

import hmi.xml.XMLStructureAdapter;
import hmi.xml.XMLTokenizer;

import java.util.HashMap;

/**
 * A SpecConstraint is a name/value pair describing a constraint for the embedded spec.<br>
 * &lt;constraint name="..." value="..."/&gt;
 * @author welberge
 */
class SpecConstraint extends XMLStructureAdapter
{
    public String name;
    public String value;
    public String namespace;

    @Override
    public void decodeAttributes(HashMap<String, String> attrMap, XMLTokenizer tokenizer)
    {
        name = getRequiredAttribute("name", attrMap, tokenizer);
        value = getRequiredAttribute("value", attrMap, tokenizer);
        namespace = getOptionalAttribute("namespace", attrMap, null);
    }

    @Override
    public StringBuilder appendAttributeString(StringBuilder buf)
    {
        appendAttribute(buf, "name", name);
        appendAttribute(buf, "value", value);
        if (namespace != null)
        {
            appendAttribute(buf, "namespace", namespace);
        }
        return super.appendAttributeString(buf);
    }

    /*
     * The XML Stag for XML encoding
     */
    private static final String XMLTAG = "constraint";

    /**
     * The XML Stag for XML encoding -- use this static method when you want to see if a given String equals
     * the xml tag for this class
     */
    public static String xmlTag()
    {
        return XMLTAG;
    }

    /**
     * The XML Stag for XML encoding -- use this method to find out the run-time xml tag of an object
     */
    @Override
    public String getXMLTag()
    {
        return XMLTAG;
    }

}
