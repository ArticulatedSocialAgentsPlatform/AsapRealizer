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
package asap.animationengine.transitions;

import hmi.animation.VJoint;
import hmi.math.Quat4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import asap.animationengine.AnimationPlayer;

/**
 * Slerp transition from current pose to predefined end pose, typically for use
 * in an animationplanplayer without using the animationplayer
 * 
 * @author welberge
 */
public class SlerpTransitionToPoseMU extends TransitionToPoseMU
{
    public SlerpTransitionToPoseMU()
    {
        super();
    }
    
    public SlerpTransitionToPoseMU(List<VJoint> j, List<VJoint> startPoseJoints, float ep[])
    {
        super(j,startPoseJoints,ep);
    }
    
    @Override
    public TransitionMU copy(AnimationPlayer player)
    {
        this.aniPlayer = player;
        ArrayList<VJoint> startPoseJoints = new ArrayList<VJoint>();
        float[] ep = null;
        if (endPose != null)
        {
            ep = Arrays.copyOf(endPose, endPose.length);
        }

        if (startJoints == null)
        {
            startPoseJoints.addAll(player.getVCurr().getParts());
        }
        else
        {
            for (VJoint vj : startJoints)
            {
                VJoint vNew = player.getVCurrPartBySid(vj.getSid());
                startPoseJoints.add(vNew);
            }
        }

        if (joints != null)
        {
            ArrayList<VJoint> newJoints = new ArrayList<VJoint>();
            for (VJoint vj : joints)
            {
                VJoint newJ = player.getVCurrPartBySid(vj.getSid());
                if (newJ != null)
                {
                    newJoints.add(newJ);
                }
            }
            return new SlerpTransitionToPoseMU(newJoints, startPoseJoints, ep);
        }
        else
        {
            return new SlerpTransitionToPoseMU(player.getVCurr().getParts(), startPoseJoints, ep);
        }
    }

    
    /**
     * Set the current pose of the associated set of joints
     */
    @Override
    public void setStartPose()
    {
        int i = 0;
        startPose = new float[joints.size() * 4];
        for (VJoint v : startJoints)
        {
            v.getRotation(startPose, i);
            i += 4;
        }
    }

    @Override
    public void play(double t)
    {
        if (startPose != null)
        {
            Quat4f.interpolateArrays(result, startPose, endPose, (float) t);
            int i = 0;
            for (VJoint vj : joints)
            {
                vj.setRotation(result, i);
                i += 4;
            }
        }
    }

    @Override
    public Set<String> getAdditiveJoints()
    {
        return ImmutableSet.of();
    }
}
