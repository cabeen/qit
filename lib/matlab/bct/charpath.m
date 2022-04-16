function  [lambda,efficiency,ecc,radius,diameter] = charpath(D,diagonal_dist)
%CHARPATH       Characteristic path length, global efficiency and related statistics
%
%   lambda = charpath(D);
%   lambda = charpath(D,1);
%   [lambda,efficiency] = charpath(D);
%   [lambda,efficiency,ecc,radius,diameter] = charpath(D);
%
%   The characteristic path length is the average shortest path length in 
%   the network. The global efficiency is the average inverse shortest path
%   length in the network.
%
%   Input:      D,              distance matrix
%               diagonal_dist   optional argument
%                               include distances on the main diagonal
%                               (diagonal_dist=0 by default)
%
%   Outputs:    lambda,         characteristic path length
%               efficiency,     global efficiency
%               ecc,            eccentricity (for each vertex)
%               radius,         radius of graph
%               diameter,       diameter of graph
%
%   Notes:
%       The input distance matrix may be obtained with any of the distance
%   functions, e.g. distance_bin, distance_wei.
%       Characteristic path length is calculated as the global mean of 
%   the distance matrix D, excludings any 'Infs' and excluding distances on
%   the main diagonal (unless diagonal_dist=1; by default diagonal_dist=0).
%
%
%   Olaf Sporns, Indiana University, 2002/2007/2008
%   Mika Rubinov, U Cambridge, 2010/2015

% Modification history
% 2002: original (OS)
% 2010: incorporation of global efficiency (MR)
% 2015: exclusion of diagonal weights by default (MR) 

n = size(D,1);
if ~exist('diagonal_dist','var') || ~diagonal_dist
    D(1:n+1:end) = Inf;                 %set diagonal distance to inf
end

% Mean of finite entries of D(G)
lambda     = sum(sum(   D(D~=Inf)))/length(nonzeros(D~=Inf));

% Efficiency: mean of inverse entries of D(G)
efficiency = sum(sum(1./D(D~=Inf)))/length(nonzeros(D~=Inf));

% Eccentricity for each vertex (note: ignore 'Inf') 
ecc = max(D.*(D~=Inf),[],2);

% Radius of graph
radius = min(ecc);

% Diameter of graph
diameter = max(ecc);

