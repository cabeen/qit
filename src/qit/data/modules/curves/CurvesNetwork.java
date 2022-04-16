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

package qit.data.modules.curves;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.base.utils.PathUtils;
import qit.data.datasets.Affine;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Schema;
import qit.data.datasets.Table;
import qit.data.modules.mask.MaskMeasure;
import qit.data.source.TableSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.MatrixUtils;
import qit.data.utils.TableUtils;
import qit.math.utils.MathUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ModuleUnlisted
@ModuleDescription("Construct a structural network model from tractography curves")
@ModuleAuthor("Ryan Cabeen")
public class CurvesNetwork implements Module
{

    @ModuleInput
    @ModuleDescription("input curves")
    private Curves curves;

    @ModuleInput
    @ModuleDescription("input regions of interest")
    private Mask regions;

    @ModuleInput
    @ModuleDescription("a lookup table for label names (see name and index attribute options)")
    private Table lookup = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the window size in voxels")
    private int window = 1;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the minima connection size")
    private int count = 3;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the volume resolution")
    private double delta = 1;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the density threshold")
    private double thresh = 0.5;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the name of the field storing the index for each region in the lookup table")
    private String index = "index";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the name of the field storing the name of each region in the lookup table")
    private String name = "name";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("include relative network measures (each connection is normalized by the total brain value)")
    private boolean relative = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("include corrected network measures (each connection is normalized by the average region volume)")
    private boolean corrected = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("save connection geometry")
    private boolean save = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("backup existing data")
    private boolean backup = false;

    @ModuleParameter
    @ModuleDescription("the output directory")
    private String output;

