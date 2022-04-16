function [M Q]=community_louvain(W,gamma,M0,B)
%COMMUNITY_LOUVAIN     Optimal community structure
%
%   M = community_louvain(W);
%   [M,Q] = community_louvain(W,gamma);
%   [M,Q] = community_louvain(W,gamma,M0);
%   [M,Q] = community_louvain(W,gamma,M0,'potts');
%   [M,Q] = community_louvain(W,[],[],B);
%
%   The optimal community structure is a subdivision of the network into
%   nonoverlapping groups of nodes which maximizes the number of within-
%	group edges, and minimizes the number of between-group edges.
%
%   This function is a fast and accurate multi-iterative generalization of the
%	Louvain community detection algorithm. This function subsumes and improves upon,
%		modularity_louvain_und.m, modularity_finetune_und.m,
%		modularity_louvain_dir.m, modularity_finetune_dir.m,
%	and additionally allows to optimize other objective functions (includes built-in
%	Potts Model Hamiltonian, allows for custom objective-function matrices).
%
%   Input:      W       directed/undirected weighted/binary connection matrix.
%               gamma,  resolution parameter (optional)
%                                gamma  > 1     detects smaller modules
%                           0 <= gamma  < 1     detects larger modules
%                                gamma  = 1     (default) 'classic' modularity
%               M0,    	initial community affiliation vector (optional)
%               B,      objective-function type or custom objective-function matrix (optional)
%                          	'modularity'  		Modularity (default)
%                           'potts'         	Potts model Hamiltonian
%                          	 B              	custom square N*N objective-function matrix
%
%   Outputs:    M,     	community structure
%               Q,    	optimized statistic (modularity)
%
%   References: Blondel et al. (2008)  J. Stat. Mech. P10008.
%               Reichardt and Bornholdt (2006) Phys. Rev. E 74, 016110. 
%               Ronhovde and Nussinov (2008) Phys. Rev. E 80, 016109
%               Sun et al. (2008) Europhysics Lett 86, 28004.
%
%   Mika Rubinov, U Cambridge 2015


W=double(W);                                            % convert from logical
n=length(W);
s=sum(W(:));                                            % sum of edges (each undirected edge is counted twice)

if min(W(:))<-1e-10
    error('W must not contain negative weights.')
end

if ~exist('B','var') || isempty(B)
    B = 'modularity';
end
if ( ~exist('gamma','var') || isempty(gamma)) && ischar(B)
    gamma = 1;
end
if ~exist('M0','var') || isempty(M0)
    M0=1:n;
elseif numel(M0)~=n
    error('M0 must contain n elements.')
end

[~,~,Mb] = unique(M0);
M = Mb;

if ischar(B)
    switch B
        case 'modularity';
            B = W-gamma*(sum(W,2)*sum(W,1))/s;
        case 'potts';
            B = W-gamma*(~W);
        otherwise;
            error('Unknown objective function.');
    end
else
    B = double(B);
    if ~isequal(size(W),size(B))
        error('W and B must have the same size.')
    end
    if max(max(abs(B-B.')))>1e-10
        warning('B is not symmetric, enforcing symmetry.')
    end
    if exist('gamma','var')
        warning('Value of gamma is ignored in generalized mode.')
    end
end

B = (B+B.')/2;                                          % symmetrize modularity matrix
Hnm=zeros(n,n);                                         % node-to-module degree
for m=1:max(Mb)                                         % loop over modules
    Hnm(:,m)=sum(B(:,Mb==m),2);
end
H=sum(Hnm,2);                                           % node degree
Hm=sum(Hnm,1);                                          % module degree

Q0 = -inf;
Q = sum(B(bsxfun(@eq,M0,M0.')))/s;                     % compute modularity
first_iteration = true;
while Q-Q0>1e-10
    flag = true;                                        % flag for within-hierarchy search
    while flag;
        flag = false;
        for u=randperm(n)                               % loop over all nodes in random order
            ma = Mb(u);                                 % current module of u
            dQ = Hnm(u,:) - Hnm(u,ma) + B(u,u);
            dQ(ma) = 0;                                 % (line above) algorithm condition
            
            [max_dQ mb] = max(dQ);                      % maximal increase in modularity and corresponding module
            if max_dQ>1e-10;                            % if maximal increase is positive
                flag = true;
                Mb(u) = mb;                             % reassign module
                
                Hnm(:,mb) = Hnm(:,mb)+B(:,u);           % change node-to-module strengths
                Hnm(:,ma) = Hnm(:,ma)-B(:,u);
                Hm(mb) = Hm(mb)+H(u);                   % change module strengths
                Hm(ma) = Hm(ma)-H(u);
            end
        end
    end
    [~,~,Mb] = unique(Mb);                              % new module assignments
    
    M0 = M;
    if first_iteration
        M=Mb;
        first_iteration=false;
    else
        for u=1:n                                       % loop through initial module assignments
            M(M0==u)=Mb(u);                            % assign new modules
        end
    end
    
    n=max(Mb);                                          % new number of modules
    B1=zeros(n);                                        % new weighted matrix
    for u=1:n
        for v=u:n
            bm=sum(sum(B(Mb==u,Mb==v)));                % pool weights of nodes in same module
            B1(u,v)=bm;
            B1(v,u)=bm;
        end
    end
    B=B1;
    
    Mb=1:n;                                             % initial module assignments
    Hnm=B;                                              % node-to-module strength
    H=sum(B);                                           % node strength
    Hm=H;                                               % module strength
    
    Q0=Q;
    Q=trace(B)/s;                                      % compute modularity
end
