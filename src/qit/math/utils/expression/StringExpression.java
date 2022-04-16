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

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import qit.math.utils.MathUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

public class StringExpression
{
    private static final char DOT = '.';
    private static final char MINUS = '-';

    public static final String TRUE = "1";
    public static final String FALSE = "0";
    public static final String PI = "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679";

    private MathContext mc = MathContext.DECIMAL32;
    private String expression = null;
    private List<String> rpn = null;
    private Map<String, OperatorExpression<String>> operators = Maps.newHashMap();
    private Map<String, FunctionExpression<String>> functions = Maps.newHashMap();
    private Map<String, String> variables = Maps.newHashMap();

    private class StringTokenizer implements Iterator<String>
    {
        /* Actual position in expression string. */
        private int pos = 0;

        /* The original input expression. */
        private String input;

        /* The previous token or <code>null</code> if none. */
        private String previousToken;

        public StringTokenizer(String input)
        {
            this.input = input.trim();
        }

        @Override
        public boolean hasNext()
        {
            return (this.pos < this.input.length());
        }

        private char peekNextChar()
        {
            if (this.pos < (this.input.length() - 1))
            {
                return this.input.charAt(this.pos + 1);
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
            if (this.pos >= this.input.length())
            {
                return this.previousToken = null;
            }

            char ch = this.input.charAt(this.pos);
            while (Character.isWhitespace(ch) && this.pos < this.input.length())
            {
                ch = this.input.charAt(++this.pos);
            }

            if (ch == '\'')
            {
                this.pos += 1;
                ch = this.pos == this.input.length() ? 0 : this.input.charAt(this.pos);
                while ((ch != '\'') && (this.pos < this.input.length()))
                {
                    token.append(this.input.charAt(this.pos));
                    this.pos += 1;
                    ch = this.pos == this.input.length() ? 0 : this.input.charAt(this.pos);
                }
                this.pos += 1;
            }
            else if (ch == '"')
            {
                this.pos += 1;
                ch = this.pos == this.input.length() ? 0 : this.input.charAt(this.pos);
                while ((ch != '"') && (this.pos < this.input.length()))
                {
                    token.append(this.input.charAt(this.pos));
                    this.pos += 1;
                    ch = this.pos == this.input.length() ? 0 : this.input.charAt(this.pos);
                }
                this.pos += 1;
            }
            else if (Character.isDigit(ch))
            {
                while ((Character.isDigit(ch) || ch == DOT)
                        && (this.pos < this.input.length()))
                {
                    token.append(this.input.charAt(this.pos++));
                    ch = this.pos == this.input.length() ? 0 : this.input.charAt(this.pos);
                }
            }
            else if (ch == MINUS
                    && Character.isDigit(peekNextChar())
                    && ("(".equals(this.previousToken)
                    || ",".equals(this.previousToken)
                    || this.previousToken == null
                    || StringExpression.this.operators.containsKey(this.previousToken)))
            {
                token.append(MINUS);
                this.pos++;
                token.append(next());
            }
            else if (Character.isLetter(ch) || (ch == '_'))
            {
                while ((Character.isLetter(ch) || Character.isDigit(ch) || (ch == '_')) && (this.pos < this.input.length()))
                {
                    token.append(this.input.charAt(this.pos++));
                    ch = this.pos == this.input.length() ? 0 : this.input.charAt(this.pos);
                }
            }
            else if (ch == '(' || ch == ')' || ch == ',')
            {
                token.append(ch);
                this.pos++;
            }
            else
            {
                while (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '_'
                        && !Character.isWhitespace(ch) && ch != '('
                        && ch != ')' && ch != ',' && (this.pos < this.input.length()))
                {
                    token.append(this.input.charAt(this.pos));
                    this.pos++;
                    ch = this.pos == this.input.length() ? 0 : this.input.charAt(this.pos);
                    if (ch == MINUS)
                    {
                        break;
                    }
                }
                if (!StringExpression.this.operators.containsKey(token.toString()))
                {
                    throw new ExceptionExpression("Unknown operator '" + token
                            + "' at position " + (this.pos - token.length() + 1));
                }
            }
            return this.previousToken = token.toString();
        }

        @Override
        public void remove()
        {
            throw new ExceptionExpression("remove() not supported");
        }

        public int getPos()
        {
            return pos;
        }
    }