    @Override
    public Module run()
    {
        try
        {
            String tmpDir = PathUtils.tmpDir(this.output);
            String matricesDir = PathUtils.join(tmpDir, "matrices");
            String curvesDir = PathUtils.join(tmpDir, "curves");
            String mapsDir = PathUtils.join(tmpDir, "curves.map");

            PathUtils.mkdirs(tmpDir);
            PathUtils.mkdirs(matricesDir);
            PathUtils.mkdirs(curvesDir);
            PathUtils.mkdirs(mapsDir);

            Logging.info("using tmp dir: " + tmpDir);

            Logging.info("computing whole brain measures");
            Record wholeMeasures = measure(this.curves);
            TableSource.createNarrow(wholeMeasures).write(PathUtils.join(tmpDir, "curves.csv"));

            Logging.info("computing region measures");
            MaskMeasure maskMeasure = new MaskMeasure();
            maskMeasure.input = this.regions;
            maskMeasure.lookup = this.lookup;
            maskMeasure.lutIndexField = this.index;
            maskMeasure.lutNameField = this.name;
            Table regionTable = maskMeasure.run().output;
            regionTable.write(PathUtils.join(tmpDir, "regions.csv"));

            double wholeNumber = Double.valueOf(wholeMeasures.get(Curves.NUM_CURVES));
            double wholeVolume = Double.valueOf(wholeMeasures.get(Curves.VOLUME));
            double wholeDensityMax = Double.valueOf(wholeMeasures.get(Curves.DENSITY_MAX));
            double wholeDensityMean = Double.valueOf(wholeMeasures.get(Curves.DENSITY_MEAN));
            double wholeDensitySum = Double.valueOf(wholeMeasures.get(Curves.DENSITY_SUM));
            double wholeLengthMax = Double.valueOf(wholeMeasures.get(Curves.LENGTH_MAX));
            double wholeLengthMean = Double.valueOf(wholeMeasures.get(Curves.LENGTH_MEAN));
            double wholeLengthSum = Double.valueOf(wholeMeasures.get(Curves.LENGTH_SUM));
            double wholeAttrMax = 0;
            double wholeAttrMean = 0;
            double wholeAttrSum = 0;

            String attr = this.curves.has("FA") ? "FA" : this.curves.has("frac") ? "frac" : null;

            if (attr != null)
            {
                wholeAttrMax = Double.valueOf(wholeMeasures.get(attr + "_max"));
                wholeAttrMean = Double.valueOf(wholeMeasures.get(attr + "_mean"));
                wholeAttrSum = Double.valueOf(wholeMeasures.get(attr + "_sum"));
            }

            Map<Integer, Double> regionVolumes = MaskUtils.volumes(this.regions);

            Record lut = TableUtils.createLookup(this.lookup, this.index, this.name);
            List<String> idxes = Lists.newArrayList(lut.keySet());
            List<String> names = Lists.newArrayList();
            for (String key : idxes)
            {
                names.add(lut.get(key));
            }
            PathUtils.write(names, PathUtils.join(tmpDir, "nodes.txt"));

            Map<String, Record> maps = Maps.newLinkedHashMap();
            Map<String, Matrix> matrices = Maps.newLinkedHashMap();

            Logging.info("computing connections");
            List<Connection> connections = connections(this.curves, this.regions, this.window);

            List<String> curvesNames = Lists.newArrayList();
            for (int i = 0; i < idxes.size(); i++)
            {
                String iIndex = idxes.get(i);
                String iName = lut.get(iIndex);
                double iVolume = regionVolumes.get(Integer.valueOf(iIndex));

                for (int j = i; j < idxes.size(); j++)
                {
                    String jIndex = idxes.get(j);
                    String jName = lut.get(jIndex);
                    double jVolume = regionVolumes.get(Integer.valueOf(jIndex));

                    double ijVolume = (iVolume + jVolume) / 2.0;

                    String ijName = iName + "_" + jName;

                    Logging.info("extracting net: " + ijName);
                    Curves net = net(curves, connections, Integer.valueOf(iIndex), Integer.valueOf(jIndex));

                    if (net.size() > this.count && MathUtils.nonzero(ijVolume))
                    {
                        Logging.info("computing measures: " + ijName);
                        Record measures = measure(net);

                        double cxnNumber = Double.valueOf(measures.get(Curves.NUM_CURVES));
                        double cxnVolume = Double.valueOf(measures.get(Curves.VOLUME));
                        double cxnDensityMax = Double.valueOf(measures.get(Curves.DENSITY_MAX));
                        double cxnDensityMean = Double.valueOf(measures.get(Curves.DENSITY_MEAN));
                        double cxnDensitySum = Double.valueOf(measures.get(Curves.DENSITY_SUM));
                        double cxnLengthMax = Double.valueOf(measures.get(Curves.LENGTH_MAX));
                        double cxnLengthMean = Double.valueOf(measures.get(Curves.LENGTH_MEAN));
                        double cxnLengthSum = Double.valueOf(measures.get(Curves.LENGTH_SUM));

                        Record cxnMeasures = new Record();

                        cxnMeasures.with("num_curves", cxnNumber);
                        cxnMeasures.with("volume", cxnVolume);
                        cxnMeasures.with("density_sum", cxnDensitySum);
                        cxnMeasures.with("density_mean", cxnDensityMean);
                        cxnMeasures.with("density_max", cxnDensityMax);
                        cxnMeasures.with("length_sum", cxnLengthSum);
                        cxnMeasures.with("length_mean", cxnLengthMean);
                        cxnMeasures.with("length_max", cxnLengthMax);

                        if (this.relative)
                        {
                            cxnMeasures.with("relative_num_curves", cxnNumber / wholeNumber);
                            cxnMeasures.with("relative_volume", cxnVolume / wholeVolume);
                            cxnMeasures.with("relative_density_sum", cxnDensitySum / wholeDensitySum);
                            cxnMeasures.with("relative_density_mean", cxnDensityMean / wholeDensityMean);
                            cxnMeasures.with("relative_density_max", cxnDensityMax / wholeDensityMax);
                            cxnMeasures.with("relative_length_sum", cxnLengthSum / wholeLengthSum);
                            cxnMeasures.with("relative_length_mean", cxnLengthMean / wholeLengthMean);
                            cxnMeasures.with("relative_length_max", cxnLengthMax / wholeLengthMax);
                        }

                        if (this.corrected)
                        {
                            cxnMeasures.with("corrected_num_curves", cxnNumber / ijVolume);
                            cxnMeasures.with("corrected_volume", cxnVolume / ijVolume);
                            cxnMeasures.with("corrected_density_sum", cxnDensitySum / ijVolume);
                            cxnMeasures.with("corrected_density_mean", cxnDensityMean / ijVolume);
                            cxnMeasures.with("corrected_density_max", cxnDensityMax / ijVolume);
                            cxnMeasures.with("corrected_length_sum", cxnLengthSum / ijVolume);
                            cxnMeasures.with("corrected_length_mean", cxnLengthMean / ijVolume);
                            cxnMeasures.with("corrected_length_max", cxnLengthMax / ijVolume);
                        }

                        if (attr != null && measures.containsKey(attr + "_mean"))
                        {
                            double cxnAttrMax = Double.valueOf(measures.get(attr + "_max"));
                            double cxnAttrMean = Double.valueOf(measures.get(attr + "_mean"));
                            double cxnAttrSum = Double.valueOf(measures.get(attr + "_sum"));

                            for (String cxnName : cxnMeasures.keys())
                            {
                                double cxnValue = Double.valueOf(cxnMeasures.get(cxnName));
                                cxnMeasures.with(cxnName + "_attr_sum", cxnAttrSum * cxnValue);
                                cxnMeasures.with(cxnName + "_attr_mean", cxnAttrMean * cxnValue);
                                cxnMeasures.with(cxnName + "_attr_max", cxnAttrMax * cxnValue);
                            }

                            cxnMeasures.with("attr_sum", cxnAttrSum);
                            cxnMeasures.with("attr_mean", cxnAttrMean);
                            cxnMeasures.with("attr_max", cxnAttrMax);

                            if (this.relative)
                            {
                                cxnMeasures.with("relative_attr_sum", cxnAttrSum / wholeAttrSum);
                                cxnMeasures.with("relative_attr_mean", cxnAttrMean / wholeAttrMean);
                                cxnMeasures.with("relative_attr_max", cxnAttrMax / wholeAttrMax);
                            }

                            if (this.corrected)
                            {
                                cxnMeasures.with("corrected_attr_sum", cxnAttrSum / ijVolume);
                                cxnMeasures.with("corrected_attr_mean", cxnAttrMean / ijVolume);
                                cxnMeasures.with("corrected_attr_max", cxnAttrMax / ijVolume);
                            }
                        }

                        for (String key : cxnMeasures.keySet())
                        {
                            double value = Double.valueOf(cxnMeasures.get(key));

                            if (i != j)
                            {
                                if (!matrices.containsKey(key))
                                {
                                    int num = names.size();
                                    matrices.put(key, new Matrix(num, num));
                                }

                                Matrix matrix = matrices.get(key);
                                matrix.set(i, j, value);
                                matrix.set(j, i, value);
                            }

                            if (!maps.containsKey(key))
                            {
                                maps.put(key, new Record());
                            }

                            maps.get(key).with(ijName, value);
                        }

                        curvesNames.add(ijName);

                        if (this.save)
                        {
                            Logging.info("writing curves: " + ijName);
                            net.write(PathUtils.join(curvesDir, ijName + ".vtk.gz"));
                        }
                    }
                    else
                    {
                        Logging.info("skipping connection: " + ijName);
                    }
                }
            }

            Logging.info("writing curves list");
            PathUtils.write(curvesNames, curvesDir + ".txt");

            List<String> matricesNames = Lists.newArrayList();
            for (String rawName : matrices.keySet())
            {
                Matrix rawMatrix = matrices.get(rawName);

                double sum = MatrixUtils.sum(rawMatrix);
                double max = MatrixUtils.max(rawMatrix);

                double normSum = MathUtils.zero(sum) ? 1.0 : 1.0 / sum;
                double normMax = MathUtils.zero(max) ? 1.0 : 1.0 / max;

                Matrix normSumMatrix = rawMatrix.times(normSum);
                Matrix normMaxMatrix = rawMatrix.times(normMax);
                String normSumName = "normsum_" + rawName;
                String normMaxName = "normmax_" + rawName;

                Logging.info("writing matrices based on " + rawName);
                rawMatrix.write(PathUtils.join(matricesDir, rawName + ".txt"));
                normSumMatrix.write(PathUtils.join(matricesDir, normSumName + ".txt"));
                normMaxMatrix.write(PathUtils.join(matricesDir, normMaxName + ".txt"));

                matricesNames.add(rawName);
                matricesNames.add(normSumName);
                matricesNames.add(normMaxName);
            }

            Logging.info("writing matrices list");
            PathUtils.write(matricesNames, matricesDir + ".txt");

            for (String mapName : maps.keySet())
            {
                Record map = maps.get(mapName);

                Logging.info("writing map: " + mapName);
                map.write(PathUtils.join(mapsDir, mapName + ".csv"));
            }

            PathUtils.move(tmpDir, this.output, this.backup);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Logging.error("curves connection failed: " + e.getMessage());
        }

        return this;
    }

