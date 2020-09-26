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
package asap.emitterengine;

import java.util.ArrayList;

import asap.realizerport.RealizerPort;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/**
 * Emits bml behaviors to a RealizerPort.
 * 
 * @author Dennis Reidsma
 */
public abstract class Emitter 
{

    public abstract void setRealizerPort(RealizerPort port);
    public abstract void setId(String id);
    public abstract String getId();

    public abstract void setCharacterId(String id);
    public abstract String getCharacterId();
    
    public static String namespace()
    {
      return "http://hmi.ewi.utwente.nl/bmlt";
    }
    public abstract String getNamespace();
    public static String xmlTag() 
    {
      return "emitter";
    }
    public abstract String getXMLTag();
    
    /** start emitter. Needs to be already connected to realizerport. */
    public abstract void start();
    
    /** 
     * stop and clean up emitter as soon as possible. After stop was called, 
     * no new BML should be emitted, but cleaning up emission threads may take a while. 
     */
    public abstract void stop();

    public  boolean specifiesFloatParameter(String name)
    {
      return false;
    }
    public  boolean specifiesStringParameter(String name)
    {
      return false;
    }
    public  ArrayList<String> getOptionalParameters()
    {
      return new ArrayList<String>();
    }
    public  ArrayList<String> getRequiredParameters()
    {
      return new ArrayList<String>();
    }
    public abstract void setParameterValue(String name, String value);
    public abstract void setFloatParameterValue(String name, float value);
    public abstract String getParameterValue(String name);
    public abstract float getFloatParameterValue(String name);
    public abstract boolean hasValidParameters();
    
}
