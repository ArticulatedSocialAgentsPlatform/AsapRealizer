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

import hmi.environmentbase.Embodiment;
import hmi.xml.XMLStructureAdapter;
import hmi.xml.XMLTokenizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import saiba.bml.BMLInfo;
import saiba.bml.core.Behaviour;
import asap.binding.SpecParameterDefault;
import asap.realizer.feedback.FeedbackManager;
import asap.realizer.pegboard.BMLBlockPeg;
import asap.realizer.planunit.ParameterException;
import asap.middlewareengine.embodiment.*;
import asap.middlewareengine.planunit.*;

public class MiddlewareBinding extends XMLStructureAdapter
{
    private ArrayList<MiddlewareUnitSpec> specs = new ArrayList<MiddlewareUnitSpec>();
    private Logger logger = LoggerFactory.getLogger(MiddlewareBinding.class.getName());
    private MiddlewareEmbodiment middlewareEmbodiment = null;

    public MiddlewareBinding(Embodiment embodiment)
    {
    	if (embodiment instanceof MiddlewareEmbodiment)
        {
            this.middlewareEmbodiment = (MiddlewareEmbodiment) embodiment;
        } else 
        {
            throw new RuntimeException("MiddlewareBinding requires an embodiment of type MiddlewareEmbodiment");	
        }
    }

    public List<TimedMiddlewareUnit> getMiddlewareUnit(FeedbackManager fbManager, BMLBlockPeg bbPeg, Behaviour b)
    {
        ArrayList<TimedMiddlewareUnit> tzus = new ArrayList<TimedMiddlewareUnit>();
        for (MiddlewareUnitSpec s : specs)
        {
            if (s.getType().equals(b.getXMLTag())
                    && hasEqualNameSpace(b,s.getSpecnamespace()) )
            {
                if (!s.satisfiesConstraints(b))
                {
                     //System.out.println("Constraint mismatch: "+b.getNamespace()+","+s.getSpecnamespace()+","+b.getXMLTag()+","+s.getType());
                }
                else
                {
                    //System.out.println("Found type and constraint match");
                    MiddlewareUnit zuCopy = s.middlewareUnit.copy(middlewareEmbodiment);
                    TimedMiddlewareUnit tzu = zuCopy.createTMU(fbManager, bbPeg, b.getBmlId(), b.id);
                    tzus.add(tzu);

                    // System.out.println("set def params");
                    // set default parameter values
                    for (SpecParameterDefault zupc : s.getParameterDefaults())
                    {
                        try
                        {
                            zuCopy.setParameterValue(zupc.name, zupc.value);
                        }
                        catch (ParameterException e)
                        {
                            logger.warn("Error in setting default value in getMiddlewareUnit, parameter " + zupc.name, e);
                        }
                        logger.debug("Setting parameter {} to default {}", zupc.name, zupc.value);
                    }

                    // System.out.println("Map params");
                    // map parameters
                    for (String param : s.getParameters())
                    {
                        if (b.specifiesParameter(param))
                        {
                            String value = b.getStringParameterValue(param);
                            try
                            {
                                zuCopy.setParameterValue(s.getParameter(param), value);
                            }
                            catch (ParameterException e)
                            {
                                logger.warn("Error in parameter mapping in getMiddlewareUnit, parameter " + param, e);
                            }
                            logger.debug("Setting parameter {} mapped to  {}", param, s.getParameter(param));
                        }
                    }
                    
                    // If preparation fails, drop this PU
                    /*
                    try
                    {
                        nuCopy.prepareImages();
                    }
                    catch (NUPrepareException e)
                    {
                        logger.error(e.getMessage());
                        tnus.remove(tnu);
                    }
                    */
                }
            }
        }
        return tzus;

    }

    @Override
    public void decodeContent(XMLTokenizer tokenizer) throws IOException
    {
        while (tokenizer.atSTag())
        {
            String tag = tokenizer.getTagName();
            if (tag.equals(MiddlewareUnitSpec.xmlTag()))
            {
                MiddlewareUnitSpec nuSpec = new MiddlewareUnitSpec();
                nuSpec.readXML(tokenizer);
                if (nuSpec.middlewareUnit != null) specs.add(nuSpec); // don't add failed  units to the binding
                else logger.warn("Dropped Middleware unit spec because we could not construct the Middleware unit");
                // println(null) causes error in Android
                // System.out.println(puSpec.getSpecnamespace());
            }
            else
            {
            	throw new RuntimeException("cannot read unknow tag in middlewarebinding: " + tag);
            }
        }
    }

    /*
     * The XML Stag for XML encoding
     */
    private static final String XMLTAG = "middlewarebinding";

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
    private boolean hasEqualNameSpace(Behaviour b, String ns)
    {
        if(b.getNamespace() == null && ns == null) return true;
        if(ns==null && b.getNamespace().equals(BMLInfo.BMLNAMESPACE))return true;
        if(ns==null)return false;
        if(ns.equals(b.getNamespace()))return true;
        return false;
    }
 
}