    public StringExpression(String expression)
    {
        this.expression = expression;
        addOperator(new OperatorExpression<String>("+", 20, true)
        {
            @Override
            public String eval(String v1, String v2)
            {
                if (StringUtils.isNumeric(v1) && StringUtils.isNumeric(v2))
                {
                    return new Double(Double.valueOf(v1) + Double.valueOf(v2)).toString();
                }
                else
                {
                    return v1 + v2;
                }
            }
        });
        addOperator(new OperatorExpression<String>("-", 20, true)
        {
            @Override
            public String eval(String v1, String v2)
            {
                return new Double(Double.valueOf(v1) - Double.valueOf(v2)).toString();
            }
        });
        addOperator(new OperatorExpression<String>("*", 30, true)
        {
            @Override
            public String eval(String v1, String v2)
            {
                return new Double(Double.valueOf(v1) * Double.valueOf(v2)).toString();
            }
        });
        addOperator(new OperatorExpression<String>("/", 30, true)
        {
            @Override
            public String eval(String v1, String v2)
            {
                return new Double(Double.valueOf(v1) / Double.valueOf(v2)).toString();
            }
        });
        addOperator(new OperatorExpression<String>("%", 30, true)
        {
            @Override
            public String eval(String v1, String v2)
            {
                return new Double(Double.valueOf(v1) % Double.valueOf(v2)).toString();
            }
        });
        addOperator(new OperatorExpression<String>("^", 40, false)
        {
            @Override
            public String eval(String v1t, String v2t)
            {
                /*-
				 * Thanks to Gene Marin:
				 * http://stackoverflow.com/questions/3579779/how-to-do-a-fractional-power-on-String-in-java
				 */

                BigDecimal v1 = BigDecimal.valueOf(Double.valueOf(v1t));
                BigDecimal v2 = BigDecimal.valueOf(Double.valueOf(v2t));

                int signOf2 = v2.signum();
                double dn1 = v1.doubleValue();
                v2 = v2.multiply(new BigDecimal(signOf2)); // n2 is now positive
                BigDecimal remainderOf2 = v2.remainder(BigDecimal.ONE);
                BigDecimal n2IntPart = v2.subtract(remainderOf2);
                BigDecimal intPow = v1.pow(n2IntPart.intValueExact(), mc);
                BigDecimal doublePow = new BigDecimal(Math.pow(dn1,
                        remainderOf2.doubleValue()));

                BigDecimal result = intPow.multiply(doublePow, mc);
                if (signOf2 == -1)
                {
                    result = BigDecimal.ONE.divide(result, mc.getPrecision(),
                            RoundingMode.HALF_UP);
                }
                return result.toString();
            }
        });
        addOperator(new OperatorExpression<String>("&&", 4, false)
        {
            @Override
            public String eval(String v1, String v2)
            {
                return test(v1) && test(v2) ? StringExpression.TRUE : StringExpression.FALSE;
            }
        });

        addOperator(new OperatorExpression<String>("||", 2, false)
        {
            @Override
            public String eval(String v1, String v2)
            {
                return test(v1) || test(v2) ? StringExpression.TRUE : StringExpression.FALSE;
            }
        });

        addOperator(new OperatorExpression<String>(">", 10, false)
        {
            @Override
            public String eval(String v1t, String v2t)
            {
                BigDecimal v1 = BigDecimal.valueOf(Double.valueOf(v1t));
                BigDecimal v2 = BigDecimal.valueOf(Double.valueOf(v2t));
                return v1.compareTo(v2) == 1 ? StringExpression.TRUE : StringExpression.FALSE;
            }
        });

        addOperator(new OperatorExpression<String>(">=", 10, false)
        {
            @Override
            public String eval(String v1t, String v2t)
            {
                BigDecimal v1 = BigDecimal.valueOf(Double.valueOf(v1t));
                BigDecimal v2 = BigDecimal.valueOf(Double.valueOf(v2t));
                return v1.compareTo(v2) >= 0 ? StringExpression.TRUE : StringExpression.FALSE;
            }
        });

        addOperator(new OperatorExpression<String>("<", 10, false)
        {
            @Override
            public String eval(String v1t, String v2t)
            {
                BigDecimal v1 = BigDecimal.valueOf(Double.valueOf(v1t));
                BigDecimal v2 = BigDecimal.valueOf(Double.valueOf(v2t));
                return v1.compareTo(v2) == -1 ? StringExpression.TRUE : StringExpression.FALSE;
            }
        });

        addOperator(new OperatorExpression<String>("<=", 10, false)
        {
            @Override
            public String eval(String v1t, String v2t)
            {
                BigDecimal v1 = BigDecimal.valueOf(Double.valueOf(v1t));
                BigDecimal v2 = BigDecimal.valueOf(Double.valueOf(v2t));
                return v1.compareTo(v2) <= 0 ? StringExpression.TRUE : StringExpression.FALSE;
            }
        });

        addOperator(new OperatorExpression<String>("=", 7, false)
        {
            @Override
            public String eval(String v1t, String v2t)
            {
                if (StringUtils.isNumeric(v1t) && StringUtils.isNumeric(v2t))
                {
                    BigDecimal v1 = BigDecimal.valueOf(Double.valueOf(v1t));
                    BigDecimal v2 = BigDecimal.valueOf(Double.valueOf(v2t));
                    return v1.compareTo(v2) == 0 ? StringExpression.TRUE : StringExpression.FALSE;
                }
                else
                {
                    return v1t.equals(v2t) ? StringExpression.TRUE : StringExpression.FALSE;
                }
            }
        });
        addOperator(new OperatorExpression<String>("==", 7, false)
        {
            @Override
            public String eval(String v1, String v2)
            {
                return operators.get("=").eval(v1, v2);
            }
        });

        addOperator(new OperatorExpression<String>("!=", 7, false)
        {
            @Override
            public String eval(String v1t, String v2t)
            {
                if (StringUtils.isNumeric(v1t) && StringUtils.isNumeric(v2t))
                {
                    BigDecimal v1 = BigDecimal.valueOf(Double.valueOf(v1t));
                    BigDecimal v2 = BigDecimal.valueOf(Double.valueOf(v2t));
                    return v1.compareTo(v2) != 0 ? StringExpression.TRUE : StringExpression.FALSE;
                }
                else
                {
                    return !v1t.equals(v2t) ? StringExpression.TRUE : StringExpression.FALSE;
                }
            }
        });
        addOperator(new OperatorExpression<String>("<String>", 7, false)
        {
            @Override
            public String eval(String v1, String v2)
            {
                return operators.get("!=").eval(v1, v2);
            }
        });

        addFunction(new FunctionExpression<String>("NOT", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                return test(parameters.get(0)) ? StringExpression.FALSE : StringExpression.TRUE;
            }
        });

