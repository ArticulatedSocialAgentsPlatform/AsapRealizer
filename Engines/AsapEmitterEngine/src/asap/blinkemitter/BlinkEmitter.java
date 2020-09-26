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
package asap.blinkemitter;

import hmi.util.StringUtil;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asap.emitterengine.Emitter;
import asap.realizerport.RealizerPort;

/**
 * Emits blink behaviors to a RealizerBridge with a certain probabilitydistribution. Distribution
 * parameters can be modified on the fly; the emitter will immediately change the blinking behavior.
 * Next blink bml block will always start after the previous blink block (append-after) -- even if
 * it were accidentally fired too early.
 * 
 * basic implementation: wait time equally distributed over avg-range..avg+range
 * better implementation:
 * - better calculation of expected non-blink duration (see Weissenfeld Liu Ostermann Figure 2)
 * - itti et al: model of blinkgenerator and blinksuppressor, also coupled to saccades (!)
 * 
 * What this emitter does NOT do is couple blinks to, e.g., ongoing speech, emotions, external
 * events, etc
 * 
 * @author Dennis Reidsma
 */
public class BlinkEmitter extends Emitter implements Runnable
{

    private Thread theThread = null;
    private Logger logger = LoggerFactory.getLogger(BlinkEmitter.class.getName());
    private String id = null;
    private String characterId = null;

    /** The realizerbridge */
    protected RealizerPort realizerBridge = null;

    private long lastblink = 0;

    private double currentwaitingtime = 0;

    private int blinkcount = 0;

    private boolean parameterschanged = false;

    private String scheduling = "";

    boolean running = true;

    // basic method parameters
    private double averagewaitingtime = 5; // seconds

    private double range = 3; // avg-range < waitingtime < avg+range

    private static BlinkEmitterInfo blinkEmitterInfo = new BlinkEmitterInfo();

    public BlinkEmitter()
    {
    }

    public void run()
    {
        lastblink = System.currentTimeMillis();
        while (running)
        {
            long now = System.currentTimeMillis();
            if (!running) break;
            if (parameterschanged) setWaitForNextBlink();
            if (averagewaitingtime == 0d) setWaitForNextBlink();
            if ((double) now - (double) lastblink >= currentwaitingtime * 1000d)
            {
                emitBlink(); // time past to blink -- blink now
                setWaitForNextBlink();
            }
            try
            {
                Thread.sleep((int) (currentwaitingtime * 1000d + lastblink - now));
            }
            catch (Exception ex)
            {
                logger.debug("Error waiting for the blink: ", ex);
            }
        }
        logger.debug("Stopping BlinkEmitter thread");
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

    protected void emitBlink()
    {
        String _characterId = "";
        String _bmlId = "blinkbml";
        if (characterId != null) {
            _characterId = " characterId=\""+characterId+"\" ";
            _bmlId = characterId+_bmlId;
        }

        lastblink = System.currentTimeMillis();
        scheduling = "composition=\"APPEND-AFTER("+_bmlId + blinkcount + ")\"";
        String interrupter = "";
        if(blinkcount>1)
        {
            interrupter = " bmla:interrupt=\""+_bmlId +blinkcount+"\" ";
        }
        
        String bml = "<bml xmlns=\"http://www.bml-initiative.org/bml/bml-1.0\" " + _characterId +" id=\"" + _bmlId + (blinkcount + 1) + "\" " + scheduling +interrupter
                + "xmlns:bmla=\"http://www.asap-project.org/bmla\"><faceLexeme id=\"b1\"  lexeme=\"BLINK\" start=\"0\" end=\"0.15\" "
                + "amount=\"1\" attackPeak=\"0.03\" relax=\"0.12\"/>";        
        realizerBridge.performBML(bml + "</bml>");
        blinkcount++;
    }

    protected void setWaitForNextBlink()
    {
        if (averagewaitingtime == 0)
        {
            currentwaitingtime = 1000f;
        }
        else
        {
            basicMethodSetWaitForNextBlink();
        }
    }

    /**
     * Basic method: average waiting time; actual time distributed equally over avg-range..avg+range
     */
    protected void basicMethodSetWaitForNextBlink()
    {
        currentwaitingtime = Math.random() * 2 * range - range + averagewaitingtime;
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
    public void setId(String id)
    {
        this.id = id;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public void setCharacterId(String characterId)
    {
        this.characterId = characterId;
    }

    @Override
    public String getCharacterId()
    {
        return this.characterId;
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

    static final String XMLTAG = "blinkemitter";

    public static String xmlTag()
    {
        return XMLTAG;
    }

    @Override
    public String getXMLTag()
    {
        return XMLTAG;
    }

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
        return blinkEmitterInfo.specifiesFloatParameter(name);
    }

    public boolean specifiesStringParameter(String name)
    {
        return blinkEmitterInfo.specifiesStringParameter(name);
    }

    public ArrayList<String> getOptionalParameters()
    {
        return blinkEmitterInfo.getOptionalParameters();
    }

    public ArrayList<String> getRequiredParameters()
    {
        return blinkEmitterInfo.getRequiredParameters();
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
