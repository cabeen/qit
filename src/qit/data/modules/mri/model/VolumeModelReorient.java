/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qit.data.modules.mri.model;

import qit.base.ModelType;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.models.Noddi;
import qit.data.models.Tensor;
import qit.data.utils.VectsUtils;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.volume.VolumeFunction;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

@ModuleDescription("Reorient the fiber orientations of a model volume.  If both a flip and swap are specified, the flip is performed first. This only works for tensor, noddi, and fibers volumes")
@ModuleAuthor("Ryan Cabeen")
public class VolumeModelReorient implements Module
{
    @ModuleInput
    @ModuleDescription("input model volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a model name (default will try to detect it)")
    public String model = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("flip a coodinate (x, y, z, or none)")
    public String flip = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("swap a pair of coordinates (xy, xz, yz, or none)")
    public String swap = null;

    @ModuleOutput
    @ModuleDescription("output error volume")
    public Volume output;

    @Override
    public VolumeModelReorient run()
    {
        this.output = new VolumeFunction(factory(this.input.getDim())).withInput(this.input).withMessages(false).run();

        return this;
    }

    public Supplier<VectFunction> factory(int dim)
    {
        return () ->
        {
            ModelType type = ModelUtils.select(VolumeModelReorient.this.input != null ? VolumeModelReorient.this.input.getModel() : null, VolumeModelReorient.this.model);

            switch (type)
            {
                case Tensor:
                    return functionTensor(new Tensor());
                case Fibers:
                    return functionFibers(new Fibers(Fibers.count(dim)));
                case Noddi:
                    return functionNoddi(new Noddi());
                default:
                    throw new RuntimeException("unsupported model type: " + type.toString());
            }
        };
    }

    private VectFunction functionTensor(final Tensor proto)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                proto.setEncoding(input);
                
                if (VolumeModelReorient.this.flip != null)
                {
                    String lowflips = VolumeModelReorient.this.flip.toLowerCase();
                    for (int i = 0; i < lowflips.length(); i++)
                    {
                        String lowflip = lowflips.substring(i, i+1);
                        if (lowflip.equals("x") || lowflip.equals("i"))
                        {
                            proto.vecs = VectsUtils.apply(proto.vecs, VectFunctionSource.scale(-1.0, 1.0, 1.0));
                        }
                        else if (lowflip.equals("y") || lowflip.equals("j"))
                        {
                            proto.vecs = VectsUtils.apply(proto.vecs, VectFunctionSource.scale(1.0, -1.0, 1.0));
                        }
                        else if (lowflip.equals("z") || lowflip.equals("k"))
                        {
                            proto.vecs = VectsUtils.apply(proto.vecs, VectFunctionSource.scale(1.0, 1.0, -1.0));
                        }
                        else
                        {
                           // continue
                        }
                    }
                }

                if (VolumeModelReorient.this.swap != null)
                {
                    String lowswap = VolumeModelReorient.this.swap.toLowerCase();
                    if (lowswap.equals("xy") || lowswap.equals("yx"))
                    {
                        proto.vecs = VectsUtils.apply(proto.vecs, VectFunctionSource.swap(0, 1, 3));
                    }
                    else if (lowswap.equals("yz") || lowswap.equals("zy"))
                    {
                        proto.vecs = VectsUtils.apply(proto.vecs, VectFunctionSource.swap(1, 2, 3));
                    }
                    else if (lowswap.equals("xz") || lowswap.equals("zx"))
                    {
                        proto.vecs = VectsUtils.apply(proto.vecs, VectFunctionSource.swap(0, 2, 3));
                    }
                    else
                    {
                        // continue
                    }
                }

                output.set(proto.getEncoding());
            }
        }.init(proto.getEncodingSize(), proto.getEncodingSize());
    }

    private VectFunction functionNoddi(final Noddi proto)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                proto.setEncoding(input);

                if (VolumeModelReorient.this.flip != null)
                {
                    String lowflips = VolumeModelReorient.this.flip.toLowerCase();
                    for (int i = 0; i < lowflips.length(); i++)
                    {
                        String lowflip = lowflips.substring(i, i+1);
                        if (lowflip.equals("x") || lowflip.equals("i"))
                        {
                            proto.dir = VectFunctionSource.scale(-1.0, 1.0, 1.0).apply(proto.dir);
                        }
                        else if (lowflip.equals("y") || lowflip.equals("j"))
                        {
                            proto.dir = VectFunctionSource.scale(1.0, -1.0, 1.0).apply(proto.dir);
                        }
                        else if (lowflip.equals("z") || lowflip.equals("k"))
                        {
                            proto.dir = VectFunctionSource.scale(1.0, 1.0, -1.0).apply(proto.dir);
                        }
                        else
                        {
                            // continue
                        }
                    }
                }

                if (VolumeModelReorient.this.swap != null)
                {
                    String lowswap = VolumeModelReorient.this.swap.toLowerCase();
                    if (lowswap.equals("xy") || lowswap.equals("yx"))
                    {
                        proto.dir = VectFunctionSource.swap(0, 1, 3).apply(proto.dir);
                    }
                    else if (lowswap.equals("yz") || lowswap.equals("zy"))
                    {
                        proto.dir = VectFunctionSource.swap(1, 2, 3).apply(proto.dir);
                    }
                    else if (lowswap.equals("xz") || lowswap.equals("zx"))
                    {
                        proto.dir = VectFunctionSource.swap(0, 2, 3).apply(proto.dir);
                    }
                    else
                    {
                        // continue
                    }
                }

                output.set(proto.getEncoding());
            }
        }.init(proto.getEncodingSize(), proto.getEncodingSize());
    }

    private VectFunction functionFibers(final Fibers proto)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                proto.setEncoding(input);

                if (VolumeModelReorient.this.flip != null)
                {
                    String lowflips = VolumeModelReorient.this.flip.toLowerCase();
                    for (int i = 0; i < lowflips.length(); i++)
                    {
                        String lowflip = lowflips.substring(i, i+1);
                        if (lowflip.equals("x") || lowflip.equals("i"))
                        {
                            proto.lines = VectsUtils.apply(proto.lines, VectFunctionSource.scale(-1.0, 1.0, 1.0));
                        }
                        else if (lowflip.equals("y") || lowflip.equals("j"))
                        {
                            proto.lines = VectsUtils.apply(proto.lines, VectFunctionSource.scale(1.0, -1.0, 1.0));
                        }
                        else if (lowflip.equals("z") || lowflip.equals("k"))
                        {
                            proto.lines = VectsUtils.apply(proto.lines, VectFunctionSource.scale(1.0, 1.0, -1.0));
                        }
                        else
                        {
                            // continue
                        }
                    }
                }

                if (VolumeModelReorient.this.swap != null)
                {
                    String lowswap = VolumeModelReorient.this.swap.toLowerCase();
                    if (lowswap.equals("xy") || lowswap.equals("yx"))
                    {
                        proto.lines = VectsUtils.apply(proto.lines, VectFunctionSource.swap(0, 1, 3));
                    }
                    else if (lowswap.equals("yz") || lowswap.equals("zy"))
                    {
                        proto.lines = VectsUtils.apply(proto.lines, VectFunctionSource.swap(1, 2, 3));
                    }
                    else if (lowswap.equals("xz") || lowswap.equals("zx"))
                    {
                        proto.lines = VectsUtils.apply(proto.lines, VectFunctionSource.swap(0, 2, 3));
                    }
                    else
                    {
                        // continue
                    }
                }

                output.set(proto.getEncoding());
            }
        }.init(proto.getEncodingSize(), proto.getEncodingSize());
    }
}
