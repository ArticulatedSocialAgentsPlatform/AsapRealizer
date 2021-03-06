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

import hmi.xml.XMLScanException;
import hmi.xml.XMLStructureAdapter;
import hmi.xml.XMLTokenizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

/**
 * Maps BML behavior parameters to planunit parameters
 * @author welberge
 *
 */
public class SpecParameterMap extends XMLStructureAdapter
{
    private HashMap<String, String> parametermap = new HashMap<String, String>();

    @Override
    public void decodeContent(XMLTokenizer tokenizer) throws IOException
    {
        while (tokenizer.atSTag())
        {
            String tag = tokenizer.getTagName();
            if (tag.equals(SpecParameter.xmlTag()))
            {
                SpecParameter mup = new SpecParameter();
                mup.readXML(tokenizer);
                parametermap.put(mup.src, mup.dst);
            }
            else
            {
                throw new XMLScanException("Unknown XML element "+tag+" in parametermap");
            }
        }
    }

    public Set<String> getParameters()
    {
        return parametermap.keySet();
    }

    public String getParameter(String src)
    {
        return parametermap.get(src);
    }
    
    public static final String XMLTAG = "parametermap";

    public static String xmlTag()
    {
        return XMLTAG;
    }

    @Override
    public String getXMLTag()
    {
        return XMLTAG;
    }
}
