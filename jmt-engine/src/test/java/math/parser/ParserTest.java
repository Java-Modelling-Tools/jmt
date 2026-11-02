/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package math.parser;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import random.engine.MersenneTwister;

class ParserTest {

	private static final double EPS = 1e-9;

	private static double eval(String s) {
		return new Parser(s).getValue();
	}

	@Test
	void multiplicationBindsTighterThanAddition() {
		assertEquals(14.0, eval("2+3*4"), EPS);
		assertEquals(10.0, eval("2*3+4"), EPS);
	}

	@Test
	void parenthesesOverridePrecedence() {
		assertEquals(20.0, eval("(2+3)*4"), EPS);
		assertEquals(-3.0, eval("1-(2+2)"), EPS);
	}

	@Test
	void subtractionAndDivisionAreLeftAssociative() {
		assertEquals(-5.0, eval("2-3-4"), EPS);
		assertEquals(2.0, eval("16/4/2"), EPS);
	}

	@Test
	void powerIsRightAssociative() {
		assertEquals(512.0, eval("2^3^2"), EPS); // 2^(3^2)
	}

	@Test
	void powerBindsTighterThanUnaryMinus() {
		assertEquals(-4.0, eval("-2^2"), EPS);
		assertEquals(4.0, eval("(-2)^2"), EPS);
	}

	@Test
	void unaryMinusAfterOperatorIsAccepted() {
		assertEquals(-6.0, eval("2*-3"), EPS);
	}

	@Test
	void moduloOperatorComputesRemainder() {
		assertEquals(1.0, eval("7%3"), EPS);
	}

	@Test
	void numbersSupportDecimalsAndExponents() {
		assertEquals(0.5, eval("0.5"), EPS);
		assertEquals(2000.0, eval("2e3"), EPS);
		assertEquals(0.025, eval("2.5e-2"), EPS);
	}

	@Test
	void builtInConstantsAreCaseInsensitive() {
		assertEquals(Math.PI, eval("pi"), EPS);
		assertEquals(Math.E, eval("E"), EPS);
	}

	@Test
	void functionsAreEvaluated() {
		assertEquals(0.0, eval("sin(0)"), EPS);
		assertEquals(1.0, eval("COS(0)"), EPS);
		assertEquals(3.0, eval("sqrt(9)"), EPS);
		assertEquals(5.0, eval("abs(-5)"), EPS);
		assertEquals(Math.E, eval("exp(1)"), EPS);
		assertEquals(1.0, eval("ln(e)"), EPS);
		assertEquals(2.0, eval("log(100)"), EPS);
		assertEquals(3.0, eval("log2(8)"), EPS);
		assertEquals(24.0, eval("gamma(5)"), 1e-6); // (5-1)!
		assertEquals(0.0, eval("erf(0)"), EPS);
		assertEquals(1.0, eval("erfc(0)"), EPS);
		assertEquals(Math.PI / 4, eval("atan(1)"), EPS);
		assertEquals(0.0, eval("sinh(0)"), EPS);
		assertEquals(1.0, eval("cosh(0)"), EPS);
		assertEquals(0.0, eval("tanh(0)"), EPS);
		assertEquals(0.0, eval("asinh(0)"), EPS);
		assertEquals(0.0, eval("acosh(1)"), EPS);
		assertEquals(0.0, eval("atanh(0)"), EPS);
		assertEquals(0.0, eval("asin(0)"), EPS);
		assertEquals(0.0, eval("acos(1)"), EPS);
		assertEquals(0.0, eval("tan(0)"), EPS);
		assertEquals(1.0, eval("cot(pi/4)"), 1e-6);
	}

	@Test
	void randFunctionScalesEngineOutput() {
		Parser p = new Parser("rand(10)");
		p.setEngine(new MersenneTwister(42));
		double v = p.getValue();
		assertTrue(v >= 0.0 && v < 10.0);
		assertSame(p.getEngine(), p.getEngine());
	}

