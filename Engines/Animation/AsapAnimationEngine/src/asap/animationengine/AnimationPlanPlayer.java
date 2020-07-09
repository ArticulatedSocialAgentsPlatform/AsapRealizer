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
package asap.animationengine;

import hmi.animation.AdditiveRotationBlend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import asap.animationengine.gaze.RestGaze;
import asap.animationengine.motionunit.TimedAnimationUnit;
import asap.animationengine.restpose.RestPose;
import asap.realizer.feedback.FeedbackManager;
import asap.realizer.feedback.NullFeedbackManager;
import asap.realizer.pegboard.PegBoard;
import asap.realizer.planunit.PlanManager;
import asap.realizer.planunit.PlanPlayer;
import asap.realizer.planunit.PlanUnitPriorityComparator;
import asap.realizer.planunit.SingleThreadedPlanPlayer;
import asap.realizer.planunit.TimedPlanUnit;
import asap.realizer.planunit.TimedPlanUnitPlayException;
import asap.realizer.planunit.TimedPlanUnitPlayer;
import asap.realizer.planunit.TimedPlanUnitState;
import asap.realizerport.BMLFeedbackListener;

import com.google.common.collect.Sets;

/**
 * Specialized PlanPlayer that handles conflict resolution for TimedMotionUnits
 * @author hvanwelbergen
 */
@Slf4j
public class AnimationPlanPlayer implements PlanPlayer
{
    private final SingleThreadedPlanPlayer<TimedAnimationUnit> defPlayer;
    private final PlanManager<TimedAnimationUnit> planManager;
    private final FeedbackManager fbManager;
    private final TimedPlanUnitPlayer tpuPlayer;
    private final RestPose defaultRestPose;
    private final RestGaze defaultRestGaze;
    private RestPose currentRestPose;
    private RestGaze currentRestGaze;
    private PegBoard pegBoard;
    
    @Setter
    private AdditiveRotationBlend additiveBlender;

    
    public void setRestPose(RestPose rp)
    {
        currentRestPose = rp;
    }

    public RestPose getRestPose()
    {
        return currentRestPose;
    }

    public RestGaze getRestGaze()
    {
        return currentRestGaze;
    }

    public void setRestGaze(RestGaze g)
    {
        currentRestGaze = g;
    }

    public AnimationPlanPlayer(RestPose defaultRestPose, RestGaze defaultRestGaze, FeedbackManager fbm, PlanManager<TimedAnimationUnit> planManager,
            TimedPlanUnitPlayer tpuCallback, PegBoard pegBoard)
    {
        defPlayer = new SingleThreadedPlanPlayer<>(fbm, planManager, tpuCallback);
        fbManager = fbm;
        tpuPlayer = tpuCallback;
        this.planManager = planManager;
        this.defaultRestPose = defaultRestPose;
        this.defaultRestGaze = defaultRestGaze;
        this.pegBoard = pegBoard;
        currentRestPose = defaultRestPose;
        currentRestGaze = defaultRestGaze;
    }

    // /only update 5 seconds in the future (for performance reasons)
    private static final double UPDATE_BUFFER = 5;

    private void updateTiming(double t)
    {
        List<TimedAnimationUnit> tuUpdateFailed = new ArrayList<>();
        // check which units should be playing
        for (TimedAnimationUnit tmu : planManager.getPlanUnits())
        {
            if (tmu.isSubUnit()) continue;
            if (tmu.isPlaying() || tmu.isLurking())
            {
                if (tmu.getStartTime() < t + UPDATE_BUFFER)
                {
                    try
                    {
                        tmu.updateTiming(t);
                    }
                    catch (TimedPlanUnitPlayException e)
                    {
                        tuUpdateFailed.add(tmu);
                        log.warn("updateTiming failure, TimedMotionUnit dropped", e);
                        continue;
                    }
                }
            }
        }
        planManager.removePlanUnits(tuUpdateFailed, t);
    }

    @Override
    public synchronized void play(double t)
    {
        List<TimedAnimationUnit> tmuRemove = new ArrayList<>();
        List<TimedAnimationUnit> playingPlanUnits = new ArrayList<>();
        Set<String> physicalJoints = new HashSet<String>();
        Set<String> kinematicJoints = new HashSet<String>();

        playingPlanUnits.clear();
        tmuRemove.clear();
        log.debug("plan Units: {}", planManager.getPlanUnits());

        // long time = System.nanoTime();
        updateTiming(t);
        // log.debug("update time: {} ms", (System.nanoTime()-time)/1000000d );

        // check which units should be playing
        for (TimedAnimationUnit tmu : planManager.getPlanUnits())
        {
            if (t >= tmu.getStartTime() && (tmu.isPlaying() || tmu.isLurking()))
            {
                playingPlanUnits.add(tmu);
            }
        }

        log.debug("playing plan Units: {}", playingPlanUnits);

        // sort by priority
        Collections.sort(playingPlanUnits, new PlanUnitPriorityComparator());

        // playback
        List<TimedAnimationUnit> tmuAdd = playback(t, tmuRemove, playingPlanUnits, physicalJoints, kinematicJoints);
        planManager.addPlanUnits(tmuAdd);

        tpuPlayer.handlePlayExceptions(t, fbManager);
        tpuPlayer.handleStopExceptions(t);
        for (TimedPlanUnit tmuR : tmuRemove)
        {
            tpuPlayer.stopUnit(tmuR, t);
        }
        planManager.removeFinishedPlanUnits();
        currentRestPose.play(t, Sets.union(kinematicJoints, currentRestGaze.getKinematicJoints()), physicalJoints);     
        additiveBlender.blend();
        currentRestGaze.play(t, kinematicJoints, physicalJoints);        
    }

