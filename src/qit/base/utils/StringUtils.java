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
import qit.base.structs.DataRecord;
import qit.data.datasets.Record;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * utilities for combinatorial things
 */
public class StringUtils
{
    public static String[] split(String strToSplit, char delimiter)
    {
        // this is an alternative split function that does not use regex

        List<String> arr = Lists.newArrayList();
        int foundPosition;
        int startIndex = 0;
        while ((foundPosition = strToSplit.indexOf(delimiter, startIndex)) > -1)
        {
            arr.add(strToSplit.substring(startIndex, foundPosition));
            startIndex = foundPosition + 1;
        }
        arr.add(strToSplit.substring(startIndex));
        return arr.toArray(new String[arr.size()]);
    }

    public static String sortWords(String s)
    {
        String[] tokens = s.split("(?=\\p{Lu})");
        Arrays.sort(tokens);

        StringBuffer out = new StringBuffer();

        for (String token : tokens)
        {
            out.append(token);
        }

        return out.toString();
    }

    public static String sort(String s)
    {
        char[] chars = s.toLowerCase().toCharArray();
        Arrays.sort(chars);
        return String.valueOf(chars);
    }

    public static List<String> sort(List<String> refs, String name)
    {
        // sort refs by edit distance to name

        String nameSorted = StringUtils.sort(name.toLowerCase());

        List<DataRecord<String>> records = Lists.newArrayList();

        for (String ref : refs)
        {
            if (ref.equals(name))
            {
                records.add(new DataRecord<>(0, ref));
            }
            else
            {
                int dist = StringUtils.editDistanceMin(name, ref);
                int distSorted = StringUtils.editDistanceMin(nameSorted, StringUtils.sortWords(ref));

                records.add(new DataRecord<>(Math.min(dist, distSorted), ref));
            }
        }

        Collections.sort(records);

        List<String> out = Lists.newArrayList();
        for (DataRecord<String> record : records)
        {
            out.add(record.getData());
        }

        return out;
    }

    public static int editDistanceMin(String word1, String word2)
    {
        int len1 = word1.length();
        int len2 = word2.length();

        // len1+1, len2+1, because finally return dp[len1][len2]
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++)
        {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++)
        {
            dp[0][j] = j;
        }

        //iterate though, and check last char
        for (int i = 0; i < len1; i++)
        {
            char c1 = word1.charAt(i);
            for (int j = 0; j < len2; j++)
            {
                char c2 = word2.charAt(j);

                //if last two chars equal
                if (c1 == c2)
                {
                    //update dp value for +1 length
                    dp[i + 1][j + 1] = dp[i][j];
                }
                else
                {
                    int replace = dp[i][j] + 1;
                    int insert = dp[i][j + 1] + 1;
                    int delete = dp[i + 1][j] + 1;

                    int min = replace > insert ? insert : replace;
                    min = delete > min ? min : delete;
                    dp[i + 1][j + 1] = min;
                }
            }
        }

        return dp[len1][len2];
    }

    public static Map<String,String> clean(List<String> values)
    {
        Map<String,String> clean = Maps.newHashMap();

        for (String field : values)
        {
            String nfield = field;
            if (field.contains("."))
            {
                nfield = field.replace('.', '_');
            }

            if (field.contains("-"))
            {
                nfield = field.replace('-', '_');
            }

            clean.put(field, nfield);
        }

        return clean;
    }
}