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
package asap.breathingemitter;


import java.util.ArrayList;

import asap.emitterengine.Emitter;
import asap.emitterengine.EmitterInfo;
import asap.emitterengine.bml.CreateEmitterBehaviour;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/**
 
 * @author Dennis Reidsma
 */
public class BreathingEmitterInfo extends EmitterInfo
{

    public BreathingEmitterInfo()
    {
      optionalParameters.add("range");
      optionalParameters.add("avgwaitingtime");
    }
    
    static final String BMLTNAMESPACE = "http://hmi.ewi.utwente.nl/bmlt";
    
    public static String namespace()
    {
      return BMLTNAMESPACE;
    }
    @Override
    public String getNamespace()
    {
      return BMLTNAMESPACE;
    }
    
    static final String XMLTAG = "breathingemitter";
    
    public static String xmlTag()
    {
      return XMLTAG;
    }
    @Override
    public String getXMLTag()
    {
      return XMLTAG;
    }

    @Override
    public  boolean specifiesFloatParameter(String name)
    {
      return optionalParameters.contains(name) || requiredParameters.contains(name);
    }
    @Override
    public  boolean specifiesStringParameter(String name)
    {
      return false;
    }
    
    private  ArrayList<String> optionalParameters = new ArrayList<String>();
    private  ArrayList<String> requiredParameters = new ArrayList<String>();
    
    @Override
    public  ArrayList<String> getOptionalParameters()
    {
      return optionalParameters;
    }

    @Override
    public  ArrayList<String> getRequiredParameters()
    {
      return requiredParameters;
    }

    @Override
    public Class<? extends Emitter> getEmitterClass()
    {
      return BreathingEmitter.class;
    }
    @Override
    public Class<? extends CreateEmitterBehaviour> getCreateEmitterBehaviour()
    {
      return CreateBreathingEmitterBehaviour.class;
    }
         
}
