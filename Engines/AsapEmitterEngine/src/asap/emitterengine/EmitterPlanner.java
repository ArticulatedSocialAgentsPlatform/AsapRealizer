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
import java.util.List;

import saiba.bml.BMLInfo;
import saiba.bml.core.Behaviour;
import asap.emitterengine.bml.CreateEmitterBehaviour;
import asap.emitterengine.planunit.CreateEmitterEU;
import asap.emitterengine.planunit.TimedEmitterUnit;
import asap.realizer.AbstractPlanner;
import asap.realizer.BehaviourPlanningException;
import asap.realizer.SyncAndTimePeg;
import asap.realizer.feedback.FeedbackManager;
import asap.realizer.pegboard.BMLBlockPeg;
import asap.realizer.pegboard.OffsetPeg;
import asap.realizer.pegboard.TimePeg;
import asap.realizer.planunit.KeyPosition;
import asap.realizer.planunit.ParameterException;
import asap.realizer.planunit.PlanManager;
import asap.realizer.scheduler.LinearStretchResolver;
import asap.realizer.scheduler.TimePegAndConstraint;
import asap.realizer.scheduler.UniModalResolver;
import asap.realizerport.RealizerPort;


public class EmitterPlanner extends AbstractPlanner<TimedEmitterUnit>
{

    // private static Logger logger = LoggerFactory.getLogger(EmitterPlanner.class.getName());

    private UniModalResolver resolver;

    private EmitterInfo emitterInfo = null;
    private RealizerPort realizerPort = null;
    private String characterId = null;

    public EmitterPlanner(FeedbackManager bfm, PlanManager<TimedEmitterUnit> planManager, EmitterInfo ei, RealizerPort rp, String charId)
    {
        super(bfm, planManager);
        resolver = new LinearStretchResolver();
        emitterInfo = ei;
        realizerPort = rp;
        characterId = charId;
        /* register the Emitter BML behaviors with the BML parser... */
        BMLInfo.addBehaviourType(emitterInfo.getXMLTag(), emitterInfo.getCreateEmitterBehaviour());

    }

    /**
     * Creates a TimedEmitterUnit that satisfies sacs and adds it to the plan. All registered BMLFeedbackListener are linked to this TimedEmitterUnit.
     */
    @Override
    public List<SyncAndTimePeg> addBehaviour(BMLBlockPeg bbPeg, Behaviour b, List<TimePegAndConstraint> sacs, TimedEmitterUnit teu)
            throws BehaviourPlanningException
    {
        List<SyncAndTimePeg> satps = new ArrayList<SyncAndTimePeg>();

        if (teu == null)
        {
            if (b instanceof CreateEmitterBehaviour)
            {
                CreateEmitterBehaviour ceb = (CreateEmitterBehaviour) b;
                CreateEmitterEU eu = new CreateEmitterEU();
                try
                {
                    Emitter em = (Emitter) emitterInfo.getEmitterClass().newInstance();
                    em.setCharacterId(characterId);
                    em.setRealizerPort(realizerPort);
                    eu.setEmitter(em);
                }
                catch (IllegalAccessException | InstantiationException e)
                {
                    throw new BehaviourPlanningException(b, "Behavior " + b.id
                            + " could not be constructed because the emitter could not be created, behavior omitted.");
                }
                try
                {
                    // set parameters
                    for (String name : emitterInfo.getRequiredParameters())
                    {
                        eu.setParameterValue(name, ceb.getStringParameterValue(name));
                    }
                    for (String name : emitterInfo.getOptionalParameters())
                    {
                        if (ceb.getStringParameterValue(name) != null)
                        {
                            eu.setParameterValue(name, ceb.getStringParameterValue(name));
                        }
                    }
                }
                catch (ParameterException e)
                {
                    throw new BehaviourPlanningException(b, "Behavior " + b.id
                            + " could not be constructed because the parameters could not be set, behavior omitted.");
                }                
                teu = new TimedEmitterUnit(fbManager, bbPeg, b.getBmlId(), b.id, eu);
                if (!teu.getEmitterUnit().hasValidParameters())
                {
                    throw new BehaviourPlanningException(b, "Behavior " + b.id
                            + " could not be constructed because the parameters are not valid, behavior omitted.");
                }
            }
            else
            {
                throw new BehaviourPlanningException(b, "Behavior " + b.id
                        + " could not be constructed because the behaviour type is unknown: " + b.getClass().getName());
            }
        }

        // apply syncs to tnu
        teu.resolveStartAndEndKeyPositions();
        linkSynchs(teu, sacs);

        planManager.addPlanUnit(teu);

        for (KeyPosition kp : teu.getPegs().keySet())
        {
            TimePeg p = teu.getPegs().get(kp);
            satps.add(new SyncAndTimePeg(b.getBmlId(), b.id, kp.id, p));
        }
        return satps;
    }

