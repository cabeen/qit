function   [Rw] = rich_club_wu(CIJ,varargin)

% [Rw] = rich_club_wu(CIJ,varargin) % rich club curve for weighted graph
%
%
% inputs:
%               CIJ:       weighted connection matrix
%
%               optional:
%               k-level: max level of RC(k).
%               When k-level is not given, k-level will be set to max of degree of CIJ
%                
% output:
%               rich:         rich-club curve
%
% adopted from Opsahl et al. Phys Rev Lett, 2008, 101(16)
%  
% Martijn van den Heuvel, University Medical Center Utrecht, 2011
%
% For details see 'Rich club organization of the human connectome', Martijn van den Heuvel
% and Olaf Sporns, J Neuroscience 2011 31(44)
%
% =========================================================================

NofNodes = size(CIJ,2); %number of nodes
NodeDegree = sum((CIJ~=0)); %define degree of each node

%define to which level rc should be computed
if size(varargin,2)==1
    klevel = varargin{1};
elseif isempty(varargin)
   klevel = max(NodeDegree);   
else
    error('number of inputs incorrect. Should be [CIJ], or [CIJ, klevel]')
end


%wrank contains the ranked weights of the network, with strongest connections on top

wrank = sort(CIJ(:), 'descend');
    
%loop over all possible k-levels 
for kk = 1:klevel

    SmallNodes=find(NodeDegree<kk);

    if isempty(SmallNodes);
        Rw(kk)=NaN;
        continue
    end
    
    %remove small nodes with NodeDegree<kk
    CutoutCIJ=CIJ;
    CutoutCIJ(SmallNodes,:)=[];
    CutoutCIJ(:,SmallNodes)=[];

    %total weight of connections in subset E>r
    Wr = sum(CutoutCIJ(:));

    %total number of connections in subset E>r
    Er = length(find(CutoutCIJ~=0));

    %E>r number of connections with max weight in network
    wrank_r = wrank(1:1:Er);

    %weighted rich-club coefficient
    Rw(kk)=Wr / sum(wrank_r);

end
 
    
    
    
    
        
