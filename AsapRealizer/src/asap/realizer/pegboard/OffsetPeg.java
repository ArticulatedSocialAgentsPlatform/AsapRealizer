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
package asap.realizer.pegboard;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * OffsetPegs define TimePegs that remains at a fixed time offset from a 'regular' TimePeg. 
 * 
 * An OffsetPeg can refer to another OffsetPeg as it's 'link'.
 *  
 * @author welberge
 */
@ThreadSafe
public final class OffsetPeg extends TimePeg
{
    @GuardedBy("this")
    private TimePeg link;
    
    @GuardedBy("this")
    private double offset = 0;
    
    public synchronized double getOffset()
    {
        return offset;
    }
    
    public OffsetPeg(TimePeg l, double o)
    {
        super(l.bmlBlockPeg);
        link = l;
        offset = o;
        setAbsoluteTime(l.isAbsoluteTime());        
    }
    
    public OffsetPeg(TimePeg l, double o,BMLBlockPeg bmp)
    {
        super(bmp);
        link = l;
        offset = o;
        setAbsoluteTime(l.isAbsoluteTime());        
    }
    
    public synchronized void setOffset(double o)
    {
        offset = o;        
    }
    
    public synchronized void setLink(TimePeg p)
    {
        link = p;
        setAbsoluteTime(link.isAbsoluteTime());
    }
    
    @Override
    public synchronized TimePeg getLink()
    {
        return link;
    }
    
    /**
     * get the value of the SynchronisationPoint
     * @return the value of the SynchronisationPoint
     */
    @Override
    public synchronized double getLocalValue()
    {
        if(link.getLocalValue()==TimePeg.VALUE_UNKNOWN)return TimePeg.VALUE_UNKNOWN;
        return link.getValue(bmlBlockPeg)+offset;
    }
    
    
    @Override
    public synchronized void setLocalValue(double v)
    {
        if(v == TimePeg.VALUE_UNKNOWN)
        {
            link.setLocalValue(TimePeg.VALUE_UNKNOWN);
        }
        else
        {
            link.setValue(v-offset, bmlBlockPeg);
        }        
    }
    
    @Override
    public synchronized String toString()
    {
        return "OffsetTime peg with value "+getLocalValue()+" ,offset "+offset + " global value: "+ getGlobalValue();
    }
}
