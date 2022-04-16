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

package qitview.main;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import qit.base.utils.PathUtils;
import qit.math.structs.Box;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

public class Settings
{
    public boolean backup = false;
    public boolean clobber = false;
    public boolean colorshare = false;

    public float bgRed = Constants.BG_RED_DEFAULT;
    public float bgGreen = Constants.BG_GREEN_DEFAULT;
    public float bgBlue = Constants.BG_BLUE_DEFAULT;

    public double split = 0.33;
    public double halve = 0.5;
    public double referenceWindow = 1.0;

    public boolean showGeometry2D = true;
    public boolean showVolume2D = true;
    public boolean showMask2D = true;
    public boolean showCross2D = true;
    public boolean showOverlay2D = true;

    public double xposMouse = Constants.XPOS_FACTOR;
    public double yposMouse = Constants.YPOS_FACTOR;
    public double zposMouse = Constants.ZPOS_FACTOR;
    public double scaleMouse = Constants.SCALE_FACTOR;
    public double xrotMouse = Constants.XROT_FACTOR;
    public double yrotMouse = Constants.YROT_FACTOR;

    public float ambientLight = Constants.AMBIENT_DEFAULT;
    public float diffuseLight = Constants.DIFFUSE_DEFAULT;
    public float specularLight = Constants.SPECULAR_DEFAULT;

    public float boxRed = Constants.BOX_RED_DEFAULT;
    public float boxGreen = Constants.BOX_GREEN_DEFAULT;
    public float boxBlue = Constants.BOX_BLUE_DEFAULT;

    public int lineWidth = Constants.BOX_WIDTH_DEFAULT;

    public boolean scaleVisible = false;
    public boolean scaleBoxVisible = false;

    public float scaleRed = Constants.SCALE_RED_DEFAULT;
    public float scaleGreen = Constants.SCALE_GREEN_DEFAULT;
    public float scaleBlue = Constants.SCALE_BLUE_DEFAULT;

    public int scaleWidth = Constants.SCALE_WIDTH_DEFAULT;
    public double scaleValue = 0;

    public Box detail = null;
    public boolean anatomical = false;
    public boolean transparent = false;

    public double xrotAuto = 0.0;
    public double yrotAuto = 0.0;
    public boolean autoSort = false;

    public boolean orthoView = false;
    public double fov = Constants.FOV_DEFAULT;

    public boolean frameVisible = false;
    public boolean boxVisible = false;

    public void debug()
    {
        try
        {
            Viewer.getInstance().control.setStatusMessage("Printing debugging information");
            for (Field field : this.getClass().getFields())
            {
                Viewer.getInstance().control.setStatusMessage(String.format("  %s: %s", field.getName(), field.get(this).toString()));
            }
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    public void save(String fn) throws FileNotFoundException
    {
        // TODO: figure out how to write slicers, colormaps, and annotations

        try
        {
            PathUtils.mkpar(fn);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);
        PrintStream stream = new PrintStream(fn);
        stream.println(json);
        stream.close();
    }

    public void load(String fn) throws IOException
    {
        try
        {
            if (!new File(fn).exists())
            {
                throw new RuntimeException("settings does not exist: " + fn);
            }

            Gson gson = new Gson();
            String json = Files.toString(new File(fn), Charsets.UTF_8);
            Settings state = gson.fromJson(json, Settings.class);

            Set<Field> fields = Sets.newHashSet(state.getClass().getFields());
            fields.retainAll(Sets.newHashSet(this.getClass().getFields()));

            for (Field field : fields)
            {
                // this will overwrite transient variables unless we check for this
                if (!Modifier.isTransient(field.getModifiers()))
                {
                    field.set(this, field.get(state));
                }
            }
        }
        catch (Exception e)
        {
            Viewer.getInstance().control.setStatusMessage("failed to load settings");
            e.printStackTrace();
        }
    }
}