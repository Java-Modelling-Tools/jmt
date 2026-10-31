package solvers.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** The six exception classes are plain carriers; their hierarchy decides what catch blocks in QNModel and the solvers see. */
class ExceptionsTest {

	@Test
	void messageIsKeptByEveryException() {
		assertEquals("m", new BTFMatrixErrorException("m").getMessage());
		assertEquals("m", new InconsistentLinearSystemException("m").getMessage());
		assertEquals("m", new InputFileParserException("m").getMessage());
		assertEquals("m", new IllegalValueInInputFileException("m").getMessage());
		assertEquals("m", new InternalErrorException("m").getMessage());
		assertNull(new UndefinedMultiplyException().getMessage());
	}

	@Test
	void illegalValueIsAParserException() {
		Exception e = new IllegalValueInInputFileException("bad");
		assertTrue(e instanceof InputFileParserException);
		assertThrows(InputFileParserException.class, () -> {
			throw new IllegalValueInInputFileException("bad");
		});
	}

	@Test
	void allAreCheckedExceptions() {
		for (Class<?> c : new Class<?>[] { BTFMatrixErrorException.class, InconsistentLinearSystemException.class, InputFileParserException.class,
				IllegalValueInInputFileException.class, InternalErrorException.class, UndefinedMultiplyException.class }) {
			assertTrue(Exception.class.isAssignableFrom(c));
			assertFalse(RuntimeException.class.isAssignableFrom(c), c.getSimpleName() + " must stay checked");
		}
	}

	@Test
	void unrelatedExceptionsAreNotCaughtAsEachOther() {
		assertFalse(InputFileParserException.class.isAssignableFrom(InternalErrorException.class));
		assertFalse(InternalErrorException.class.isAssignableFrom(InconsistentLinearSystemException.class));
	}
}
