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

package jmt.engine.jaba;

import java.util.Vector;

import jmt.engine.jaba.Hull.Vertex;

/**
 * Created by IntelliJ IDEA.
 * User: Andrea
 * Date: 5-ago-2005
 * Time: 15.01.25
 * To change this template use File | Settings | File Templates.
 */
public class Segment3D {

	private double beta11;
	private double beta12;
	private double beta13;
	private double beta21;
	private double beta22;
	private double beta23;
	private Vertex s1;
	private Vertex s2;

	public Segment3D() {

	}

	public Segment3D(double beta11, double beta12, double beta13, double beta21, double beta22, double beta23, Vertex s1, Vertex s2) {
		this.beta11 = beta11;
		this.beta12 = beta12;
		this.beta13 = beta13;
		this.beta21 = beta21;
		this.beta22 = beta22;
		this.beta23 = beta23;
		this.s1 = s1;
		this.s2 = s2;
	}

	public Segment3D(BetaVertex b1, BetaVertex b2, Vertex s1, Vertex s2) {
		beta11 = b1.getX();
		beta12 = b1.getY();
		beta13 = b1.getZ();
		beta21 = b2.getX();
		beta22 = b2.getY();
		beta23 = b2.getZ();
		this.s1 = s1;
		this.s2 = s2;
	}

	/**
* Create a vector of segments. It is used to check not to use the same
* segment twice in 3D space construction
	 * @param tri
	 * @return
	 */
	public Vector<Object> CreateLt(Vector<Object> tri) {
		Vector<Object> out = new Vector<Object>();

		for (int i = 0; i < tri.size(); i++) {
			double[] abetas = ((Sector3D) tri.get(i)).getBetas(0);
			double[] bbetas = ((Sector3D) tri.get(i)).getBetas(1);
			double[] cbetas = ((Sector3D) tri.get(i)).getBetas(2);
			Vertex s0 = ((Sector3D) tri.get(i)).getS0();
			Vertex s1 = ((Sector3D) tri.get(i)).getS1();
			Vertex s2 = ((Sector3D) tri.get(i)).getS2();

			// Segment S0-S1
			out.addElement(new Segment3D(abetas[0], abetas[1], abetas[2], bbetas[0], bbetas[1], bbetas[2], s0, s1));
			// Segment S0-S2
			out.addElement(new Segment3D(abetas[0], abetas[1], abetas[2], cbetas[0], cbetas[1], cbetas[2], s0, s2));
			// Segment S1-S2
			out.addElement(new Segment3D(bbetas[0], bbetas[1], bbetas[2], cbetas[0], cbetas[1], cbetas[2], s1, s2));

		}

		return out;
	}

	public BetaVertex getBeta(int stat) {
		if (stat == 0) {
			return new BetaVertex(beta11, beta12, beta13);
		} else if (stat == 1) {
			return new BetaVertex(beta21, beta22, beta23);
		} else {
			return new BetaVertex(-1, -1, -1);
		}
	}

	public double getBeta11() {
		return beta11;
	}

	public double getBeta12() {
		return beta12;
	}

	public double getBeta13() {
		return beta13;
	}

	public double getBeta21() {
		return beta21;
	}

	public double getBeta22() {
		return beta22;
	}

	public double getBeta23() {
		return beta23;
	}

	public Vertex getS1() {
		return s1;
	}

	public Vertex getS2() {
		return s2;
	}

	@Override
	public String toString() {
		return (s1 + "[" + beta11 + "," + beta12 + "]" + "-" + s2 + "[" + beta21 + "," + beta22 + "]");
	}

