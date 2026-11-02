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
package jmt.common.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExceptionsTest {

	private final Throwable cause = new RuntimeException("root");

	@Test
	void netException_fullConstructorExposesOriginAndType() {
		Object origin = new Object();
		NetException inner = new NetException("inner");
		NetException e = new NetException(origin, 7, "msg", inner);
		assertEquals("msg", e.getMessage());
		assertSame(origin, e.thrownBy());
		assertEquals(7, e.getType());
		NetException e2 = new NetException(origin, 3, "m2");
		assertEquals(3, e2.getType());
		assertSame(origin, e2.thrownBy());
	}

	@Test
	void netException_messageOnlyHasNoOrigin() {
		NetException e = new NetException("just text");
		assertEquals("just text", e.getMessage());
		assertNull(e.thrownBy());
		assertEquals(0, e.getType());
	}

	@Test
	void expressionParseException_keepsLocationOrMinusOne() {
		ExpressionParseException e = new ExpressionParseException(4, "bad");
		assertEquals(4, e.getLoc());
		assertEquals("bad", e.getMessage());
		assertEquals(-1, new ExpressionParseException("x").getLoc());
	}

	@Test
	void loadException_keepsMessageAndCause() {
		assertEquals("m", new LoadException("m").getMessage());
		LoadException e = new LoadException("m", cause);
		assertSame(cause, e.getCause());
	}

	@Test
	void messageOnlyExceptions_keepMessage() {
		assertEquals("m", new IncorrectDistributionParameterException("m").getMessage());
		assertNull(new IncorrectDistributionParameterException().getMessage());
		assertEquals("m", new IncorrectMeasureParameterException("m").getMessage());
		assertNull(new IncorrectMeasureParameterException().getMessage());
	}

	@Test
	void inputDataException_allConstructors() {
		assertNull(new InputDataException().getMessage());
		assertEquals("m", new InputDataException("m").getMessage());
		assertSame(cause, new InputDataException(cause).getCause());
		InputDataException e = new InputDataException("m", cause);
		assertEquals("m", e.getMessage());
		assertSame(cause, e.getCause());
	}

	@Test
	void solverException_allConstructors() {
		assertNull(new SolverException().getMessage());
		assertEquals("m", new SolverException("m").getMessage());
		assertSame(cause, new SolverException(cause).getCause());
		SolverException e = new SolverException("m", cause);
		assertEquals("m", e.getMessage());
		assertSame(cause, e.getCause());
	}

	@Test
	void unsupportedModelException_allConstructors() {
		assertNull(new UnsupportedModelException().getMessage());
		assertEquals("m", new UnsupportedModelException("m").getMessage());
		assertSame(cause, new UnsupportedModelException(cause).getCause());
		UnsupportedModelException e = new UnsupportedModelException("m", cause);
		assertEquals("m", e.getMessage());
		assertSame(cause, e.getCause());
	}
}
