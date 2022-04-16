
disp('started dki modeling')

disp('loading data')
dwinii = load_untouch_nii('dwi.nii.gz');
masknii = load_untouch_nii('mask.nii.gz');
grads = [load('bvecs.txt'), load('bvals.txt')];

dwi = dwinii.img;
mask = masknii.img;

disp('fitting model')
[b0, dt] = dki_fit(dwi, grads, mask>0);

disp('extracting tensor paramters')
[fa, md, ad, rd, fe, mk, ak, rk] = dki_parameters(dt,mask>0);

disp('extracting wmti paramters')
[awf, eas, ias] = wmti_parameters(dt,mask>0);

disp('writing output')
mkdir('models.dki');
out = masknii;
out.hdr.dime.datatype = dwinii.hdr.dime.datatype;

out.img = b0;
save_untouch_nii(out, 'models.dki/dki_b0.nii.gz');

out.img = fa;
save_untouch_nii(out, 'models.dki/dki_fa.nii.gz');

out.img = md;
save_untouch_nii(out, 'models.dki/dki_md.nii.gz');

out.img = rd;
save_untouch_nii(out, 'models.dki/dki_rd.nii.gz');

out.img = ad;
save_untouch_nii(out, 'models.dki/dki_ad.nii.gz');

out.img = awf;
save_untouch_nii(out, 'models.dki/dki_awf.nii.gz');

out.img = fe;
save_untouch_nii(out, 'models.dki/dki_fe.nii.gz');

out.img = mk;
save_untouch_nii(out, 'models.dki/dki_mk.nii.gz');

out.img = ak;
save_untouch_nii(out, 'models.dki/dki_ak.nii.gz');

out.img = rk;
save_untouch_nii(out, 'models.dki/dki_rk.nii.gz');

out.img = eas.de1;
save_untouch_nii(out, 'models.dki/dki_de1.nii.gz');

out.img = eas.de2;
save_untouch_nii(out, 'models.dki/dki_de2.nii.gz');

out.img = eas.de3;
save_untouch_nii(out, 'models.dki/dki_de3.nii.gz');

out.img = eas.tort;
save_untouch_nii(out, 'models.dki/dki_tort.nii.gz');

out.img = eas.de_perp;
save_untouch_nii(out, 'models.dki/dki_de_perp.nii.gz');

out.img = ias.da1;
save_untouch_nii(out, 'models.dki/dki_da1.nii.gz');

out.img = ias.da2;
save_untouch_nii(out, 'models.dki/dki_da2.nii.gz');

out.img = ias.da3;
save_untouch_nii(out, 'models.dki/dki_da3.nii.gz');

out.img = ias.da_perp;
save_untouch_nii(out, 'models.dki/dki_da_perp.nii.gz');

out = dwinii;
out.hdr.dime.dim(5) = 21;
out.img = dt;
save_untouch_nii(out, 'models.dki/dki_dt.nii.gz');

disp('finished dki modeling')

exit;
