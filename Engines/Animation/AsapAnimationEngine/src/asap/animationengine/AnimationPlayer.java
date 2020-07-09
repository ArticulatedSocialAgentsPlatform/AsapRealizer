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
import hmi.animation.Hanim;
import hmi.animation.Skeleton;
import hmi.animation.SkeletonPose;
import hmi.animation.VJoint;
import hmi.animation.VJointPartsMap;
import hmi.animation.VObjectTransformCopier;
import hmi.math.Quat4f;
import hmi.math.Vec3f;
import hmi.mixedanimationenvironment.MixedAnimationPlayer;
import hmi.physics.PhysicalHumanoid;
import hmi.physics.PhysicalJoint;
import hmi.physics.PhysicalSegment;
import hmi.physics.controller.PhysicalController;
import hmi.physics.mixed.MixedSystem;
import hmi.util.PhysicsSync;
import hmi.worldobjectenvironment.WorldObjectManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableList;

import lombok.extern.slf4j.Slf4j;
import net.jcip.annotations.GuardedBy;
import asap.animationengine.gaze.GazeInfluence;
import asap.animationengine.gaze.RestGaze;
import asap.animationengine.mixed.MixedPlayer;
import asap.animationengine.motionunit.AnimationUnit;
import asap.animationengine.motionunit.TimedAnimationUnit;
import asap.animationengine.restpose.RestPose;
import asap.animationengine.restpose.SkeletonPoseRestPose;
import asap.realizer.Player;
import asap.realizer.feedback.FeedbackManager;
import asap.realizer.pegboard.BMLBlockPeg;
import asap.realizer.pegboard.PegBoard;
import asap.realizer.pegboard.TimePeg;
import asap.realizer.planunit.TimedPlanUnitState;

/**
 * Manages the execution of a plan containing TimedMotionUnits. The AnimationPlayer is different from the DefaultPlayer in that the TimedMotionUnits
 * act on a separate body rather than directly on the virtual human, and that their results are combined and applied to the virtual human at the end
 * of each play.
 * 
 * @author Herwin
 */
@Slf4j
public class AnimationPlayer implements Player, MixedAnimationPlayer
{
    private final VJoint vPrev;
    private final VJoint vCurr;
    private final VJoint vNext;
    private final VJointPartsMap vPrevMap;
    private final VJointPartsMap vNextMap;
    private final VJointPartsMap vCurrMap;
    // private final VJoint vAdditive;

    private PhysicalHumanoid pHuman;

    private VObjectTransformCopier votcCurrToPrev;
    private VObjectTransformCopier votcNextToCurr;
    private AdditiveRotationBlend additiveBlender;
    private boolean prevValid;

    @GuardedBy("this")
    private List<PhysicalController> controllers = new ArrayList<PhysicalController>();

    private final AnimationPlanPlayer app;

    private List<MixedSystem> mSystems;
    private MixedPlayer mPlayer;
    private float stepTime;

    private Skeleton prevSkel, curSkel, nextSkel;
    private SkeletonPose vPrevStartPose;
    private SkeletonPose vCurrStartPose;
    private SkeletonPose vNextStartPose;
    private boolean prevValidOld = false;
    protected WorldObjectManager woManager;

    public void setRestPose(RestPose rp)
    {
        app.setRestPose(rp);
    }

    public RestPose getRestPose()
    {
        return app.getRestPose();
    }

    public RestGaze getRestGaze()
    {

        return app.getRestGaze();
    }

    public void setRestGaze(RestGaze g)
    {
        app.setRestGaze(g);
    }

    public AnimationPlayer(VJoint vP, VJoint vC, VJoint vN, List<MixedSystem> m, float h, AnimationPlanPlayer planPlayer)
    {
        this(vP, vC, vN, m, h, null, planPlayer);
    }

    private void setAdditiveToIdentity()
    {
        additiveBlender.setIdentityRotation();
    }

