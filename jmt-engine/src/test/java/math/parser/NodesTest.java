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

class NodesTest {

	private static final double EPS = 1e-12;

	@Test
	void constantNodeByNameResolvesBuiltIn() {
		ConstantNode pi = new ConstantNode("pi");
		assertEquals(Math.PI, pi.getValue(), EPS);
		assertEquals("pi", pi.toString());
		assertEquals(ExpressionNode.CONSTANT_NODE, pi.getType());
		assertThrows(IllegalArgumentException.class, () -> new ConstantNode("tau"));
	}

	@Test
	void constantNodeFormatsIntegersWithoutDecimalPoint() {
		assertEquals("3", new ConstantNode(3.0).toString());
		assertEquals("2.5", new ConstantNode(2.5).toString());
		assertEquals("e", new ConstantNode(1).toString()); // int ctor selects built-in constant by index
	}

	@Test
	void leafNodesHaveCountOneDepthOneAndNoChildren() {
		ExpressionNode c = new ConstantNode(1.0);
		ExpressionNode v = new VariableNode("x", false);
		for (ExpressionNode n : new ExpressionNode[] { c, v }) {
			assertEquals(1, n.count());
			assertEquals(1, n.getDepth());
			assertEquals(0, n.getChildrenNodes().length);
		}
		assertEquals(ExpressionNode.VARIABLE_NODE, v.getType());
		assertEquals("x", v.getSubtype());
	}

	@Test
	void constantCloneIsIndependentCopyWithSameValue() {
		ConstantNode c = new ConstantNode(4.5);
		c.setVariable("x", 1); // no-op
		assertEquals(4.5, ((ConstantNode) c.clone()).getValue(), EPS);
	}

	@Test
	void variableCloneKeepsValueAndUninitialisedFlag() {
		VariableNode v = new VariableNode("x", true);
		assertThrows(EvaluationException.class, () -> ((VariableNode) v.clone()).getValue());
		v.setVariable("y", 3); // different name: ignored
		assertThrows(EvaluationException.class, v::getValue);
		v.setVariable("x", 3);
		assertEquals(3.0, ((VariableNode) v.clone()).getValue(), EPS);
	}

	@Test
	void operatorNodeEvaluatesEachOperator() {
		ExpressionNode a = new ConstantNode(7.0);
		ExpressionNode b = new ConstantNode(2.0);
		assertEquals(9.0, new OperatorNode(a, b, '+').getValue(), EPS);
		assertEquals(5.0, new OperatorNode(a, b, '-').getValue(), EPS);
		assertEquals(14.0, new OperatorNode(a, b, '*').getValue(), EPS);
		assertEquals(3.5, new OperatorNode(a, b, '/').getValue(), EPS);
		assertEquals(1.0, new OperatorNode(a, b, '%').getValue(), EPS);
		assertEquals(49.0, new OperatorNode(a, b, '^').getValue(), EPS);
		assertEquals(0.0, new OperatorNode(a, b, '?').getValue(), 0.0); // unknown operator: defined as 0
	}

	@Test
	void divisionByZeroFollowsIeee() {
		assertTrue(Double.isInfinite(new OperatorNode(new ConstantNode(1.0), new ConstantNode(0.0), '/').getValue()));
	}

	@Test
	void operatorNodeStructureQueries() {
		OperatorNode n = new OperatorNode(new ConstantNode(1.0), new OperatorNode(new ConstantNode(2.0), new VariableNode("x", false), '*'), '+');
		assertEquals(5, n.count());
		assertEquals(3, n.getDepth());
		assertEquals("+", n.getSubtype());
		assertEquals(ExpressionNode.OPERATOR_NODE, n.getType());
		assertEquals(2, n.getChildrenNodes().length);
		n.setVariable("x", 10);
		assertEquals(21.0, n.getValue(), EPS);
		OperatorNode copy = (OperatorNode) n.clone();
		copy.setVariable("x", 0);
		assertEquals(1.0, copy.getValue(), EPS);
		assertEquals(21.0, n.getValue(), EPS); // original unaffected
		assertEquals("1+2*x", n.toString());
	}

	@Test
	void operatorToStringBracketsFollowPrecedence() {
		ExpressionNode one = new ConstantNode(1.0), two = new ConstantNode(2.0), three = new ConstantNode(3.0);
		ExpressionNode sum = new OperatorNode(one, two, '+');
		assertEquals("(1+2)*3", new OperatorNode(sum, three, '*').toString());
		assertEquals("3/(1+2)", new OperatorNode(three, sum, '/').toString());
		assertEquals("3-(1+2)", new OperatorNode(three, sum, '-').toString());
		assertEquals("1+2-3", new OperatorNode(sum, three, '-').toString());
		assertEquals("(1+2)%3", new OperatorNode(sum, three, '%').toString());
		assertEquals("(1+2)^3", new OperatorNode(sum, three, '^').toString());
		ExpressionNode prod = new OperatorNode(one, two, '*');
		assertEquals("1*2/3", new OperatorNode(prod, three, '/').toString());
	}

	@Test
	void functionNodeByNameAndByIndexAgree() {
		FunctionNode byName = new FunctionNode(new ConstantNode(9.0), "sqrt");
		FunctionNode byIndex = new FunctionNode(new ConstantNode(9.0), 17);
		assertEquals("sqrt", byName.getSubtype());
		assertEquals(byName.getSubtype(), byIndex.getSubtype());
		assertEquals(3.0, byName.getValue(), EPS);
		assertEquals(ExpressionNode.FUNCTION_NODE, byName.getType());
		assertEquals(2, byName.count());
		assertEquals(2, byName.getDepth());
		assertEquals(1, byName.getChildrenNodes().length);
		assertEquals("sqrt(9)", byName.toString());
	}

	@Test
	void functionNodeRejectsUnknownName() {
		assertThrows(IllegalArgumentException.class, () -> new FunctionNode(new ConstantNode(1.0), "nope"));
	}

	@Test
	void functionNodeCloneAndSetVariablePropagate() {
		Parser p = new Parser("sin(x)");
		FunctionNode f = (FunctionNode) p.getTree();
		FunctionNode copy = (FunctionNode) f.clone();
		copy.setVariable("x", Math.PI / 2);
		assertEquals(1.0, copy.getValue(), EPS);
		assertEquals(0.0, f.getValue(), EPS);
	}

	@Test
	void negationToStringBracketsOnlyCompoundOperands() {
		ExpressionNode x = new VariableNode("x", false);
		assertEquals("-x", new FunctionNode(x, "-").toString());
		assertEquals("-sin(x)", new FunctionNode(new FunctionNode(x, "sin"), "-").toString());
		assertEquals("-(x+1)", new FunctionNode(new OperatorNode(x, new ConstantNode(1.0), '+'), "-").toString());
		assertEquals("-(-x)", new FunctionNode(new FunctionNode(x, "-"), "-").toString());
	}

	@Test
	void parseAndEvaluationExceptionsCarryMessageAndCause() {
		Throwable cause = new RuntimeException("boom");
		assertEquals("m", new ParseError("m").getMessage());
		assertSame(cause, new ParseError(cause).getCause());
		assertSame(cause, new ParseError("m", cause).getCause());
		assertEquals(3, new ParseError("m", 3).getPosition());
		assertNull(new EvaluationException().getMessage());
		assertEquals("m", new EvaluationException("m").getMessage());
		assertSame(cause, new EvaluationException(cause).getCause());
		assertSame(cause, new EvaluationException("m", cause).getCause());
	}
}
