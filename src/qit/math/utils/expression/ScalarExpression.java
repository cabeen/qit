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

public class ScalarExpression
{
    private static final char DOT = '.';
    private static final char MINUS = '-';

    public static final BigDecimal PI = new BigDecimal(
            "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");
    public static final BigDecimal DELTA = new BigDecimal(1e-6);
    public static final BigDecimal EPSILON = new BigDecimal(1e-9);

    private MathContext mc = MathContext.DECIMAL32;
    private String expression = null;
    private List<String> rpn = null;
    private Map<String, OperatorExpression<BigDecimal>> operators = new HashMap<>();
    private Map<String, FunctionExpression<BigDecimal>> functions = new HashMap<>();
    private Map<String, BigDecimal> variables = new HashMap<>();

    private class Tokenizer implements Iterator<String>
    {
        private int pos = 0;
        private String input;
        private String previousToken;

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
                while ((Character.isDigit(ch) || ch == DOT)
                        && (pos < input.length()))
                {
                    token.append(input.charAt(pos++));
                    ch = pos == input.length() ? 0 : input.charAt(pos);
                }
            }
            else if (ch == MINUS
                    && Character.isDigit(peekNextChar())
                    && ("(".equals(previousToken) || ",".equals(previousToken)
                    || previousToken == null || operators
                    .containsKey(previousToken)))
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
                while (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '_'
                        && !Character.isWhitespace(ch) && ch != '('
                        && ch != ')' && ch != ',' && (pos < input.length()))
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
                    throw new ExceptionExpression("Unknown operator '" + token
                            + "' at position " + (pos - token.length() + 1));
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
    public ScalarExpression(String expression)
    {
        this.expression = expression;
        addOperator(new OperatorExpression<BigDecimal>("+", 20, true)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                return v1.add(v2, mc);
            }
        });
        addOperator(new OperatorExpression<BigDecimal>("-", 20, true)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                return v1.subtract(v2, mc);
            }
        });
        addOperator(new OperatorExpression<BigDecimal>("*", 30, true)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                return v1.multiply(v2, mc);
            }
        });
        addOperator(new OperatorExpression<BigDecimal>("/", 30, true)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                return v1.divide(v2, mc);
            }
        });
        addOperator(new OperatorExpression<BigDecimal>("//", 30, false)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                return v2.abs().compareTo(DELTA) == -1 ? BigDecimal.ZERO : v1.divide(v2, mc);
            }
        });
        addOperator(new OperatorExpression<BigDecimal>("%", 30, true)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                return v1.remainder(v2, mc);
            }
        });
        addOperator(new OperatorExpression<BigDecimal>("^", 40, false)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                /*-
				 * Thanks to Gene Marin:
				 * http://stackoverflow.com/questions/3579779/how-to-do-a-fractional-power-on-bigdecimal-in-java
				 */
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
                return result;
            }
        });
        addOperator(new OperatorExpression<BigDecimal>("&&", 4, false)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                boolean b1 = !v1.equals(BigDecimal.ZERO);
                boolean b2 = !v2.equals(BigDecimal.ZERO);
                return b1 && b2 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new OperatorExpression<BigDecimal>("||", 2, false)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                boolean b1 = !v1.equals(BigDecimal.ZERO);
                boolean b2 = !v2.equals(BigDecimal.ZERO);
                return b1 || b2 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new OperatorExpression<BigDecimal>(">", 10, false)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                return v1.compareTo(v2) == 1 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new OperatorExpression<BigDecimal>(">=", 10, false)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                return v1.compareTo(v2) >= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new OperatorExpression<BigDecimal>("<", 10, false)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                return v1.compareTo(v2) == -1 ? BigDecimal.ONE
                        : BigDecimal.ZERO;
            }
        });

        addOperator(new OperatorExpression<BigDecimal>("<=", 10, false)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                return v1.compareTo(v2) <= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new OperatorExpression<BigDecimal>("=", 7, false)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                return v1.compareTo(v2) == 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });
        addOperator(new OperatorExpression<BigDecimal>("==", 7, false)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                return operators.get("=").eval(v1, v2);
            }
        });

        addOperator(new OperatorExpression<BigDecimal>("!=", 7, false)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                return v1.compareTo(v2) != 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });
        addOperator(new OperatorExpression<BigDecimal>("<>", 7, false)
        {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2)
            {
                return operators.get("!=").eval(v1, v2);
            }
        });

        addFunction(new FunctionExpression<BigDecimal>("NOT", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                boolean zero = parameters.get(0).compareTo(BigDecimal.ZERO) == 0;
                return zero ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addFunction(new FunctionExpression<BigDecimal>("IF", 3)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                boolean isTrue = !parameters.get(0).equals(BigDecimal.ZERO);
                return isTrue ? parameters.get(1) : parameters.get(2);
            }
        });

        addFunction(new FunctionExpression<BigDecimal>("TIMES", 2)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double v = parameters.get(0).doubleValue();
                double p = parameters.get(1).doubleValue();
                double r = v * p;

                r = Double.isNaN(r) || Double.isInfinite(r) ? 0 : r;
                return new BigDecimal(r, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("PROD", 2)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double v = parameters.get(0).doubleValue();
                double p = parameters.get(1).doubleValue();
                double r = v * p;

                r = Double.isNaN(r) || Double.isInfinite(r) ? 0 : r;
                return new BigDecimal(r, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("PLUS", 2)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double v = parameters.get(0).doubleValue();
                double p = parameters.get(1).doubleValue();
                double r = v + p;

                r = Double.isNaN(r) || Double.isInfinite(r) ? 0 : r;
                return new BigDecimal(r, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("SUM", 2)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double v = parameters.get(0).doubleValue();
                double p = parameters.get(1).doubleValue();
                double r = v + p;

                r = Double.isNaN(r) || Double.isInfinite(r) ? 0 : r;
                return new BigDecimal(r, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("RANDOM", 0)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double d = Math.random();
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("POW", 2)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double v = parameters.get(0).doubleValue();
                double p = parameters.get(1).doubleValue();
                double r = Math.pow(v, p);

                r = Double.isNaN(r) || Double.isInfinite(r) ? 0 : r;
                return new BigDecimal(r, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("SIN", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double d = Math.sin(Math.toRadians(parameters.get(0).doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("COS", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double d = Math.cos(Math.toRadians(parameters.get(0).doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("TAN", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double d = Math.tan(Math.toRadians(parameters.get(0).doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("ASIN", 1)
        { // added by av
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double d = Math.toDegrees(Math.asin(parameters.get(0).doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("ACOS", 1)
        { // added by av
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double d = Math.toDegrees(Math.acos(parameters.get(0).doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("ATAN", 1)
        { // added by av
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double d = Math.toDegrees(Math.atan(parameters.get(0).doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("SINH", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double d = Math.sinh(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("COSH", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double d = Math.cosh(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("TANH", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double d = Math.tanh(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("RAD", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double d = Math.toRadians(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("DEG", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double d = Math.toDegrees(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("MAX", 2)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                BigDecimal v1 = parameters.get(0);
                BigDecimal v2 = parameters.get(1);
                return v1.compareTo(v2) > 0 ? v1 : v2;
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("MIN", 2)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                BigDecimal v1 = parameters.get(0);
                BigDecimal v2 = parameters.get(1);
                return v1.compareTo(v2) < 0 ? v1 : v2;
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("DIVSAFE", 2)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                BigDecimal v1 = parameters.get(0);
                BigDecimal v2 = parameters.get(1);
                return MathUtils.zero(v2.doubleValue()) ? new BigDecimal(0.0) : v1.divide(v2);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("ABS", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                return parameters.get(0).abs(mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("LOG", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double d = Math.log(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("EXP", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double d = Math.exp(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("LOG10", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                double d = Math.log10(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("ROUNDP", 2)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                BigDecimal toRound = parameters.get(0);
                int precision = parameters.get(1).intValue();
                return toRound.setScale(precision, mc.getRoundingMode());
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("ROUND", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                return parameters.get(0).setScale(0, mc.getRoundingMode());
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("FLOOR", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                BigDecimal toRound = parameters.get(0);
                return toRound.setScale(0, RoundingMode.FLOOR);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("CEIL", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
                BigDecimal toRound = parameters.get(0);
                return toRound.setScale(0, RoundingMode.CEILING);
            }
        });
        addFunction(new FunctionExpression<BigDecimal>("SQRT", 1)
        {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters)
            {
				/*
				 * From The Java Programmers Guide To numerical Computing
				 * (Ronald Mak, 2003)
				 */
                BigDecimal x = parameters.get(0);
                if (x.compareTo(BigDecimal.ZERO) == 0)
                {
                    return new BigDecimal(0);
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

                return new BigDecimal(ix, mc.getPrecision());
            }
        });

        variables.put("PI", PI);
        variables.put("DELTA", DELTA);
        variables.put("DEL", DELTA);
        variables.put("EPSILON", EPSILON);
        variables.put("EPS", EPSILON);
        variables.put("TRUE", BigDecimal.ONE);
        variables.put("FALSE", BigDecimal.ZERO);
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
                    throw new ExceptionExpression("Parse error for function '"
                            + lastFunction + "'");
                }
            }
            else if (operators.containsKey(token))
            {
                OperatorExpression<BigDecimal> o1 = operators.get(token);
                String token2 = stack.isEmpty() ? null : stack.peek();
                while (operators.containsKey(token2)
                        && ((o1.isLeftAssoc() && o1.getPrecedence() <= operators
                        .get(token2).getPrecedence()) || (o1
                        .getPrecedence() < operators.get(token2)
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
                        && functions.containsKey(stack.peek().toUpperCase(Locale.ROOT)))
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
                throw new RuntimeException("Unknown operator or function: "
                        + element);
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
    public BigDecimal eval()
    {

        Stack<BigDecimal> stack = new Stack<BigDecimal>();

        for (String token : getRPN())
        {
            if (operators.containsKey(token))
            {
                BigDecimal v1 = stack.pop();
                BigDecimal v2 = stack.pop();
                stack.push(operators.get(token).eval(v2, v1));
            }
            else if (variables.containsKey(token))
            {
                stack.push(variables.get(token).round(mc));
            }
            else if (functions.containsKey(token.toUpperCase(Locale.ROOT)))
            {
                FunctionExpression<BigDecimal> f = functions.get(token.toUpperCase(Locale.ROOT));
                ArrayList<BigDecimal> p = new ArrayList<>();
                for (int i = 0; i < f.getNumParams(); i++)
                {
                    p.add(0, stack.pop());
                }
                BigDecimal fResult = f.eval(p);
                stack.push(fResult);
            }
            else
            {
                stack.push(new BigDecimal(token, mc));
            }
        }
        return stack.pop().stripTrailingZeros();
    }

    /**
     * Adds an operator to the list of supported operators.
     *
     * @param operator The operator to add.
     * @return The previous operator with that name, or <code>null</code> if
     * there was none.
     */
    public void addOperator(OperatorExpression<BigDecimal> operator)
    {
        this.operators.put(operator.getOper(), operator);
    }

    /**
     * Adds a function to the list of supported functions
     *
     * @param function The function to add.
     * @return The previous operator with that name, or <code>null</code> if
     * there was none.
     */
    public void addFunction(FunctionExpression<BigDecimal> function)
    {
        this.functions.put(function.getName(), function);
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable name.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public ScalarExpression setVariable(String variable, BigDecimal value)
    {
        this.variables.put(variable, value);
        return this;
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable to set.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public ScalarExpression setVariable(String variable, String value)
    {
        if (isNumber(value))
        {
            variables.put(variable, new BigDecimal(value));
        }
        else
        {
            expression = expression.replaceAll("\\b" + variable + "\\b", "(" + value + ")");
            rpn = null;
        }
        return this;
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable to set.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public ScalarExpression with(String variable, BigDecimal value)
    {
        return setVariable(variable, value);
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable to set.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public ScalarExpression and(String variable, String value)
    {
        return setVariable(variable, value);
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable to set.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public ScalarExpression and(String variable, BigDecimal value)
    {
        return setVariable(variable, value);
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable to set.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public ScalarExpression with(String variable, String value)
    {
        return setVariable(variable, value);
    }

    /**
     * Get an iterator for this expression, allows iterating over an expression
     * token by token.
     *
     * @return A new iterator instance for this expression.
     */
    public Iterator<String> getExpressionTokenizer()
    {
        return new Tokenizer(this.expression);
    }

    /**
     * Cached access to the RPN notation of this expression, ensures only one
     * calculation of the RPN per expression instance. If no cached instance
     * exists, a new one will be created and put to the cache.
     *
     * @return The cached RPN instance.
     */
    private List<String> getRPN()
    {
        if (rpn == null)
        {
            rpn = shuntingYard(this.expression);
        }
        return rpn;
    }
}