    private static List<Connection> connections(Curves curves, Mask rois, int window)
    {
        List<Connection> out = Lists.newArrayList();
        Sampling samp = rois.getSampling();

        // Precompute the offsets for the search window
        int size = 2 * window + 1;
        int volume = size * size * size;
        int[][] offsets = new int[volume][3];
        for (int idx = 0; idx < volume; idx++)
        {
            int i = idx % size;
            int tmp = (idx - i) / size;
            int j = tmp % size;
            int k = (tmp - j) / size;

            offsets[idx][0] = i - window;
            offsets[idx][1] = j - window;
            offsets[idx][2] = k - window;
        }

        Set<Integer> startRois = new HashSet<>();
        Set<Integer> endRois = new HashSet<>();

        for (int curveIdx = 0; curveIdx < curves.size(); curveIdx++)
        {
            Curve curve = curves.get(curveIdx);

            Sample headSample = samp.nearest(curve.getHead());
            Sample tailSample = samp.nearest(curve.getTail());

            int startI = headSample.getI();
            int startJ = headSample.getJ();
            int startK = headSample.getK();

            int endI = tailSample.getI();
            int endJ = tailSample.getJ();
            int endK = tailSample.getK();

            startRois.clear();
            for (int[] offset : offsets)
            {
                int startSearchI = startI + offset[0];
                int startSearchJ = startJ + offset[1];
                int startSearchK = startK + offset[2];
                Sample startSearchSample = new Sample(startSearchI, startSearchJ, startSearchK);

                if (samp.contains(startSearchSample))
                {
                    startRois.add(rois.get(startSearchSample));
                }
            }

            endRois.clear();
            for (int[] offset : offsets)
            {
                int endSearchI = endI + offset[0];
                int endSearchJ = endJ + offset[1];
                int endSearchK = endK + offset[2];
                Sample endSearchSample = new Sample(endSearchI, endSearchJ, endSearchK);

                if (samp.contains(endSearchSample))
                {
                    endRois.add(rois.get(endSearchSample));
                }
            }

            for (Integer startRoi : startRois)
            {
                for (Integer endRoi : endRois)
                {
                    int minRoi = Math.min(startRoi, endRoi);
                    int maxRoi = Math.max(startRoi, endRoi);
                    out.add(new Connection(curveIdx, minRoi, maxRoi));
                }
            }
        }

        return out;
    }