	@Test
	void implicitMultiplicationBetweenNumberAndVariable() {
		Parser p = new Parser("2x+1");
		p.setVariable("x", 5);
		assertEquals(11.0, p.getValue(), EPS);
	}

	@Test
	void implicitMultiplicationBetweenNumberAndFunction() {
		assertEquals(6.0, eval("2sqrt(9)"), EPS);
	}

	@Test
	void variablesDefaultToZeroUnlessErrorRequested() {
		assertEquals(3.0, eval("x+3"), EPS);
	}

	@Test
	void uninitialisedVariableThrowsEvaluationExceptionWhenErrorRequested() {
		Parser p = new Parser("x+3", true);
		EvaluationException e = assertThrows(EvaluationException.class, p::getValue);
		assertTrue(e.getMessage().contains("'x'"));
	}

	@Test
	void setVariableUpdatesValueAndIsCaseSensitive() {
		Parser p = new Parser("x*y", true);
		p.setVariable("x", 3);
		p.setVariable("y", 4);
		assertEquals(12.0, p.getValue(), EPS);
		p.setVariable("x", 5);
		assertEquals(20.0, p.getValue(), EPS);
		assertThrows(EvaluationException.class, () -> {
			Parser q = new Parser("X", true);
			q.setVariable("x", 1);
			q.getValue();
		});
	}

	@Test
	void parsedFunctionsAndVariablesAreCollected() {
		Parser p = new Parser("sin(x)+cos(y)*x");
		assertEquals(new java.util.HashSet<String>(java.util.Arrays.asList("sin", "cos")), p.getParsedFunctions());
		assertEquals(new java.util.HashSet<String>(java.util.Arrays.asList("x", "y")), p.getParsedVariables());
	}

	@Test
	void unsupportedCharactersAreStrippedFromInput() {
		Parser p = new Parser(" 2 $+ 3 ");
		assertEquals("2+3", p.getInputString());
		assertEquals(5.0, p.getValue(), EPS);
	}

	@Test
	void getExpressionAddsOnlyNecessaryParentheses() {
		assertEquals("(2+3)*4", new Parser("(2+3)*4").getExpression());
		assertEquals("2+3*4", new Parser("2+(3*4)").getExpression());
		assertEquals("1-(2+3)", new Parser("1-(2+3)").getExpression());
		assertEquals("-x", new Parser("-x").getExpression());
		assertEquals("-(2+3)", new Parser("-(2+3)").getExpression());
		assertEquals("sin(x)", new Parser("sin(x)").getExpression());
		assertEquals("(a+b)/c", new Parser("(a+b)/c").getExpression());
		assertEquals("a/(b*c)", new Parser("a/(b*c)").getExpression());
		assertEquals("pi*2", new Parser("pi*2").getExpression());
		assertEquals("(a%b)^2", new Parser("(a%b)^2").getExpression());
	}

	@Test
	void getTreeIsCachedAcrossCalls() {
		Parser p = new Parser("1+2");
		assertSame(p.getTree(), p.getTree());
	}

	private static void assertParseError(String expr) {
		assertThrows(ParseError.class, () -> new Parser(expr).getValue(), expr);
	}

	@Test
	void malformedExpressionsRaiseParseError() {
		assertParseError("2+");
		assertParseError("*3");
		assertParseError("(1+2");
		assertParseError("1+2)");
		assertParseError("()");
		assertParseError("sin+3"); // function without argument list
		assertParseError("sin(3");
		assertParseError("2pi");
		assertParseError("2^-1");
		assertParseError("1..2");
		assertParseError("");
	}

	@Test
	void parseErrorReportsPositionOfFailure() {
		ParseError e = assertThrows(ParseError.class, () -> new Parser("(1+2").getTree());
		assertTrue(e.getPosition() > 0);
		assertTrue(e.getMessage().contains("expected ')'"));
	}

	@Test
	void setVariableOnMalformedExpressionRaisesParseError() {
		assertThrows(ParseError.class, () -> new Parser("1+").setVariable("x", 1));
	}
}
