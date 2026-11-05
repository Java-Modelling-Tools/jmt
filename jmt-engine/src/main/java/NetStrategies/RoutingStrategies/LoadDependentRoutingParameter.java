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

package NetStrategies.RoutingStrategies;

import jmt.common.exception.IncorrectDistributionParameterException;
import QueueNet.NetNode;
import random.Empirical;
import random.EmpiricalEntry;
import random.EmpiricalPar;
import random.engine.RandomEngine;

/**
 * Created by IntelliJ IDEA.
 * User: Ashanka
 * Date: 9/4/11
 * Time: 10:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoadDependentRoutingParameter implements Comparable<Object> {

	private int from;
	private String[] nodeNames;
	private Empirical distribution;
	private EmpiricalPar param;
	private NetNode[] nodes;

	public LoadDependentRoutingParameter(Integer from, EmpiricalEntry entires[]) throws IncorrectDistributionParameterException {
		this.from = from.intValue();
		nodeNames = new String[entires.length];
		double[] probabilities = new double[entires.length];
		for (int i = 0; i < entires.length; i++) {
			nodeNames[i] = (String) entires[i].getValue();
			probabilities[i] = entires[i].getProbability();
		}
		distribution = new Empirical();

		param = new EmpiricalPar(probabilities);
	}

	public NetNode getOutNode(NetNode ownerNode) {
		if (nodes == null) {
			//it is the first execution of this method: find the NetNode objects
			nodes = new NetNode[nodeNames.length];
			for (int i = 0; i < nodeNames.length; i++) {
				nodes[i] = ownerNode.getNetSystem().getNode(nodeNames[i]);
			}
		}

		try {
			//the empirical distribution returns the position of the chosen output node
			int nodePos = (int) Math.round(distribution.nextRand(param));
			if (nodePos >= 0) {
				return nodes[nodePos];
			}
		} catch (IncorrectDistributionParameterException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Compares this object with the specified object for order.  Returns a
	 * negative integer, zero, or a positive integer as this object is less
	 * than, equal to, or greater than the specified object.
	 * <p>
	 * <p>The implementor must ensure <code>sgn(x.compareTo(y)) ==
	 * -sgn(y.compareTo(x))</code> for all <code>x</code> and <code>y</code>.  (This
	 * implies that <code>x.compareTo(y)</code> must throw an exception iff
	 * <code>y.compareTo(x)</code> throws an exception.)
	 * <p>
	 * <p>The implementor must also ensure that the relation is transitive:
	 * <code>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</code> implies
	 * <code>x.compareTo(z)&gt;0</code>.
	 * <p>
	 * <p>Finally, the implementor must ensure that <code>x.compareTo(y)==0</code>
	 * implies that <code>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</code>, for
	 * all <code>z</code>.
	 * <p>
	 * <p>It is strongly recommended, but <i>not</i> strictly required that
	 * <code>(x.compareTo(y)==0) == (x.equals(y))</code>.  Generally speaking, any
	 * class that implements the <code>Comparable</code> interface and violates
	 * this condition should clearly indicate this fact.  The recommended
	 * language is "Note: this class has a natural ordering that is
	 * inconsistent with equals."
	 * <p>
	 * <p>In the foregoing description, the notation
	 * <code>sgn(</code><i>expression</i><code>)</code> designates the mathematical
	 * <i>signum</i> function, which is defined to return one of <code>-1</code>,
	 * <code>0</code>, or <code>1</code> according to whether the value of
	 * <i>expression</i> is negative, zero or positive.
	 *
	 * @param o the object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object
	 *         is less than, equal to, or greater than the specified object.
	 * @throws ClassCastException if the specified object's type prevents it
	 *                            from being compared to this object.
	 */
	public int compareTo(Object o) {
		if (o instanceof LoadDependentRoutingParameter) {
			return this.from - ((LoadDependentRoutingParameter) o).from;
		} else if (o instanceof Integer) {
			return this.from - ((Integer) o).intValue();
		} else {
			throw new ClassCastException("Incorrect Class to compare");
		}
	}

	public void setRandomEngine(RandomEngine engine) {
		distribution.setRandomEngine(engine); 		
	}

}
