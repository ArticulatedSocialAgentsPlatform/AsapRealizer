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
package asap.faceengine.faceunit;

import hmi.faceanimation.FaceController;
import hmi.faceanimation.converters.EmotionConverter;
import hmi.faceanimation.converters.FACS2MorphConverter;
import hmi.faceanimation.converters.FACSConverter;
import asap.motionunit.MotionUnit;
import asap.realizer.feedback.FeedbackManager;
import asap.realizer.pegboard.BMLBlockPeg;
import asap.realizer.pegboard.PegBoard;

/**
 * A facial animation, typically with a peak-like structure. 
 * Contains a set of keys that map to 'world' time to animation time.
 * @author Dennis Reidsma
 */
public interface FaceUnit extends MotionUnit
{
    boolean hasValidParameters();    
   
    /**
     * Creates the TimedFaceUnit corresponding to this face unit
     * @param bmlId     BML block id
     * @param id         behaviour id
     * @return          the TFU
     */
    TimedFaceUnit createTFU(FeedbackManager bfm, BMLBlockPeg bbPeg,String bmlId,String id, PegBoard pb);    
    
    /**
     * Create a copy of this face unit and link it to the faceplayer
     */
    FaceUnit copy(FaceController fc, FACSConverter fconv, EmotionConverter econv, FACS2MorphConverter f2mconv); 

    void interruptFromHere();
}