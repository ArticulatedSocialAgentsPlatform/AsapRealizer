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
package asap.animationengine.noise;

import saiba.bml.feedback.BMLSyncPointProgressFeedback;
import asap.animationengine.AnimationPlayer;
import asap.animationengine.motionunit.TimedAnimationMotionUnit;
import asap.motionunit.MUPlayException;
import asap.motionunit.TMUPlayException;
import asap.realizer.feedback.FeedbackManager;
import asap.realizer.pegboard.BMLBlockPeg;
import asap.realizer.pegboard.PegBoard;
import asap.realizer.planunit.KeyPosition;
import asap.realizer.planunit.TimedPlanUnitPlayException;

/**
 * Timed motion unit for noise motion units.
 * Specific to noise motion units is that they run in line with "real time". They call the play function of noise motion units
 * with a value t = globaltime - starttime(MU)
 * 
 * @author Dennis Reidsma
 * 
 */
public class NoiseTMU extends TimedAnimationMotionUnit
{
    private NoiseMU nmu;

    // double startTime = 0;

    public NoiseTMU(FeedbackManager bfm, BMLBlockPeg bmlBlockPeg, String bmlId, String id, NoiseMU nmu, PegBoard pb, AnimationPlayer aniPlayer)
    {
        super(bfm, bmlBlockPeg, bmlId, id, nmu, pb, aniPlayer);
        this.nmu = nmu;
    }

    @Override
    public void startUnit(double time) throws TimedPlanUnitPlayException
    {
        // startTime = time;
        sendProgress(0d, time);
    }

    @Override
    public void playUnit(double time) throws TMUPlayException
    {
        try
        {
            // logger.debug("Timed Motion Unit play {}",time);
            nmu.play(time);
        }
        catch (MUPlayException ex)
        {
            throw new TMUPlayException(ex.getLocalizedMessage(), this, ex);
        }
    }

    @Override
    public void stopUnit(double time)
    {
        sendProgress(1d, time);
    }

    /**
     * Send progress feedback for all key positions passed at canonical time t.
     * 
     * @param t canonical time 0 &lt= t &lt=1
     * @param time time since start of BML execution
     */
    protected void sendProgress(double t, double time)
    {
        for (KeyPosition k : nmu.getKeyPositions())
        {
            if (k.time <= t)
            {
                if (!progressHandled.contains(k))
                {
                    String bmlId = getBMLId();
                    String behaviorId = getId();
                    String syncId = k.id;
                    double bmlBlockTime = time - bmlBlockPeg.getValue();
                    feedback(new BMLSyncPointProgressFeedback(bmlId, behaviorId, syncId, bmlBlockTime, time));
                    progressHandled.add(k);
                }
            }
        }
    }

}
