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
import hmi.faceanimation.converters.FACSConverter;
import hmi.faceanimation.converters.FACS2MorphConverter;
import hmi.faceanimation.model.ActionUnit;
import hmi.faceanimation.model.ActionUnit.Symmetry;
import hmi.faceanimation.model.FACS;
import hmi.faceanimation.model.FACS.Side;
import hmi.faceanimation.model.FACSConfiguration;
import asap.realizer.planunit.KeyPosition;
import asap.realizer.planunit.ParameterException;
import asap.realizer.planunit.ParameterNotFoundException;

/**
 * A basic facial animation unit consisting of one AU value. The key positions are: start, attackPeak,
 * relax, end. This descripes an apex-like intensity development: Between start and attackPeak, the face
 * configuration is blended in; between relax and end the face configuration is blended out.
 * 
 * Parameter cosntraints: side valuefits with AU... (e.g., no null for asymmetric AU, no RIGHT, LEFT
 * for symmetric)
 * 
 * @author Dennis Reidsma
 */
public class AUFU extends FACSFU
{

    enum AUFUSide
    {
        LEFT, RIGHT, BOTH
    }

    private AUFUSide side = null;
    private int aunr = -1;

    @Override
    public void setFloatParameterValue(String name, float value) throws ParameterException
    {
        if (name.equals("intensity"))
        {
            intensity.set(value);
            prevIntensity.set(value);
        }
        else throw new ParameterNotFoundException(name);
        setAU(side, aunr, intensity.floatValue());
    }

    @Override
    public void setParameterValue(String name, String value) throws ParameterException
    {
        if (name.equals("au"))
        {
            aunr = Integer.parseInt(value);
            // System.out.println("aunr: " + aunr);
        }
        else if (name.equals("side"))
        {
            side = AUFUSide.BOTH;
            if (value.equals("LEFT")) side = AUFUSide.LEFT;
            else if (value.equals("RIGHT")) side = AUFUSide.RIGHT;
            // System.out.println("side: " + side);
        }
        else
        {
            super.setParameterValue(name, value);
        }
        setAU(side, aunr, intensity.floatValue());
    }

    @Override
    public String getParameterValue(String name) throws ParameterException
    {
        if (name.equals("side"))
        {
            if (side == null)
            {
                throw new ParameterNotFoundException(name);
            }
            return "" + side;
        }
        return super.getParameterValue(name);
    }

    @Override
    public float getFloatParameterValue(String name) throws ParameterException
    {
        if (name.equals("au")) return aunr;
        return super.getFloatParameterValue(name);
    }

    @Override
    public boolean hasValidParameters()
    {
        ActionUnit au = FACS.getActionUnit(aunr);
        if (au == null) return false;
        if (intensity.floatValue() > 1) return false;
        if (au.getSymmetry() != Symmetry.ASYMMETRIC) return (side == AUFUSide.BOTH) || (side == null);

        return (side != null);
    }

    public void setAU(AUFUSide s, int i, float intens)
    {
        side = s;
        aunr = i;
        intensity.set(intens);
        prevIntensity.set(intens);
        if (!hasValidParameters())
        {
            return;
        }
        facsConfig = new FACSConfiguration();
        ActionUnit au = FACS.getActionUnit(aunr);
        float newval = intensity.floatValue();
        if (au.getSymmetry() != Symmetry.ASYMMETRIC)
        {
            facsConfig.setValue(Side.NONE, au.getIndex(), newval);
        }
        else
        {
            if (side == AUFUSide.LEFT) facsConfig.setValue(Side.LEFT, au.getIndex(), newval);
            if (side == AUFUSide.RIGHT) facsConfig.setValue(Side.RIGHT, au.getIndex(), newval);
            if (side == AUFUSide.BOTH)
            {
                
                facsConfig.setValue(Side.LEFT, au.getIndex(), newval);
                facsConfig.setValue(Side.RIGHT, au.getIndex(), newval);
            }
        }        
    }

    /**
     * Create a copy of this face unit and link it to the faceContrller
     */
    public FaceUnit copy(FaceController fc, FACSConverter fconv, EmotionConverter econv, FACS2MorphConverter f2mconv)
    {
        AUFU result = new AUFU();
        result.setFaceController(fc);
        result.setFACSConverter(fconv);
        result.setAU(side, aunr, intensity.floatValue());
        for (KeyPosition keypos : getKeyPositions())
        {
            result.addKeyPosition(keypos.deepCopy());
        }
        return result;
    }
}