        addFunction(new FunctionExpression<String>("IF", 3)
        {
            @Override
            public String eval(List<String> parameters)
            {
                return test(parameters.get(0)) ? parameters.get(1) : parameters.get(2);
            }
        });

        addFunction(new FunctionExpression<String>("RANDOM", 0)
        {
            @Override
            public String eval(List<String> parameters)
            {
                double d = Math.random();
                return new BigDecimal(d, mc).toString();
            }
        });
        addFunction(new FunctionExpression<String>("SIN", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                double d = Math.sin(Math.toRadians(Double.valueOf(parameters.get(0))));
                return new BigDecimal(d, mc).toString();
            }
        });
        addFunction(new FunctionExpression<String>("COS", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                double d = Math.cos(Math.toRadians(Double.valueOf(parameters.get(0))));
                return new BigDecimal(d, mc).toString();
            }
        });
        addFunction(new FunctionExpression<String>("TAN", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                double d = Math.tan(Math.toRadians(Double.valueOf(parameters.get(0))));
                return new BigDecimal(d, mc).toString();
            }
        });
        addFunction(new FunctionExpression<String>("ASIN", 1)
        { // added by av
            @Override
            public String eval(List<String> parameters)
            {
                double d = Math.toDegrees(Math.asin(Double.valueOf(parameters.get(0))));
                return new BigDecimal(d, mc).toString();
            }
        });
        addFunction(new FunctionExpression<String>("ACOS", 1)
        { // added by av
            @Override
            public String eval(List<String> parameters)
            {
                double d = Math.toDegrees(Math.acos(Double.valueOf(parameters.get(0))));
                return new BigDecimal(d, mc).toString();
            }
        });
        addFunction(new FunctionExpression<String>("ATAN", 1)
        { // added by av
            @Override
            public String eval(List<String> parameters)
            {
                double d = Math.toDegrees(Math.atan(Double.valueOf(parameters.get(0))));
                return new BigDecimal(d, mc).toString();
            }
        });
        addFunction(new FunctionExpression<String>("SINH", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                double d = Math.sinh(Double.valueOf(parameters.get(0)));
                return new BigDecimal(d, mc).toString();
            }
        });
        addFunction(new FunctionExpression<String>("COSH", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                double d = Math.cosh(Double.valueOf(parameters.get(0)));
                return new BigDecimal(d, mc).toString();
            }
        });
        addFunction(new FunctionExpression<String>("TANH", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                double d = Math.tanh(Double.valueOf(parameters.get(0)));
                return new BigDecimal(d, mc).toString();
            }
        });
        addFunction(new FunctionExpression<String>("RAD", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                double d = Math.toRadians(Double.valueOf(parameters.get(0)));
                return new BigDecimal(d, mc).toString();
            }
        });
        addFunction(new FunctionExpression<String>("DEG", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                double d = Math.toDegrees(Double.valueOf(parameters.get(0)));
                return new BigDecimal(d, mc).toString();
            }
        });
        addFunction(new FunctionExpression<String>("MAX", 2)
        {
            @Override
            public String eval(List<String> parameters)
            {
                BigDecimal v1 = BigDecimal.valueOf(Double.valueOf(parameters.get(0)));
                BigDecimal v2 = BigDecimal.valueOf(Double.valueOf(parameters.get(1)));

                return v1.compareTo(v2) > 0 ? parameters.get(0) : parameters.get(1);
            }
        });
        addFunction(new FunctionExpression<String>("MIN", 2)
        {
            @Override
            public String eval(List<String> parameters)
            {
                BigDecimal v1 = BigDecimal.valueOf(Double.valueOf(parameters.get(0)));
                BigDecimal v2 = BigDecimal.valueOf(Double.valueOf(parameters.get(1)));

                return v1.compareTo(v2) < 0 ? parameters.get(0) : parameters.get(1);
            }
        });
        addFunction(new FunctionExpression<String>("ABS", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {

                BigDecimal v1 = BigDecimal.valueOf(Double.valueOf(parameters.get(0)));
                return v1.abs().toString();
            }
        });
        addFunction(new FunctionExpression<String>("LOG", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                double d = Math.log(Double.valueOf(parameters.get(0)));
                return new BigDecimal(d, mc).toString();
            }
        });
        addFunction(new FunctionExpression<String>("EXP", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                double d = Math.exp(Double.valueOf(parameters.get(0)));
                return new BigDecimal(d, mc).toString();
            }
        });
        addFunction(new FunctionExpression<String>("LOG10", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                double d = Math.log10(Double.valueOf(parameters.get(0)));
                return new BigDecimal(d, mc).toString();
            }
        });
        addFunction(new FunctionExpression<String>("ROUND", 2)
        {
            @Override
            public String eval(List<String> parameters)
            {
                BigDecimal toRound = BigDecimal.valueOf(Double.valueOf(parameters.get(0)));
                int precision = BigDecimal.valueOf(Double.valueOf(parameters.get(1))).intValue();
                return toRound.setScale(precision, mc.getRoundingMode()).toString();
            }
        });
        addFunction(new FunctionExpression<String>("FLOOR", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                BigDecimal toRound = BigDecimal.valueOf(Double.valueOf(parameters.get(0)));
                return toRound.setScale(0, RoundingMode.FLOOR).toString();
            }
        });
        addFunction(new FunctionExpression<String>("CEIL", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                BigDecimal toRound = BigDecimal.valueOf(Double.valueOf(parameters.get(0)));
                return toRound.setScale(0, RoundingMode.CEILING).toString();
            }
        });
        addFunction(new FunctionExpression<String>("SQRT", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
				/*
				 * From The Java Programmers Guide To numerical Computing
				 * (Ronald Mak, 2003)
				 */

                BigDecimal x = BigDecimal.valueOf(Double.valueOf(parameters.get(0)));
                if (x.compareTo(BigDecimal.ZERO) == 0)
                {
                    return new BigDecimal(0).toString();
                }
                if (x.signum() < 0)
                {
                    throw new ExceptionExpression(
                            "Argument to SQRT() function must not be negative");
                }
                BigInteger n = x.movePointRight(mc.getPrecision() << 1)
                        .toBigInteger();

                int bits = (n.bitLength() + 1) >> 1;
                BigInteger ix = n.shiftRight(bits);
                BigInteger ixPrev;

                do
                {
                    ixPrev = ix;
                    ix = ix.add(n.divide(ix)).shiftRight(1);
                    // Give other threads a chance to work;
                    Thread.yield();
                }
                while (ix.compareTo(ixPrev) != 0);

                return new BigDecimal(ix, mc.getPrecision()).toString();
            }
        });
        addFunction(new FunctionExpression<String>("CAT", 2)
        {
            @Override
            public String eval(List<String> parameters)
            {
                String a = parameters.get(0);
                String b = parameters.get(1);
                return a + b;
            }
        });

        for (String name : new String[]{"EQ", "EQUAL", "EQUALS"})
        {
            addFunction(new FunctionExpression<String>(name, 2)
            {
                @Override
                public String eval(List<String> parameters)
                {
                    String a = parameters.get(0);
                    String b = parameters.get(1);

                    return operators.get("=").eval(a, b);
                }
            });
        }
        addFunction(new FunctionExpression<String>("CLOSE", 2)
        {
            @Override
            public String eval(List<String> parameters)
            {
                String a = parameters.get(0);
                String b = parameters.get(1);

                if (StringUtils.isNumeric(a) && StringUtils.isNumeric(b))
                {
                    return Math.abs(Double.valueOf(a) - Double.valueOf(b)) < 1e-6 ? StringExpression.TRUE : StringExpression.FALSE;
                }
                else
                {
                    return a.equals(b) ? StringExpression.TRUE : StringExpression.FALSE;
                }
            }
        });
        addFunction(new FunctionExpression<String>("WITHIN", 3)
        {
            @Override
            public String eval(List<String> parameters)
            {
                String a = parameters.get(0);
                String b = parameters.get(1);
                String c = parameters.get(2);

                if (StringUtils.isNumeric(a) && StringUtils.isNumeric(b) && StringUtils.isNumeric(c))
                {
                    return Math.abs(Double.valueOf(a) - Double.valueOf(b)) < Double.valueOf(c) ? StringExpression.TRUE : StringExpression.FALSE;
                }
                else
                {
                    return a.equals(b) ? StringExpression.TRUE : StringExpression.FALSE;
                }
            }
        });
        addFunction(new FunctionExpression<String>("STARTSWITH", 2)
        {
            @Override
            public String eval(List<String> parameters)
            {
                String a = parameters.get(0);
                String b = parameters.get(1);
                return a.startsWith(b) ? StringExpression.TRUE : StringExpression.FALSE;
            }
        });
        addFunction(new FunctionExpression<String>("ENDSWITH", 2)
        {
            @Override
            public String eval(List<String> parameters)
            {
                String a = parameters.get(0);
                String b = parameters.get(1);
                return a.endsWith(b) ? StringExpression.TRUE : StringExpression.FALSE;
            }
        });
        addFunction(new FunctionExpression<String>("CONTAINS", 2)
        {
            @Override
            public String eval(List<String> parameters)
            {
                String a = parameters.get(0);
                String b = parameters.get(1);
                return a.contains(b) ? StringExpression.TRUE : StringExpression.FALSE;
            }
        });
        addFunction(new FunctionExpression<String>("LOWER", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                String a = parameters.get(0);
                return a.toLowerCase();
            }
        });
        addFunction(new FunctionExpression<String>("UPPER", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                String a = parameters.get(0);
                return a.toUpperCase();
            }
        });
        addFunction(new FunctionExpression<String>("LENGTH", 1)
        {
            @Override
            public String eval(List<String> parameters)
            {
                String a = parameters.get(0);
                return String.valueOf(a.length());
            }
        });

        addFunction(new FunctionExpression<String>("SUBSTRING", 3)
        {
            @Override
            public String eval(List<String> parameters)
            {
                String a = parameters.get(0);
                int b = Integer.valueOf(parameters.get(2));
                int c = Integer.valueOf(parameters.get(2));
                return a.substring(b, c);
            }
        });

        variables.put("PI", PI);
        variables.put("TRUE", StringExpression.TRUE);
        variables.put("FALSE", StringExpression.FALSE);
    }

    public static boolean test(String value)
    {
        if (StringUtils.isNumeric(value))
        {
            double v = Double.valueOf(value);
            return !MathUtils.zero(v);
        }
        else
        {
            return value.toLowerCase().equals("t") || value.toLowerCase().equals("true");
        }
    }

    private boolean isNumber(String st)
    {
        if (st.charAt(0) == MINUS && st.length() == 1)
        {
            return false;
        }
        for (char ch : st.toCharArray())
        {
            if (!Character.isDigit(ch) && ch != MINUS
                    && ch != DOT)
            {
                return false;
            }
        }
        return true;
    }

    private List<String> shuntingYard(String expression)
    {
        List<String> outputQueue = new ArrayList<String>();
        Stack<String> stack = new Stack<String>();

        StringTokenizer tokenizer = new StringTokenizer(expression);

        String lastFunction = null;
        String previousToken = null;
        while (tokenizer.hasNext())
        {
            String token = tokenizer.next();
            if (isNumber(token))
            {
                outputQueue.add(token);
            }
            else if (this.variables.containsKey(token))
            {
                outputQueue.add(token);
            }
            else if (this.functions.containsKey(token.toUpperCase(Locale.ROOT)))
            {
                stack.push(token);
                lastFunction = token;
            }
            else if (",".equals(token))
            {
                while (!stack.isEmpty() && !"(".equals(stack.peek()))
                {
                    outputQueue.add(stack.pop());
                }
                if (stack.isEmpty())
                {
                    throw new ExceptionExpression("Parse error for function '"
                            + lastFunction + "'");
                }
            }
            else if (this.operators.containsKey(token))
            {
                OperatorExpression<String> o1 = this.operators.get(token);
                String token2 = stack.isEmpty() ? null : stack.peek();
                while (this.operators.containsKey(token2)
                        && ((o1.isLeftAssoc() && o1.getPrecedence() <= this.operators
                        .get(token2).getPrecedence()) || (o1
                        .getPrecedence() < this.operators.get(token2)
                        .getPrecedence())))
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
                if (!stack.isEmpty()
                        && this.functions.containsKey(stack.peek().toUpperCase(Locale.ROOT)))
                {
                    outputQueue.add(stack.pop());
                }
            }
            else
            {
                outputQueue.add(token);
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
            if (!this.operators.containsKey(element))
            {
                throw new RuntimeException("Unknown operator or function: "
                        + element);
            }
            outputQueue.add(element);
        }
        return outputQueue;
    }

    public String eval()
    {
        Stack<String> stack = new Stack<String>();

        for (String token : getRPN())
        {
            if (operators.containsKey(token))
            {
                String v1 = stack.pop();
                String v2 = stack.pop();
                stack.push(operators.get(token).eval(v2, v1));
            }
            else if (variables.containsKey(token))
            {
                stack.push(variables.get(token));
            }
            else if (functions.containsKey(token.toUpperCase(Locale.ROOT)))
            {
                FunctionExpression<String> f = functions.get(token.toUpperCase(Locale.ROOT));
                ArrayList<String> p = new ArrayList<String>(f.getNumParams());
                for (int i = 0; i < f.getNumParams(); i++)
                {
                    p.add(0, stack.pop());
                }
                String fResult = f.eval(p);
                stack.push(fResult);
            }
            else
            {
                stack.push(token);
            }
        }
        return stack.pop();
    }

    public StringExpression addOperator(OperatorExpression<String> operator)
    {
        this.operators.put(operator.getOper(), operator);
        return this;
    }

    public StringExpression addFunction(FunctionExpression<String> function)
    {
        this.functions.put(function.getName(), function);
        return this;
    }

    public StringExpression with(String variable, String value)
    {
        this.variables.put(variable, value);
        return this;
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