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
package asap.animationengine.restpose;

import hmi.animation.ConfigList;
import hmi.animation.Hanim;
import hmi.animation.SkeletonInterpolator;
import hmi.animation.VJoint;
import hmi.animation.VObjectTransformCopier;
import hmi.animation.motiongraph.MotionGraph;
import hmi.animation.motiongraph.alignment.IAlignment;
import hmi.animation.motiongraph.xml.MotionGraphXML;
import hmi.math.Quat4f;
import hmi.math.Vec3f;
import hmi.util.Resources;
import hmi.xml.XMLTokenizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import asap.animationengine.AnimationPlayer;
import asap.animationengine.motionunit.AnimationUnit;
import asap.animationengine.motionunit.MUSetupException;
import asap.animationengine.motionunit.TimedAnimationMotionUnit;
import asap.animationengine.motionunit.TimedAnimationUnit;
import asap.animationengine.transitions.BlendingMotionUnit;
import asap.animationengine.transitions.T1RTransitionToPoseMU;
import asap.animationengine.transitions.TransitionMU;
import asap.realizer.feedback.FeedbackManager;
import asap.realizer.pegboard.BMLBlockPeg;
import asap.realizer.pegboard.OffsetPeg;
import asap.realizer.pegboard.PegBoard;
import asap.realizer.pegboard.TimePeg;
import asap.realizer.planunit.KeyPosition;
import asap.realizer.planunit.ParameterException;
import asap.realizer.planunit.TimedPlanUnitState;

/**
 * TODO Javadoc.
 * @author yannick-broeker
 */
public class MotionGraphRestPose implements RestPose
{
    private AnimationPlayer aniPlayer;
    
    private VJoint restPoseTree; // Holds the pose on a VJoint structure. Joints not in the pose are set to have identity rotation.
    private MotionGraph motionGraph;
    private SkeletonInterpolator motion;
    private double startTime;

    public MotionGraphRestPose(MotionGraph test, SkeletonInterpolator motion)
    {
        this.motionGraph = test;
        this.motion = motion;
    }

