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
package asap.emitterengine.loader;

import hmi.environmentbase.EmbodimentLoader;
import hmi.environmentbase.Environment;
import hmi.environmentbase.Loader;
import hmi.xml.XMLStructureAdapter;
import hmi.xml.XMLTokenizer;

import java.io.IOException;
import java.util.HashMap;

import asap.emitterengine.EmitterInfo;
import asap.emitterengine.EmitterPlanner;
import asap.emitterengine.planunit.TimedEmitterUnit;
import asap.realizer.DefaultEngine;
import asap.realizer.DefaultPlayer;
import asap.realizer.Engine;
import asap.realizer.Player;
import asap.realizer.planunit.PlanManager;
import asap.realizer.planunit.PlanPlayer;
import asap.realizer.planunit.SingleThreadedPlanPlayer;
import asap.realizerembodiments.AsapRealizerEmbodiment;
import asap.realizerembodiments.EngineLoader;


public class EmitterEngineLoader implements EngineLoader
{
    private XMLStructureAdapter adapter = new XMLStructureAdapter();

    private Engine engine = null;
    private PlanManager<TimedEmitterUnit> planManager = null;
    private Player player = null;
    private String id = "";
    private String characterId = "";

    private Class<?> emitterInfoClass = null;

    private AsapRealizerEmbodiment are = null;

    @Override
    public void readXML(XMLTokenizer tokenizer, String loaderId, String vhId, String vhName, Environment[] environments,
            Loader... requiredLoaders) throws IOException
    {
        id = loaderId;
        characterId = vhId;
        for (Loader e : requiredLoaders)
        {
            if (e instanceof EmbodimentLoader && ((EmbodimentLoader) e).getEmbodiment() instanceof AsapRealizerEmbodiment) are = (AsapRealizerEmbodiment) ((EmbodimentLoader) e)
                    .getEmbodiment();
        }
        if (are == null)
        {
            throw new RuntimeException("EmitterEngineLoader requires an EmbodimentLoader containing a AsapRealizerEmbodiment");
        }
        while (!tokenizer.atETag("Loader"))
        {
            readSection(tokenizer);
        }
        constructEngine(tokenizer);
    }

    @Override
    public void unload()
    {
        engine.shutdown();
    }

    protected void readSection(XMLTokenizer tokenizer) throws IOException
    {
        HashMap<String, String> attrMap = null;
        if (tokenizer.atSTag("EmitterInfo"))
        {
            attrMap = tokenizer.getAttributes();
            try
            {
                emitterInfoClass = Class.forName(adapter.getRequiredAttribute("class", attrMap, tokenizer));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new RuntimeException("Cannnot load emitter class: " + e);
            }
            tokenizer.takeEmptyElement("EmitterInfo");
        }
        else
        {
            throw tokenizer.getXMLScanException("Unknown tag in Loader content");
        }
    }

    /** tokenizer used for throwing scanexceptions */
    private void constructEngine(XMLTokenizer tokenizer)
    {
        EmitterInfo ei = null;
        try
        {

            ei = (EmitterInfo) emitterInfoClass.newInstance();
        }
        catch (Exception ex)
        {
            throw tokenizer.getXMLScanException("Cannot create EmitterEngine because " + emitterInfoClass + " cannot be instantiated");
        }
        planManager = new PlanManager<TimedEmitterUnit>();
        PlanPlayer planPlayer = new SingleThreadedPlanPlayer<TimedEmitterUnit>(are.getFeedbackManager(), planManager);
        player = new DefaultPlayer(planPlayer);
        EmitterPlanner planner = new EmitterPlanner(are.getFeedbackManager(), planManager, ei, are.getRealizerPort(), characterId);
        engine = new DefaultEngine<TimedEmitterUnit>(planner, player, planManager);
        engine.setId(id);
        engine.setCharacterId(characterId);

        // add engine to realizer;
        are.addEngine(engine);

    }

    /** Return the Engine that was constructed from the XML specification */
    public Engine getEngine()
    {
        return engine;
    }

    public PlanManager<TimedEmitterUnit> getPlanManager()
    {
        return planManager;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String newId)
    {
        id = newId;
    }
}
