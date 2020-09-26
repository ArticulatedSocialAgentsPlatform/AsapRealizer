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

import hmi.util.StringUtil;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asap.emitterengine.Emitter;
import asap.realizerport.RealizerPort;

/**
 * based on simple blink emitter; uses shoulder animation to breathe
 * 
 * @author Dennis Reidsma
 */
public class BreathingEmitter extends Emitter implements Runnable
{

    private Thread theThread = null;
    private Logger logger = LoggerFactory.getLogger(BreathingEmitter.class.getName());
    private String id = null;
    private String characterId = null;

    /** The realizerbridge */
    protected RealizerPort realizerBridge = null;

    private long lastbreath = 0;

    private double currentwaitingtime = 0;

    private int breathcount = 0;

    private boolean parameterschanged = false;

    private String scheduling = "";

    boolean running = true;

    // basic method parameters
    private double averagewaitingtime = 5; // seconds

    private double range = 3; // avg-range < waitingtime < avg+range

    private static BreathingEmitterInfo breathingEmitterInfo = new BreathingEmitterInfo();

    public BreathingEmitter()
    {
    }

    public void run()
    {
        lastbreath = System.currentTimeMillis();
        while (running)
        {
            long now = System.currentTimeMillis();
            if (!running) break;
            if (parameterschanged) currentwaitingtime = setWaitForNextBreath();
            if (averagewaitingtime == 0d) currentwaitingtime = setWaitForNextBreath();
            if ((double) now - (double) lastbreath >= currentwaitingtime * 1000d)
            {
                currentwaitingtime = setWaitForNextBreath();
                emitBreath(); // time past to breathe -- breathe now
            }
            try
            {
                Thread.sleep((int) (currentwaitingtime * 1000d + lastbreath - now));
            }
            catch (Exception ex)
            {
                logger.debug("Error waiting for the breath: ", ex);
            }
        }
        logger.debug("Stopping BreathEmitter thread");
    }

    public void start()
    {
        theThread = new Thread(this);
        theThread.start();
    }

    public void stopRunning()
    {
        running = false;
        try
        {
            theThread.interrupt();
        }
        catch (Exception ex)
        {
        }
    }

    protected void emitBreath()
    {
        String _characterId = "";
        String _bmlId = "breathbml";
        if (characterId != null) {
            _characterId = " characterId=\""+characterId+"\" ";
            _bmlId = characterId+_bmlId;
        }

        lastbreath = System.currentTimeMillis();
        scheduling = "composition=\"APPEND-AFTER("+_bmlId+ breathcount + ")\"";
        String bml = "<bml xmlns=\"http://www.bml-initiative.org/bml/bml-1.0\" " + _characterId + " id=\""+_bmlId+ (breathcount + 1) + "\" "
                + scheduling + " xmlns:bmla=\"http://www.asap-project.org/bmla\">" + "<gesture id=\"b1\" lexeme=\"breathe\" start=\"0\" "
                + "ready=\"0\" stroke=\"" + (currentwaitingtime / 4) + "\" relax=\"" + (2 * currentwaitingtime / 3) + "\" end=\""
                + currentwaitingtime + "\" "
                // + "amount=\"1\""
                + " />";
        if (breathcount > 1)
        {
            bml += "<bmla:interrupt id=\"interruptPrevBreath\" target=\""+ _bmlId + breathcount + "\">" + "</bmla:interrupt>";
        }
        realizerBridge.performBML(bml + "</bml>");

        breathcount++;
        // System.out.println("Breath! " + breathcount);

    }

    protected double setWaitForNextBreath()
    {
        if (averagewaitingtime == 0)
        {
            return 1000f;
        }
        else
        {
            return basicMethodSetWaitForNextBreath();
        }

        // long now = System.currentTimeMillis();
        // if ((double)now-(double)lastbreath>= currentwaitingtime*1000d)
        // {
        // emitBreath(); //time past to breath -- breath now
        // setWaitForNextBreath();
        // }
    }

    /**
     * Basic method: average waiting time; actual time distributed equally over avg-range..avg+range
     */
    protected double basicMethodSetWaitForNextBreath()
    {
        return Math.random() * 2 * range - range + averagewaitingtime;
    }

    public void basicMethodSetAvg(double w)
    {
        averagewaitingtime = w;
        parameterschanged = true;
        try
        {
            theThread.interrupt();
        }
        catch (Exception ex)
        {
        }
    }

    public void basicMethodSetRange(double r)
    {
        range = r;
        parameterschanged = true;
        try
        {
            theThread.interrupt();
        }
        catch (Exception ex)
        {
        }
    }

    @Override
    public void setRealizerPort(RealizerPort port)
    {
        realizerBridge = port;
    }

    @Override
    public void setCharacterId(String characterId)
    {
        this.characterId = id;
    }

    @Override
    public String getCharacterId()
    {
        return this.characterId;
    }

    @Override
    public void setId(String id)
    {
        this.id = id;
    }

    @Override
    public String getId()
    {
        return id;
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

    /** start emitter. Needs to be already connected to realizerport. */
    // @Override
    // public void start() ... its a thread already...

    /**
     * stop and clean up emitter as soon as possible. After stop was called, no new BML should be emitted,
     * but cleaning up emission threads may take a while.
     */
    @Override
    public void stop()
    {
        stopRunning();
    }

    public boolean specifiesFloatParameter(String name)
    {
        return breathingEmitterInfo.specifiesFloatParameter(name);
    }

    public boolean specifiesStringParameter(String name)
    {
        return breathingEmitterInfo.specifiesStringParameter(name);
    }

    public ArrayList<String> getOptionalParameters()
    {
        return breathingEmitterInfo.getOptionalParameters();
    }

    public ArrayList<String> getRequiredParameters()
    {
        return breathingEmitterInfo.getRequiredParameters();
    }

    @Override
    public void setParameterValue(String name, String value)
    {
        if (StringUtil.isNumeric(value))
        {
            setFloatParameterValue(name, Float.parseFloat(value));
        }
        else
        {
            throw new RuntimeException("Cannot set parameter");
        }
    }

    @Override
    public void setFloatParameterValue(String name, float value)
    {
        if (name.equals("range")) basicMethodSetRange((double) value);
        if (name.equals("avgwaitingtime")) basicMethodSetAvg((double) value);
    }

    @Override
    public String getParameterValue(String name)
    {
        if (specifiesFloatParameter(name)) return "" + getFloatParameterValue(name);
        return null;
    }

    @Override
    public float getFloatParameterValue(String name)
    {
        if (name.equals("range")) return (float) range;
        if (name.equals("avgwaitingtime")) return (float) averagewaitingtime;
        return 0;
    }

    @Override
    public boolean hasValidParameters()
    {
        return (averagewaitingtime >= 0 && range < averagewaitingtime);
    }

}
