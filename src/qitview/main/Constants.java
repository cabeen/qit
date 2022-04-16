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

import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;

import java.awt.Color;

public class Constants
{
    public static final String TITLE = "qitview";

    public static final int KEY_WAIT = 300;

    public static final Integer FPS_TIMEOUT = 3000;
    public static final Integer FPS_FAST = 60;
    public static final Integer FPS_SLOW = 1;

    public static final Float FOV_DEFAULT = 25f;
    public static final Integer WIDTH = 800;
    public static final Integer HEIGHT = 720;
    public static final Integer USER_WIDTH = 400;
    public static final Integer USER_HEIGHT = 250;
    public static final Integer MSG_HEIGHT = 350;
    public static final Integer MSG_WIDTH = 850;

    public static final String GLOBAL_SETTINGS = "globals.json";

    public static final String VIEW_TOGGLE = "Toggle Visibility of Selection";
    public static final String VIEW_LIST_NEXT = "Toggle Visibility to Next Item";
    public static final String VIEW_LIST_PREV = "Toggle Visibility to Previous Item";
    public static final String VIEW_SHOW_ONLY = "Show Only Selection";
    public static final String VIEW_SHOW_ALL = "Show Everything";

    public static final String VIEW_MOVE_UP = "Move Item Up List";
    public static final String VIEW_MOVE_DOWN = "Move Item Down List";
    public static final String VIEW_MOVE_TOP = "Move Item to Top";
    public static final String VIEW_MOVE_BOTTOM = "Move Item to Bottom";

    public static final String VIEW_ZOOM_DETAIL = "Zoom Detail (only selection) (3D View)";
    public static final String VIEW_ZOOM_OVERVIEW = "Zoom Overview (everything) (3D View)";

    public static final String VIEW_TOGGLE_MASK_2D = "Toggle Visibility of the Mask (2D View)";
    public static final String VIEW_TOGGLE_CROSS_2D = "Toggle Visibility of the Crosshairs (2D View)";
    public static final String VIEW_TOGGLE_OVERLAY_2D = "Toggle Visibility of the Overlay (2D View)";
    public static final String VIEW_RESET_VIEW_2D = "Reset the 2D view position and zoom";

    public static final String VIEW_LAYOUT_3D = "Set to 3D viewer";
    public static final String VIEW_LAYOUT_I = "Set to the 2D viewer of slice I";
    public static final String VIEW_LAYOUT_J = "Set to the 2D viewer of slice J";
    public static final String VIEW_LAYOUT_K = "Set to the 2D viewer of slice K";
    public static final String VIEW_LAYOUT_I3D = "Set to the 2D/3D viewer of slice I";
    public static final String VIEW_LAYOUT_J3D = "Set to the 2D/3D viewer of slice J";
    public static final String VIEW_LAYOUT_K3D = "Set to the 2D/3D viewer of slice K";
    public static final String VIEW_LAYOUT_1BY3 = "Set to the 3D over slice views";
    public static final String VIEW_LAYOUT_2BY2 = "Set to the grid view";

    public static final String VIEW_POSE_RIGHT = "Zoom Right (3D View)";
    public static final String VIEW_POSE_LEFT = "Zoom Left (3D View)";
    public static final String VIEW_POSE_TOP = "Zoom Top (3D View)";
    public static final String VIEW_POSE_BOTTOM = "Zoom Bottom (3D View)";
    public static final String VIEW_POSE_FRONT = "Zoom Front (3D View)";
    public static final String VIEW_POSE_BACK = "Zoom Back (3D View)";
    public static final String VIEW_POSE_ANGLES = "Zoom Angles (3D View)";
    public static final String VIEW_POSE_RESET = "Reset the 3D view position and rotation";

    public static final String VIEW_SLICE_I = "Toggle Visibility of Slice I (3D View)";
    public static final String VIEW_SLICE_J = "Toggle Visibility of Slice J (3D View)";
    public static final String VIEW_SLICE_K = "Toggle Visibility of Slice K (3D View)";

