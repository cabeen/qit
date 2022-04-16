#! /bin/bash
##############################################################################
#
# Quantitative Imaging Toolkit (QIT) (c) 2012-2022 Ryan Cabeen
# All rights reserved.
#
# The Software remains the property of Ryan Cabeen ("the Author").
#
# The Software is distributed "AS IS" under this Licence solely for
# non-commercial use in the hope that it will be useful, but in order
# that the Author as a charitable foundation protects its assets for
# the benefit of its educational and research purposes, the Author
# makes clear that no condition is made or to be implied, nor is any
# warranty given or to be implied, as to the accuracy of the Software,
# or that it will be suitable for any particular purpose or for use
# under any specific conditions. Furthermore, the Author disclaims
# all responsibility for the use which is made of the Software. It
# further disclaims any liability for the outcomes arising from using
# the Software.
#
# The Licensee agrees to indemnify the Author and hold the
# Author harmless from and against any and all claims, damages and
# liabilities asserted by third parties (including claims for
# negligence) which arise directly or indirectly from the use of the
# Software or the sale of any products based on the Software.
#
# No part of the Software may be reproduced, modified, transmitted or
# transferred in any form or by any means, electronic or mechanical,
# without the express permission of the Author. The permission of
# the Author is not required if the said reproduction, modification,
# transmission or transference is done without financial return, the
# conditions of this Licence are imposed upon the receiver of the
# product, and all original and amended source code is included in any
# transmitted product. You may be held legally responsible for any
# copyright infringement that is caused or encouraged by your failure to
# abide by these terms and conditions.
#
# You are not permitted under this Licence to use this Software
# commercially. Use for which any financial return is received shall be
# defined as commercial use, and includes (1) integration of all or part
# of the source code or the Software into a product for sale or license
# by or on behalf of Licensee to third parties or (2) use of the
# Software or any derivative of it for research with the final aim of
# developing software products for sale or license to a third party or
# (3) use of the Software or any derivative of it for research with the
# final aim of developing non-software products for sale or license to a
# third party, or (4) use of the Software to provide any service to an
# external organisation for which payment is received.
#
##############################################################################

qitdiffout=$1
pvsout=$2
results=${qitdiffout}/pvs

function runit
{
  echo "[info] running command: $@"
  $@
  if [ $? != 0 ]; then
    echo "[error] command failed: $@"
    exit;
  fi
}

function xfm 
{
  input=${qitdiffout}/$1.nii.gz
  output=${results}/$2.nii.gz

  echo "input: ${input}"
  echo "output: ${output}"

  if [ -e ${input} ] && [ ! -e ${output} ]; then
    mkdir -p $(dirname ${results}/${output})

    runit qit --verbose --debug VolumeTransform \
      --interp Tricubic \
      --input ${input} \
      --reference ${pvsout}/t1w.nii.gz \
      --deform ${qitdiffout}/tone.diff.reg/tone2diff.nii.gz \
      --output ${output}
  fi
}

function nifticp
{
  input=$1
  output=$2

  mkdir -p $(dirname ${output})

  if [ -e ${input}.nii ] && [ ! -e ${output}.nii.gz ]; then
    runit cp ${input}.nii ${output}.nii
    runit gzip ${output}.nii
    runit rm ${output}.nii
  elif [ -e ${input}.nii.gz ] && [ ! -e ${output}.nii.gz ]; then 
    runit cp ${input}.nii.gz ${output}.nii.gz
  elif [ ! -e ${input}.nii.gz ] && [ ! -e ${input}.nii ]; then
    echo "[error] nifti image not found: ${input}"; exit 1
  fi
}

if [ $# -eq 0 ]; then echo "usage: $(basename $0) qitdiffout pvsout"; exit 1; fi

echo "[info] started"

mkdir -p ${results}/brain
nifticp ${pvsout}/epc.final ${results}/brain/epc
nifticp ${pvsout}/t1w ${results}/brain/t1w
nifticp ${pvsout}/t2w ${results}/brain/t2w

mkdir -p ${results}/regions
nifticp ${pvsout}/pvs.mask.final ${results}/regions/pvs_mask

for p in S0 FA MD RD AD CS CL CP; do
  xfm diff.models.dti/dti_${p} models/models.dti/dti_${p}
done

for p in S0 FA MD RD AD CS CL CP FW; do
  xfm diff.models.fwdti/dti_${p} models/models.fwdti/dti_${p}
done

for p in ficvf odi fiso; do
  xfm diff.models.noddi/noddi_${p} models/models.noddi/noddi_${p}
done

for p in diff frac; do
  xfm diff.models.mcsmt/mcsmt_${p} models/models.mcsmt/mcsmt_${p}
done

echo "[info] finished"

##############################################################################
# End of file
##############################################################################
