package qit.math.utils.optim.fed;

import java.lang.*;

/**
 * This class tests the Fzero class.
 *
 * @author Steve Verrill
 * @version .5 --- April 18, 2001
 */


public class FzeroTest extends Object implements Fzero_methods
{

    int id_f_to_zero;
    double d, e, f;

    FzeroTest(int idtemp, double dtemp, double etemp, double ftemp)
    {

        id_f_to_zero = idtemp;
        d = dtemp;
        e = etemp;
        f = ftemp;

    }

    public static void main(String args[])
    {

        int another;
        int idtemp;
        double dtemp, etemp, ftemp;

        double b[] = new double[2];
        double c[] = new double[2];
        double r, re, ae;

        int iflag[] = new int[2];

        dtemp = etemp = ftemp = 0.0;

        another = 1;

        while (another == 1)
        {

/*

   Console is a public domain class described in Cornell
   and Horstmann's Core Java (SunSoft Press, Prentice-Hall).

*/

            idtemp = Console.readInt("\nFor which function do you " +
                    "want to find zeros?\n\n" +
                    "1 -- (x - d)(x - e)\n" +
                    "2 -- (x - d)(x - e)(x - f)\n" +
                    "3 -- sin(x)\n\n");

            if (idtemp == 1)
            {

                dtemp = Console.readDouble("\nWhat is the d value?  ");
                etemp = Console.readDouble("\nWhat is the e value?  ");

            }
            else if (idtemp == 2)
            {

                dtemp = Console.readDouble("\nWhat is the d value?  ");
                etemp = Console.readDouble("\nWhat is the e value?  ");
                ftemp = Console.readDouble("\nWhat is the f value?  ");

            }

            FzeroTest fzerotest = new FzeroTest(idtemp, dtemp, etemp, ftemp);

            b[1] = Console.readDouble("\nWhat is the b value?  ");
            c[1] = Console.readDouble("\nWhat is the c value?  ");
            r = (b[1] + c[1]) / 2.0;
            re = Console.readDouble("\nWhat is the re value?  ");
            ae = Console.readDouble("\nWhat is the ae value?  ");

            Fzero.fzero(fzerotest, b, c, r, re, ae, iflag);

            System.out.print("\nThe b value is " + b[1] + "\n");
            System.out.print("\nThe iflag value is " + iflag[1] + "\n");

            another = Console.readInt("\nAnother test?" +
                    "   0 - no   1 - yes\n\n");

        }

        System.out.print("\n");

    }


    public double f_to_zero(double x)
    {

        double ff;

        if (id_f_to_zero == 1)
        {

            ff = (x - d) * (x - e);

        }
        else if (id_f_to_zero == 2)
        {

            ff = (x - d) * (x - e) * (x - f);

        }
        else
        {

            ff = Math.sin(x);

        }

        return ff;

    }


}




