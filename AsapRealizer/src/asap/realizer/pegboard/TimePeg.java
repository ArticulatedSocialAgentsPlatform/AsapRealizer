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
 * BML SynchronisationPoint, used in the realizer as intermediate, slightly flexible timing structure.
 * 
 * One can construct OffsetPegs that define a TimePegs that remains at a fixed time offset from a 'regular' TimePeg.
 * 
 * Sub classes should retain thread-safety and need only to implement the get/setLocalTime functions. Global and relative to other block timing is
 * handled here by utility functions that make use of the local timing functions.
 * 
 * All Time Pegs are declared relative to the start time of a BMLBlock. 
 * @author welberge
 */
@ThreadSafe
public class TimePeg 
{
    public static final double VALUE_UNKNOWN = -Double.MAX_VALUE;
    
    protected final BMLBlockPeg bmlBlockPeg;
    
    
    
    @GuardedBy("this")
    private double value;
    
    @GuardedBy("this")
    private  boolean absoluteTime;
    
    public TimePeg(BMLBlockPeg bmp)
    {
        bmlBlockPeg = bmp;
        value = VALUE_UNKNOWN;
        absoluteTime = false;
    }
    
    public synchronized BMLBlockPeg getBmlBlockPeg()
    {
        return bmlBlockPeg;
    }

    public synchronized String getBmlId()
    {
        return bmlBlockPeg.getId();
    }
    
    /**
     * @return true if the timepeg is linked to an absolute time value.
     */
    public synchronized boolean isAbsoluteTime()
    {
        return absoluteTime;        
    }
    
    /**
     * Sets whether this timepeg links to an absolute time value (default is false).
     */
    public synchronized void setAbsoluteTime(boolean absTime)
    {
        absoluteTime = absTime;        
    }
    
    /**
     * Get the 'underlying' TimePeg. Used to get the linked 'real' TimePeg for OffsetPegs, AfterPegs, mainly in schedulers.
     */
    public synchronized TimePeg getLink()
    {
        return this;
    }
    
    /**
     * get the value of the SynchronisationPoint
     * @return the value of the SynchronisationPoint
     */
    public synchronized double getLocalValue()
    {
        return value;
    }
    
    /**
     * Set the value of the SynchronisationPoint.
     * @param v the new value
     */
    public synchronized void setLocalValue(double v)
    {
        value = v;
    }
    
    public final synchronized double getGlobalValue()
    {
        if(getLocalValue()==TimePeg.VALUE_UNKNOWN)return TimePeg.VALUE_UNKNOWN;
        return bmlBlockPeg.getValue()+getLocalValue();
    }
    
    /**
     * Set the value of the SynchronisationPoint, relative to p.
     * @param v the new value
     */
    public final synchronized void setValue(double v, BMLBlockPeg p)
    {
        setLocalValue(v+p.getValue()-bmlBlockPeg.getValue());
    }
    
    /**
     * Get the value of the Synchronization point, relative to p
     */
    public final synchronized double getValue(BMLBlockPeg p)
    {
        if(getLocalValue()==TimePeg.VALUE_UNKNOWN)return TimePeg.VALUE_UNKNOWN;
        return getLocalValue()+bmlBlockPeg.getValue()-p.getValue();
    }
    
    /**
     * Set the value of the SynchronisationPoint.
     * @param v the new value
     */
    public final synchronized void setGlobalValue(double v)
    {
        if(v == TimePeg.VALUE_UNKNOWN)
        {
            setLocalValue(TimePeg.VALUE_UNKNOWN);
        }
        else
        {
            setLocalValue(v - bmlBlockPeg.getValue());
        }
    }
    
    @Override
    public synchronized String toString()
    {
        return "Time peg with local value "+getLocalValue()+" global value "+getGlobalValue();
    }    
}