    private List<TimedAnimationUnit> playback(double t, List<TimedAnimationUnit> tmuRemove, List<TimedAnimationUnit> playingPlanUnits,
            Set<String> physicalJoints, Set<String> kinematicJoints)
    {
        List<TimedAnimationUnit> tmuAdd = new ArrayList<>();
        for (TimedAnimationUnit tmu : playingPlanUnits)
        {
            if (Sets.intersection(tmu.getKinematicJoints(), kinematicJoints).isEmpty()
                    && Sets.intersection(tmu.getKinematicJoints(), physicalJoints).isEmpty()
                    && Sets.intersection(tmu.getPhysicalJoints(), kinematicJoints).isEmpty())
            {
                tpuPlayer.playUnit(tmu, t);
                if (!tmu.isDone())
                {
                    kinematicJoints.addAll(tmu.getKinematicJoints());
                    physicalJoints.addAll(tmu.getPhysicalJoints());
                }
            }
            else
            {
                if (tmu.isLurking() && !tmu.isSubUnit())
                {
                    fbManager.puException(tmu, "Dropping " + tmu.getBMLId() + ":" + tmu.getId() + "with priority " + tmu.getPriority()
                            + " for higher priority behaviors before it was even started", t);
                }
                log.debug("Dropping {}:{}", tmu.getBMLId(), tmu.getId());
                Set<String> cleanup = new HashSet<String>(tmu.getKinematicJoints());
                cleanup.removeAll(kinematicJoints);
                cleanup.addAll(tmu.getPhysicalJoints());
                cleanup.removeAll(physicalJoints);
                TimedAnimationUnit tmuCleanup = this.currentRestPose.createTransitionToRest(NullFeedbackManager.getInstance(), cleanup, t,
                        tmu.getBMLId(), tmu.getId(), tmu.getBMLBlockPeg(), pegBoard);
                tmuCleanup.setSubUnit(true);

                tmuRemove.add(tmu);
                tmuAdd.add(tmuCleanup);
            }
        }
        if (!tmuAdd.isEmpty())
        {
            tmuAdd.addAll(playback(t, tmuRemove, tmuAdd, physicalJoints, kinematicJoints));
        }
        return tmuAdd;
    }

    @Override
    public void stopPlanUnit(String bmlId, String id, double globalTime)
    {
        defPlayer.stopPlanUnit(bmlId, id, globalTime);
    }

    @Override
    public void stopBehaviourBlock(String bmlId, double time)
    {
        defPlayer.stopBehaviourBlock(bmlId, time);
    }

    @Override
    public void interruptPlanUnit(String bmlId, String id, double globalTime)
    {
        defPlayer.interruptPlanUnit(bmlId, id, globalTime);
    }

    @Override
    public void interruptBehaviourBlock(String bmlId, double time)
    {
        defPlayer.interruptBehaviourBlock(bmlId, time);
    }

    @Override
    public void reset(double time)
    {
        currentRestPose = defaultRestPose;
        defaultRestPose.initialRestPose(time);
        currentRestGaze = defaultRestGaze;
        defPlayer.reset(time);
    }

    public RestPose getDefaultRestPose()
    {
        return defaultRestPose;
    }

    @Override
    public void setBMLBlockState(String bmlId, TimedPlanUnitState state)
    {
        defPlayer.setBMLBlockState(bmlId, state);
    }

    @Override
    public void shutdown()
    {
        defPlayer.shutdown();
    }

    public void addFeedbackListener(BMLFeedbackListener fl)
    {
        defPlayer.addFeedbackListener(fl);
    }

    public void updateTiming(String bmlId)
    {
        List<TimedAnimationUnit> failedBehaviors = new ArrayList<>();
        for (TimedAnimationUnit tmu : planManager.getPlanUnits(bmlId))
        {
            try
            {
                tmu.updateTiming(0);
            }
            catch (TimedPlanUnitPlayException e)
            {
                log.warn("Failure in updating the timing of TimedAnimationUnit {}, TimedAnimationUnit removed", tmu);
                failedBehaviors.add(tmu);
            }
        }
        planManager.removePlanUnits(failedBehaviors, 0);
    }

}
