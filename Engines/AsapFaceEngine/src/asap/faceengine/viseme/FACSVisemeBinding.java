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
package asap.faceengine.viseme;

import hmi.faceanimation.FaceController;
import hmi.faceanimation.converters.FACSConverter;
import hmi.faceanimation.model.FACSConfiguration;
import saiba.bml.core.Behaviour;
import asap.faceengine.faceunit.FACSFU;
import asap.faceengine.faceunit.TimedFaceUnit;
import asap.realizer.feedback.FeedbackManager;
import asap.realizer.feedback.NullFeedbackManager;
import asap.realizer.pegboard.BMLBlockPeg;
import asap.realizer.pegboard.PegBoard;

/**
 * Implementation that realizers visemes as FACS expressions
 * 
 * @author Dennis Reidsma
 */
public class FACSVisemeBinding implements VisemeBinding
{
    private VisemeToFACSMapping visemeMapping;
    private FACSConverter fconv;

    public FACSVisemeBinding(VisemeToFACSMapping mapping, FACSConverter fconv)
    {
        visemeMapping = mapping;
        this.fconv = fconv;
    }

    @Override
    public TimedFaceUnit getVisemeUnit(FeedbackManager bfm, BMLBlockPeg bbPeg, Behaviour b, int viseme, FaceController fc, PegBoard pb)
    // dur, ..
    { // times are relative, and just used to determine whether peak is
      // a-centric
        FACSFU visemeFU = new FACSFU();

        if (viseme == -1) viseme = 0;
        FACSConfiguration facsConf = visemeMapping.getFACSConfigurationForViseme(viseme);

        if (facsConf != null)
        {
            visemeFU.setConfig(facsConf);
        }

        TimedFaceUnit tfu = visemeFU.copy(fc, fconv, null, null).createTFU(bfm, bbPeg, b.getBmlId(), b.id, pb);
        // time pegs not yet set. Here we just arrange relative timing
        tfu.getKeyPosition("attackPeak").time = 0.5;
        tfu.getKeyPosition("relax").time = 0.5;

        return tfu;
    }

    @Override
    public TimedFaceUnit getVisemeUnit(BMLBlockPeg bbPeg, Behaviour b, int viseme, FaceController fc, PegBoard pb)
    {
        return getVisemeUnit(NullFeedbackManager.getInstance(), bbPeg, b, viseme, fc, pb);
    }

}
