package qit.main;

import com.google.common.collect.Lists;
import qit.base.CliMain;
import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Curves;
import qit.data.datasets.Record;
import qit.data.datasets.Vects;
import qit.data.modules.curves.*;
import qit.data.utils.vects.cluster.VectsClusterDPM;
import qit.data.utils.CurvesUtils;
import qit.data.utils.curves.CurvesClusterSCPTInject;
import qit.math.utils.MathUtils;
import smile.validation.AdjustedRandIndex;

import java.io.IOException;
import java.util.List;

/*
 This study performs an evaluation of various curve clustering algorithms given some manually labeled curves 
 */
public class CurveClusterEvaluation implements CliMain
{
    private static final String SCPT_POINTS = "points";
    private static final String SCPT_THRESH = "thresh";
    private static final String QUICK_POINTS = "points";
    private static final String QUICK_THRESH = "thresh";
    private static final String HIER_THRESH = "thresh";
    private static final String SPECT_THRESH = "thresh";

    public static void main(String[] args)
    {
        new CurveClusterEvaluation().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.console();

            Logging.info("starting analysis");

            Logging.info("parsing arguments");
            Record map = new Record();
            for (String arg : args)
            {
                String[] tokens = arg.split("=");
                Global.assume(tokens.length == 2, "expected two tokens");
                map.with(tokens[0], tokens[1]);
            }

            Global.assume(map.containsKey("type"), "no type found");
            Global.assume(map.containsKey("input"), "no input found");
            Global.assume(map.containsKey("output"), "no output found");

            String input = map.remove("input");
            String output = map.remove("output");

            Logging.info("reading curves from " + input);
            Curves curves = Curves.read(input);

            Logging.info("preprocessing curves");
            curves.shuffle();
            if (map.containsKey("fibers"))
            {
                int fibers = Integer.valueOf(map.get("fibers"));
                boolean[] filter = CurvesUtils.selectByCount(curves, fibers);
                curves.keep(filter);
            }

            Global.assume(curves.has(Curves.LABEL), "no curve labels found");

            int[] truth = CurvesUtils.attrGetLabelsPerCurve(curves, Curves.LABEL);
            curves.remove(Curves.LABEL);

            Logging.info("clustering curves");

            long start = System.currentTimeMillis();
            curves = cluster(curves, map);
            long time = System.currentTimeMillis() - start;
            int fibers = curves.size();

            map.with("time", String.valueOf(time));
            map.with("fibers", String.valueOf(fibers));

            Logging.info("evaluating clustering");
            int[] test = CurvesUtils.attrGetLabelsPerCurve(curves, Curves.LABEL);
            double ari = new AdjustedRandIndex().measure(truth, test);
            map.with("ari", String.valueOf(ari));

            int expected = MathUtils.counts(truth).size();
            int found = MathUtils.counts(test).size();
            map.with("expected", String.valueOf(expected));
            map.with("found", String.valueOf(found));

            Logging.info("writing output to " + output);
            map.write(output);

            Logging.info("finished analysis");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Logging.error("failed to run analysis");
        }
    }

    private static Curves cluster(Curves curves, Record map)
    {
        String type = map.get("type");
        switch (type)
        {
            case "spectral":
                return spectral(curves, map);
            case "scpt":
                return scpt(curves, map);
            case "hierarchical":
                return hierarchical(curves, map);
            case "quickbundle":
                return quickbundle(curves, map);
            default:
                Logging.error("invalid method: " + type);
                return null;
        }
    }

    private static Curves scpt(Curves curves, Record map)
    {
        int points = 50;
        double thresh = 30;

        if (map.containsKey(SCPT_POINTS))
        {
            points = Integer.valueOf(map.get(SCPT_POINTS));
        }
        else
        {
            map.with(SCPT_POINTS, String.valueOf(points));
        }

        if (map.containsKey(SCPT_THRESH))
        {
            thresh = Double.valueOf(map.get(SCPT_THRESH));
        }
        else
        {
            map.with(SCPT_THRESH, String.valueOf(thresh));
        }

        CurvesClosestPointTransform transform = new CurvesClosestPointTransform();
        {
            CurvesLandmarks landmarker = new CurvesLandmarks();
            landmarker.input = curves;
            landmarker.subsamp = 1000;
            landmarker.eps = 1.0;
            landmarker.num = points;

            Vects landmarks = landmarker.getOutput();
            transform.landmarks = landmarks;
        }

        VectsClusterDPM cluster = new VectsClusterDPM();
        {
            cluster.withK(2);
            cluster.withLambda(thresh * thresh * transform.landmarks.size());
            cluster.withRestarts(2);
            cluster.withMaxIter(100);
        }

        CurvesClusterSCPTInject cop = new CurvesClusterSCPTInject();
        cop.withCurves(curves);
        cop.withTransform(transform);
        cop.withCluster(cluster);
        cop.withEpsilon(1.0);
        cop.run();

        return curves;
    }

    private static Curves quickbundle(Curves curves, Record map)
    {
        int samples = 5;
        double thresh = 30;

        if (map.containsKey(QUICK_POINTS))
        {
            samples = Integer.valueOf(map.get(QUICK_POINTS));
        }
        else
        {
            map.with(QUICK_POINTS, String.valueOf(samples));
        }

        if (map.containsKey(QUICK_THRESH))
        {
            thresh = Double.valueOf(map.get(QUICK_THRESH));
        }
        else
        {
            map.with(QUICK_THRESH, String.valueOf(thresh));
        }

        CurvesClusterQuickBundle qbop = new CurvesClusterQuickBundle();
        qbop.input = curves;
        qbop.samples = samples;
        qbop.thresh = thresh;
        return qbop.run().output;
    }

    private static Curves hierarchical(Curves curves, Record map)
    {
        double thresh = 30;

        if (map.containsKey(HIER_THRESH))
        {
            thresh = Double.valueOf(map.get(HIER_THRESH));
        }
        else
        {
            map.with(HIER_THRESH, String.valueOf(thresh));
        }

        CurvesClusterHierarchical op = new CurvesClusterHierarchical();
        op.input = curves;
        op.thresh = thresh;
        op.density = 5.0;
        return op.run().output;
    }

    private static Curves spectral(Curves curves, Record map)
    {
        int num = 20;

        if (map.containsKey(SPECT_THRESH))
        {
            num = Integer.valueOf(map.get(SPECT_THRESH));
        }
        else
        {
            map.with(SPECT_THRESH, String.valueOf(num));
        }

        CurvesClusterSpectral sop = new CurvesClusterSpectral();
        sop.density = 5.0;
        sop.input = curves;
        sop.num = num;
        return sop.run().output;
    }
}