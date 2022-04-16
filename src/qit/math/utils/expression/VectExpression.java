/*
 * Copyright 2012 Udo Klimaschewski
 * 
 * http://UdoJava.com/
 * http://about.me/udo.klimaschewski
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */

package qit.math.utils.expression;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.math.source.VectFunctionSource;
import qit.math.utils.MathUtils;

import java.math.MathContext;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

public class VectExpression
{
    private static final char DOT = '.';
    private static final char MINUS = '-';

    public static final Vect PI = VectSource.create1D(3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679);
    public static final Vect DELTA = VectSource.create1D(1e-6);
    public static final Vect EPSILON = VectSource.create1D(1e-9);
    public static final Vect ONE = VectSource.create1D(1);
    public static final Vect ZERO = VectSource.create1D(0);

    private MathContext mc = MathContext.DECIMAL32;

    private String expression = null;
    private List<String> rpn = null;
    private Map<String, OperatorExpression<Vect>> operators = Maps.newHashMap();
    private Map<String, FunctionExpression<Vect>> functions = Maps.newHashMap();
    private Map<String, Vect> variables = Maps.newHashMap();

    /**
     * ScalarExpression tokenizer that allows to iterate over a {@link String}
     * expression token by token. Blank characters will be skipped.
     */
    private class Tokenizer implements Iterator<String>
    {
        /**
         * Actual position in expression string.
         */
        private int pos = 0;

        /**
         * The original input expression.
         */
        private String input;
        /**
         * The previous token or <code>null</code> if none.
         */
        private String previousToken;

        /**
         * Creates a new tokenizer for an expression.
         *
         * @param input The expression string.
         */
        public Tokenizer(String input)
        {
            this.input = input.trim();
        }

        @Override
        public boolean hasNext()
        {
            return (pos < input.length());
        }

        /**
         * Peek at the next character, without advancing the iterator.
         *
         * @return The next character or character 0, if at end of string.
         */
        private char peekNextChar()
        {
            if (pos < (input.length() - 1))
            {
                return input.charAt(pos + 1);
            }
            else
            {
                return 0;
            }
        }

        @Override
        public String next()
        {
            StringBuilder token = new StringBuilder();
            if (pos >= input.length())
            {
                return previousToken = null;
            }
            char ch = input.charAt(pos);
            while (Character.isWhitespace(ch) && pos < input.length())
            {
                ch = input.charAt(++pos);
            }
            if (Character.isDigit(ch))
            {
                while ((Character.isDigit(ch) || ch == DOT) && (pos < input.length()))
                {
                    token.append(input.charAt(pos++));
                    ch = pos == input.length() ? 0 : input.charAt(pos);
                }
            }
            else if (ch == MINUS && Character.isDigit(peekNextChar()) && ("(".equals(previousToken) || ",".equals(previousToken) || previousToken == null || operators.containsKey(previousToken)))
            {
                token.append(MINUS);
                pos++;
                token.append(next());
            }
            else if (Character.isLetter(ch) || (ch == '_'))
            {
                while ((Character.isLetter(ch) || Character.isDigit(ch) || (ch == '_')) && (pos < input.length()))
                {
                    token.append(input.charAt(pos++));
                    ch = pos == input.length() ? 0 : input.charAt(pos);
                }
            }
            else if (ch == '(' || ch == ')' || ch == ',')
            {
                token.append(ch);
                pos++;
            }
            else
            {
                while (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '_' && !Character.isWhitespace(ch) && ch != '(' && ch != ')' && ch != ',' && (pos < input.length()))
                {
                    token.append(input.charAt(pos));
                    pos++;
                    ch = pos == input.length() ? 0 : input.charAt(pos);
                    if (ch == MINUS)
                    {
                        break;
                    }
                }
                if (!operators.containsKey(token.toString()))
                {
                    throw new ExceptionExpression("Unknown operator '" + token + "' at position " + (pos - token.length() + 1));
                }
            }
            return previousToken = token.toString();
        }

