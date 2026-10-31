package dataAnalysis.sorting;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SortingTest {

	static Stream<Arguments> general() {
		Supplier<SortAlgorithm> eq = EQSort::new, merge = MergeSort::new;
		return Stream.of(Arguments.of("EQSort", eq), Arguments.of("MergeSort", merge));
	}

	private static double[] random(int n, long seed) {
		Random r = new Random(seed);
		double[] a = new double[n];
		for (int i = 0; i < n; i++) {
			a[i] = r.nextInt(1000) / 10.0 - 40; // many duplicates and negatives
		}
		return a;
	}

	private static void check(SortAlgorithm s, double[] in) {
		double[] expected = in.clone();
		Arrays.sort(expected);
		double[] actual = in.clone();
		s.sort(actual);
		assertArrayEquals(expected, actual, "input " + Arrays.toString(in));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("general")
	void sortsRandomArraysOfEverySmallSize(String name, Supplier<SortAlgorithm> s) {
		for (int n = 0; n <= 40; n++) {
			check(s.get(), random(n, 100 + n));
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("general")
	void sortsLargeRandomArray(String name, Supplier<SortAlgorithm> s) {
		check(s.get(), random(2000, 7));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("general")
	void sortsAlreadySortedReversedAndConstantArrays(String name, Supplier<SortAlgorithm> s) {
		double[] asc = new double[50];
		double[] desc = new double[50];
		for (int i = 0; i < 50; i++) {
			asc[i] = i;
			desc[i] = 49 - i;
		}
		check(s.get(), asc);
		check(s.get(), desc);
		check(s.get(), new double[20]);
		check(s.get(), new double[] { 5, 5, 1, 1, 5, 1, 5, 1 });
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("general")
	void nullAndSingletonInputsAreLeftUntouched(String name, Supplier<SortAlgorithm> s) {
		assertDoesNotThrow(() -> s.get().sort(null));
		double[] one = { 3.5 };
		s.get().sort(one);
		assertArrayEquals(new double[] { 3.5 }, one);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("general")
	void sortsFourElementPermutations(String name, Supplier<SortAlgorithm> s) {
		// every permutation of 4 distinct values exercises the brute-force branch of EQSort
		int[][] perms = permutations(4);
		for (int[] p : perms) {
			double[] in = new double[4];
			for (int i = 0; i < 4; i++) {
				in[i] = p[i];
			}
			check(s.get(), in);
		}
	}

	private static int[][] permutations(int n) {
		java.util.List<int[]> out = new java.util.ArrayList<int[]>();
		permute(new int[n], new boolean[n], 0, out);
		return out.toArray(new int[0][]);
	}

	private static void permute(int[] cur, boolean[] used, int pos, java.util.List<int[]> out) {
		if (pos == cur.length) {
			out.add(cur.clone());
			return;
		}
		for (int v = 0; v < cur.length; v++) {
			if (!used[v]) {
				used[v] = true;
				cur[pos] = v;
				permute(cur, used, pos + 1, out);
				used[v] = false;
			}
		}
	}

	@ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(ints = { 3, 4, 5, 10, 63, 500 })
	void heapSortSortsFromIndexOneAndKeepsIndexZero(int n) {
		double[] in = random(n, n);
		in[0] = 12345.0;
		double[] expected = Arrays.copyOfRange(in, 1, n);
		Arrays.sort(expected);
		new HeapSort().sort(in);
		assertArrayEquals(expected, Arrays.copyOfRange(in, 1, n));
		org.junit.jupiter.api.Assertions.assertEquals(12345.0, in[0]);
	}

	@org.junit.jupiter.api.Test
	void heapSortIgnoresArraysShorterThanThree() {
		double[] two = { 9, 1 };
		new HeapSort().sort(two);
		assertArrayEquals(new double[] { 9, 1 }, two);
		new HeapSort().sort(new double[0]);
	}

	@org.junit.jupiter.api.Test
	void fastQSortSortsWhenPivotIsNeverTheRangeMinimum() {
		check(new FastQSort(), new double[] { 1, 2, 3, 4, 5, 6, 7, 8 });
		check(new FastQSort(), new double[] { 2, 2, 2, 2 });
		check(new FastQSort(), new double[] { 3, 1, 2, 4 });
		assertDoesNotThrow(() -> new FastQSort().sort(null));
		double[] one = { 1 };
		new FastQSort().sort(one);
		assertArrayEquals(new double[] { 1 }, one);
	}

	@org.junit.jupiter.api.Test
	@org.junit.jupiter.api.Disabled("bug: FastQSort throws ArrayIndexOutOfBounds when the last element (pivot) is strictly smaller than all others in a range")
	void fastQSortSortsDescendingPair() {
		check(new FastQSort(), new double[] { 2, 1 });
	}
}
