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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

/** a jython interpreter */
public class Interpreter
{
    public static void interactive()
    {
        Logging.info("loading interactive console");
        PythonInterpreter interp = build();
        String root = Global.getRoot();
        String scripts = Global.getScriptsDir();
        String sep = File.separator;
        String console = root + sep + "lib" + sep + "modules" + sep + "jythonconsole-0.0.7";
        
        interp.exec("sys.path.append(\"" + scripts + "\")");
        interp.exec("sys.path.append(\"" + console + "\")");
        interp.exec("import console");
        interp.exec("console.main(namespace=localvars)");
    }

    public static PythonInterpreter build()
    {
        PythonInterpreter interp = new PythonInterpreter();

        interp.set("__name__", "__main__");
        interp.exec("import sys");

//        Reflections.log = null; // turn off messages
        Reflections reflections = new Reflections("qit", new SubTypesScanner(false));
        Set<Class<? extends Object>> classes = reflections.getSubTypesOf(Object.class);
        for (Class<? extends Object> cla : classes)
        {
            String name = cla.getName();
            if (name.contains("$"))
            {
                continue;
            }

            List<String> tokens = Arrays.asList(StringUtils.split(name, '.'));
            int s = tokens.size();

            if (s < 2)
            {
                continue;
            }

            String p = s == 2 ? tokens.get(0) : StringUtils.join(tokens.subList(0, s - 1), ".");
            String n = tokens.get(s - 1);
            String cmd = "from " + p + " import " + n;

            try
            {
                interp.exec(cmd);
            }
            catch (RuntimeException e)
            {
                Logging.info("warning: command failed: " + cmd);
            }
        }

        PyObject localvars = interp.getLocals();
        interp.set("localvars", localvars);

        return interp;
    }
}
