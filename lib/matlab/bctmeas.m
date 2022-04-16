function [] = bctmeas(mat_fn, names_fn, out_dir)

names = textread(names_fn, '%s');

wei = load(mat_fn);
invwei = 1.0 ./ (wei + 1e-6);
bin = double(wei > 0);
dist_wei = distance_wei(wei);
dist_invwei = distance_wei(invwei);
dist_bin = distance_bin(bin);
null_wei = null_model_und_sign_port(wei, 500, 0.1);
null_invwei = 1.0 ./ (null_wei + 1e-6);
null_bin = null_model_und_sign_port(bin, 500, 0.1);
dist_null_wei = distance_wei(null_wei);
dist_null_invwei = distance_wei(null_invwei);
dist_null_bin = distance_bin(null_bin);

local_degrees = degrees_und(wei);
local_strengths = strengths_und(wei);
local_efficiency_wei = efficiency_wei(wei, 1);
local_efficiency_bin = efficiency_bin(bin, 1);
local_clustering_coef_wei = clustering_coef_wu(wei);
local_clustering_coef_bin = clustering_coef_bu(bin);
local_clustering_coef_null_wei = clustering_coef_wu(null_wei);
local_clustering_coef_null_bin = clustering_coef_bu(null_bin);
local_between_wei = betweenness_wei(dist_wei);
local_between_null_wei = betweenness_wei(dist_null_wei);

global_assortivity = assortativity_bin(bin, 0);
global_density = density_und(wei);
global_degree = mean(degrees_und(wei));
global_strength = mean(strengths_und(wei));
global_efficiency_wei = efficiency_wei(wei);
global_efficiency_bin = efficiency_bin(bin);
[Mwei, global_modularity_wei] = community_louvain(wei);
[Mbin, global_modularity_bin] = community_louvain(bin);
global_transitivity_wei = transitivity_wu(wei);
global_transitivity_bin = transitivity_bu(bin);

char_path_length_wei = charpath(dist_wei);
char_path_length_invwei = charpath(dist_invwei);
char_path_length_bin = charpath(dist_bin);
char_path_length_null_wei = charpath(dist_null_wei);
char_path_length_null_invwei = charpath(dist_null_invwei);
char_path_length_null_bin = charpath(dist_null_bin);
char_path_length_norm_wei = char_path_length_wei / char_path_length_null_wei;
char_path_length_norm_invwei = char_path_length_invwei / char_path_length_null_invwei;
char_path_length_norm_bin = char_path_length_bin / char_path_length_null_bin;

mean_clustering_coef_wei = mean(clustering_coef_wu(wei));
mean_clustering_coef_bin = mean(clustering_coef_bu(bin));
mean_clustering_coef_null_wei = mean(clustering_coef_wu(null_wei));
mean_clustering_coef_null_bin = mean(clustering_coef_bu(null_bin));
mean_clustering_coef_norm_wei = mean_clustering_coef_wei / mean_clustering_coef_null_wei;
mean_clustering_coef_norm_bin = mean_clustering_coef_bin / mean_clustering_coef_null_bin;

small_world_wei = mean_clustering_coef_norm_wei / char_path_length_norm_wei;
small_world_invwei = mean_clustering_coef_norm_wei / char_path_length_norm_invwei;
small_world_bin= mean_clustering_coef_norm_bin / char_path_length_norm_bin;

i = 1;
globals = cell(28,2); 

