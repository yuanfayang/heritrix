/* ExampleBean
 *
 * Created on October 18, 2006
 *
 * Copyright (C) 2006 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.openmbeans.annotations;


import java.math.BigDecimal;
import java.math.BigInteger;

import javax.management.Notification;


@Header(desc="The example bean for unit testing purposes.")
public class ExampleBean extends Bean {
    
    
    @Operation(desc="Performs a logical AND on two booleans.", impact=ACTION)
    public boolean and(
            
            @Parameter(name="a", desc="The first boolean.", def="false")
            boolean a,
            
            @Parameter(name="b", desc="The second boolean.", def="false")
            boolean b) 
    {
        return a && b;
    }


    @Operation(desc="Sums two bytes.", impact=ACTION)    
    public byte sumByte(
            
            @Parameter(name="a", desc="The first byte to sum.", def="0")
            byte a, 
            
            @Parameter(name="b", desc="The second byte to sum.", def="0")
            byte b) 
    {
        return (byte)(a + b);
    }


     
    @Operation(desc="Sums two chars.", impact=ACTION)    
    public char sumChar(
            
            @Parameter(name="a", desc="The first char to sum.", def="0")            
            char a, 

            @Parameter(name="b", desc="The second char to sum.", def="0")            
            char b) 
    {
        return (char)(a + b);
    }


    @Operation(desc="Sums two doubles.", impact=ACTION)    
    public double sumDouble(
            
            @Parameter(name="a", desc="The first double to sum.", def="0")            
            double a, 
            
            @Parameter(name="b", desc="The second double to sum.", def="0")
            double b) 
    {
        return a + b;
    }
    

    @Operation(desc="Sums two floats.", impact=ACTION)    
    public float sumFloat(

            @Parameter(name="a", desc="The first float to sum.", def="0")
            float a, 
            
            @Parameter(name="b", desc="The second float to sum.", def="0")
            float b) 
    {
        return a + b;
    }


    @Operation(desc="Sums two ints.", impact=ACTION)
    public int sumInt(
            
            @Parameter(name="a", desc="The first int to sum.", def="0")
            int a, 
            
            @Parameter(name="b", desc="The second int to sum.", def="0")
            int b) 
    {
        return a + b;
    }


    @Operation(desc="Sums two shorts.", impact=ACTION)
    public short sumShort(
            
            @Parameter(name="a", desc="The first short to sum.", def="0")
            short a, 
            
            @Parameter(name="b", desc="The second short to sum.", def="0")
            short b) 
    {
        return (short)(a + b);
    }


    @Operation(desc="Sums two longs.", impact=ACTION)
    public long sumLong(
            
            @Parameter(name="a", desc="The first long to sum.", def="0")
            long a,
            
            @Parameter(name="b", desc="The second long to sum.", def="0")
            long b) 
    {
        return a + b;
    }


    @Operation(desc="Sums two BigIntegers.", impact=ACTION)
    public BigInteger sumBigInteger(

            @Parameter(name="a", desc="The first BigInteger to sum.", def="0")
            BigInteger a,
            
            @Parameter(name="b", desc="The second BigInteger to sum.", def="0")
            BigInteger b)
    {
        return a.add(b);
    }


    @Operation(desc="Sums two BigDecimals.", impact=ACTION)
    public BigDecimal sumBigDecimal(

            @Parameter(name="a", desc="The first BigDecimal to sum.", def="0")
            BigDecimal a,
            
            @Parameter(name="b", desc="The second BigDecimal to sum.", def="0")
            BigDecimal b)
    {
        return a.add(b);
    }


    @Operation(desc="Concatenates two strings.", impact=ACTION)
    public String concatenate(
            
            @Parameter(name="a", desc="The first string to concatenate.", def="")
            String a,

            @Parameter(name="b", desc="The second string to concatenate.", def="")
            String b)
    {
        return a + b;
    }


    @Emitter(desc="Example notification.", types={"foo", "bar"})
    protected void emit(Notification n) {
        
    }
}
