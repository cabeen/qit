/*
    Fzero.java copyright claim:

    This software is based on the public domain fzero routine.
    The FORTRAN version can be found at

    www.netlib.org

    This software was translated from the FORTRAN version
    to Java by a US government employee on official time.  
    Thus this software is also in the public domain.


    The translator's mail address is:

    Steve Verrill 
    USDA Forest Products Laboratory
    1 Gifford Pinchot Drive
    Madison, Wisconsin
    53705


    The translator's e-mail address is:

    steve@ws13.fpl.fs.fed.us


***********************************************************************

DISCLAIMER OF WARRANTIES:

THIS SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND. 
THE TRANSLATOR DOES NOT WARRANT, GUARANTEE OR MAKE ANY REPRESENTATIONS 
REGARDING THE SOFTWARE OR DOCUMENTATION IN TERMS OF THEIR CORRECTNESS, 
RELIABILITY, CURRENTNESS, OR OTHERWISE. THE ENTIRE RISK AS TO 
THE RESULTS AND PERFORMANCE OF THE SOFTWARE IS ASSUMED BY YOU. 
IN NO CASE WILL ANY PARTY INVOLVED WITH THE CREATION OR DISTRIBUTION 
OF THE SOFTWARE BE LIABLE FOR ANY DAMAGE THAT MAY RESULT FROM THE USE 
OF THIS SOFTWARE.

Sorry about that.

***********************************************************************


History:

Date        Translator        Changes

4/17/01     Steve Verrill     Translated

*/


package qit.math.utils.optim.fed;


/**
 * <p>
 * This class was translated by a statistician from the FORTRAN
 * version of dfzero.  It is NOT an official translation.  When
 * public domain Java optimization routines become available
 * from professional numerical analysts, then <b>THE CODE PRODUCED
 * BY THE NUMERICAL ANALYSTS SHOULD BE USED</b>.
 * <p>
 * <p>
 * Meanwhile, if you have suggestions for improving this
 * code, please contact Steve Verrill at steve@ws13.fpl.fs.fed.us.
 *
 * @author Steve Verrill
 * @version .5 --- April 17, 2001
 */

public class Fzero extends Object
{

    /**
     * <p>
     * This method searches for a zero of a function f(x) between
     * the given values b and c until the width of the interval
     * (b,c) has collapsed to within a tolerance specified by
     * the stopping criterion, Math.abs(b-c) <= 2.0*(rw*Math.abs(b)+ae).
     * The method used is an efficient combination of bisection
     * and the secant rule.
     * The introductory comments from
     * the FORTRAN version are provided below.
     * <p>
     * This method is a translation from FORTRAN to Java of the Netlib
     * (actually it is in the SLATEC library which is available at Netlib)
     * function dfzero.  In the FORTRAN code, L.F. Shampine and H.A. Watts
     * are listed as the authors.
     * <p>
     * Translated by Steve Verrill, April 17, 2001.
     *
     * @param zeroclass A class that defines a method, f_to_zero,
     *                  that returns a value that is to be zeroed.
     *                  The class must implement
     *                  the Fzero_methods interface (see the definition
     *                  in Fzero_methods.java).  See FzeroTest.java
     *                  for an example of such a class.
     *                  f_to_zero must have one
     *                  double valued argument.
     * @param b[1]      One endpoint of the initial interval.  The value returned
     *                  for b[1] is usually the better approximation to a
     *                  zero of f_to_zero.
     * @param c[1]      The other endpoint of the initial interval.
     * @param r         Initial guess of a zero.  A (better) guess of a zero
     *                  of f_to_zero which could help in
     *                  speeding up convergence.  If f_to_zero(b) and f_to_zero(r) have
     *                  opposite signs, a root will be found in the interval
     *                  (b,r); if not, but f_to_zero(r) and f_to_zero(c) have opposite
     *                  signs, a root will be found in the interval (r,c);
     *                  otherwise, the interval (b,c) will be searched for a
     *                  possible root.  When no better guess is known, it is
     *                  recommended that r be set to b or c; because if r is
     *                  not interior to the interval (b,c), it will be ignored.
     * @param re        Relative error used for rw in the stopping criterion.
     *                  If the requested re is less than machine precision,
     *                  then rw is set to approximately machine precision.
     * @param ae        Absolute error used in the stopping criterion.  If the
     *                  given interval (b,c) contains the origin, then a
     *                  nonzero value should be chosen for AE.
     * @param iflag[1]  A status code.  User must check iflag[1] after each call.
     *                  Control returns to the user from dfzero in all cases.
     *                  <p>
     *                  1:  b is within the requested tolerance of a zero.
     *                  The interval (b,c) collapsed to the requested
     *                  tolerance, the function changes sign in (b,c), and
     *                  f_to_zero(x) decreased in magnitude as (b,c) collapsed.
     *                  <p>
     *                  2:  f_to_zero(b) = 0.  However, the interval (b,c) may not have
     *                  collapsed to the requested tolerance.
     *                  <p>
     *                  3:  b may be near a singular point of f_to_zero(x).
     *                  The interval (b,c) collapsed to the requested tol-
     *                  erance and the function changes sign in (b,c), but
     *                  f_to_zero(x) increased in magnitude as (b,c) collapsed,i.e.
     *                  Math.abs(f_to_zero(b out)) >
     *                  Math.max(Math.abs(f_to_zero(b in)),
     *                  Math.abs(f_to_zero(c in)))
     *                  <p>
     *                  4:  No change in sign of f_to_zero(x) was found although the
     *                  interval (b,c) collapsed to the requested tolerance.
     *                  The user must examine this case and decide whether
     *                  b is near a local minimum of f_to_zero(x), or b is near a
     *                  zero of even multiplicity, or neither of these.
     *                  <p>
     *                  5:  Too many (> 500) function evaluations used.
     */