        @Override
        public void remove()
        {
            throw new ExceptionExpression("remove() not supported");
        }

        /**
         * Get the actual character position in the string.
         *
         * @return The actual character position.
         */
        public int getPos()
        {
            return pos;
        }

    }

    /**
     * Creates a new expression instance from an expression string.
     *
     * @param expression The expression. E.g. <code>"2.4*sin(3)/(2-4)"</code> or
     *                   <code>"sin(y)>0 & max(z, 3)>3"</code>
     */
    public VectExpression(String expression)
    {
        this.expression = expression;
        addOperator(new OperatorExpression<Vect>("+", 20, true)
        {
            @Override
            public Vect eval(Vect v1, Vect v2)
            {
                if (v1.size() == 1)
                {
                    return v2.plus(v1.get(0));
                }
                else if (v2.size() == 1)
                {
                    return v1.plus(v2.get(0));
                }
                else if (v1.size() == v2.size())
                {
                    return v1.plus(v2);
                }
                else
                {
                    throw new RuntimeException("invalid addition expression");
                }
            }
        });
        addOperator(new OperatorExpression<Vect>("-", 20, true)
        {
            @Override
            public Vect eval(Vect v1, Vect v2)
            {
                if (v1.size() == 1)
                {
                    return v1.minus(v2.get(0));
                }
                else if (v2.size() == 1)
                {
                    return v1.minus(v2.get(0));
                }
                else if (v1.size() == v2.size())
                {
                    return v1.minus(v2);
                }
                else
                {
                    throw new RuntimeException("invalid subtraction expression");
                }
            }
        });
        addOperator(new OperatorExpression<Vect>("*", 30, true)
        {
            @Override
            public Vect eval(Vect v1, Vect v2)
            {
                if (v1.size() == 1)
                {
                    return v2.times(v1.get(0));
                }
                else if (v2.size() == 1)
                {
                    return v1.times(v2.get(0));
                }
                else if (v1.size() == v2.size())
                {
                    return v1.times(v2);
                }
                else
                {
                    throw new RuntimeException("invalid subtraction expression");
                }
            }
        });
        addOperator(new OperatorExpression<Vect>("/", 30, true)
        {
            @Override
            public Vect eval(Vect v1, Vect v2)
            {
                return operators.get("*").eval(v1, v2.recip());
            }
        });
        addOperator(new OperatorExpression<Vect>("&&", 4, false)
        {
            @Override
            public Vect eval(Vect v1, Vect v2)
            {
                boolean b1 = !v1.equals(ZERO);
                boolean b2 = !v2.equals(ZERO);
                return b1 && b2 ? ONE : ZERO;
            }
        });

        addOperator(new OperatorExpression<Vect>("||", 2, false)
        {
            @Override
            public Vect eval(Vect v1, Vect v2)
            {
                boolean b1 = !v1.equals(ZERO);
                boolean b2 = !v2.equals(ZERO);
                return b1 || b2 ? ONE : ZERO;
            }
        });

        addOperator(new OperatorExpression<Vect>(">", 10, false)
        {
            @Override
            public Vect eval(Vect v1, Vect v2)
            {
                return v1.get(0) > v2.get(0) ? ONE : ZERO;
            }
        });

        addOperator(new OperatorExpression<Vect>(">=", 10, false)
        {
            @Override
            public Vect eval(Vect v1, Vect v2)
            {
                return v1.get(0) >= v2.get(0) ? ONE : ZERO;
            }
        });

        addOperator(new OperatorExpression<Vect>("<", 10, false)
        {
            @Override
            public Vect eval(Vect v1, Vect v2)
            {
                return v1.get(0) < v2.get(0) ? ONE : ZERO;
            }
        });

        addOperator(new OperatorExpression<Vect>("<=", 10, false)
        {
            @Override
            public Vect eval(Vect v1, Vect v2)
            {
                return v1.get(0) <= v2.get(0) ? ONE : ZERO;
            }
        });

        addOperator(new OperatorExpression<Vect>("=", 7, false)
        {
            @Override
            public Vect eval(Vect v1, Vect v2)
            {
                return v1.equals(v2) ? ONE : ZERO;
            }
        });
        addOperator(new OperatorExpression<Vect>("==", 7, false)
        {
            @Override
            public Vect eval(Vect v1, Vect v2)
            {
                return operators.get("=").eval(v1, v2);
            }
        });

        addOperator(new OperatorExpression<Vect>("!=", 7, false)
        {
            @Override
            public Vect eval(Vect v1, Vect v2)
            {
                return !v1.equals(v2) ? ONE : ZERO;
            }
        });
        addOperator(new OperatorExpression<Vect>("<>", 7, false)
        {
            @Override
            public Vect eval(Vect v1, Vect v2)
            {
                return operators.get("!=").eval(v1, v2);
            }
        });

        addFunction(new FunctionExpression<Vect>("NOT", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                boolean zero = parameters.get(0).equals(ZERO);
                return zero ? ONE : ZERO;
            }
        });