    private void setVNextToIdentity()
    {
        for (VJoint vj : vNextMap.getJoints())
        {
            if (vj.getSid() != null)
            {
                vj.setRotation(Quat4f.getIdentity());
            }
        }
    }

    // if a joint in vnext has the identity value
    // put the (non-identity) value of vCurrent back (needed for smooth physical simulation)
    private void applyCurrentOnVNext()
    {
        float q[] = Quat4f.getQuat4f();
        float t[] = Vec3f.getVec3f();
        for (VJoint vj : vNextMap.getJoints())
        {
            if (vj.getSid().equals(Hanim.HumanoidRoot)) {
                vCurrMap.get(vj.getSid()).getTranslation(t);
                vj.setTranslation(t);
            }
        	
            // XXX:ugliness, the eyes move so fast that they might have identity rotation in the next frame and non-identity rotation in the previous
            if (vj.getSid() != null && !vj.getSid().equals(Hanim.l_eyeball_joint) && !vj.getSid().equals(Hanim.r_eyeball_joint))
            {
                vj.getRotation(q);
                if (Quat4f.epsilonEquals(q, Quat4f.getIdentity(), 0.001f))
                {
                    vCurrMap.get(vj.getSid()).getRotation(q);
                    vj.setRotation(q);
                }
            }
        }
    }

    public AnimationPlayer(VJoint vP, VJoint vC, VJoint vN, List<MixedSystem> m, float h, WorldObjectManager wom,
            AnimationPlanPlayer planPlayer)
    {
        vPrev = vP;
        vCurr = vC;
        vNext = vN;
        vPrevMap = new VJointPartsMap(vP);
        vCurrMap = new VJointPartsMap(vC);
        vNextMap = new VJointPartsMap(vN);

        // vAdditiveMap = new VJointPartsMap(vAdditive);

        prevSkel = new Skeleton("prevSkel", vPrev);
        curSkel = new Skeleton("curSkel", vCurr);
        nextSkel = new Skeleton("nextSkel", vNext);

        woManager = wom;
        planPlayer.getRestGaze().setAnimationPlayer(this);// XXX ugly..
        // VJoint[] emptyArray = new VJoint[0];

        vPrevStartPose = new SkeletonPose("prev", prevSkel, "TR");
        vCurrStartPose = new SkeletonPose("cur", curSkel, "TR");
        vNextStartPose = new SkeletonPose("next", nextSkel, "TR");
        vPrevStartPose.setFromTarget();
        vCurrStartPose.setFromTarget();
        vNextStartPose.setFromTarget();
        additiveBlender = new AdditiveRotationBlend(vNext, ImmutableList.of(), vNext);
        // setAdditiveToIdentity();
        planPlayer.setAdditiveBlender(additiveBlender);
        
        mSystems = m;
        pHuman = mSystems.get(0).getPHuman();
        pHuman.setEnabled(true);
        
        int humanoidRootIdx = vNext.getParts().indexOf(getVNextPartBySid(Hanim.HumanoidRoot));
        votcNextToCurr = VObjectTransformCopier.newInstanceFromVJointTree(vNext, vCurr, "T1R");
        votcNextToCurr.setRootTranslationIdx(humanoidRootIdx);
        votcCurrToPrev = VObjectTransformCopier.newInstanceFromVJointTree(vCurr, vPrev, "T1R");
        votcNextToCurr.setRootTranslationIdx(humanoidRootIdx);

        mPlayer = new MixedPlayer(mSystems.get(0), vPrev, vCurr, vNext);
        mPlayer.setH(h);
        stepTime = h;
        app = planPlayer;
        prevValid = false;
    }

    public double getTransitionToRestDuration(Set<String> joints)
    {
        return app.getRestPose().getTransitionToRestDuration(vCurr, joints);
    }

    public double getGazeTransitionToRestDuration(GazeInfluence influence)
    {
        return app.getRestGaze().getTransitionToRestDuration(influence);
    }

