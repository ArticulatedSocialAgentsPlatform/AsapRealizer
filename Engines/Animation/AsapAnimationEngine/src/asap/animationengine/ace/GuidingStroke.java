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
package asap.animationengine.ace;

import hmi.math.Mat4f;
import hmi.math.Vec3f;
import lombok.Getter;
import lombok.Setter;

/**
 * ase class for strokes, which define a trajectory
 * representation consisting of subsequent guiding strokes
 * @author hvanwelbergen
 * @author Stefan Kopp (original C++ version)
 */
public class GuidingStroke
{
    protected float[] endPos = Vec3f.getVec3f();
    private float[] endDir = Vec3f.getVec3f(); // optional
    
    @Getter
    private double vGain; // velocity gain factor    
    
    @Getter @Setter
    private double eDt; // anticipated duration
    
    
    @Getter
    GStrokePhaseID phaseId;

    public GuidingStroke()
    {
        phaseId = GStrokePhaseID.STP_PREP;
        vGain = 1;
        eDt = 0;
    }

    public GuidingStroke(GStrokePhaseID phaseId, float[] ep, float ed[])
    {
        Vec3f.set(endPos,ep);
        Vec3f.set(endDir, ed);
        this.phaseId = phaseId;
        vGain = 1;        
        eDt = 0;

    }

    /**
     * Copies endPos to result
     */
    public void getEndPos(float[] result)
    {
        Vec3f.set(result, endPos);
    }
    
    public float[] getEndPos()
    {
        return Vec3f.getVec3f(endPos);
    }

    /**
     * Transforms the boundary constraints with 4x4 matrix m
     */
    public void transform(float[] m)
    {
        Mat4f.transformPoint(m, endPos);
        Mat4f.transformVector(m, endDir);
    }

    public double getArcLengthFrom(float[] sPos)
    {
        float tmp[] = Vec3f.getVec3f();
        Vec3f.sub(tmp, endPos,sPos);
        return Vec3f.length(tmp);
    }

    @Override
    public String toString()
    {
        return "GStroke: -> " + "endPos: "+Vec3f.toString(endPos) + "endDir: " + Vec3f.toString(endDir) + "(Phase:" + phaseId + ")";
    }
}
