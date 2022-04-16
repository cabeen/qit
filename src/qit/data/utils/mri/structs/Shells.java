package qit.data.utils.mri.structs;

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.base.structs.Pair;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;

import java.util.List;

public class Shells
{
    private Gradients gradients;
    private Vect shells;
    private List<Pair<Integer, List<Integer>>> shellMap = Lists.newArrayList();

    public Shells(Gradients gradients)
    {
        this.gradients = gradients;
        this.shellMap = Lists.newArrayList();
        for (int shell : gradients.getShells(true))
        {
            this.shellMap.add(Pair.of(shell, gradients.getShellsIdx(shell)));
        }

        this.shells = VectSource.createND(this.shellMap.size());
        for (int i = 0; i < this.shellMap.size(); i++)
        {
            this.shells.set(i, this.shellMap.get(i).a);
        }

        Logging.info("shells: " + shells.toString());
    }

    public Vect mean(Vect signal)
    {
        Vect means = VectSource.createND(this.shells.size());
        for (int j = 0; j < this.shellMap.size(); j++)
        {
            means.set(j, signal.sub(this.shellMap.get(j).b).mean());
        }

        return means;
    }

    public Vect expand(Vect shellvals)
    {
        Vect out = new Vect(this.gradients.size());

        for (int j = 0; j < this.shellMap.size(); j++)
        {
            for (int i : this.shellMap.get(j).b)
            {
                out.set(i, shellvals.get(j));
            }
        }

        return out;
    }

    public Vect shells()
    {
        return this.shells.copy();
    }
}