    public TimedAnimationUnit createTransitionToRest(FeedbackManager fbm, Set<String> joints, double startTime, double duration,
            String bmlId, String id, BMLBlockPeg bmlBlockPeg, PegBoard pb)
    {
        return app.getRestPose().createTransitionToRest(fbm, joints, startTime, duration, bmlId, id, bmlBlockPeg, pb);
    }

    public AnimationUnit createTransitionToRest(Set<String> joints)
    {
        return app.getRestPose().createTransitionToRest(joints);
    }

    public TimedAnimationUnit createTransitionToRest(FeedbackManager fbm, Set<String> joints, TimePeg startPeg, TimePeg endPeg,
            String bmlId, String id, BMLBlockPeg bmlBlockPeg, PegBoard pb)
    {
        return app.getRestPose().createTransitionToRest(fbm, joints, startPeg, endPeg, bmlId, id, bmlBlockPeg, pb);
    }

    @Override
    public synchronized void stopBehaviour(String bmlId, String id, double globalTime)
    {
        app.stopPlanUnit(bmlId, id, globalTime);
    }

    @Override
    public synchronized void interruptBehaviour(String bmlId, String id, double globalTime)
    {
        app.interruptPlanUnit(bmlId, id, globalTime);
    }

    public WorldObjectManager getWoManager()
    {
        return woManager;
    }

    /**
     * Resets all controllers, mixed systems (that is: restores their start physical humanoids and sets connector velocity to 0) and sets vPrev, vNext
     * and vCurr to their pose at initialization
     * 
     * Stops all timed motion units and removes them from the plan
     */
    @Override
    public synchronized void reset(double time)
    {
        prevValid = false;

        vPrevStartPose.setToTarget();
        vCurrStartPose.setToTarget();
        vNextStartPose.setToTarget();
        additiveBlender.clear();
        synchronized (PhysicsSync.getSync())
        {
            for (PhysicalController p : controllers)
            {
                p.reset();
            }
            for (MixedSystem ms : mSystems)
            {
                ms.reset(vNext);
            }
        }
        app.reset(time);
    }

    /**
     * Sets the reset pose to the current vNext. The reset pose gets set to all the mixed systems whenever the the reset function is called. Typical
     * workflow:<br>
     * 1. player = new AnimationPlayer(...<br>
     * 2. vNext = player.getVNext(); <br>
     * 3. set pose in vNext <br>
     * 4. vNext.calculateMatrices();<br>
     * 5. player.setResetPose();<br>
     */
    public synchronized void setResetPose()
    {
        vNext.calculateMatrices();
        vNextStartPose.setFromTarget();
        synchronized (PhysicsSync.getSync())
        {
            for (MixedSystem ms : mSystems)
            {
                ms.setResetPose(vNext);
                ms.reset(vNext);
            }
        }
    }

    @Override
    public void play(double time)
    {
    }

    @Override
    public synchronized void playStep(double prevTime)
    {
        log.debug("time {}", prevTime);
        setAdditiveToIdentity();
        controllers.clear();
        prevValidOld = prevValid;

        playKinematics(prevTime);
        playPhysics(prevTime);
    }

    private void playPhysics(double prevTime)
    {
        List<String> controlledJoints = getRequiredPhJoints();
        MixedSystem bestMatch = getBestMixedSystemMatch(controlledJoints, getDesiredPhJoints(controlledJoints));

        synchronized (PhysicsSync.getSync())
        {
            if (bestMatch == null)
            {
                log.warn("Could not find a mixed system that contains joints: {} ", controlledJoints);
                bestMatch = mPlayer.getSystem();
            }

            if (bestMatch != mPlayer.getSystem())
            {
                log.debug("Switching system {}", prevTime);
                mPlayer.switchSystem(bestMatch, stepTime, vPrev, vCurr, vNext, prevValidOld);
                pHuman.setEnabled(false);
                pHuman = mPlayer.getSystem().getPHuman();
                pHuman.setEnabled(true);
                for (PhysicalController p : controllers)
                {
                    p.setPhysicalHumanoid(pHuman);
                }
            }
            mPlayer.play(stepTime);
            pHuman.updateCOM(stepTime);

            for (PhysicalController p : controllers)
            {
                p.setPhysicalHumanoid(pHuman); // Could be done more elegantly,
                                               // not needed to do this every
                                               // frame?
                // But at least ensures that the controller always runs on the
                // correct physical humanoid now.
                p.update(stepTime);
            }
        }
    }

