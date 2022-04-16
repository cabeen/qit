%------------ BCT ------------------------------------%
qithome = getenv('QIT_DIR');
bctdir = sprintf('%s/lib/bct',qitdir);
if (exist(bctdir) == 7)
    addpath(bctdir);
end
clear bctdir
%-----------------------------------------------------%