    private static Curves net(Curves curves, List<Connection> seg, int a, int b)
    {
        Curves ncurves = new Curves();

        for (String name : curves.names())
        {
            ncurves.add(name, curves.proto(name));
        }

        for (Connection s : seg)
        {
            if ((s.startIdx == a && s.endIdx == b) || (s.startIdx == b && s.endIdx == a))
            {
                ncurves.add(curves.get(s.curveIdx));
            }
        }

        return ncurves;
    }

    private static class Connection
    {
        public Connection(int cidx, int sidx, int eidx)
        {
            this.curveIdx = cidx;
            this.startIdx = sidx;
            this.endIdx = eidx;
        }

        public int curveIdx;
        public int startIdx;
        public int endIdx;
    }

    public static List<Connection> create(Table table)
    {
        Global.assume(table.hasField(Curves.CURVE_IDX), "table does not contain segmentation fields");
        Global.assume(table.hasField(Curves.START_IDX), "table does not contain segmentation fields");
        Global.assume(table.hasField(Curves.END_IDX), "table does not contain segmentation fields");

        int ctidx = table.getSchema().getIndex(Curves.CURVE_IDX);
        int stidx = table.getSchema().getIndex(Curves.START_IDX);
        int etidx = table.getSchema().getIndex(Curves.END_IDX);

        List<Connection> seg = Lists.newArrayList();

        for (Object[] row : table)
        {
            int curveIdx = Integer.valueOf(row[ctidx].toString());
            int startIdx = Integer.valueOf(row[stidx].toString());
            int endIdx = Integer.valueOf(row[etidx].toString());
            seg.add(new Connection(curveIdx, startIdx, endIdx));
        }

        return seg;
    }

    public static Table create(List<Connection> seg)
    {
        Schema schema = new Schema();
        schema.add(Curves.CURVE_IDX);
        schema.add(Curves.START_IDX);
        schema.add(Curves.END_IDX);

        Table out = new Table(schema);

        int idx = 0;
        Record map = new Record();
        for (Connection s : seg)
        {
            map.with(Curves.CURVE_IDX, String.valueOf(s.curveIdx));
            map.with(Curves.START_IDX, String.valueOf(s.startIdx));
            map.with(Curves.END_IDX, String.valueOf(s.endIdx));

            out.addRecord(idx++, map);
            map.clear();
        }
        return out;
    }

    private Record measure(Curves curves)
    {
        Set<String> select = Sets.newHashSet();

        select.add(Curves.NUM_CURVES);
        select.add(Curves.VOLUME);
        select.add(Curves.DENSITY_MAX);
        select.add(Curves.DENSITY_MEAN);
        select.add(Curves.DENSITY_SUM);
        select.add(Curves.LENGTH_MAX);
        select.add(Curves.LENGTH_MEAN);
        select.add(Curves.LENGTH_SUM);

        if (curves.has("FA"))
        {
            select.add("FA_max");
            select.add("FA_mean");
            select.add("FA_sum");
        }

        if (curves.has("frac"))
        {
            select.add("frac_max");
            select.add("frac_mean");
            select.add("frac_sum");
        }

        return CurvesMeasure.measure(curves, this.delta, this.thresh).select(select);
    }
}
