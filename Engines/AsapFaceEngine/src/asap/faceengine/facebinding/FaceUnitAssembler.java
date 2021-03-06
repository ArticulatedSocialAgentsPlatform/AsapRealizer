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
package asap.faceengine.facebinding;

import hmi.faceanimation.converters.FACS2MorphConverter;
import hmi.faceanimation.model.FACSConfiguration;
import hmi.util.Resources;
import hmi.xml.XMLStructureAdapter;
import hmi.xml.XMLTokenizer;

import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asap.faceengine.faceunit.AUFU;
import asap.faceengine.faceunit.FACS2MorphFU;
import asap.faceengine.faceunit.FACSFU;
import asap.faceengine.faceunit.FaceUnit;
import asap.faceengine.faceunit.MorphFU;
import asap.faceengine.faceunit.PlutchikFU;

/**
 * Constructs a faceunit from the XML description
 * @author hvanwelbergen
 *
 */
@Slf4j
class FaceUnitAssembler extends XMLStructureAdapter
{
    private static Logger logger = LoggerFactory.getLogger(FaceUnitAssembler.class.getName());

    private FaceUnit faceUnit;

    public FaceUnitAssembler()
    {
    }

    @Override
    public void decodeAttributes(HashMap<String, String> attrMap, XMLTokenizer tokenizer)
    {
        String type = getRequiredAttribute("type", attrMap, tokenizer);
        String className = getOptionalAttribute("class", attrMap, null);
        
        if (type.equals("Morph"))
        {
            MorphFU fu = new MorphFU();
            faceUnit = fu;
        }
        else if(type.equals("class"))
        {
            if (className != null)
            {
                Class<?> muClass;
                try
                {
                    muClass = Class.forName(className);
                }
                catch (ClassNotFoundException e)
                {
                    faceUnit = null;
                    log.warn("Cannot instantiate MotionUnit \"{}\"", className);
                    log.warn("Exception: ", e);
                    return;
                }
                if (!FaceUnit.class.isAssignableFrom(muClass))
                {
                    faceUnit = null;
                    log.warn("{} does not implement the MotionUnit interface", className);
                    return;
                }
                
                try
                {
                    faceUnit = (FaceUnit) (muClass.newInstance());
                }
                catch (InstantiationException e)
                {
                    faceUnit = null;
                    log.warn("Cannot instantiate MotionUnit \"{}\"", className);
                    log.warn("Exception: ", e);

                }
                catch (IllegalAccessException e)
                {
                    faceUnit = null;
                    log.warn("Cannot instantiate MotionUnit \"{}\"", className);
                    log.warn("Exception: ", e);
                }                
            }
        }
        else if (type.equals("Plutchik"))
        {
            PlutchikFU fu = new PlutchikFU();
            faceUnit = fu;
        }
        else if (type.equals("AU"))
        {
            AUFU fu = new AUFU();
            faceUnit = fu;
        }
        else if (type.equals("AU2Morph"))
        {
            FACS2MorphFU fu = new FACS2MorphFU();
            faceUnit = fu;
            FACS2MorphConverter f2mconv = null;

            String f2mResourcePath = getOptionalAttribute("facs2morphmappingresources", attrMap, "");
            String f2mResourceFileName = getOptionalAttribute("facs2morphmappingfilename", attrMap, "");
            if (!f2mResourceFileName.equals(""))
            {
            	f2mconv = new FACS2MorphConverter();
            	f2mconv.readXML(new Resources(f2mResourcePath).getReader(f2mResourceFileName));
            }
            
            fu.setFACS2MorphConverter(f2mconv);
        }
        else if (type.equals("FACS2Morph"))
        {
			FACS2MorphFU fu = new FACS2MorphFU();
			faceUnit = fu;
			String filename = getRequiredAttribute("filename", attrMap, null);
			FACSConfiguration fc = new FACSConfiguration();
			try {
				fc.readXML(new Resources("").getReader(filename));
				fu.setConfig(fc);
			} catch (Exception e) {
				faceUnit = null;
				logger.warn("Cannot read FACS configuration from file \"{}\"; error: {}", filename, e.getMessage());
			}
		} 
        else if (type.equals("FACS"))
        {
            FACSFU fu = new FACSFU();
            faceUnit = fu;
            String filename = getRequiredAttribute("filename", attrMap, null);
            FACSConfiguration fc = new FACSConfiguration();
            try
            {
                fc.readXML(new Resources("").getReader(filename));
                fu.setConfig(fc);
            }
            catch (Exception e)
            {
                faceUnit = null;
                logger.warn("Cannot read FACS configuration from file \"{}\"; error: {}", filename, e.getMessage());
            }
        }
        else
        {
            logger.warn("Cannot read FaceUnit type \"{}\" in FaceBinding; omitting this FaceUnit", type);
        }
    }

    /**
     * @return the faceUnit
     */
    public FaceUnit getFaceUnit()
    {
        return faceUnit;
    }

    /*
     * The XML Stag for XML encoding
     */
    private static final String XMLTAG = "FaceUnit";

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