globals{i,1} = 'assortitivity';                 globals{i,2} = global_assortivity;            i = i + 1;
globals{i,1} = 'density';                       globals{i,2} = global_density;                i = i + 1;
globals{i,1} = 'mean_degree';                   globals{i,2} = global_degree;                 i = i + 1;
globals{i,1} = 'mean_strength';                 globals{i,2} = global_strength;               i = i + 1;
globals{i,1} = 'efficiency_wei';                globals{i,2} = global_efficiency_wei;         i = i + 1;
globals{i,1} = 'efficiency_bin';                globals{i,2} = global_efficiency_bin;         i = i + 1;
globals{i,1} = 'modularity_wei';                globals{i,2} = global_modularity_wei;         i = i + 1;
globals{i,1} = 'modularity_bin';                globals{i,2} = global_modularity_bin;         i = i + 1;
globals{i,1} = 'transitivity_wei';              globals{i,2} = global_transitivity_wei;       i = i + 1;
globals{i,1} = 'transitivity_bin';              globals{i,2} = global_transitivity_bin;       i = i + 1;
globals{i,1} = 'char_path_length_wei';          globals{i,2} = char_path_length_wei;          i = i + 1;
globals{i,1} = 'char_path_length_invwei';       globals{i,2} = char_path_length_invwei;       i = i + 1;
globals{i,1} = 'char_path_length_bin';          globals{i,2} = char_path_length_bin;          i = i + 1;
globals{i,1} = 'char_path_length_null_wei';     globals{i,2} = char_path_length_null_wei;     i = i + 1;
globals{i,1} = 'char_path_length_null_invwei';  globals{i,2} = char_path_length_null_invwei;  i = i + 1;
globals{i,1} = 'char_path_length_null_bin';     globals{i,2} = char_path_length_null_bin;     i = i + 1;
globals{i,1} = 'char_path_length_norm_wei';     globals{i,2} = char_path_length_norm_wei;     i = i + 1;
globals{i,1} = 'char_path_length_norm_invwei';  globals{i,2} = char_path_length_norm_invwei;  i = i + 1;
globals{i,1} = 'char_path_length_norm_bin';     globals{i,2} = char_path_length_norm_bin;     i = i + 1;
globals{i,1} = 'mean_clustering_coef_wei';      globals{i,2} = mean_clustering_coef_wei;      i = i + 1;
globals{i,1} = 'mean_clustering_coef_bin';      globals{i,2} = mean_clustering_coef_bin;      i = i + 1;
globals{i,1} = 'mean_clustering_coef_null_wei'; globals{i,2} = mean_clustering_coef_null_wei; i = i + 1;
globals{i,1} = 'mean_clustering_coef_null_bin'; globals{i,2} = mean_clustering_coef_null_bin; i = i + 1;
globals{i,1} = 'mean_clustering_coef_norm_wei'; globals{i,2} = mean_clustering_coef_norm_wei; i = i + 1;
globals{i,1} = 'mean_clustering_coef_norm_bin'; globals{i,2} = mean_clustering_coef_norm_bin; i = i + 1;
globals{i,1} = 'small_world_wei';               globals{i,2} = small_world_wei;               i = i + 1;
globals{i,1} = 'small_world_invwei';            globals{i,2} = small_world_invwei;            i = i + 1;
globals{i,1} = 'small_world_bin';               globals{i,2} = small_world_bin;               i = i + 1;

local_dir = fullfile(out_dir, 'local');
mkdir(out_dir);
mkdir(local_dir);

fid = fopen(fullfile(out_dir, 'global.csv'), 'w');
fprintf(fid, 'name,value\n');
for i=1:length(globals),
    fprintf(fid, '%s,%g\n', globals{i,1}, globals{i,2});
end
fclose(fid);

fid = fopen(fullfile(local_dir, 'degree.csv'), 'w');
fprintf(fid, 'name,value\n');
for i=1:length(names),
    fprintf(fid, '%s,%g\n', names{i}, local_degrees(i));
end
fclose(fid);

fid = fopen(fullfile(local_dir, 'strength.csv'), 'w');
fprintf(fid, 'name,value\n');
for i=1:length(names),
    fprintf(fid, '%s,%g\n', names{i}, local_strengths(i));
end
fclose(fid);

fid = fopen(fullfile(local_dir, 'local_efficiency_wei.csv'), 'w');
fprintf(fid, 'name,value\n');
for i=1:length(names),
    fprintf(fid, '%s,%g\n', names{i}, local_efficiency_wei(i));
end
fclose(fid);

fid = fopen(fullfile(local_dir, 'local_efficiency_bin.csv'), 'w');
fprintf(fid, 'name,value\n');
for i=1:length(names),
    fprintf(fid, '%s,%g\n', names{i}, local_efficiency_bin(i));
end
fclose(fid);

fid = fopen(fullfile(local_dir, 'clustering_coef_wei.csv'), 'w');
fprintf(fid, 'name,value\n');
for i=1:length(names),
    fprintf(fid, '%s,%g\n', names{i}, local_clustering_coef_wei(i));
end
fclose(fid);

fid = fopen(fullfile(local_dir, 'clustering_coef_bin.csv'), 'w');
fprintf(fid, 'name,value\n');
for i=1:length(names),
    fprintf(fid, '%s,%g\n', names{i}, local_clustering_coef_bin(i));
end
fclose(fid);

fid = fopen(fullfile(local_dir, 'clustering_coef_null_wei.csv'), 'w');
fprintf(fid, 'name,value\n');
for i=1:length(names),
    fprintf(fid, '%s,%g\n', names{i}, local_clustering_coef_null_wei(i));
end
fclose(fid);

fid = fopen(fullfile(local_dir, 'clustering_coef_null_bin.csv'), 'w');
fprintf(fid, 'name,value\n');
for i=1:length(names),
    fprintf(fid, '%s,%g\n', names{i}, local_clustering_coef_null_bin(i));
end
fclose(fid);

fid = fopen(fullfile(local_dir, 'between_wei.csv'), 'w');
fprintf(fid, 'name,value\n');
for i=1:length(names),
    fprintf(fid, '%s,%g\n', names{i}, local_between_wei(i));
end
fclose(fid);

fid = fopen(fullfile(local_dir, 'between_null_wei.csv'), 'w');
fprintf(fid, 'name,value\n');
for i=1:length(names),
    fprintf(fid, '%s,%g\n', names{i}, local_between_null_wei(i));
end
fclose(fid);