    public static final String VIEW_CHAN_NEXT = "Show Next Channel";
    public static final String VIEW_CHAN_PREV = "Show Previous Channel";

    public static final String DATA_CREATE_MASK = "Create Mask From Selection";
    public static final String DATA_CREATE_SPHERE = "Create Sphere From Selection";
    public static final String DATA_CREATE_BOX = "Create Box From Selection";
    public static final String DATA_CREATE_VECTS = "Create an Empty Vects Object";

    public static final String DATA_AUTO_MIN_MAX = "Detect Intensity Range";
    public static final String DATA_SORT_FACES = "Sort Mesh Faces";

    public static final String DATA_VOLUME_TO_MASK = "Convert Volumes to Masks";
    public static final String DATA_MASK_TO_VOLUME = "Convert Masks to Volumes";

    public static final String DATA_DELETE_FILENAMES = "Clear Filenames";
    public static final String DATA_DELETE_VALUE = "Clear Values";
    public static final String DATA_DELETE_INTERACTION = "Clear Interaction";

    public static final String DATA_DELETE_SELECTION = "Delete Selected Data";
    public static final String DATA_DELETE_ALL = "Delete All Data";

    public static final String TAB_GLOBAL = "Global";
    public static final String TAB_INFO = "Info";
    public static final String TAB_VIEW = "View";
    public static final String TAB_EDIT = "Edit";
    public static final String TAB_QUERY = "Query";

    public static final String FILE_MENU_SHOT_1X = "Take Screenshot (1x)";
    public static final String FILE_MENU_SHOT_2X = "Take Screenshot (2x)";
    public static final String FILE_MENU_SHOT_3X = "Take Screenshot (3x)";
    public static final String FILE_MENU_SHOT_NX = "Take Screenshot (Nx)";

    public static final String SETTINGS_COLORMAPS_SOLID = "Solid Colormaps";
    public static final String SETTINGS_COLORMAPS_DISCRETE = "Discrete Colormaps";
    public static final String SETTINGS_COLORMAPS_SCALAR = "Scalar Colormaps";

    public static final String SETTINGS_RENDERING = "Rendering Settings";
    public static final String SETTINGS_REFERENCE = "Reference Settings";
    public static final String SETTINGS_INTERACTION = "Interaction Settings";
    public static final String SETTINGS_ANNOTATION = "Annotation Settings";
    public static final String SETTINGS_DATA = "Data Settings";

    public static final String SETTINGS_PROCESSES = "Process Manager";
    public static final String SETTINGS_MODULES = "Modules Manager";
    public static final String SETTINGS_ADVANCED = "Advanced Settings";
    public static final String SETTINGS_REPL = "Interpreter";
    public static final String SETTINGS_DIAGNOSTICS = "Print Diagnostics";

    public static final String HELP_ABOUT = "About";
    public static final String HELP_CITE = "How to cite QIT";
    public static final String HELP_BUILD = "Build Information";
    public static final String HELP_LICENSE = "License";
    public static final String HELP_MANUAL = "User Manual";
    public static final String MANUAL_URI = "http://cabeen.io/qitwiki";
    public static final String ABOUT_MSG =
            "qitview is a tool for interactive 3D data analysis and visualization \n" +
                    "and is part of the Quantitative Imaging Toolkit (QIT).  You can learn \n" +
                    "more about it at http://cabeen.io/qitwiki. \n";

    public static final String SETTINGS_BG_SET = "Choose BG";
    public static final String SETTINGS_BG_RESET = "Reset BG";
    public static final String SETTINGS_BG_BLACK = "Black BG";

    public static final String FILE_MENU_BACKUPS = "Enable File Backups";
    public static final String FILE_MENU_CLOBBER = "Enable File Clobbering";

