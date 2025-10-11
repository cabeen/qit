
if len(args) != 4:
    print("usage: zscore.py input.nii.gz mask.nii.gz output.nii.gz")
else:
	img = Volume.read(args[1])
	mask = Mask.read(args[2])
	stats = VolumeVoxelStats().withInput(img).withMask(mask).run()

	output = img.proto()
	for sample in img.getSampling():
			newval = (img.get(sample, 0) - stats.mean) / stats.std
			output.set(sample, 0, newval)

	output.write(args[3])
