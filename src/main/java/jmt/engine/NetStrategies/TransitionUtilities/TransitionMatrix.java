package jmt.engine.NetStrategies.TransitionUtilities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

/**
 * <p>Title: Transition Matrix</p>
 * <p>Description: This class implements the transition matrix.</p>
 *
 * @author Lulai Zhu
 * Date: 06-10-2016
 * Time: 14.00.00
 */
public class TransitionMatrix {

	private HashMap<String, TransitionVector> matrix;
	private int total;
	private int[] totals;

	public TransitionMatrix(int size) {
		matrix = new HashMap<String, TransitionVector>();
		total = 0;
		totals = new int[size];
		Arrays.fill(totals, 0);
	}

	public TransitionMatrix(TransitionVector[] vectors) {
		this(vectors[0].size());
		for (int i = 0; i < vectors.length; i++) {
			matrix.put(vectors[i].getKey(), vectors[i]);
			total += vectors[i].getTotal();
			for (int j = 0; j < vectors[i].size(); j++) {
				totals[j] += vectors[i].getEntry(j);
			}
		}
	}

	public Set<String> keySet() {
		return matrix.keySet();
	}

	public TransitionVector getVector(String key) {
		return matrix.get(key);
	}

	public void setVector(TransitionVector vector) {
		String key = vector.getKey();
		if (matrix.containsKey(key)) {
			total -= matrix.get(key).getTotal();
			for (int i = 0; i < matrix.get(key).size(); i++) {
				totals[i] -= matrix.get(key).getEntry(i);
			}
		}
		matrix.put(key, vector);
		total += vector.getTotal();
		for (int i = 0; i < vector.size(); i++) {
			totals[i] += vector.getEntry(i);
		}
	}

	public int getEntry(String key, int index) {
		if (!matrix.containsKey(key)) {
			return -1;
		}
		return matrix.get(key).getEntry(index);
	}

	public void setEntry(String key, int index, int value) {
		if (!matrix.containsKey(key)) {
			return;
		}
		total -= matrix.get(key).getEntry(index);
		totals[index] -= matrix.get(key).getEntry(index);
		matrix.get(key).setEntry(index, value);
		total += matrix.get(key).getEntry(index);
		totals[index] += matrix.get(key).getEntry(index);
	}

	public int getTotal() {
		return total;
	}

	public int getTotal(String key) {
		if (!matrix.containsKey(key)) {
			return -1;
		}
		return matrix.get(key).getTotal();
	}

	public int getTotal(int index) {
		if (index < 0 || index >= totals.length) {
			return -1;
		}
		return totals[index];
	}

}