	/**
* Starting from the sides of the sectors where they saturate 2 stations obtained by joining them
* where saturate 3 stations adds the segments created and removes those already used
* @param sett2stat
* @param lt
	 * @return
	 */
	public Vector<Object> FixLtFromJoin(Vector<Object> sect2station, Vector<Object> lt) {
		//Vector ltout = new Vector();
		for (int i = 0; i < sect2station.size(); i++) {
			for (int j = 0; j < lt.size(); j++) {
// I take advantage of the fact that a sector with this sequence is created from the join of 2 triangles:
// TrA S0 - TrB S0 - TrA S1 - TrB S1

// Caution!! No control over betas, I'm assuming (eg) stations
// 0 and 1 are always present only on at most 2 triangles, and therefore are united

if (((Sector3D) sect2station.get(i)).getS0() == ((Segment3D) lt.get(j)).getS1()
&& ((Sector3D) sect2station.get(i)).getS2() == ((Segment3D) lt.get(j)).getS2()) {
lt.removeElementAt(j);
j--;
}
}

// For each 2-station 3D sector I must then add the new sides created by joining
// the sectors.

// Add the segment made by the first station that saturates
lt.addElement(new Segment3D(((Sector3D) sect2station.get(i)).getBeta(0, 1), ((Sector3D) sect2station.get(i)).getBeta(0, 2),
((Sector3D) sect2station.get(i)).getBeta(0, 3), ((Sector3D) sect2station.get(i)).getBeta(1, 1), ((Sector3D) sect2station.get(i)). getBeta(
1, 2), ((Sector3D) sect2station.get(i)).getBeta(1, 3), ((Sector3D) sect2station.get(i)).getS0(), ((Sector3D) sect2station.get(i) )
.getS1()));
// Add the segment made by the second station that saturates
			lt.addElement(new Segment3D(((Sector3D) sect2station.get(i)).getBeta(2, 1), ((Sector3D) sect2station.get(i)).getBeta(2, 2),
					((Sector3D) sect2station.get(i)).getBeta(2, 3), ((Sector3D) sect2station.get(i)).getBeta(3, 1), ((Sector3D) sect2station.get(i)).getBeta(
							3, 2), ((Sector3D) sect2station.get(i)).getBeta(3, 3), ((Sector3D) sect2station.get(i)).getS2(), ((Sector3D) sect2station.get(i))
							.getS(3)));
		}
		return lt;
	}

	public Vector<Object> FixLtFrom2Dxy(Vector<Sector2D> resxy) {
		Vector<Object> out = new Vector<Object>();
		for (int i = 0; i < resxy.size(); i++) {
			Vertex st1 = new Vertex((resxy.get(i).getP1()).x, (resxy.get(i).getP1()).y,
// put -1 to indicate that the original coordinate is lost
-1);
Vertex st2 = new Vertex((resxy.get(i).getP2()).x, (resxy.get(i).getP2()).y, -1);
out.addElement(new Segment3D(resxy.get(i).getBeta1(), resxy.get(i).getBeta11(),
// the points coming from xy have by definition beta3=0
0, resxy.get(i).getBeta2(), resxy.get(i).getBeta22(), 0, st1, st2));
}
return out;
}

public Vector<Object> FixLtFrom2Dxz(Vector<Sector2D> resxz) {
Vector<Object> out = new Vector<Object>();
for (int i = 0; i < resxz.size(); i++) {
Vertex st1 = new Vertex((resxz.get(i).getP1()).x,
// put -1 to indicate that the original coordinate is lost
-1, (resxz.get(i).getP1()).y);
Vertex st2 = new Vertex((resxz.get(i).getP2()).x, -1, (resxz.get(i).getP2()).y);
out.addElement(new Segment3D(resxz.get(i).getBeta1(),
// the points coming from xz have by definition beta2=0
0.0, resxz.get(i).getBeta11(), resxz.get(i).getBeta2(), 0.0, resxz.get(i).getBeta22(), st1, st2));
}
return out;
}

public Vector<Object> FixLtFrom2Dyz(Vector<Sector2D> resyz) {
Vector<Object> out = new Vector<Object>();
for (int i = 0; i < resyz.size(); i++) {
Vertex st1 = new Vertex(
// put -1 to indicate that the original coordinate is lost
-1, (resyz.get(i).getP1()).x, (resyz.get(i).getP1()).y);
Vertex st2 = new Vertex(-1, (resyz.get(i).getP2()).x, (resyz.get(i).getP2()).y);
out.addElement(new Segment3D(
// the points coming from yz have by definition beta2=0
					0.0, resyz.get(i).getBeta1(), resyz.get(i).getBeta11(), 0, resyz.get(i).getBeta2(), resyz.get(i).getBeta22(), st1, st2));
		}
		return out;
	}
}
