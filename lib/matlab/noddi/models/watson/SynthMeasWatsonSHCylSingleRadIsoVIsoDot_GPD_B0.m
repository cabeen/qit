function [E,J]=SynthMeasWatsonSHCylSingleRadIsoVIsoDot_GPD_B0(x, protocol, fibredir, roots)
% Substrate: Impermeable cylinders with one radius in a homogeneous background.
% Orientation distribution: Watson's distribution with SH approximation
% Signal approximation: Gaussian phase distribution.
% Notes: This version includes an isotropic diffusion compartment with its own
% diffusivity.
% This version includes a stationary water compartment.
% Includes a free parameter for the measurement at b=0.
%
% [E,J]=SynthMeasWatsonSHCylSingleRadIsoVIsoDot_GPD_B0(x, protocol, fibredir, roots)
% returns the measurements E according to the model and the Jacobian J of the
% measurements with respect to the parameters.  The Jacobian does not
% include derivates with respect to the fibre direction.
%
% x is the list of model parameters in SI units:
% x(1) is the volume fraction of the intracellular space.
% x(2) is the free diffusivity of the material inside and outside the cylinders.
% x(3) is the hindered diffusivity outside the cylinders in perpendicular directions.
% x(4) is the radius of the cylinders.
% x(5) is the concentration parameter of the Watson's distribution.
% x(6) is the volume fraction of the isotropic compartment.
% x(7) is the diffusivity of the isotropic compartment.
% x(8) is the volume fraction of the isotropic restriction.
% x(9) is the measurement at b=0.
%
% protocol is the object containing the acquisition protocol.
%
% fibredir is a unit vector along the symmetry axis of the Watson's
% distribution.  It must be in Cartesian coordinates [x y z]' with size [3 1].
%
% roots contains solutions to the Bessel function equation from function
% BesselJ_RootsCyl.
%
% author: Gary Hui Zhang (gary.zhang@ucl.ac.uk)
%

S0 = x(9);

% Call the other function to get normalized measurements.
if(nargout == 1)
    Enorm=SynthMeasWatsonSHCylSingleRadIsoVIsoDot_GPD(x, protocol, fibredir, roots);
else
    [Enorm,Jnorm]=SynthMeasWatsonSHCylSingleRadIsoVIsoDot_GPD(x, protocol, fibredir, roots);
end

E = Enorm*S0;

if(nargout>1)
    J = Jnorm*S0;
    J(:,9) = Enorm;
end

