package solvers.dataStructures;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.junit.jupiter.api.Test;

import solvers.exceptions.InternalErrorException;

/** Tests for {@link EnhancedVector} and its subclasses (population, population change, multiplicities). */
class EnhancedVectorTest {

	private static EnhancedVector ev(Integer... v) {
		return new EnhancedVector(v);
	}

	@Test
	void constructors() {
		assertEquals(0, new EnhancedVector().size());
		assertEquals(ev(1, 2, 3), new EnhancedVector(new int[] { 1, 2, 3 }));
		assertEquals(ev(4, 4, 4), new EnhancedVector(4, 3));
		assertEquals(0, new EnhancedVector(9, 0).size());
	}

	@Test
	void sumsAndPredicates() {
		assertEquals(6, ev(1, 2, 3).sum());
		assertEquals(0, ev().sum());
		assertTrue(ev().isZeroVector());
		assertTrue(ev(0, 0).isZeroVector());
		assertFalse(ev(0, 1).isZeroVector());
		assertTrue(ev(0, -1).containsMinusOne());
		assertFalse(ev(0, 1).containsMinusOne());
		assertEquals(2, ev(0, 3, 0).countZeroElements());
		assertEquals(0, ev().countZeroElements());
		assertEquals(ev(2).getAsBigRational(0), new BigRational(2));
	}

	@Test
	void firstNonZeroElement() throws InternalErrorException {
		assertEquals(2, ev(0, 0, 5).findFirstNonZeroElement());
		assertEquals(1, ev(0, -3, 5).findFirstNonZeroElement());
		assertThrows(InternalErrorException.class, () -> ev(0, 0).findFirstNonZeroElement());
		assertThrows(InternalErrorException.class, () -> ev().findFirstNonZeroElement());
	}

	@Test
	void minMax() throws OperationNotSupportedException {
		assertEquals(7, ev(3, 7, -2).max());
		assertEquals(-2, ev(3, 7, -2).min());
		assertEquals(5, ev(5).max());
		assertEquals(5, ev(5).min());
		assertThrows(OperationNotSupportedException.class, () -> ev().max());
		assertThrows(OperationNotSupportedException.class, () -> ev().min());
	}

	@Test
	void addAndSubtract() {
		assertEquals(ev(5, 7, 9), ev(1, 2, 3).addVec(ev(4, 5, 6)));
		assertEquals(ev(-3, -3, -3), ev(1, 2, 3).subVec(ev(4, 5, 6)));
		assertEquals(ev(), ev().addVec(ev()));
		EnhancedVector a = ev(1, 2);
		a.addVec(ev(1, 1));
		assertEquals(ev(1, 2), a, "addVec must not mutate the receiver");
		assertThrows(UnsupportedOperationException.class, () -> ev(1).addVec(ev(1, 2)));
		assertThrows(UnsupportedOperationException.class, () -> ev(1).subVec(ev(1, 2)));
	}

	@Test
	void minusOnePlusOneRestoreAreOneBasedAndLifo() {
		EnhancedVector v = ev(2, 2, 2);
		v.minusOne(1);
		assertEquals(ev(1, 2, 2), v);
		v.plusOne(3);
		assertEquals(ev(1, 2, 3), v);
		v.restore();
		assertEquals(ev(1, 2, 2), v);
		v.restore();
		assertEquals(ev(2, 2, 2), v);
		v.restore(); // empty history: no-op
		assertEquals(ev(2, 2, 2), v);
	}

	@Test
	void index0IsRecordedButDoesNotChangeVector() {
		EnhancedVector v = ev(2, 2);
		v.minusOne(0);
		v.plusOne(0);
		assertEquals(ev(2, 2), v);
		v.restore();
		v.restore();
		assertEquals(ev(2, 2), v);
	}

	@Test
	void copyIsIndependentAndKeepsDynamicType() {
		EnhancedVector v = ev(1, 2);
		EnhancedVector c = v.copy();
		c.set(0, 9);
		assertEquals(ev(1, 2), v);
		assertEquals(PopulationVector.class, new PopulationVector(1, 2).copy().getClass());
		assertEquals(MultiplicitiesVector.class, new MultiplicitiesVector(1, 2).copy().getClass());
		assertEquals(PopulationChangeVector.class, new PopulationChangeVector(1, 2).copy().getClass());
	}

	@Test
	void equalsIgnoreHistoryComparesContentOnly() {
		EnhancedVector a = ev(1, 2);
		EnhancedVector b = ev(1, 2);
		b.minusOne(1);
		b.restore();
		assertTrue(a.equalsIgnoreHistory(b));
		assertFalse(a.equalsIgnoreHistory(ev(1, 3)));
	}

