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

package qit.base;

import java.io.IOException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/** logging utilities */
public class Logging
{
    public static Logger LOGGER = null;
    static public boolean MEMORY = false;
    static public boolean DEBUG = false;
    static public boolean PROGRESS = true;

    public static final String LOG_PATTERN = "%5r [qit] %m%n";

    static
    {
        LOGGER = Logger.getLogger("qit");
        LOGGER.removeAllAppenders();
        LOGGER.addAppender(new ConsoleAppender(new PatternLayout(LOG_PATTERN)));
        LOGGER.setLevel(Level.OFF);
    }

    public static void disable()
    {
        LOGGER.setLevel(Level.OFF);
        LOGGER.removeAllAppenders();
    }

    public static void debug()
    {
        DEBUG = true;
    }

    public static void console()
    {
        LOGGER.setLevel(Level.INFO);
    }

    public static void file(String fn) throws IOException
    {
        LOGGER.setLevel(Level.INFO);
        LOGGER.addAppender(new FileAppender(new PatternLayout(LOG_PATTERN), fn));
    }

    public static void memory()
    {
        MEMORY = true;
    }

    public static void progress(String msg)
    {
        if (PROGRESS)
        {
            info(msg);
        }
    }

    public static void info(boolean verbose, String msg)
    {
        if (verbose)
        {
            info(msg);
        }
    }

    public static void info(String msg)
    {
        if (MEMORY)
        {
            long free = Runtime.getRuntime().freeMemory();
            long total = Runtime.getRuntime().totalMemory();
            long used = total - free;
            LOGGER.info("... memory usage:");
            LOGGER.info("    total: " + total);
            LOGGER.info("    used: " + used);
            LOGGER.info("    free: " + free);
        }

        LOGGER.info(msg);
    }

    public static void info(String msg, String sa)
    {
        info(String.format(msg, sa));
    }

    public static void info(String msg, String sa, String sb)
    {
        info(String.format(msg, sa, sb));
    }

    public static void info(String msg, String sa, String sb, String sc)
    {
        info(String.format(msg, sa, sb, sc));
    }

    public static void info(String msg, Integer sa)
    {
        info(String.format(msg, sa));
    }

    public static void info(String msg, Integer sa, Integer sb)
    {
        info(String.format(msg, sa, sb));
    }

    public static void info(String msg, Integer sa, Integer sb, Integer sc)
    {
        info(String.format(msg, sa, sb, sc));
    }

    public static void info(String msg, Double sa)
    {
        info(String.format(msg, sa));
    }

    public static void info(String msg, Double sa, Double sb)
    {
        info(String.format(msg, sa, sb));
    }

    public static void info(String msg, Double sa, Double sb, Double sc)
    {
        info(String.format(msg, sa, sb, sc));
    }

    public static void infosub(String msg, String sa)
    {
        info(String.format(msg, sa));
    }

    public static void infosub(String msg, String sa, String sb)
    {
        info(String.format(msg, sa, sb));
    }

    public static void infosub(String msg, String sa, String sb, String sc)
    {
        info(String.format(msg, sa, sb, sc));
    }

    public static void infosub(String msg, Integer sa)
    {
        info(String.format(msg, sa));
    }

    public static void infosub(String msg, Integer sa, Integer sb)
    {
        info(String.format(msg, sa, sb));
    }

    public static void infosub(String msg, Integer sa, Integer sb, Integer sc)
    {
        info(String.format(msg, sa, sb, sc));
    }

    public static void infosub(String msg, Integer sa, Integer sb, Integer sc, Integer sd)
    {
        info(String.format(msg, sa, sb, sc, sd));
    }

    public static void infosub(String msg, Double sa)
    {
        info(String.format(msg, sa));
    }

    public static void infosub(String msg, Double sa, Double sb)
    {
        info(String.format(msg, sa, sb));
    }

    public static void infosub(String msg, Double sa, Double sb, Double sc)
    {
        info(String.format(msg, sa, sb, sc));
    }

    public static void error(String msg)
    {
        System.err.println("ERROR: " + msg);
        throw new RuntimeException("ERROR: " + msg);
    }
}