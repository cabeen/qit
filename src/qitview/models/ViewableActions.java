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

package qitview.models;

import com.google.common.collect.Lists;
import org.apache.commons.io.FilenameUtils;
import qit.base.Dataset;
import qit.base.Logging;
import qit.base.utils.PathUtils;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.formats.vects.RawVectsCoder;
import qit.data.modules.curves.*;
import qit.data.modules.mask.*;
import qit.data.modules.mesh.MeshComponents;
import qit.data.modules.mesh.MeshMeasure;
import qit.data.modules.mesh.MeshSetVects;
import qit.data.modules.mesh.MeshSimplify;
import qit.data.modules.mesh.MeshSmooth;
import qit.data.modules.mesh.MeshSubdivide;
import qit.data.modules.mesh.MeshTransform;
import qit.data.modules.mri.model.VolumeModelFeature;
import qit.data.modules.neuron.NeuronFilter;
import qit.data.modules.neuron.NeuronMeasure;
import qit.data.modules.neuron.NeuronMesh;
import qit.data.modules.neuron.NeuronStitch;
import qit.data.modules.volume.VolumeEnhanceContrast;
import qit.data.modules.volume.VolumeFilterGaussian;
import qit.data.modules.volume.VolumeFilterMedian;
import qit.data.modules.volume.VolumeMarchingCubes;
import qit.data.modules.volume.VolumeMeasure;
import qit.data.modules.volume.VolumeSegmentForeground;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.modules.volume.VolumeThresholdOtsu;
import qit.data.source.MaskSource;
import qitview.main.Viewer;
import qitview.panels.Viewables;
import qitview.views.MeshView;
import qitview.widgets.SwingUtils;