    public MotionGraphRestPose(XMLTokenizer tok)
    {
        MotionGraphXML mgXML = new MotionGraphXML();
        try
        {
            mgXML.readXML(tok);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        motionGraph = mgXML.getMotionGraph();
        motion = alignStart(motionGraph.next(), 0);
    }

    public MotionGraphRestPose() throws IOException
    {
        MotionGraphXML xml = new MotionGraphXML();
        xml.readXML(new File("motiongraph.xml"));
        motionGraph = xml.getMotionGraph();
        motion = alignStart(motionGraph.next(), 0);
    }

    @Override
    public RestPose copy(AnimationPlayer player)
    {
        MotionGraphRestPose copy = new MotionGraphRestPose(motionGraph, motion);
        copy.setAnimationPlayer(player);

        return copy;
    }

    @Override
    public void setAnimationPlayer(AnimationPlayer player)
    {
        this.aniPlayer = player;
        restPoseTree = player.getVCurr().copyTree("rest-");
        Set<String> ids = Sets.newHashSet(motion.getPartIds());
        for (VJoint vj : restPoseTree.getParts())
        {
            if(ids.contains(vj.getSid()))
            {
                vj.setRotation(Quat4f.getIdentity());
            }
        }
    }

    @Override
    public void setResource(Resources res)
    {

    }

    private SkeletonInterpolator alignTime(SkeletonInterpolator motion, double time)
    {
        ConfigList config = new ConfigList(motion.getConfigSize());
        String configType = motion.getConfigType();
        String[] partIds = motion.getPartIds();

        double startTime = motion.getStartTime();
        for (int i = 0; i < motion.size(); i++)
        {
            config.addConfig(motion.getTime(i) - startTime + time, motion.getConfig(i));
        }

        return new SkeletonInterpolator(partIds, config, configType);
    }

    private SkeletonInterpolator alignStart(SkeletonInterpolator motion, double time)
    {
        ConfigList config = new ConfigList(motion.getConfigSize());
        String configType = motion.getConfigType();
        String[] partIds = motion.getPartIds();

        float[] config0 = motion.getConfig(0).clone();

        double startTime = motion.getStartTime();
        for (int i = 0; i < motion.size(); i++)
        {

            //motion.getConfig(i)[Vec3f.X] = motion.getConfig(i)[Vec3f.X] - config0[Vec3f.X] + 0;
            //motion.getConfig(i)[Vec3f.Z] = motion.getConfig(i)[Vec3f.Z] - config0[Vec3f.Z] + 0;
            config.addConfig(motion.getTime(i) - startTime + time, motion.getConfig(i));
        }

        return new SkeletonInterpolator(partIds, config, configType);
    }

    public SkeletonInterpolator alignMotions(SkeletonInterpolator motion, SkeletonInterpolator newMotion, IAlignment align, double time)
    {
        newMotion = align.align(motion, newMotion, 1);
        return alignTime(newMotion, time);
    }

    @Override
    public void play(double time, Set<String> kinematicJoints, Set<String> physicalJoints)
    {
        time -= startTime;
        if (time > motion.getEndTime())
        {
            SkeletonInterpolator next = motionGraph.next();
            motion = alignMotions(motion, next, motionGraph.getAlign(), time);
        }

        motion.setTarget(restPoseTree);
        motion.time(time);
        float q[] = Quat4f.getQuat4f();
        for (String part : motion.getPartIds())
        {
            if (!kinematicJoints.contains(part) && !physicalJoints.contains(part))
            {
                if(restPoseTree.getPartBySid(part)!=null)
                {
                    restPoseTree.getPartBySid(part).getRotation(q);
                    aniPlayer.getVNextPartBySid(part).setRotation(q);
                }                
            }            
        }
        if (!kinematicJoints.contains(Hanim.HumanoidRoot))
        {
            float t[] = Vec3f.getVec3f();
            restPoseTree.getPartBySid(Hanim.HumanoidRoot).getTranslation(t);
            aniPlayer.getVNextPartBySid(Hanim.HumanoidRoot).setTranslation(t);
        }
    }

    @Override
    public TimedAnimationUnit createTransitionToRest(FeedbackManager fbm, Set<String> joints, double startTime, String bmlId, String id,
            BMLBlockPeg bmlBlockPeg, PegBoard pb)
    {
        return createTransitionToRest(fbm, joints, startTime, 1, bmlId, id, bmlBlockPeg, pb);
    }

    @Override
    public TimedAnimationMotionUnit createTransitionToRest(FeedbackManager fbm, Set<String> joints, double startTime, double duration,
            String bmlId, String id, BMLBlockPeg bmlBlockPeg, PegBoard pb)
    {
        TimePeg startPeg = new TimePeg(bmlBlockPeg);
        startPeg.setGlobalValue(startTime);
        TimePeg endPeg = new OffsetPeg(startPeg, duration);
        return createTransitionToRest(fbm, joints, startPeg, endPeg, bmlId, id, bmlBlockPeg, pb);
    }

    @Override
    public TimedAnimationMotionUnit createTransitionToRest(FeedbackManager fbm, Set<String> joints, TimePeg startPeg, TimePeg endPeg,
            String bmlId, String id, BMLBlockPeg bmlBlockPeg, PegBoard pb)
    {
        TransitionMU mu = createTransitionToRest(joints);
        mu.addKeyPosition(new KeyPosition("start", 0));
        mu.addKeyPosition(new KeyPosition("end", 1));
        TimedAnimationMotionUnit tmu = new TimedAnimationMotionUnit(fbm, bmlBlockPeg, bmlId, id, mu, pb, aniPlayer);
        tmu.setTimePeg("start", startPeg);
        tmu.setTimePeg("end", endPeg);
        tmu.setState(TimedPlanUnitState.LURKING);
        return tmu;
    }

    @Override
    public double getTransitionToRestDuration(VJoint vCurrent, Set<String> joints)
    {
        return 1;
    }

    @Override
    public TransitionMU createTransitionToRestFromVJoints(Collection<VJoint> joints)
    {
        float rotations[] = new float[joints.size() * 4];
        int i = 0;
        List<VJoint> targetJoints = new ArrayList<VJoint>();
        List<VJoint> startJoints = new ArrayList<VJoint>();
        List<VJoint> endJoints = new ArrayList<VJoint>();
        for (VJoint joint : joints)
        {
            VJoint vj = restPoseTree.getPartBySid(joint.getSid());
            vj.getRotation(rotations, i);
            targetJoints.add(joint);
            startJoints.add(aniPlayer.getVCurrPartBySid(joint.getSid()));
            endJoints.add(vj);
            i += 4;
        }        
        BlendingMotionUnit mu = new BlendingMotionUnit(targetJoints, startJoints, endJoints);
        mu.setStartPose();
        return mu;
    }
    
    @Override
    public TransitionMU createTransitionToRest(Set<String> joints)
    {
        float rotations[] = new float[joints.size() * 4];
        int i = 0;
        List<VJoint> targetJoints = new ArrayList<VJoint>();
        List<VJoint> startJoints = new ArrayList<VJoint>();
        List<VJoint> endJoints = new ArrayList<VJoint>();
        for (String joint : joints)
        {
            VJoint vj = restPoseTree.getPartBySid(joint);
            vj.getRotation(rotations, i);
            targetJoints.add(aniPlayer.getVNextPartBySid(joint));
            startJoints.add(aniPlayer.getVCurrPartBySid(joint));
            endJoints.add(vj);
            i += 4;
        }        
        BlendingMotionUnit mu = new BlendingMotionUnit(targetJoints, startJoints, endJoints);
        mu.setStartPose();
        return mu;
    }

    @Override
    public void initialRestPose(double time)
    {
        motion.setTarget(restPoseTree);
        motion.time(0);
        VObjectTransformCopier.newInstanceFromVJointTree(restPoseTree, aniPlayer.getVCurr(), "T1R").copyConfig();
        VObjectTransformCopier.newInstanceFromVJointTree(restPoseTree, aniPlayer.getVNext(), "T1R").copyConfig();
        VObjectTransformCopier.newInstanceFromVJointTree(restPoseTree, aniPlayer.getVPrev(), "T1R").copyConfig();
    }
    
    @Override
    public void initialRestPose(double time, VJoint dst)
    {
        motion.setTarget(restPoseTree);
        motion.time(0);
        VObjectTransformCopier.newInstanceFromVJointTree(restPoseTree, dst, "T1R").copyConfig();
    }

    @Override
    public void setParameterValue(String name, String value) throws ParameterException
    {
    }

    private void setRotConfig(VJoint poseTree, int startIndex, float[] config)
    {
        int i = 0;
        for(VJoint vj:poseTree.getParts())
        {
            if(vj.getSid()!=null)
            {
                vj.getRotation(config, startIndex+i);
                i+=4;
            }
        }
    }
    
    @Override
    public PostureShiftTMU createPostureShiftTMU(FeedbackManager bbf, BMLBlockPeg bmlBlockPeg, String bmlId, String id, PegBoard pb)
            throws MUSetupException
    {
        List<VJoint> targetJoints = new ArrayList<VJoint>();
        List<VJoint> startJoints = new ArrayList<VJoint>();
        
        motion.setTarget(restPoseTree);
        motion.time(0);
        
        for(VJoint vj:restPoseTree.getParts())
        {
            if(vj.getSid()!=null)
            {
                targetJoints.add(aniPlayer.getVNextPartBySid(vj.getSid()));
                startJoints.add(aniPlayer.getVCurrPartBySid(vj.getSid()));
            }
        }
        
        AnimationUnit mu;
        float config[]= new float[targetJoints.size()*4+3];
        restPoseTree.getTranslation(config);
        setRotConfig(restPoseTree, 3, config);
        mu = new T1RTransitionToPoseMU(startJoints, targetJoints, config);
        return new PostureShiftTMU(bbf, bmlBlockPeg, bmlId, id, mu.copy(aniPlayer), pb, this, aniPlayer);        
    }

    @Override
    public void start(double time)
    {
        startTime = time;        
    }

}