    @Override
    public TimedEmitterUnit resolveSynchs(BMLBlockPeg bbPeg, Behaviour b, List<TimePegAndConstraint> sac) throws BehaviourPlanningException
    {
        TimedEmitterUnit teu;
        if (b instanceof CreateEmitterBehaviour)
        {
            CreateEmitterBehaviour ceb = (CreateEmitterBehaviour) b;
            CreateEmitterEU eu = new CreateEmitterEU();
            try
            {
                Emitter em = (Emitter) emitterInfo.getEmitterClass().newInstance();
                em.setCharacterId(characterId);
                em.setRealizerPort(realizerPort);
                eu.setEmitter(em);
            }
            catch (IllegalAccessException | InstantiationException e)
            {
                throw new BehaviourPlanningException(b, "Behavior " + b.id
                        + " could not be constructed because the emitter could not be created, behavior omitted.");
            }
            try
            {

                // set parameters
                for (String name : emitterInfo.getRequiredParameters())
                {
                    eu.setParameterValue(name, ceb.getStringParameterValue(name));
                }
                for (String name : emitterInfo.getOptionalParameters())
                {
                    if (ceb.getStringParameterValue(name) != null)
                    {
                        eu.setParameterValue(name, ceb.getStringParameterValue(name));
                    }
                }
            }
            catch (ParameterException e)
            {
                throw new BehaviourPlanningException(b, "Behavior " + b.id
                        + " could not be constructed because the parameters could not be set, behavior omitted.");
            }            
            teu = new TimedEmitterUnit(fbManager, bbPeg, b.getBmlId(), b.id, eu);
            if (!teu.getEmitterUnit().hasValidParameters())
            {
                throw new BehaviourPlanningException(b, "Behavior " + b.id
                        + " could not be constructed because the parameters are not valid, behavior omitted.");
            }
        }
        else
        {
            throw new BehaviourPlanningException(b, "Behavior " + b.id
                    + " could not be constructed because the behaviour type is unknown: " + b.getClass().getName());
        }

        teu.resolveStartAndEndKeyPositions();
        resolver.resolveSynchs(bbPeg, b, sac, teu);
        return teu;
    }

    // link synchpoints in sac to tnu
    private void linkSynchs(TimedEmitterUnit teu, List<TimePegAndConstraint> sacs)
    {
        for (TimePegAndConstraint s : sacs)
        {
            for (KeyPosition kp : teu.getEmitterUnit().getKeyPositions())
            {
                if (s.syncId.equals(kp.id))
                {
                    if (s.offset == 0)
                    {
                        teu.setTimePeg(kp, s.peg);
                    }
                    else
                    {
                        teu.setTimePeg(kp, new OffsetPeg(s.peg, -s.offset));
                    }
                }
            }
        }
    }

    @Override
    public List<Class<? extends Behaviour>> getSupportedBehaviours()
    {
        List<Class<? extends Behaviour>> list = new ArrayList<Class<? extends Behaviour>>();
        list.add(emitterInfo.getCreateEmitterBehaviour());
        return list;
    }

    @Override
    public List<Class<? extends Behaviour>> getSupportedDescriptionExtensions()
    {
        List<Class<? extends Behaviour>> list = new ArrayList<Class<? extends Behaviour>>();
        return list;
    }

    @Override
    public double getRigidity(Behaviour beh)
    {
        return 0;
    }

}
