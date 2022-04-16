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

package qit.data.utils.volume;

import qit.data.datasets.Matrix;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.math.structs.VectFunction;

public class VolumeInterpTricubic extends VectFunction
{
    public static final String NAME = "Tricubic";

    private static final int DIM = 64;

    private Volume volume;

    private boolean cache = false;
    private Vect[] coefs;

    public VolumeInterpTricubic(Volume v)
    {
        super(3, v.getDim());
        this.volume = v;
    }

    public VolumeInterpTricubic withCache(boolean v)
    {
        this.cache = v;
        return this;
    }

    private static final Matrix C = new Matrix(new double[][]{
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {-3, 3, 0, 0, 0, 0, 0, 0, -2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {2, -2, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {-3, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, -3, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {9, -9, -9, 9, 0, 0, 0, 0, 6, 3, -6, -3, 0, 0, 0, 0, 6, -6, 3, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {-6, 6, 6, -6, 0, 0, 0, 0, -3, -3, 3, 3, 0, 0, 0, 0, -4, 4, -2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, -2, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {2, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 2, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {-6, 6, 6, -6, 0, 0, 0, 0, -4, -2, 4, 2, 0, 0, 0, 0, -3, 3, -3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, -1, -2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {4, -4, -4, 4, 0, 0, 0, 0, 2, 2, -2, -2, 0, 0, 0, 0, 2, -2, 2, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 3, 0, 0, 0, 0, 0, 0, -2, -1, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0, -1, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, -9, -9, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 3, -6, -3, 0, 0, 0, 0, 6, -6, 3, -3, 0, 0, 0, 0, 4, 2, 2, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -6, 6, 6, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, -3, 3, 3, 0, 0, 0, 0, -4, 4, -2, 2, 0, 0, 0, 0, -2, -2, -1, -1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -6, 6, 6, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -4, -2, 4, 2, 0, 0, 0, 0, -3, 3, -3, 3, 0, 0, 0, 0, -2, -1, -2, -1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, -4, -4, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, -2, -2, 0, 0, 0, 0, 2, -2, 2, -2, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0},
            {-3, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, -3, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {9, -9, 0, 0, -9, 9, 0, 0, 6, 3, 0, 0, -6, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, -6, 0, 0, 3, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 2, 0, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {-6, 6, 0, 0, 6, -6, 0, 0, -3, -3, 0, 0, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -4, 4, 0, 0, -2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, -2, 0, 0, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0, 0, 0, -1, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, -9, 0, 0, -9, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 3, 0, 0, -6, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, -6, 0, 0, 3, -3, 0, 0, 4, 2, 0, 0, 2, 1, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -6, 6, 0, 0, 6, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, -3, 0, 0, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -4, 4, 0, 0, -2, 2, 0, 0, -2, -2, 0, 0, -1, -1, 0, 0},
            {9, 0, -9, 0, -9, 0, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 3, 0, -6, 0, -3, 0, 6, 0, -6, 0, 3, 0, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 2, 0, 2, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 9, 0, -9, 0, -9, 0, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 3, 0, -6, 0, -3, 0, 6, 0, -6, 0, 3, 0, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 2, 0, 2, 0, 1, 0},
            {-27, 27, 27, -27, 27, -27, -27, 27, -18, -9, 18, 9, 18, 9, -18, -9, -18, 18, -9, 9, 18, -18, 9, -9, -18, 18, 18, -18, -9, 9, 9, -9, -12, -6, -6, -3, 12, 6, 6, 3, -12, -6, 12, 6, -6, -3, 6, 3, -12, 12, -6, 6, -6, 6, -3, 3, -8, -4, -4, -2, -4, -2, -2, -1},
            {18, -18, -18, 18, -18, 18, 18, -18, 9, 9, -9, -9, -9, -9, 9, 9, 12, -12, 6, -6, -12, 12, -6, 6, 12, -12, -12, 12, 6, -6, -6, 6, 6, 6, 3, 3, -6, -6, -3, -3, 6, 6, -6, -6, 3, 3, -3, -3, 8, -8, 4, -4, 4, -4, 2, -2, 4, 4, 2, 2, 2, 2, 1, 1},
            {-6, 0, 6, 0, 6, 0, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 0, -3, 0, 3, 0, 3, 0, -4, 0, 4, 0, -2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0, -2, 0, -1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, -6, 0, 6, 0, 6, 0, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 0, -3, 0, 3, 0, 3, 0, -4, 0, 4, 0, -2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0, -2, 0, -1, 0, -1, 0},
            {18, -18, -18, 18, -18, 18, 18, -18, 12, 6, -12, -6, -12, -6, 12, 6, 9, -9, 9, -9, -9, 9, -9, 9, 12, -12, -12, 12, 6, -6, -6, 6, 6, 3, 6, 3, -6, -3, -6, -3, 8, 4, -8, -4, 4, 2, -4, -2, 6, -6, 6, -6, 3, -3, 3, -3, 4, 2, 4, 2, 2, 1, 2, 1},
            {-12, 12, 12, -12, 12, -12, -12, 12, -6, -6, 6, 6, 6, 6, -6, -6, -6, 6, -6, 6, 6, -6, 6, -6, -8, 8, 8, -8, -4, 4, 4, -4, -3, -3, -3, -3, 3, 3, 3, 3, -4, -4, 4, 4, -2, -2, 2, 2, -4, 4, -4, 4, -2, 2, -2, 2, -2, -2, -2, -2, -1, -1, -1, -1},
            {2, 0, 0, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {-6, 6, 0, 0, 6, -6, 0, 0, -4, -2, 0, 0, 4, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 3, 0, 0, -3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, -1, 0, 0, -2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {4, -4, 0, 0, -4, 4, 0, 0, 2, 2, 0, 0, -2, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 0, 0, 2, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -6, 6, 0, 0, 6, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -4, -2, 0, 0, 4, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 3, 0, 0, -3, 3, 0, 0, -2, -1, 0, 0, -2, -1, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, -4, 0, 0, -4, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, -2, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 0, 0, 2, -2, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0},
            {-6, 0, 6, 0, 6, 0, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, -4, 0, -2, 0, 4, 0, 2, 0, -3, 0, 3, 0, -3, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0, -1, 0, -2, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, -6, 0, 6, 0, 6, 0, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -4, 0, -2, 0, 4, 0, 2, 0, -3, 0, 3, 0, -3, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0, -1, 0, -2, 0, -1, 0},
            {18, -18, -18, 18, -18, 18, 18, -18, 12, 6, -12, -6, -12, -6, 12, 6, 12, -12, 6, -6, -12, 12, -6, 6, 9, -9, -9, 9, 9, -9, -9, 9, 8, 4, 4, 2, -8, -4, -4, -2, 6, 3, -6, -3, 6, 3, -6, -3, 6, -6, 3, -3, 6, -6, 3, -3, 4, 2, 2, 1, 4, 2, 2, 1},
            {-12, 12, 12, -12, 12, -12, -12, 12, -6, -6, 6, 6, 6, 6, -6, -6, -8, 8, -4, 4, 8, -8, 4, -4, -6, 6, 6, -6, -6, 6, 6, -6, -4, -4, -2, -2, 4, 4, 2, 2, -3, -3, 3, 3, -3, -3, 3, 3, -4, 4, -2, 2, -4, 4, -2, 2, -2, -2, -1, -1, -2, -2, -1, -1},
            {4, 0, -4, 0, -4, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, -2, 0, -2, 0, 2, 0, -2, 0, 2, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 4, 0, -4, 0, -4, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, -2, 0, -2, 0, 2, 0, -2, 0, 2, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0},
            {-12, 12, 12, -12, 12, -12, -12, 12, -8, -4, 8, 4, 8, 4, -8, -4, -6, 6, -6, 6, 6, -6, 6, -6, -6, 6, 6, -6, -6, 6, 6, -6, -4, -2, -4, -2, 4, 2, 4, 2, -4, -2, 4, 2, -4, -2, 4, 2, -3, 3, -3, 3, -3, 3, -3, 3, -2, -1, -2, -1, -2, -1, -2, -1},
            {8, -8, -8, 8, -8, 8, 8, -8, 4, 4, -4, -4, -4, -4, 4, 4, 4, -4, 4, -4, -4, 4, -4, 4, 4, -4, -4, 4, 4, -4, -4, 4, 2, 2, 2, 2, -2, -2, -2, -2, 2, 2, -2, -2, 2, 2, -2, -2, 2, -2, 2, -2, 2, -2, 2, -2, 1, 1, 1, 1, 1, 1, 1, 1}
    });

    private double get(int i, int j, int k, int d)
    {
        return this.volume.getSampling().contains(i, j, k) ? this.volume.get(i, j, k, d) : 0;
    }

    private Vect coefs(int si, int sj, int sk, int d)
    {
        // Extract the local vocal values and calculate partial derivatives.
        Vect x = VectSource.create(new double[]{
                // values of f(x,y,z) at each corner.
                this.get(si, sj, sk, d), this.get(si + 1, sj, sk, d), this.get(si, sj + 1, sk, d),
                this.get(si + 1, sj + 1, sk, d), this.get(si, sj, sk + 1, d), this.get(si + 1, sj, sk + 1, d),
                this.get(si, sj + 1, sk + 1, d), this.get(si + 1, sj + 1, sk + 1, d),
                // partial df/dx
                0.5 * (this.get(si + 1, sj, sk, d) - this.get(si - 1, sj, sk, d)),
                0.5 * (this.get(si + 2, sj, sk, d) - this.get(si, sj, sk, d)),
                0.5 * (this.get(si + 1, sj + 1, sk, d) - this.get(si - 1, sj + 1, sk, d)),
                0.5 * (this.get(si + 2, sj + 1, sk, d) - this.get(si, sj + 1, sk, d)),
                0.5 * (this.get(si + 1, sj, sk + 1, d) - this.get(si - 1, sj, sk + 1, d)),
                0.5 * (this.get(si + 2, sj, sk + 1, d) - this.get(si, sj, sk + 1, d)),
                0.5 * (this.get(si + 1, sj + 1, sk + 1, d) - this.get(si - 1, sj + 1, sk + 1, d)),
                0.5 * (this.get(si + 2, sj + 1, sk + 1, d) - this.get(si, sj + 1, sk + 1, d)),
                // partial df/dy
                0.5 * (this.get(si, sj + 1, sk, d) - this.get(si, sj - 1, sk, d)),
                0.5 * (this.get(si + 1, sj + 1, sk, d) - this.get(si + 1, sj - 1, sk, d)),
                0.5 * (this.get(si, sj + 2, sk, d) - this.get(si, sj, sk, d)),
                0.5 * (this.get(si + 1, sj + 2, sk, d) - this.get(si + 1, sj, sk, d)),
                0.5 * (this.get(si, sj + 1, sk + 1, d) - this.get(si, sj - 1, sk + 1, d)),
                0.5 * (this.get(si + 1, sj + 1, sk + 1, d) - this.get(si + 1, sj - 1, sk + 1, d)),
                0.5 * (this.get(si, sj + 2, sk + 1, d) - this.get(si, sj, sk + 1, d)),
                0.5 * (this.get(si + 1, sj + 2, sk + 1, d) - this.get(si + 1, sj, sk + 1, d)),
                // partial df/dz
                0.5 * (this.get(si, sj, sk + 1, d) - this.get(si, sj, sk - 1, d)),
                0.5 * (this.get(si + 1, sj, sk + 1, d) - this.get(si + 1, sj, sk - 1, d)),
                0.5 * (this.get(si, sj + 1, sk + 1, d) - this.get(si, sj + 1, sk - 1, d)),
                0.5 * (this.get(si + 1, sj + 1, sk + 1, d) - this.get(si + 1, sj + 1, sk - 1, d)),
                0.5 * (this.get(si, sj, sk + 2, d) - this.get(si, sj, sk, d)),
                0.5 * (this.get(si + 1, sj, sk + 2, d) - this.get(si + 1, sj, sk, d)),
                0.5 * (this.get(si, sj + 1, sk + 2, d) - this.get(si, sj + 1, sk, d)),
                0.5 * (this.get(si + 1, sj + 1, sk + 2, d) - this.get(si + 1, sj + 1, sk, d)),
                // partial d2f/dxdy
                0.25 * (this.get(si + 1, sj + 1, sk, d) - this.get(si - 1, sj + 1, sk, d) - this.get(si + 1, sj - 1, sk, d) + this.get(si - 1, sj - 1, sk, d)),
                0.25 * (this.get(si + 2, sj + 1, sk, d) - this.get(si, sj + 1, sk, d) - this.get(si + 2, sj - 1, sk, d) + this.get(si, sj - 1, sk, d)),
                0.25 * (this.get(si + 1, sj + 2, sk, d) - this.get(si - 1, sj + 2, sk, d) - this.get(si + 1, sj, sk, d) + this.get(si - 1, sj, sk, d)),
                0.25 * (this.get(si + 2, sj + 2, sk, d) - this.get(si, sj + 2, sk, d) - this.get(si + 2, sj, sk, d) + this.get(si, sj, sk, d)),
                0.25 * (this.get(si + 1, sj + 1, sk + 1, d) - this.get(si - 1, sj + 1, sk + 1, d) - this.get(si + 1, sj - 1, sk + 1, d) + this.get(si - 1, sj - 1, sk + 1, d)),
                0.25 * (this.get(si + 2, sj + 1, sk + 1, d) - this.get(si, sj + 1, sk + 1, d) - this.get(si + 2, sj - 1, sk + 1, d) + this.get(si, sj - 1, sk + 1, d)),
                0.25 * (this.get(si + 1, sj + 2, sk + 1, d) - this.get(si - 1, sj + 2, sk + 1, d) - this.get(si + 1, sj, sk + 1, d) + this.get(si - 1, sj, sk + 1, d)),
                0.25 * (this.get(si + 2, sj + 2, sk + 1, d) - this.get(si, sj + 2, sk + 1, d) - this.get(si + 2, sj, sk + 1, d) + this.get(si, sj, sk + 1, d)),
                // partial d2f/dxdz
                0.25 * (this.get(si + 1, sj, sk + 1, d) - this.get(si - 1, sj, sk + 1, d) - this.get(si + 1, sj, sk - 1, d) + this.get(si - 1, sj, sk - 1, d)),
                0.25 * (this.get(si + 2, sj, sk + 1, d) - this.get(si, sj, sk + 1, d) - this.get(si + 2, sj, sk - 1, d) + this.get(si, sj, sk - 1, d)),
                0.25 * (this.get(si + 1, sj + 1, sk + 1, d) - this.get(si - 1, sj + 1, sk + 1, d) - this.get(si + 1, sj + 1, sk - 1, d) + this.get(si - 1, sj + 1, sk - 1, d)),
                0.25 * (this.get(si + 2, sj + 1, sk + 1, d) - this.get(si, sj + 1, sk + 1, d) - this.get(si + 2, sj + 1, sk - 1, d) + this.get(si, sj + 1, sk - 1, d)),
                0.25 * (this.get(si + 1, sj, sk + 2, d) - this.get(si - 1, sj, sk + 2, d) - this.get(si + 1, sj, sk, d) + this.get(si - 1, sj, sk, d)),
                0.25 * (this.get(si + 2, sj, sk + 2, d) - this.get(si, sj, sk + 2, d) - this.get(si + 2, sj, sk, d) + this.get(si, sj, sk, d)),
                0.25 * (this.get(si + 1, sj + 1, sk + 2, d) - this.get(si - 1, sj + 1, sk + 2, d) - this.get(si + 1, sj + 1, sk, d) + this.get(si - 1, sj + 1, sk, d)),
                0.25 * (this.get(si + 2, sj + 1, sk + 2, d) - this.get(si, sj + 1, sk + 2, d) - this.get(si + 2, sj + 1, sk, d) + this.get(si, sj + 1, sk, d)),
                // partial d2f/dydz
                0.25 * (this.get(si, sj + 1, sk + 1, d) - this.get(si, sj - 1, sk + 1, d) - this.get(si, sj + 1, sk - 1, d) + this.get(si, sj - 1, sk - 1, d)),
                0.25 * (this.get(si + 1, sj + 1, sk + 1, d) - this.get(si + 1, sj - 1, sk + 1, d) - this.get(si + 1, sj + 1, sk - 1, d) + this.get(si + 1, sj - 1, sk - 1, d)),
                0.25 * (this.get(si, sj + 2, sk + 1, d) - this.get(si, sj, sk + 1, d) - this.get(si, sj + 2, sk - 1, d) + this.get(si, sj, sk - 1, d)),
                0.25 * (this.get(si + 1, sj + 2, sk + 1, d) - this.get(si + 1, sj, sk + 1, d) - this.get(si + 1, sj + 2, sk - 1, d) + this.get(si + 1, sj, sk - 1, d)),
                0.25 * (this.get(si, sj + 1, sk + 2, d) - this.get(si, sj - 1, sk + 2, d) - this.get(si, sj + 1, sk, d) + this.get(si, sj - 1, sk, d)),
                0.25 * (this.get(si + 1, sj + 1, sk + 2, d) - this.get(si + 1, sj - 1, sk + 2, d) - this.get(si + 1, sj + 1, sk, d) + this.get(si + 1, sj - 1, sk, d)),
                0.25 * (this.get(si, sj + 2, sk + 2, d) - this.get(si, sj, sk + 2, d) - this.get(si, sj + 2, sk, d) + this.get(si, sj, sk, d)),
                0.25 * (this.get(si + 1, sj + 2, sk + 2, d) - this.get(si + 1, sj, sk + 2, d) - this.get(si + 1, sj + 2, sk, d) + this.get(si + 1, sj, sk, d)),
                // partial d3f/dxdydz
                0.125 * (this.get(si + 1, sj + 1, sk + 1, d) - this.get(si - 1, sj + 1, sk + 1, d) - this.get(si + 1, sj - 1, sk + 1, d) + this.get(si - 1, sj - 1, sk + 1, d) - this.get(si + 1, sj + 1, sk - 1, d) + this.get(si - 1, sj + 1, sk - 1, d) + this.get(si + 1, sj - 1, sk - 1, d) - this.get(si - 1, sj - 1, sk - 1, d)),
                0.125 * (this.get(si + 2, sj + 1, sk + 1, d) - this.get(si, sj + 1, sk + 1, d) - this.get(si + 2, sj - 1, sk + 1, d) + this.get(si, sj - 1, sk + 1, d) - this.get(si + 2, sj + 1, sk - 1, d) + this.get(si, sj + 1, sk - 1, d) + this.get(si + 2, sj - 1, sk - 1, d) - this.get(si, sj - 1, sk - 1, d)),
                0.125 * (this.get(si + 1, sj + 2, sk + 1, d) - this.get(si - 1, sj + 2, sk + 1, d) - this.get(si + 1, sj, sk + 1, d) + this.get(si - 1, sj, sk + 1, d) - this.get(si + 1, sj + 2, sk - 1, d) + this.get(si - 1, sj + 2, sk - 1, d) + this.get(si + 1, sj, sk - 1, d) - this.get(si - 1, sj, sk - 1, d)),
                0.125 * (this.get(si + 2, sj + 2, sk + 1, d) - this.get(si, sj + 2, sk + 1, d) - this.get(si + 2, sj, sk + 1, d) + this.get(si, sj, sk + 1, d) - this.get(si + 2, sj + 2, sk - 1, d) + this.get(si, sj + 2, sk - 1, d) + this.get(si + 2, sj, sk - 1, d) - this.get(si, sj, sk - 1, d)),
                0.125 * (this.get(si + 1, sj + 1, sk + 2, d) - this.get(si - 1, sj + 1, sk + 2, d) - this.get(si + 1, sj - 1, sk + 2, d) + this.get(si - 1, sj - 1, sk + 2, d) - this.get(si + 1, sj + 1, sk, d) + this.get(si - 1, sj + 1, sk, d) + this.get(si + 1, sj - 1, sk, d) - this.get(si - 1, sj - 1, sk, d)),
                0.125 * (this.get(si + 2, sj + 1, sk + 2, d) - this.get(si, sj + 1, sk + 2, d) - this.get(si + 2, sj - 1, sk + 2, d) + this.get(si, sj - 1, sk + 2, d) - this.get(si + 2, sj + 1, sk, d) + this.get(si, sj + 1, sk, d) + this.get(si + 2, sj - 1, sk, d) - this.get(si, sj - 1, sk, d)),
                0.125 * (this.get(si + 1, sj + 2, sk + 2, d) - this.get(si - 1, sj + 2, sk + 2, d) - this.get(si + 1, sj, sk + 2, d) + this.get(si - 1, sj, sk + 2, d) - this.get(si + 1, sj + 2, sk, d) + this.get(si - 1, sj + 2, sk, d) + this.get(si + 1, sj, sk, d) - this.get(si - 1, sj, sk, d)),
                0.125 * (this.get(si + 2, sj + 2, sk + 2, d) - this.get(si, sj + 2, sk + 2, d) - this.get(si + 2, sj, sk + 2, d) + this.get(si, sj, sk + 2, d) - this.get(si + 2, sj + 2, sk, d) + this.get(si, sj + 2, sk, d) + this.get(si + 2, sj, sk, d) - this.get(si, sj, sk, d))
        });

        return C.times(x);
    }

    public void apply(Vect input, Vect output)
    {
        Sampling sampling = this.volume.getSampling();

        if (!sampling.contains(input))
        {
            output.setAll(0.0);
            return;
        }

        if (this.cache && this.coefs == null)
        {
            this.coefs = new Vect[sampling.size() * this.volume.getDim()];
        }

        Vect voxel = sampling.voxel(input);

        int ni = sampling.numI();
        double ci = voxel.getX();
        int si = (int) Math.min(ni - 1, Math.max(Math.floor(ci), 0));
        double pi = ci - si;

        int nj = sampling.numJ();
        double cj = voxel.getY();
        int sj = (int) Math.min(nj - 1, Math.max(Math.floor(cj), 0));
        double pj = cj - sj;

        int nk = sampling.numK();
        double ck = voxel.getZ();
        int sk = (int) Math.min(nk - 1, Math.max(Math.floor(ck), 0));
        double pk = ck - sk;

        for (int d = 0; d < this.volume.getDim(); d++)
        {
            Vect coef = null;

            if (this.cache)
            {
                int idx = sampling.index(si, sj, sk) + sampling.size() * d;
                if (this.coefs[idx] == null)
                {
                    coef = this.coefs(si, sj, sk, d);
                    this.coefs[idx] = coef;
                }
                else
                {
                    coef = this.coefs[idx];
                }
            }
            else
            {
                coef = this.coefs(si, sj, sk, d);
            }

            int ijkn = 0;
            double dzpow = 1;
            double result = 0;
            for (int k = 0; k < 4; ++k)
            {
                double dypow = 1;
                for (int j = 0; j < 4; ++j)
                {
                    result += dypow * dzpow * (coef.get(ijkn) + pi * (coef.get(ijkn + 1) + pi * (coef.get(ijkn + 2) + pi * coef.get(ijkn + 3))));
                    ijkn += 4;
                    dypow *= pj;
                }
                dzpow *= pk;
            }

            output.set(d, result);
        }
    }
}