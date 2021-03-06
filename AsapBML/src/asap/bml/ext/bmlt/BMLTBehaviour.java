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
package asap.bml.ext.bmlt;

import hmi.xml.XMLFormatting;
import hmi.xml.XMLScanException;
import hmi.xml.XMLTokenizer;

import java.io.IOException;
import java.util.HashMap;

import saiba.bml.core.Behaviour;

/**
 * Abstract class for all BMLT specific Behaviours
 * @author welberge
 */
public abstract class BMLTBehaviour extends Behaviour
{
    public BMLTBehaviour(String bmlId)
    {
        super(bmlId);
    }

    public BMLTBehaviour(String bmlId, String id)
    {
        super(bmlId, id);
    }
    
    public static final String BMLTNAMESPACE = "http://hmi.ewi.utwente.nl/bmlt";

    @Override
    public String getNamespace()
    {
        return BMLTNAMESPACE;
    }

    protected HashMap<String, BMLTParameter> parameters = new HashMap<String, BMLTParameter>();

    @Override
    public String getStringParameterValue(String name)
    {
        if (parameters.get(name) != null)
        {
            return parameters.get(name).value;
        }
        return super.getStringParameterValue(name);
    }

    @Override
    public boolean specifiesParameter(String name)
    {
        if (parameters.get(name) != null) return true;
        return super.specifiesParameter(name);
    }

    @Override
    public float getFloatParameterValue(String name)
    {
        if (parameters.get(name) != null)
        {
            return Float.parseFloat(parameters.get(name).value);
        }
        return super.getFloatParameterValue(name);
    }

    @Override
    public boolean satisfiesConstraint(String name, String value)
    {
        BMLTParameter p = parameters.get(name);
        if (p != null)
        {
            return p.value.equals(value);
        }
        return super.satisfiesConstraint(name, value);
    }

    @Override
    public boolean hasContent()
    {
        if (parameters.size() > 0) return true;
        return super.hasContent();
    }

    @Override
    public StringBuilder appendContent(StringBuilder buf, XMLFormatting fmt)
    {
        for (BMLTParameter p : parameters.values())
        {
            p.appendXML(buf, fmt);
        }
        return buf;
    }

    @Override
    public void decodeContent(XMLTokenizer tokenizer) throws IOException
    {
        while (tokenizer.atSTag())
        {
            String tag = tokenizer.getTagName();
            if (tag.equals(BMLTParameter.xmlTag()))
            {
                decodeBMLTParameter(tokenizer);
            }
            else
            {
                throw new XMLScanException("Invalid content " + tag + " in BMLTBehavior " + id);
            }
        }
    }

    protected void decodeBMLTParameter(XMLTokenizer tokenizer) throws IOException
    {
        BMLTParameter param = new BMLTParameter();
        param.readXML(tokenizer);
        parameters.put(param.name, param);
    }
}