	@Test
	void compareToIsATotalOrderConsistentWithEquality() {
		List<EnhancedVector> vs = new ArrayList<>();
		for (int a = 0; a < 3; a++) {
			for (int b = 0; b < 3; b++) {
				vs.add(ev(a, b));
			}
		}
		for (EnhancedVector x : vs) {
			assertEquals(0, x.compareTo(x.copy()));
			for (EnhancedVector y : vs) {
				assertEquals(-Integer.signum(x.compareTo(y)), Integer.signum(y.compareTo(x)));
				assertEquals(x.equals(y), x.compareTo(y) == 0);
				for (EnhancedVector z : vs) {
					if (x.compareTo(y) <= 0 && y.compareTo(z) <= 0) {
						assertTrue(x.compareTo(z) <= 0);
					}
				}
			}
		}
		// smaller total sum sorts first
		assertTrue(ev(0, 1).compareTo(ev(2, 2)) < 0);
		Collections.sort(vs);
		assertEquals(ev(0, 0), vs.get(0));
		assertEquals(ev(2, 2), vs.get(vs.size() - 1));
	}

	@Test
	void populationVectorArithmetic() {
		PopulationVector p = new PopulationVector(new Integer[] { 3, 1, 0 });
		PopulationVector q = new PopulationVector(new int[] { 1, 1, 0 });
		assertEquals(new PopulationVector(new Integer[] { 4, 2, 0 }), p.addVec(q));
		assertEquals(new PopulationVector(new Integer[] { 2, 0, 0 }), p.subVec(q));
		assertEquals(p.subVec(q), p.changePopulation(new PopulationChangeVector(new Integer[] { 1, 1, 0 })));
		assertTrue(p.anyGreaterThan(q));
		assertFalse(q.anyGreaterThan(p));
		assertFalse(p.anyGreaterThan(p));
		assertFalse(new PopulationVector().anyGreaterThan(new PopulationVector()));
		assertEquals(0, new PopulationVector().sum());
	}

	@Test
	void populationChangeVectorRangeSums() {
		PopulationChangeVector c = new PopulationChangeVector(new Integer[] { 1, 2, 0, 4 });
		assertEquals(7, c.sum(0, 4));
		assertEquals(0, c.sum(2, 2));
		assertEquals(3, c.sum(0, 2));
		assertEquals(4, c.sumTail(2));
		assertEquals(7, c.sumTail(0));
		assertEquals(1, c.sumHead(0));
		assertEquals(3, c.sumHead(2));
		assertEquals(3, c.RightMostNonZero());
		assertEquals(-1, new PopulationChangeVector(0, 3).RightMostNonZero());
		assertEquals(-1, new PopulationChangeVector().RightMostNonZero());
		assertEquals(1, new PopulationChangeVector(new Integer[] { 0, 5, 0 }).RightMostNonZero());
	}

	@Test
	void multiplicitiesSingleQueueAdded() throws InternalErrorException {
		assertEquals(0, new MultiplicitiesVector(0, 3).whichSingleQueueAdded());
		assertEquals(2, new MultiplicitiesVector(new Integer[] { 0, 1, 0 }).whichSingleQueueAdded());
		assertEquals(1, new MultiplicitiesVector(new Integer[] { 1 }).whichSingleQueueAdded());
		assertThrows(InternalErrorException.class, () -> new MultiplicitiesVector(new Integer[] { 1, 1 }).whichSingleQueueAdded());
		assertThrows(InternalErrorException.class, () -> new MultiplicitiesVector(new Integer[] { 2, 0 }).whichSingleQueueAdded());
	}

	@Test
	void multiplicitiesNearlyAllOnes() {
		assertEquals(1, new MultiplicitiesVector(new Integer[] { 1, 0, 1 }).isNearlyAllOneElements());
		assertEquals(0, new MultiplicitiesVector(new Integer[] { 3, 1, 1 }).isNearlyAllOneElements());
		assertEquals(-1, new MultiplicitiesVector(new Integer[] { 1, 1, 1 }).isNearlyAllOneElements());
		assertEquals(-1, new MultiplicitiesVector(new Integer[] { 0, 0, 1 }).isNearlyAllOneElements());
		assertEquals(0, new MultiplicitiesVector(new Integer[] { 0 }).isNearlyAllOneElements());
		assertEquals(-1, new MultiplicitiesVector().isNearlyAllOneElements());
	}

	@Test
	void multiplicitiesAddVecKeepsType() {
		MultiplicitiesVector m = new MultiplicitiesVector(new Integer[] { 1, 0 });
		assertEquals(new MultiplicitiesVector(new Integer[] { 2, 1 }), m.addVec(new MultiplicitiesVector(new Integer[] { 1, 1 })));
	}

	@Test
	void tupleAccessors() {
		Tuple<String, Integer> t = new Tuple<>("a", 1);
		assertEquals("a", t.getX());
		assertEquals(1, t.getY());
		t.setX("b");
		t.setY(2);
		assertEquals("b", t.getX());
		assertEquals(2, t.getY());
		assertNotNull(t.toString());
	}
}