import javax.swing.JButton;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class ViewableActions
{
    public static final List<ViewableAction> Volume = Lists.newArrayList();
    public static final List<ViewableAction> Mask = Lists.newArrayList();
    public static final List<ViewableAction> Curves = Lists.newArrayList();
    public static final List<ViewableAction> Mesh = Lists.newArrayList();
    public static final List<ViewableAction> Gradients = Lists.newArrayList();
    public static final List<ViewableAction> Vects = Lists.newArrayList();
    public static final List<ViewableAction> Affine = Lists.newArrayList();
    public static final List<ViewableAction> Table = Lists.newArrayList();
    public static final List<ViewableAction> Neuron = Lists.newArrayList();
    public static final List<ViewableAction> Deform = Lists.newArrayList();
    public static final List<ViewableAction> Dataset = Lists.newArrayList();

    public static List<ViewableAction> getList(Viewable view)
    {
        return getList(ViewableType.getFromView(view));
    }

    public static List<ViewableAction> getList(ViewableType type)
    {
        switch (type)
        {
            case Volume:
                return Volume;
            case Mask:
                return Mask;
            case Curves:
                return Curves;
            case Mesh:
                return Mesh;
            case Gradients:
                return Gradients;
            case Vects:
                return Vects;
            case Affine:
                return Affine;
            case Neuron:
                return Neuron;
            case Deformation:
                return Deform;
            default:
                return Dataset;
        }
    }

    static
    {
        {
            Volume.add(new ViewableAction().withName("Copy Volume").withDescription("Create a copy of this volume").withFunction(data -> Optional.of(((Volume) data).copy())));
            Volume.add(new ViewableAction().withName("Create Empty Mask").withDescription("Create an empty mask for segmenting this volume").withFunction(data -> Optional.of(MaskSource.create(((Volume) data).getSampling()))));
            Volume.add(new ViewableAction().withName("Filter Gaussian").withDescription("Apply a Gaussian filter with a given sigma (-1 is voxel size)").withReplacable().withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Sigma:", "-1");
                return response.isPresent() ? Optional.of(VolumeFilterGaussian.apply((Volume) data, Double.valueOf(response.get()))) : Optional.empty();
            }));
            Volume.add(new ViewableAction().withName("Filter Median").withDescription("Apply a median filter").withReplacable().withFunction(data -> Optional.of(VolumeFilterMedian.apply((Volume) data))));
            Volume.add(new ViewableAction().withName("Enhance Contrast").withDescription("Enhance the contrast through histogram normalization").withReplacable().withFunction(data -> Optional.of(VolumeEnhanceContrast.apply((Volume) data))));
            Volume.add(new ViewableAction().withName("Segment Threshold Custom").withDescription("Apply a user-defined threshold to create a mask").withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Threshold:", "0.5");
                return response.isPresent() ? Optional.of(VolumeThreshold.apply((Volume) data, Double.valueOf(response.get()))) : Optional.empty();
            }));
            Volume.add(new ViewableAction().withName("Segment Threshold Auto").withDescription("Apply automated Otsu thresholding to create a mask").withFunction(data -> Optional.of(VolumeThresholdOtsu.apply(((Volume) data)))));
            Volume.add(new ViewableAction().withName("Segment Foreground").withDescription("Extract the foregorund through a statistical procedure").withFunction(data -> Optional.of(VolumeSegmentForeground.apply(((Volume) data)))));
            Volume.add(new ViewableAction().withName("Extract Mesh").withDescription("Extract a mesh at a given intensity level using marching cubes").withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Level:", "0.5");
                return response.isPresent() ? Optional.of(VolumeMarchingCubes.apply((Volume) data, Double.valueOf(response.get()))) : Optional.empty();
            }));
            Volume.add(new ViewableAction().withName("Extract Model Feature").withDescription("Extract a feature of a model volume").withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Feature:", "FA");
                return response.isPresent() ? Optional.of(VolumeModelFeature.apply((Volume) data, response.get())) : Optional.empty();
            }));
            Volume.add(new ViewableAction().withName("Measure").withDescription("Create a statistical summary of the volume").withFunction(data -> Optional.of(VolumeMeasure.apply(((Volume) data)))));
        }

        {
            Mask.add(new ViewableAction().withName("Copy Mask").withDescription("Create a copy of this mask").withFunction(data -> Optional.of(((Mask) data).copy())));
            Mask.add(new ViewableAction().withName("Export to Volume").withDescription("Export the mask to a volume").withFunction(data -> Optional.of(((Mask) data).copyVolume())));
            Mask.add(new ViewableAction().withName("Measure").withDescription("Create a statistical summary of the mask").withFunction(data -> Optional.of(MaskMeasure.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Extract All Meshes").withDescription("Extract a mesh for each region").withFunction(data -> Optional.of(MaskMarchingCubes.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Extract Region Mesh").withDescription("Extract a mesh for the given regions").withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Label Query:", "1");
                return response.isPresent() ? Optional.of(MaskMarchingCubes.apply((Mask) data, response.get())) : Optional.empty();
            }));
            Mask.add(new ViewableAction().withName("Extract Region Mask").withDescription("Extract a mask for the given regions").withReplacable().withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Label Query:", "1");
                return response.isPresent() ? Optional.of(MaskExtract.apply((Mask) data, response.get())) : Optional.empty();
            }));
            Mask.add(new ViewableAction().withName("Connected Components").withDescription("Label connected components").withReplacable().withFunction(data -> Optional.of(MaskComponents.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Largest Component").withDescription("Extract the largest connected component").withReplacable().withFunction(data -> Optional.of(MaskLargest.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Binarize").withDescription("Convert the mask into binary").withReplacable().withFunction(data -> Optional.of(MaskBinarize.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Erode").withDescription("Erode the mask using mathematical morphology").withReplacable().withFunction(data -> Optional.of(MaskErode.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Dilate").withDescription("Dilate the mask using mathematical morphology").withReplacable().withFunction(data -> Optional.of(MaskDilate.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Close").withDescription("Close the mask using mathematical morphology").withReplacable().withFunction(data -> Optional.of(MaskClose.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Open").withDescription("Open the mask using mathematical morphology").withReplacable().withFunction(data -> Optional.of(MaskOpen.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Fill").withDescription("Fill the empty space inside the mask").withReplacable().withFunction(data -> Optional.of(MaskFill.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Invert").withDescription("Invert the mask to swap foreground and background").withReplacable().withFunction(data -> Optional.of(MaskInvert.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Hull").withDescription("Compute the convex hull of the mask").withReplacable().withFunction(data -> Optional.of(MaskHull.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Skeletonize").withDescription("Compute a mask skeleton").withReplacable().withFunction(data -> Optional.of(MaskSkeleton.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Extract Boundary").withDescription("Extract the boundary mask").withReplacable().withFunction(data -> Optional.of(MaskBoundary.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Extract Shell").withDescription("Extract the shell mask").withReplacable().withFunction(data -> Optional.of(MaskShell.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Filter Mode").withDescription("Filter by selecting the most frequent label in each voxel neighborhood").withReplacable().withFunction(data -> Optional.of(MaskFilterMode.apply(((Mask) data)))));
            Mask.add(new ViewableAction().withName("Distance Transform").withDescription("Compute the distance tranform").withFunction(data -> Optional.of(MaskDistanceTransform.apply(((Mask) data)))));
        }

        {
            Curves.add(new ViewableAction().withName("Copy Curves").withDescription("Create a copy of these curves").withFunction(data -> Optional.of(((qit.data.datasets.Curves) data).copy())));
            Curves.add(new ViewableAction().withName("Extract Density Map").withDescription("Extract volumetric map of the curve density").withFunction(data ->
            {
                if (Viewer.getInstance().gui.hasReferenceVolume())
                {
                    Volume ref = Viewer.getInstance().gui.getReferenceVolume().getData();
                    return Optional.of(CurvesDensity.apply((Curves) data, ref));
                }
                else
                {
                    SwingUtils.showMessage("Error: you must load a reference volume");
                    return Optional.empty();
                }
            }));
            Curves.add(new ViewableAction().withName("Extract Mask").withDescription("Extract a mask of the curves").withReplacable().withFunction(data ->
            {
                if (Viewer.getInstance().gui.hasReferenceVolume())
                {
                    Volume ref = Viewer.getInstance().gui.getReferenceVolume().getData();
                    return Optional.of(CurvesMask.apply((Curves) data, ref));
                }
                else
                {
                    SwingUtils.showMessage("Error: you must load a reference volume");
                    return Optional.empty();
                }
            }));
            Curves.add(new ViewableAction().withName("Simplify").withDescription("Simplify the curves by collapsing edges").withReplacable().withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Threshold:", "1.0");
                return response.isPresent() ? Optional.of(CurvesSimplify.apply((Curves) data, Double.valueOf(response.get()))) : Optional.empty();
            }));
            Curves.add(new ViewableAction().withName("Subdivide").withDescription("Subdivide the curves by adding vertices").withReplacable().withFunction(data -> Optional.of(CurvesSubdivide.apply((Curves) data))));
            Curves.add(new ViewableAction().withName("Subsample").withDescription("Subsample the curves by sampling a subset of curves").withReplacable().withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Samples:", "10000");
                return response.isPresent() ? Optional.of(CurvesReduce.apply((Curves) data, Integer.valueOf(response.get()))) : Optional.empty();
            }));
            Curves.add(new ViewableAction().withName("Smooth Laplacian").withDescription("Smooth the curve vertices with a laplacian filter").withReplacable().withFunction(data -> Optional.of(CurvesSmooth.apply((Curves) data))));
            Curves.add(new ViewableAction().withName("Smooth LOESS").withDescription("Smooth the curve vertices with a LOESS filter").withReplacable().withFunction(data -> Optional.of(CurvesFilterLoess.apply((Curves) data))));
            Curves.add(new ViewableAction().withName("Filter by Length").withDescription("Simplify the curves by collapsing edges").withReplacable().withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Min Length:", "5.0");
                return response.isPresent() ? Optional.of(CurvesSelect.applyLength((Curves) data, Double.valueOf(response.get()))) : Optional.empty();
            }));
            Curves.add(new ViewableAction().withName("Extract Longest").withDescription("Extract the longest curve").withReplacable().withFunction(data -> Optional.of(CurvesSelect.applyLongest((Curves) data))));
            Curves.add(new ViewableAction().withName("Cluster SCPT").withDescription("Cluster using the sparse closest point transform").withReplacable().withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Threshold:", "10.0");
                return response.isPresent() ? Optional.of(CurvesClusterSCPT.applyCluster((Curves) data, Double.valueOf(response.get()))) : Optional.empty();
            }));
            Curves.add(new ViewableAction().withName("Cluster QuickBundle").withDescription("Cluster using the Quickbundles algorithm").withReplacable().withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Threshold:", "1000.0");
                return response.isPresent() ? Optional.of(CurvesClusterQuickBundle.applyCluster((Curves) data, Double.valueOf(response.get()))) : Optional.empty();
            }));
            Curves.add(new ViewableAction().withName("Simplify SCPT").withDescription("Cluster using the sparse closest point transform").withReplacable().withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Threshold:", "1.5");
                return response.isPresent() ? Optional.of(CurvesClusterSCPT.applySimplify((Curves) data, Double.valueOf(response.get()))) : Optional.empty();
            }));
            Curves.add(new ViewableAction().withName("Simplify QuickBundle").withDescription("Cluster using the Quickbundles algorithm").withReplacable().withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Threshold:", "2.0");
                return response.isPresent() ? Optional.of(CurvesClusterQuickBundle.applySimplify((Curves) data, Double.valueOf(response.get()))) : Optional.empty();
            }));
            Curves.add(new ViewableAction().withName("Image to World").withDescription("Convert the curve coordinates from image indices to world coordinates based on the reference volume").withReplacable().withFunction(data ->
            {
                if (Viewer.getInstance().gui.hasReferenceVolume())
                {
                    Volume ref = Viewer.getInstance().gui.getReferenceVolume().getData();
                    return Optional.of(CurvesTransform.applyPose((Curves) data, ref));
                }
                else
                {
                    SwingUtils.showMessage("Error: you must load a reference volume");
                    return Optional.empty();
                }
            }));
            Curves.add(new ViewableAction().withName("World to Image").withDescription("Convert the curve coordinates from world coordinates to image indices based on the reference volume").withReplacable().withFunction(data ->
            {
                if (Viewer.getInstance().gui.hasReferenceVolume())
                {
                    Volume ref = Viewer.getInstance().gui.getReferenceVolume().getData();
                    return Optional.of(CurvesTransform.applyInversePose((Curves) data, ref));
                }
                else
                {
                    SwingUtils.showMessage("Error: you must load a reference volume");
                    return Optional.empty();
                }
            }));
            Curves.add(new ViewableAction().withName("Measure").withDescription("Create a statistical summary of the curves").withFunction(data -> Optional.of(CurvesMeasure.apply(((Curves) data)))));
        }
        {
            Mesh.add(new ViewableAction().withName("Copy Mesh").withDescription("Create a copy of the mesh").withFunction(data -> Optional.of(((Mesh) data).copy())));
            Mesh.add(new ViewableAction().withName("Simplify").withDescription("Simplify the mesh by collapsing edges").withReplacable().withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Mean Face Area:", "1.0");
                return response.isPresent() ? Optional.of(MeshSimplify.apply((Mesh) data, Double.valueOf(response.get()))) : Optional.empty();
            }));
            Mesh.add(new ViewableAction().withName("Subdivide").withDescription("Subdivide the mesh by splitting each face into four smaller faces").withReplacable().withFunction(data -> Optional.of(MeshSubdivide.apply(((Mesh) data)))));
            Mesh.add(new ViewableAction().withName("Smooth").withDescription("Smooth the mesh using a Laplacian filter").withReplacable().withFunction(data -> Optional.of(MeshSmooth.apply((Mesh) data, 1))));
            Mesh.add(new ViewableAction().withName("Filter Largest").withDescription("Extract the largest connected component").withReplacable().withFunction(data -> Optional.of(MeshComponents.applyLargest(((Mesh) data)))));
            Mesh.add(new ViewableAction().withName("Filter by Area").withDescription("Extract connected component with at least the given surface area").withReplacable().withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Minimum Surface Area:", "10.0");
                return response.isPresent() ? Optional.of(MeshComponents.applyArea((Mesh) data, Double.valueOf(response.get()))) : Optional.empty();
            }));
            Mesh.add(new ViewableAction().withName("Image to World").withDescription("Convert the mesh coordinates from image indices to world coordinates based on the reference volume").withReplacable().withFunction(data ->
            {
                if (Viewer.getInstance().gui.hasReferenceVolume())
                {
                    Volume ref = Viewer.getInstance().gui.getReferenceVolume().getData();
                    return Optional.of(MeshTransform.applyPose((Mesh) data, ref));
                }
                else
                {
                    SwingUtils.showMessage("Error: you must load a reference volume");
                    return Optional.empty();
                }
            }));
            Mesh.add(new ViewableAction().withName("World to Image").withDescription("Convert the mesh coordinates from world coordinates to image indices based on the reference volume").withReplacable().withFunction(data ->
            {
                if (Viewer.getInstance().gui.hasReferenceVolume())
                {
                    Volume ref = Viewer.getInstance().gui.getReferenceVolume().getData();
                    return Optional.of(MeshTransform.applyInversePose((Mesh) data, ref));
                }
                else
                {
                    SwingUtils.showMessage("Error: you must load a reference volume");
                    return Optional.empty();
                }
            }));

            Function<Function<String, Vects>, Function<qit.base.Dataset, Optional<Dataset>>> loader = (reader) ->
            {
                return (data) ->
                {
                    Mesh mymesh = (Mesh) data;
                    String fn = Viewer.getInstance().gui.chooseLoadFiles("Choose an attribute text file").get(0);
                    Logging.info("found attribute file: " + fn);
                    Vects attr = reader.apply(fn);

                    if (attr == null)
                    {
                        return Optional.empty();
                    }

                    String guess = FilenameUtils.removeExtension(PathUtils.basename(fn));
                    Optional<String> aname = SwingUtils.getStringOptionalEventThread("Attribute Name:", guess);
                    if (aname.isPresent())
                    {
                        int msize = mymesh.vattr.size();
                        int asize = attr.size();
                        if (asize == msize || attr.getDim() == msize)
                        {
                            return Optional.of(new MeshSetVects()
                            {{
                                this.mesh = mymesh;
                                this.name = aname.get();
                                this.vects = attr;
                            }}.run().output);
                        }
                        else
                        {
                            SwingUtils.showMessage(String.format("Error: the number of attribute values (%d) does not match the number of mesh vertices (%d)", asize, msize));
                        }
                    }

                    return Optional.empty();
                };
            };

            Mesh.add(new ViewableAction().withName("Load Attribute from File").withDescription("Load a mesh attribute from a file (length should match the number of vertices)").withReplacable().withFunction(loader.apply(
                    (fn) ->
                    {
                        try
                        {
                            return qit.data.datasets.Vects.read(fn);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            return null;
                        }
                    })));
            Mesh.add(new ViewableAction().withName("Load Attribute (Float32)").withDescription("Load a mesh attribute from a file (length should match the number of vertices)").withReplacable().withFunction(loader.apply(
                    (fn) ->
                    {
                        try
                        {
                            return RawVectsCoder.readFloat32(fn);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            return null;
                        }
                    })));
            Mesh.add(new ViewableAction().withName("Load Attribute (Float64)").withDescription("Load a mesh attribute from a file (length should match the number of vertices)").withReplacable().withFunction(loader.apply(
                    (fn) ->
                    {
                        try
                        {
                            return RawVectsCoder.readFloat64(fn);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            return null;
                        }
                    })));
            Mesh.add(new ViewableAction().withName("Load Attribute (Int)").withDescription("Load a mesh attribute from a file (length should match the number of vertices)").withReplacable().withFunction(loader.apply(
                    (fn) ->
                    {
                        try
                        {
                            return RawVectsCoder.readInt(fn);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            return null;
                        }
                    })));
            Mesh.add(new ViewableAction().withName("Measure").withDescription("Create a statistical summary of the mesh").withFunction(data -> Optional.of(MeshMeasure.apply(((Mesh) data)))));
        }

        {
            Neuron.add(new ViewableAction().withName("Copy Neuron").withDescription("Create a copy of the neuron").withFunction(data -> Optional.of(((qit.data.datasets.Neuron) data).copy())));
            Neuron.add(new ViewableAction().withName("Simplify").withDescription("Simplify the neuron by collapsing edges").withReplacable().withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Threshold:", "1.0");
                return response.isPresent() ? Optional.of(NeuronFilter.applySimplify((qit.data.datasets.Neuron) data, Double.valueOf(response.get()))) : Optional.empty();
            }));
            Neuron.add(new ViewableAction().withName("Filter Laplacian").withDescription("Smooth the neuron with a Laplacian filter").withReplacable().withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Number of iterations:", "5");
                return response.isPresent() ? Optional.of(NeuronFilter.applySmoothLaplacian((qit.data.datasets.Neuron) data, Integer.valueOf(response.get()))) : Optional.empty();
            }));
            Neuron.add(new ViewableAction().withName("Filter LOESS").withDescription("Smooth the neuron with a LOESS filter").withReplacable().withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Neighborhood size:", "5");
                return response.isPresent() ? Optional.of(NeuronFilter.applySmoothLoess((qit.data.datasets.Neuron) data, Integer.valueOf(response.get()))) : Optional.empty();
            }));
            Neuron.add(new ViewableAction().withName("Cut").withDescription("Cut away distant parts of the neuron").withReplacable().withFunction(data ->
            {
                Optional<String> response = SwingUtils.getStringOptional(Viewer.getInstance().gui.getFrame(), "Cut distance:", "5");
                return response.isPresent() ? Optional.of(NeuronFilter.applyCut((qit.data.datasets.Neuron) data, Integer.valueOf(response.get()))) : Optional.empty();
            }));
            Neuron.add(new ViewableAction().withName("Stitch").withDescription("Stitch together close nodes to form one neuron").withReplacable().withFunction(data -> Optional.of(NeuronStitch.apply((qit.data.datasets.Neuron) data))));
            Neuron.add(new ViewableAction().withName("Sort").withDescription("Sort the nodes by precedence").withReplacable().withFunction(data -> Optional.of(NeuronFilter.applySort((qit.data.datasets.Neuron) data))));
            Neuron.add(new ViewableAction().withName("Relabel").withDescription("Relabel the nodes by precedence").withReplacable().withFunction(data -> Optional.of(NeuronFilter.applyRelabel((qit.data.datasets.Neuron) data))));
            Neuron.add(new ViewableAction().withName("Convert to Mesh").withDescription("Create a mesh that represents the neuron").withFunction(data -> Optional.of(NeuronMesh.apply((qit.data.datasets.Neuron) data))));
            Neuron.add(new ViewableAction().withName("Measure").withDescription("Create a statistical summary of the neuron").withFunction(data -> Optional.of(NeuronMeasure.apply(((qit.data.datasets.Neuron) data)))));
        }
    }
}