    // The best matching mSystem is the one that has (in order of importance):
    // 1. All required joints for all controllers
    // 2. The most matching desired joints
    // 3. The least total number of physical joints
    // Note that desired physical joints, when selected, still override the kinematic motion on the same joint
    private MixedSystem getBestMixedSystemMatch(List<String> controlledJoints, List<String> desiredJoints)
    {
        MixedSystem bestMatch = null;
        int jointsInPH = Integer.MAX_VALUE;
        int nrOfDesiredJoints = -1;
        for (MixedSystem m : mSystems)
        {
            PhysicalHumanoid ph = m.getPHuman();
            boolean foundAll = true;
            for (String jid : controlledJoints)
            {
                boolean found = false;
                if (ph.getRootSegment() != null && ph.getRootSegment().getSid().equals(jid))
                {
                    found = true;
                    break;
                }
                for (PhysicalJoint pj : ph.getJoints())
                {
                    if (pj.getName().equals(jid))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    foundAll = false;
                    break;
                }
            }
            if (foundAll)
            {
                // all required joints found, check for most desired joints
                int dJoints = 0;
                for (String jid : desiredJoints)
                {
                    if (ph.getRootSegment() != null && ph.getRootSegment().getSid().equals(jid))
                    {
                        dJoints++;
                        continue;
                    }
                    for (PhysicalJoint pj : ph.getJoints())
                    {
                        if (pj.getName().equals(jid))
                        {
                            dJoints++;
                            break;
                        }
                    }
                }
                if (dJoints > nrOfDesiredJoints)
                {
                    nrOfDesiredJoints = dJoints;
                    bestMatch = m;
                    jointsInPH = ph.getJoints().size();
                }
                else if (dJoints == nrOfDesiredJoints)
                {
                    if (ph.getJoints().size() < jointsInPH)
                    {
                        bestMatch = m;
                        jointsInPH = ph.getJoints().size();
                    }
                }
            }
        }
        return bestMatch;
    }

    private List<String> getDesiredPhJoints(List<String> controlledJoints)
    {
        ArrayList<String> desiredJoints = new ArrayList<String>();
        for (PhysicalController p : controllers)
        {
            for (String jid : p.getDesiredJointIDs())
            {
                if (!desiredJoints.contains(jid) && !controlledJoints.contains(jid))
                {
                    desiredJoints.add(jid);
                }
            }
        }
        return desiredJoints;
    }

    private List<String> getRequiredPhJoints()
    {
        ArrayList<String> controlledJoints = new ArrayList<String>();
        for (PhysicalController p : controllers)
        {
            for (String jid : p.getRequiredJointIDs())
            {
                if (!controlledJoints.contains(jid))
                {
                    controlledJoints.add(jid);
                }
            }
        }
        return controlledJoints;
    }

    private void playKinematics(double prevTime)
    {
        if (prevValid)
        {
            votcCurrToPrev.copyConfig();
            votcNextToCurr.copyConfig();
            setVNextToIdentity();
            app.play(prevTime);            
        }
        else
        {
            setVNextToIdentity();
            app.play(prevTime);            

            votcNextToCurr.copyConfig();
            votcCurrToPrev.copyConfig();
            prevValid = true;
        }
        applyCurrentOnVNext();
    }

