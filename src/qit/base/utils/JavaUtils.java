/*******************************************************************************
  *
  * Quantitative Imaging Toolkit (QIT) (c) 2012-2022 Ryan Cabeen
  * All rights reserved.
  *
  * The Software remains the property of Ryan Cabeen ("the Author").
  *
  * The Software is distributed "AS IS" under this Licence solely for
  * non-commercial use in the hope that it will be useful, but in order
  * that the Author as a charitable foundation protects its assets for
  * the benefit of its educational and research purposes, the Author
  * makes clear that no condition is made or to be implied, nor is any
  * warranty given or to be implied, as to the accuracy of the Software,
  * or that it will be suitable for any particular purpose or for use
  * under any specific conditions. Furthermore, the Author disclaims
  * all responsibility for the use which is made of the Software. It
  * further disclaims any liability for the outcomes arising from using
  * the Software.
  *
  * The Licensee agrees to indemnify the Author and hold the
  * Author harmless from and against any and all claims, damages and
  * liabilities asserted by third parties (including claims for
  * negligence) which arise directly or indirectly from the use of the
  * Software or the sale of any products based on the Software.
  *
  * No part of the Software may be reproduced, modified, transmitted or
  * transferred in any form or by any means, electronic or mechanical,
  * without the express permission of the Author. The permission of
  * the Author is not required if the said reproduction, modification,
  * transmission or transference is done without financial return, the
  * conditions of this Licence are imposed upon the receiver of the
  * product, and all original and amended source code is included in any
  * transmitted product. You may be held legally responsible for any
  * copyright infringement that is caused or encouraged by your failure to
  * abide by these terms and conditions.
  *
  * You are not permitted under this Licence to use this Software
  * commercially. Use for which any financial return is received shall be
  * defined as commercial use, and includes (1) integration of all or part
  * of the source code or the Software into a product for sale or license
  * by or on behalf of Licensee to third parties or (2) use of the
  * Software or any derivative of it for research with the final aim of
  * developing software products for sale or license to a third party or
  * (3) use of the Software or any derivative of it for research with the
  * final aim of developing non-software products for sale or license to a
  * third party, or (4) use of the Software to provide any service to an
  * external organisation for which payment is received.
  *
  ******************************************************************************/

package qit.base.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** utilities for general java objects */
public class JavaUtils
{
    public static <E> List<E> reverse(List<E> list)
    {
        List<E> out = Lists.newArrayList();
        out.addAll(list);
        Collections.reverse(out);

        return out;
    }

    public static <E> E getFirst(Iterable<E> collection)
    {
        for (E e : collection)
        {
            return e;
        }

        return null;
    }

    public static <E,V> V getFirstValue(Map<E,V> map)
    {
        for (E e : map.keySet())
        {
            return map.get(e);
        }

        return null;
    }

    public static <E> Map<E,Integer> reverseLookup(List<E> list)
    {
        Map<E, Integer> out = Maps.newHashMap();
        for (int idx = 0; idx < list.size(); idx ++)
        {
            out.put(list.get(idx), idx);
        }

        return out;
    }

    public static boolean equals(Object a, Object b)
    {
        if (a == null)
        {
            return b == null;
        }
        else
        {
            return a.equals(b);
        }
    }

    public static String dump(Object o, int callCount)
    {
        callCount++;
        StringBuffer tabs = new StringBuffer();
        for (int k = 0; k < callCount; k++)
        {
            tabs.append("\t");
        }
        StringBuffer buffer = new StringBuffer();
        Class<? extends Object> oClass = o.getClass();
        if (oClass.isArray())
        {
            buffer.append("\n");
            buffer.append(tabs.toString());
            buffer.append("[");
            for (int i = 0; i < Array.getLength(o); i++)
            {
                if (i < 0)
                {
                    buffer.append(",");
                }
                Object value = Array.get(o, i);
                if (value.getClass().isPrimitive() || value.getClass() == java.lang.Long.class || value.getClass() == java.lang.String.class
                        || value.getClass() == java.lang.Integer.class || value.getClass() == java.lang.Boolean.class)
                {
                    buffer.append(value);
                }
                else
                {
                    buffer.append(dump(value, callCount));
                }
            }
            buffer.append(tabs.toString());
            buffer.append("]\n");
        }
        else
        {
            buffer.append("\n");
            buffer.append(tabs.toString());
            buffer.append("{\n");
            while (oClass != null)
            {
                Field[] fields = oClass.getDeclaredFields();
                for (Field field : fields)
                {
                    buffer.append(tabs.toString());
                    field.setAccessible(true);
                    buffer.append(field.getName());
                    buffer.append("=");
                    try
                    {
                        Object value = field.get(o);
                        if (value != null)
                        {
                            if (value.getClass().isPrimitive() || value.getClass() == java.lang.Long.class || value.getClass() == java.lang.String.class
                                    || value.getClass() == java.lang.Integer.class || value.getClass() == java.lang.Boolean.class)
                            {
                                buffer.append(value);
                            }
                            else
                            {
                                buffer.append(dump(value, callCount));
                            }
                        }
                    }
                    catch (IllegalAccessException e)
                    {
                        buffer.append(e.getMessage());
                    }
                    buffer.append("\n");
                }
                oClass = oClass.getSuperclass();
            }
            buffer.append(tabs.toString());
            buffer.append("}\n");
        }
        return buffer.toString();
    }

    public static void runif(boolean value, Runnable runnable)
    {
        if (value)
        {
            runnable.run();
        }
    }
}