    public static void fzero(Fzero_methods zeroclass, double b[], double c[],
                             double r, double re, double ae, int iflag[])
    {

/*

Here is a copy of the Netlib documentation:

      SUBROUTINE DFZERO(F,B,C,R,RE,AE,IFLAG)
C***BEGIN PROLOGUE  DFZERO
C***DATE WRITTEN   700901   (YYMMDD)
C***REVISION DATE  861211   (YYMMDD)
C***CATEGORY NO.  F1B
C***KEYWORDS  LIBRARY=SLATEC,TYPE=DOUBLE PRECISION(FZERO-S DFZERO-D),
C             BISECTION,NONLINEAR,NONLINEAR EQUATIONS,ROOTS,ZEROES,
C             ZEROS
C***AUTHOR  SHAMPINE,L.F.,SNLA
C           WATTS,H.A.,SNLA
C***PURPOSE  Search for a zero of a function F(X) in a given
C            interval (B,C).  It is designed primarily for problems
C            where F(B) and F(C) have opposite signs.
C***DESCRIPTION
C
C       **** Double Precision version of FZERO ****
C
C     Based on a method by T J Dekker
C     written by L F Shampine and H A Watts
C
C            DFZERO searches for a zero of a function F(X) between
C            the given values B and C until the width of the interval
C            (B,C) has collapsed to within a tolerance specified by
C            the stopping criterion, DABS(B-C) .LE. 2.*(RW*DABS(B)+AE).
C            The method used is an efficient combination of bisection
C            and the secant rule.
C
C     Description Of Arguments
C
C     F,B,C,R,RE and AE are DOUBLE PRECISION input parameters.
C     B and C are DOUBLE PRECISION output parameters and IFLAG (flagged
C        by an * below).
C
C        F     - Name of the DOUBLE PRECISION valued external function.
C                This name must be in an EXTERNAL statement in the
C                calling program.  F must be a function of one double
C                precision argument.
C
C       *B     - One end of the interval (B,C).  The value returned for
C                B usually is the better approximation to a zero of F.
C
C       *C     - The other end of the interval (B,C)
C
C        R     - A (better) guess of a zero of F which could help in
C                speeding up convergence.  If F(B) and F(R) have
C                opposite signs, a root will be found in the interval
C                (B,R); if not, but F(R) and F(C) have opposite
C                signs, a root will be found in the interval (R,C);
C                otherwise, the interval (B,C) will be searched for a
C                possible root.  When no better guess is known, it is
C                recommended that r be set to B or C; because if R is
C                not interior to the interval (B,C), it will be ignored.
C
C        RE    - Relative error used for RW in the stopping criterion.
C                If the requested RE is less than machine precision,
C                then RW is set to approximately machine precision.
C
C        AE    - Absolute error used in the stopping criterion.  If the
C                given interval (B,C) contains the origin, then a
C                nonzero value should be chosen for AE.
C
C       *IFLAG - A status code.  User must check IFLAG after each call.
C                Control returns to the user from FZERO in all cases.
C                XERROR does not process diagnostics in these cases.
C
C                1  B is within the requested tolerance of a zero.
C                   The interval (B,C) collapsed to the requested
C                   tolerance, the function changes sign in (B,C), and
C                   F(X) decreased in magnitude as (B,C) collapsed.
C
C                2  F(B) = 0.  However, the interval (B,C) may not have
C                   collapsed to the requested tolerance.
C
C                3  B may be near a singular point of F(X).
C                   The interval (B,C) collapsed to the requested tol-
C                   erance and the function changes sign in (B,C), but
C                   F(X) increased in magnitude as (B,C) collapsed,i.e.
C                     abs(F(B out)) .GT. max(abs(F(B in)),abs(F(C in)))
C
C                4  No change in sign of F(X) was found although the
C                   interval (B,C) collapsed to the requested tolerance.
C                   The user must examine this case and decide whether
C                   B is near a local minimum of F(X), or B is near a
C                   zero of even multiplicity, or neither of these.
C
C                5  Too many (.GT. 500) function evaluations used.
C***REFERENCES  L. F. SHAMPINE AND H. A. WATTS, *FZERO, A ROOT-SOLVING
C                 CODE*, SC-TM-70-631, SEPTEMBER 1970.
C               T. J. DEKKER, *FINDING A ZERO BY MEANS OF SUCCESSIVE
C                 LINEAR INTERPOLATION*, 'CONSTRUCTIVE ASPECTS OF THE
C                 FUNDAMENTAL THEOREM OF ALGEBRA', EDITED BY B. DEJON
C                 P. HENRICI, 1969.
C***ROUTINES CALLED  D1MACH
C***END PROLOGUE  DFZERO
c
*/

        int i, ic, icnt, ierr, ipass, ipss, j, klus, kount, kprint, lun, ndeg;

        int itest[] = new int[37];
        int itmp[] = new int[16];


        double a, acbs, acmb, aw, cmb, er, fa, fb, fc, fx, fz, p, q, rel, rw,
                t, tol, wi, work, wr, z;

// 1.1102e-16 is machine precision

        er = 2.0 * 1.2e-16;

// Initialize

        z = r;

        if (r <= Math.min(b[1], c[1]) || r >= Math.max(b[1], c[1]))
        {

            z = c[1];

        }

        rw = Math.max(re, er);
        aw = Math.max(ae, 0.0);

        ic = 0;
        t = z;

        fz = zeroclass.f_to_zero(t);
        fc = fz;

        t = b[1];

        fb = zeroclass.f_to_zero(t);

        kount = 2;

        if (Blas_f77.sign_f77(1.0, fz) != Blas_f77.sign_f77(1.0, fb))
        {

            c[1] = z;

        }
        else
        {

            if (z != c[1])
            {

                t = c[1];

                fc = zeroclass.f_to_zero(t);

                kount = 3;

                if (Blas_f77.sign_f77(1.0, fz) != Blas_f77.sign_f77(1.0, fc))
                {

                    b[1] = z;

                    fb = fz;

                }

            }

        }

        a = c[1];
        fa = fc;
        acbs = Math.abs(b[1] - c[1]);
        fx = Math.max(Math.abs(fb), Math.abs(fc));

// Main loop

        while (true)
        {

            if (Math.abs(fc) < Math.abs(fb))
            {

// Perform interchange

                a = b[1];
                fa = fb;
                b[1] = c[1];
                fb = fc;
                c[1] = a;
                fc = fa;

            }

            cmb = 0.5 * (c[1] - b[1]);
            acmb = Math.abs(cmb);
            tol = rw * Math.abs(b[1]) + aw;

// Test stopping criterion and function count.

            if (acmb <= tol)
            {

// Finished.  Process the results for the proper setting
// of the flag.

                if (Blas_f77.sign_f77(1.0, fb) == Blas_f77.sign_f77(1.0, fc))
                {

                    iflag[1] = 4;

                    return;

                }


                if (Math.abs(fb) > fx)
                {

                    iflag[1] = 3;

                    return;

                }


                iflag[1] = 1;

                return;

            }

            if (fb == 0.0)
            {

                iflag[1] = 2;

                return;

            }

            if (kount >= 500)
            {

                iflag[1] = 5;

                return;

            }


/*

C              CALCULATE NEW ITERATE IMPLICITLY AS B+P/Q
C              WHERE WE ARRANGE P .GE. 0.
C              THE IMPLICIT FORM IS USED TO PREVENT OVERFLOW.

*/

            p = (b[1] - a) * fb;
            q = fa - fb;

            if (p < 0.0)
            {

                p = -p;
                q = -q;

            }

/*

C              UPDATE A AND CHECK FOR SATISFACTORY REDUCTION
C              IN THE SIZE OF THE BRACKETING INTERVAL.
C              IF NOT, PERFORM BISECTION.

*/

            a = b[1];
            fa = fb;

            ic++;

            if (ic >= 4 && 8.0 * acmb >= acbs)
            {

// Use bisection

                b[1] = .5 * (c[1] + b[1]);

            }
            else
            {

                if (ic >= 4)
                {

                    ic = 0;
                    acbs = acmb;

                }

// Test for too small a change.

                if (p <= Math.abs(q) * tol)
                {

// Increment by the tolerance.

                    b[1] += Blas_f77.sign_f77(tol, cmb);

                }
                else
                {

// Root ought to be between b[1] and (c[1] + b[1])/2.

                    if (p >= cmb * q)
                    {

// Use bisection.

                        b[1] = 0.5 * (c[1] + b[1]);

                    }
                    else
                    {

// Use the secant rule.

                        b[1] += p / q;

                    }

                }

            }

// Have completed the computation for a new iterate b[1].

            t = b[1];
            fb = zeroclass.f_to_zero(t);
            kount++;

// Decide whether the next step is an interpolation
// or an extrapolation.

            if (Blas_f77.sign_f77(1.0, fb) == Blas_f77.sign_f77(1.0, fc))
            {

                c[1] = a;
                fc = fa;

            }

        }

    }

}