    public synchronized void copyPhysics()
    {
        synchronized (PhysicsSync.getSync())
        {
            pHuman.copy();

            // copy configuration of physically steered joints on current to next, so we can use them in
            // world-IK with almost correct body postures on proc animation in the next animation step
            float q[] = new float[4];
            float tr[] = new float[3];
            for (PhysicalSegment ps : pHuman.getSegments())
            {
                VJoint vC = vCurr.getPart(ps.getSid());
                VJoint vN = vNext.getPart(ps.getSid());
                vC.getRotation(q);
                vN.setRotation(q);
                if (ps.getSid().equals(Hanim.HumanoidRoot))
                {
                    vC.getTranslation(tr);
                    vN.setTranslation(tr);
                }
            }
        }
    }

    public synchronized void addController(PhysicalController controller)
    {
        if (!controllers.contains(controller))
        {
            controllers.add(controller);
        }
    }

    /**
     * Get the set of joints that is to be animated
     * 
     * @return the vNext
     */
    public VJoint getVNext()
    {
        return vNext;
    }

    public VJoint getVNextPartBySid(String sid)
    {
        return vNextMap.get(sid);
    }

    public VJoint getVCurrPartBySid(String sid)
    {
        return vCurrMap.get(sid);
    }

    public VJoint getVPrevPartBySid(String sid)
    {
        return vPrevMap.get(sid);
    }

    public VJoint constructAdditiveBody()
    {
        VJoint vAdditive = getVCurr().copyTree("additive" + UUID.randomUUID());
        for (VJoint vj : vAdditive.getParts())
        {
            vj.setRotation(Quat4f.getIdentity());
        }
        log.info("constructing additive body for all joints ");
        additiveBlender.addVJoint(vAdditive);
        return vAdditive;
    }

    public VJoint constructAdditiveBody(Set<String> sids)
    {
        VJoint vAdditive = getVCurr().copyTree("additive" + UUID.randomUUID());
        for (VJoint vj : vAdditive.getParts())
        {
            vj.setRotation(Quat4f.getIdentity());
        }
        log.info("constructing additive body for joints " + sids);
        additiveBlender.addVJoint(vAdditive, sids);
        return vAdditive;
    }

    public void removeAdditiveBody(VJoint vj)
    {
        additiveBlender.removeVJoint(vj);
    }

    public void filterAdditiveBody(VJoint vj, Set<String> sids)
    {
    	log.info("filter additive body to joints " + sids);
        additiveBlender.filterVJoint(vj, sids);
    }

    /**
     * Get the prev set of joints that is to be animated
     * 
     * @return the vPrev
     */
    public VJoint getVPrev()
    {
        return vPrev;
    }

    /**
     * Get the current set of joints that is to be animated
     * 
     * @return the vCurr
     */
    public VJoint getVCurr()
    {
        return vCurr;
    }

    /**
     * @return the pHuman
     */
    public synchronized PhysicalHumanoid getPHuman()
    {
        return pHuman;
    }

    /**
     * @return the stepTime
     */
    public synchronized float getStepTime()
    {
        return stepTime;
    }

    /**
     * @return the mSystems
     */
    public List<MixedSystem> getMixedSystems()
    {
        return mSystems;
    }

    @Override
    public void setBMLBlockState(String bmlId, TimedPlanUnitState state)
    {
        app.setBMLBlockState(bmlId, state);
    }

    @Override
    public void stopBehaviourBlock(String bmlId, double time)
    {
        app.stopBehaviourBlock(bmlId, time);
    }

    @Override
    public void interruptBehaviourBlock(String bmlId, double time)
    {
        app.interruptBehaviourBlock(bmlId, time);
    }

    @Override
    public void shutdown()
    {

    }

    public void verifyTime(double time)
    {
    }

    @Override
    public void updateTiming(String bmlId)
    {
        app.updateTiming(bmlId);
    }

    // Return the default restpose. I.e. the pose that was set on calibration/in start pose
    public RestPose getDefaultRestPose() {
        return app.getDefaultRestPose();
    }
}
