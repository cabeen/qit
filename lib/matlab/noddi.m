
disp('started noddi modeling')

CreateROI('dwi.nii', 'mask.nii', 'NODDI_roi.mat');
protocol = FSL2Protocol('grads.bval', 'grads.bvec');
noddi = MakeModel('WatsonSHStickTortIsoV_B0');
batch_fitting_single('NODDI_roi.mat', protocol, noddi, 'FittedParams.mat');
SaveParamsAsNIfTI('FittedParams.mat', 'NODDI_roi.mat', 'mask.nii', 'models.noddi/noddi');

disp('finished noddi modeling')

exit;