        addFunction(new FunctionExpression<Vect>("IF", 3)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                boolean isTrue = !parameters.get(0).equals(ZERO);
                return isTrue ? parameters.get(1) : parameters.get(2);
            }
        });

        addFunction(new FunctionExpression<Vect>("RANDOM", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                int dim = parameters.size() == 0 ? 1 : MathUtils.round(parameters.get(0).get(0));
                return VectSource.random(dim);
            }
        });
        addFunction(new FunctionExpression<Vect>("SIN", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                Vect input = parameters.get(0);
                Vect output = input.proto();
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.sin(Math.toRadians(input.get(i))));
                }
                return output;
            }
        });
        addFunction(new FunctionExpression<Vect>("COS", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                Vect input = parameters.get(0);
                Vect output = input.proto();
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.cos(Math.toRadians(input.get(i))));
                }
                return output;
            }
        });
        addFunction(new FunctionExpression<Vect>("TAN", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                Vect input = parameters.get(0);
                Vect output = input.proto();
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.tan(Math.toRadians(input.get(i))));
                }
                return output;
            }
        });
        addFunction(new FunctionExpression<Vect>("ASIN", 1)
        { // added by av
            @Override
            public Vect eval(List<Vect> parameters)
            {
                Vect input = parameters.get(0);
                Vect output = input.proto();
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.toDegrees(Math.asin(input.get(i))));
                }
                return output;
            }
        });
        addFunction(new FunctionExpression<Vect>("ACOS", 1)
        { // added by av
            @Override
            public Vect eval(List<Vect> parameters)
            {
                Vect input = parameters.get(0);
                Vect output = input.proto();
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.toDegrees(Math.acos(input.get(i))));
                }
                return output;
            }
        });
        addFunction(new FunctionExpression<Vect>("ATAN", 1)
        { // added by av
            @Override
            public Vect eval(List<Vect> parameters)
            {
                Vect input = parameters.get(0);
                Vect output = input.proto();
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.toDegrees(Math.atan(input.get(i))));
                }
                return output;
            }
        });
        addFunction(new FunctionExpression<Vect>("SINH", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                Vect input = parameters.get(0);
                Vect output = input.proto();
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.sinh(input.get(i)));
                }
                return output;
            }
        });
        addFunction(new FunctionExpression<Vect>("COSH", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                Vect input = parameters.get(0);
                Vect output = input.proto();
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.cosh(input.get(i)));
                }
                return output;
            }
        });
        addFunction(new FunctionExpression<Vect>("TANH", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                Vect input = parameters.get(0);
                Vect output = input.proto();
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.tanh(input.get(i)));
                }
                return output;
            }
        });
        addFunction(new FunctionExpression<Vect>("RAD", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                Vect input = parameters.get(0);
                Vect output = input.proto();
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.toRadians(input.get(i)));
                }
                return output;
            }
        });
        addFunction(new FunctionExpression<Vect>("DEG", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                Vect input = parameters.get(0);
                Vect output = input.proto();
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.toDegrees(input.get(i)));
                }
                return output;
            }
        });
        addFunction(new FunctionExpression<Vect>("MAX", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectSource.create1D(parameters.get(0).max());
            }
        });
        addFunction(new FunctionExpression<Vect>("MIN", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectSource.create1D(parameters.get(0).min());
            }
        });
        addFunction(new FunctionExpression<Vect>("MEAN", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectSource.create1D(parameters.get(0).mean());
            }
        });
        addFunction(new FunctionExpression<Vect>("SUM", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectSource.create1D(parameters.get(0).sum());
            }
        });
        addFunction(new FunctionExpression<Vect>("DOT", 2)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectSource.create1D(parameters.get(0).dot(parameters.get(1)));
            }
        });
        addFunction(new FunctionExpression<Vect>("CROSS", 2)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).cross(parameters.get(1));
            }
        });
        addFunction(new FunctionExpression<Vect>("FLIP", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).times(-1);
            }
        });
        addFunction(new FunctionExpression<Vect>("FLIPX", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).times(VectSource.create3D(-1, 1, 1));
            }
        });
        addFunction(new FunctionExpression<Vect>("FLIPY", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).times(VectSource.create3D(1, -1, 1));
            }
        });
        addFunction(new FunctionExpression<Vect>("FLIPZ", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).times(VectSource.create3D(1, 1, -1));
            }
        });
        addFunction(new FunctionExpression<Vect>("FLIPXY", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).times(VectSource.create3D(-1, -1, 1));
            }
        });
        addFunction(new FunctionExpression<Vect>("FLIPYZ", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).times(VectSource.create3D(1, -1, -1));
            }
        });
        addFunction(new FunctionExpression<Vect>("FLIPXZ", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).times(VectSource.create3D(-1, 1, -1));
            }
        });
        addFunction(new FunctionExpression<Vect>("ABS", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).abs();
            }
        });
        addFunction(new FunctionExpression<Vect>("LOG", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).log();
            }
        });
        addFunction(new FunctionExpression<Vect>("EXP", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).exp();
            }
        });
        addFunction(new FunctionExpression<Vect>("LOG10", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).log10();
            }
        });
        addFunction(new FunctionExpression<Vect>("ROUND", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).round();
            }
        });
        addFunction(new FunctionExpression<Vect>("FLOOR", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).floor();
            }
        });
        addFunction(new FunctionExpression<Vect>("CEIL", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).ceil();
            }
        });
        addFunction(new FunctionExpression<Vect>("SQRT", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).sqrt();
            }
        });
        addFunction(new FunctionExpression<Vect>("POW", 2)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).pow(parameters.get(1).get(0));
            }
        });
        addFunction(new FunctionExpression<Vect>("NORMALIZE", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).normalize();
            }
        });
        addFunction(new FunctionExpression<Vect>("PERP", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).perp();
            }
        });
        addFunction(new FunctionExpression<Vect>("RECIP", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).recip();
            }
        });
        addFunction(new FunctionExpression<Vect>("NORM", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectSource.create1D(parameters.get(0).norm());
            }
        });
        addFunction(new FunctionExpression<Vect>("NORMSQ", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectSource.create1D(parameters.get(0).norm2());
            }
        });
        addFunction(new FunctionExpression<Vect>("FIRST", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectSource.create1D(parameters.get(0).first());
            }
        });
        addFunction(new FunctionExpression<Vect>("LAST", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectSource.create1D(parameters.get(0).last());
            }
        });
        addFunction(new FunctionExpression<Vect>("SPHCOORD", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectFunctionSource.sphereCoords().apply(parameters.get(0));
            }
        });
        addFunction(new FunctionExpression<Vect>("SPHTEXT", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectFunctionSource.sphereTexture().apply(parameters.get(0));
            }
        });
        addFunction(new FunctionExpression<Vect>("CAT", 2)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).cat(parameters.get(1));
            }
        });
        addFunction(new FunctionExpression<Vect>("CAT3", 3)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                Vect a = parameters.get(0);
                Vect b = parameters.get(1);
                Vect c = parameters.get(2);

                return a.cat(b).cat(c);
            }
        });
        addFunction(new FunctionExpression<Vect>("ELEM", 2)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectSource.create1D(parameters.get(0).get(MathUtils.round(parameters.get(1).get(0))));
            }
        });
        addFunction(new FunctionExpression<Vect>("GET", 2)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                int idx = MathUtils.round(parameters.get(1).get(0));
                return VectSource.create1D(parameters.get(0).get(idx));
            }
        });
        addFunction(new FunctionExpression<Vect>("SET", 3)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                Vect out = parameters.get(0).copy();
                int idx = MathUtils.round(parameters.get(1).get(0));
                double value = parameters.get(2).get(0);
                out.set(idx, value);
                return out;
            }
        });
        addFunction(new FunctionExpression<Vect>("RANGE", 3)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                int start = MathUtils.round(parameters.get(1).get(0));
                int end = MathUtils.round(parameters.get(2).get(0));
                return parameters.get(0).sub(start, end);
            }
        });
        addFunction(new FunctionExpression<Vect>("ANGLEDEG", 2)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectSource.create1D(parameters.get(0).angleDeg(parameters.get(1)));
            }
        });
        addFunction(new FunctionExpression<Vect>("ANGLELINEDEG", 2)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectSource.create1D(parameters.get(0).angleLineDeg(parameters.get(1)));
            }
        });
        addFunction(new FunctionExpression<Vect>("DIST", 2)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectSource.create1D(parameters.get(0).dist(parameters.get(1)));
            }
        });
        addFunction(new FunctionExpression<Vect>("DISTSQ", 2)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return VectSource.create1D(parameters.get(0).dist2(parameters.get(1)));
            }
        });
        addFunction(new FunctionExpression<Vect>("NAN", 1)
        {
            @Override
            public Vect eval(List<Vect> parameters)
            {
                return parameters.get(0).nan() ? ONE : ZERO;
            }
        });

        variables.put("PI", PI);
        variables.put("DELTA", DELTA);
        variables.put("DEL", DELTA);
        variables.put("EPSILON", EPSILON);
        variables.put("EPS", EPSILON);
        variables.put("TRUE", ONE);
        variables.put("FALSE", ZERO);
    }

    /**
     * Is the string a number?
     *
     * @param st The string.
     * @return <code>true</code>, if the input string is a number.
     */
    private boolean isNumber(String st)
    {
        if (st.charAt(0) == MINUS && st.length() == 1)
            return false;
        for (char ch : st.toCharArray())
        {
            if (!Character.isDigit(ch) && ch != MINUS && ch != DOT)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Implementation of the <i>Shunting Yard</i> algorithm to transform an
     * infix expression to a RPN expression.
     *
     * @param expression The input expression in infx.
     * @return A RPN representation of the expression, with each token as a list
     * member.
     */
    private List<String> shuntingYard(String expression)
    {
        List<String> outputQueue = new ArrayList<String>();
        Stack<String> stack = new Stack<String>();

        Tokenizer tokenizer = new Tokenizer(expression);

        String lastFunction = null;
        String previousToken = null;
        while (tokenizer.hasNext())
        {
            String token = tokenizer.next();
            if (isNumber(token))
            {
                outputQueue.add(token);
            }
            else if (variables.containsKey(token))
            {
                outputQueue.add(token);
            }
            else if (functions.containsKey(token.toUpperCase(Locale.ROOT)))
            {
                stack.push(token);
                lastFunction = token;
            }
            else if (Character.isLetter(token.charAt(0)))
            {
                stack.push(token);
            }
            else if (",".equals(token))
            {
                while (!stack.isEmpty() && !"(".equals(stack.peek()))
                {
                    outputQueue.add(stack.pop());
                }
                if (stack.isEmpty())
                {
                    throw new ExceptionExpression("Parse error for function '" + lastFunction + "'");
                }
            }
            else if (operators.containsKey(token))
            {
                OperatorExpression<Vect> o1 = operators.get(token);
                String token2 = stack.isEmpty() ? null : stack.peek();
                while (operators.containsKey(token2) && ((o1.isLeftAssoc() && o1.getPrecedence() <= operators.get(token2).getPrecedence()) || (o1.getPrecedence() < operators.get(token2).getPrecedence())))
                {
                    outputQueue.add(stack.pop());
                    token2 = stack.isEmpty() ? null : stack.peek();
                }
                stack.push(token);
            }
            else if ("(".equals(token))
            {
                if (previousToken != null)
                {
                    if (isNumber(previousToken))
                    {
                        throw new ExceptionExpression("Missing operator at character position " + tokenizer.getPos());
                    }
                }
                stack.push(token);
            }
            else if (")".equals(token))
            {
                while (!stack.isEmpty() && !"(".equals(stack.peek()))
                {
                    outputQueue.add(stack.pop());
                }
                if (stack.isEmpty())
                {
                    throw new RuntimeException("Mismatched parentheses");
                }
                stack.pop();
                if (!stack.isEmpty() && functions.containsKey(stack.peek().toUpperCase(Locale.ROOT)))
                {
                    outputQueue.add(stack.pop());
                }
            }
            previousToken = token;
        }
        while (!stack.isEmpty())
        {
            String element = stack.pop();
            if ("(".equals(element) || ")".equals(element))
            {
                throw new RuntimeException("Mismatched parentheses");
            }
            if (!operators.containsKey(element))
            {
                throw new RuntimeException("Unknown operator or function: " + element);
            }
            outputQueue.add(element);
        }
        return outputQueue;
    }

    /**
     * Evaluates the expression.
     *
     * @return The result of the expression.
     */
    public Vect eval()
    {

        Stack<Vect> stack = new Stack<Vect>();

        for (String token : getRPN())
        {
            if (operators.containsKey(token))
            {
                Vect v1 = stack.pop();
                Vect v2 = stack.pop();
                stack.push(operators.get(token).eval(v2, v1));
            }
            else if (variables.containsKey(token))
            {
                stack.push(variables.get(token));
            }
            else if (functions.containsKey(token.toUpperCase(Locale.ROOT)))
            {
                FunctionExpression<Vect> f = functions.get(token.toUpperCase(Locale.ROOT));
                List<Vect> p = Lists.newArrayList();
                for (int i = 0; i < f.getNumParams(); i++)
                {
                    p.add(0, stack.pop());
                }
                Vect fResult = f.eval(p);
                stack.push(fResult);
            }
            else
            {
                stack.push(VectSource.create1D(Double.valueOf(token)));
            }
        }
        return stack.pop();
    }

    public void addOperator(OperatorExpression<Vect> operator)
    {
        operators.put(operator.getOper(), operator);
    }

    public void addFunction(FunctionExpression<Vect> function)
    {
        functions.put(function.getName(), function);
    }

    public VectExpression setVariable(String variable, Vect value)
    {
        variables.put(variable, value);
        return this;
    }

    public VectExpression setVariable(String variable, String value)
    {
        if (isNumber(value))
            variables.put(variable, new Vect(value));
        else
        {
            expression = expression.replaceAll("\\b" + variable + "\\b", "(" + value + ")");
            rpn = null;
        }
        return this;
    }

    public VectExpression with(String variable, Vect value)
    {
        return setVariable(variable, value);
    }

    public VectExpression and(String variable, String value)
    {
        return setVariable(variable, value);
    }

    public VectExpression and(String variable, Vect value)
    {
        return setVariable(variable, value);
    }

    public VectExpression with(String variable, String value)
    {
        return setVariable(variable, value);
    }

    private List<String> getRPN()
    {
        if (rpn == null)
        {
            rpn = shuntingYard(this.expression);
        }
        return rpn;
    }
}