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
package asap.textengine;

import hmi.textembodiments.TextEmbodiment;
import asap.realizer.planunit.ParameterException;
import asap.realizer.planunit.ParameterNotFoundException;
/**
 * TextOutput implementation that sends the text directly to a TextEmbodiment.
 * Does not support parameter setting/getting.
 * @author reidsma
 *
 */
public class EmbodimentTextOutput implements TextOutput
{
    TextEmbodiment textEmbodiment = null;

    public EmbodimentTextOutput(TextEmbodiment te)
    {
        textEmbodiment = te;
    }

    @Override
    public void setText(String text)
    {
        textEmbodiment.setText(text);
    }

    @Override
    public void setFloatParameterValue(String parameter, float value) throws ParameterException
    {
        throw new ParameterNotFoundException(parameter);
    }

    @Override
    public void setParameterValue(String parameter, String value) throws ParameterException
    {
        throw new ParameterNotFoundException(parameter);

    }

    @Override
    public float getFloatParameterValue(String parameter) throws ParameterException
    {
        throw new ParameterNotFoundException(parameter);
    }

    @Override
    public String getParameterValue(String parameter) throws ParameterException
    {
        throw new ParameterNotFoundException(parameter);
    }

}
