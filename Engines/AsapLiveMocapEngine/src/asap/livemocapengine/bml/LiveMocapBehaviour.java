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
package asap.livemocapengine.bml;

import hmi.xml.XMLTokenizer;

import java.util.HashMap;
import java.util.List;

import lombok.Getter;
import saiba.bml.core.Behaviour;
import saiba.bml.parser.SyncPoint;

import com.google.common.collect.ImmutableList;

/**
 * Superclass for all LiveMocapBehaviours
 * @author welberge
 *
 */
public class LiveMocapBehaviour extends Behaviour
{
    @Getter private String output;
    @Getter private String input;
    
    public LiveMocapBehaviour(String bmlId)
    {
        super(bmlId);        
    }

    
    @Override
    public StringBuilder appendAttributeString(StringBuilder buf)
    {
        appendAttribute(buf, "output", output);
        appendAttribute(buf, "input", input);        
        return super.appendAttributeString(buf);
    }

    @Override
    public String getStringParameterValue(String name)
    {
        if (name.equals("output")) return output;
        if (name.equals("input")) return input;
        return super.getStringParameterValue(name);
    }
    
    @Override
    public boolean specifiesParameter(String name)
    {
        if (name.equals("output")) return true;
        if (name.equals("input")) return true;
        return super.specifiesParameter(name);
    }
    
    @Override
    public void decodeAttributes(HashMap<String, String> attrMap, XMLTokenizer tokenizer)
    {
        output = getRequiredAttribute("output", attrMap, tokenizer);
        input = getRequiredAttribute("input", attrMap, tokenizer);        
        super.decodeAttributes(attrMap, tokenizer);
    }
    
    private static final List<String> DEFAULT_SYNCS = ImmutableList.of("start","end");    
    public static List<String> getDefaultSyncPoints()
    {
        return DEFAULT_SYNCS;
    }
    
    @Override
    public void addDefaultSyncPoints()
    {
        for(String s:getDefaultSyncPoints())
        {
            addSyncPoint(new SyncPoint(bmlId, id, s));
        }        
    }    
}
