function [E,J]=SynthMeasWatsonSHStickIsoVIsoDot_B0(x, protocol, fibredir)
% Substrate: Impermeable sticks (cylinders with zero radius) in a homogeneous
% background.
% Orientation distribution: Watson's distribution with SH approximation
% Signal approximation: Not applicable
% This version includes an isotropic diffusion compartment with its own
% diffusivity.
% This version includes a stationary water compartment.
% Includes a free parameter for the measurement at b=0.
%
% [E,J]=SynthMeasWatsonSHStickTortIsoV_B0(x, protocol, fibredir)
% returns the measurements E according to the model and the Jacobian J of the
% measurements with respect to the parameters.  The Jacobian does not
% include derivates with respect to the fibre direction.
%
% x is the list of model parameters in SI units:
% x(1) is the volume fraction of the intracellular space.
% x(2) is the free diffusivity of the material inside and outside the cylinders.
% x(3) is the hindered diffusivity outside the cylinders in perpendicular directions.
% x(4) is the concentration parameter of the Watson's distribution.
% x(5) is the volume fraction of the isotropic compartment.
% x(6) is the diffusivity of the isotropic compartment.
% x(7) is the volume fraction of the isotropic restriction.
% x(8) is the measurement at b=0.
%
% protocol is the object containing the acquisition protocol.
%
% fibredir is a unit vector along the symmetry axis of the Watson's
% distribution.  It must be in Cartesian coordinates [x y z]' with size [3 1].
%
% author: Gary Hui Zhang (gary.zhang@ucl.ac.uk)
%

xcyl=[x(1) x(2) x(3) 0 x(4) x(5) x(6) x(7) x(8)];

if(nargout == 1)
    E=SynthMeasWatsonSHCylSingleRadIsoVIsoDot_GPD_B0(xcyl, protocol, fibredir, 0);
else
    [E,Jcyl]=SynthMeasWatsonSHCylSingleRadIsoVIsoDot_GPD_B0(xcyl, protocol, fibredir, 0);
end

if(nargout>1)
    J(:,1) = Jcyl(:,1);
    J(:,2) = Jcyl(:,2);
    J(:,3) = Jcyl(:,3);
    J(:,4) = Jcyl(:,5);
    J(:,5) = Jcyl(:,6);
    J(:,6) = Jcyl(:,7);
    J(:,7) = Jcyl(:,8);
    J(:,8) = Jcyl(:,9);
end

