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
package asap.realizer.planunit;

/**
 * Parameter was not found
 * @author welberge
 */
public class ParameterNotFoundException extends ParameterException
{
    private static final long serialVersionUID = 1L;
    private final String paramId;
    public String getParamId()
    {
        return paramId;
    }
    
    public ParameterNotFoundException(String param)
    {
        super("ParameterNotFound "+param);
        this.paramId = param;
    }
    
    public ParameterNotFoundException(String param,Exception ex)
    {
        this(param);
        initCause(ex);        
    }
}