    public static final String FILE_MENU_LOAD_FILES = "Load Files";
    public static final String FILE_MENU_LOAD_SCENE = "Load Scene";
    public static final String FILE_MENU_LOAD_GLOBAL = "Load Global Settings";
    public static final String FILE_MENU_QUIT = "Quit";

    public static final String FILE_MENU_SAVE_SEL_FILES = "Save Selected File(s)";
    public static final String FILE_MENU_SAVE_SEL_FILES_AS = "Save Selected File(s) As";
    public static final String FILE_MENU_SAVE_ALL_FILES = "Save All File(s)";
    public static final String FILE_MENU_SAVE_ALL_FILES_AS = "Save All File(s) As";
    public static final String FILE_MENU_SAVE_SEL_SCENE = "Save Selected Scene";
    public static final String FILE_MENU_SAVE_SEL_SCENE_AS = "Save Selected Scene As";
    public static final String FILE_MENU_SAVE_ALL_SCENE = "Save Entire Scene";
    public static final String FILE_MENU_SAVE_ALL_SCENE_AS = "Save Entire Scene As";
    public static final String FILE_MENU_SAVE_GLOBAL = "Save Global Settings to File";

    public static final String FILE_MENU_SAVE_DEFAULT_GLOBAL = "Save Default Global Settings";
    public static final String FILE_MENU_CLEAR_DEFAULT_GLOBAL = "Clear Default Global Settings";

    public static final String INTERACTION_ROTATE = "Rotate";
    public static final String INTERACTION_PAN = "Pan";
    public static final String INTERACTION_ZOOM = "Zoom";

    public static final String NOSEL = "No Selection";

    public static final Float SCALE_MIN = 0.01f;
    public static final Vect X_AXIS = VectSource.createX();
    public static final Vect Y_AXIS = VectSource.createY();
    public static final Vect Z_AXIS = VectSource.createZ();
    public static final Float XPOS_FACTOR = 0.0005f;
    public static final Float YPOS_FACTOR = 0.0005f;
    public static final Float ZPOS_FACTOR = -0.001f;
    public static final Float SCALE_FACTOR = 0.0045f;
    public static final Float XROT_FACTOR = -0.005f;
    public static final Float YROT_FACTOR = 0.005f;
    public static final Float XPOS_DEFAULT = 0f;
    public static final Float YPOS_DEFAULT = 0f;
    public static final Float ZPOS_DEFAULT = -1.5f;
    public static final Float SCALE_DEFAULT = 1.0f;
    public static final Matrix ROT_DEFAULT = MatrixSource.identity(3);

    public static final Float BOX_RED_DEFAULT = 1.0f;
    public static final Float BOX_GREEN_DEFAULT = 1.0f;
    public static final Float BOX_BLUE_DEFAULT = 1.0f;
    public static final Integer BOX_WIDTH_DEFAULT = 2;

    public static final Float SCALE_RED_DEFAULT = 1.0f;
    public static final Float SCALE_GREEN_DEFAULT = 1.0f;
    public static final Float SCALE_BLUE_DEFAULT = 1.0f;
    public static final Integer SCALE_WIDTH_DEFAULT = 1;

    public static final Float BG_RED_DEFAULT = 84f / 255;
    public static final Float BG_GREEN_DEFAULT = 88f / 255;
    public static final Float BG_BLUE_DEFAULT = 109f / 255;
    public static final Color BG_COLOR_DEFAULT = new Color(BG_RED_DEFAULT, BG_GREEN_DEFAULT, BG_BLUE_DEFAULT);

    public static final Float AMBIENT_DEFAULT = 0.05f;
    public static final Float DIFFUSE_DEFAULT = 0.30f;
    public static final Float SPECULAR_DEFAULT = 0.05f;

    public static final float[] LIGHT1 = {5f, 5f, 5f, 1f};
    public static final float[] LIGHT2 = {-5f, 5f, 5f, 1f};
    public static final float[] LIGHT3 = {0f, 0f, 5f, 1f};

    public static final String LOAD_PREFIX = "load-";
}