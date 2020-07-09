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

import hmi.animation.Hanim;
import hmi.animation.VJoint;
import hmi.math.Quat4f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import asap.animationengine.AnimationPlayer;
import asap.animationengine.motionunit.AnimationUnit;
import asap.motionunit.MUPlayException;
import asap.realizer.planunit.ParameterNotFoundException;
import com.google.common.collect.ImmutableSet;

/**
 * See PerlinNoiseMU, this is a version using additive joints
 * @author Jan Kolkmeier
 */
public class AdditivePerlinNoiseMU extends PerlinNoiseMU {
	@java.lang.SuppressWarnings("all")
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdditivePerlinNoiseMU.class);
	private VJoint additiveBody;
	private List<String> joints;
	private double t0 = -1;
	
	public AdditivePerlinNoiseMU() {
	}
	
	@Override
	public double getPreferedDuration() {
		return 1;
	}
	
	@Override
	public void play(double t) throws MUPlayException {
		if (t0 < 0) t0 = t;
		if (joints == null) {
			joints = new ArrayList<String>();
			try {
				joints.add(getParameterValue("joint"));
			} catch (ParameterNotFoundException e) {
				joints.add(Hanim.skullbase);
				log.error("Joint parameter not set, using skullbase");
			}
			additiveBody = player.constructAdditiveBody(ImmutableSet.copyOf(joints));
		}
		try {
			float rotxRad = getFloatParameterValue("offsetx") + getFloatParameterValue("baseamplitudex") * pnx1.noise((float)t * getFloatParameterValue("basefreqx"));
			float rotyRad = getFloatParameterValue("offsety") + getFloatParameterValue("baseamplitudey") * pny1.noise((float)t * getFloatParameterValue("basefreqy"));
			float rotzRad = getFloatParameterValue("offsetz") + getFloatParameterValue("baseamplitudez") * pnz1.noise((float)t * getFloatParameterValue("basefreqz"));
			double tRel = t - t0;
			if (tRel < 1.0) {
				rotxRad = (float)(tRel * rotxRad);
				rotyRad = (float)(tRel * rotyRad);
				rotzRad = (float)(tRel * rotzRad);
			}
			Quat4f.setFromRollPitchYaw(q, rotzRad, rotxRad, rotyRad);
			for (String joint : joints) {
				additiveBody.getPart(joint).setRotation(q);
			}
		} catch (Exception ex) {
			throw new MUPlayException(ex.getMessage(), this);
		}
	}
	
	@Override
	public AnimationUnit copy(AnimationPlayer p) {
		this.aniPlayer = p;
		HashMap<String, String> newparam = new HashMap<String, String>();
		newparam.putAll(parameters);
		AdditivePerlinNoiseMU pmu = new AdditivePerlinNoiseMU();
		pmu.parameters = newparam;
		pmu.player = p;
		return pmu;
	}
	
	@Override
	public Set<String> getKinematicJoints() {
		return ImmutableSet.of();
	}
	
	@Override
	public Set<String> getAdditiveJoints() {
		return ImmutableSet.copyOf(joints);
	}
	
	@Override
	public void cleanup() {
		player.removeAdditiveBody(additiveBody);
	}
}