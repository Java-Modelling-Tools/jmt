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

/**
 * Created by IntelliJ IDEA.
 * User: PoliMi
 * Date: 21-giu-2005
 * Time: 11.54.31
 * To change this template use File | Settings | File Templates.
 */
public class Util2d {

	private double EPSYLON = 0.000001; // Error costante used for modulo    

	/**
* The method checks if p is collinear at points a and b
* @param a
* @param b
* @param p
* @return true if they are collinear
	 */
	public boolean Collinear(newPoint a, newPoint b, newPoint p) {
		boolean out = false;

		if (a.x != b.x && a.y != b.y && p.x != a.x && p.x != b.x && p.y != b.y && p.y != a.y) {
			double alpha11 = (p.x - b.x);
			double alpha12 = (a.x - b.x);
			double alpha1 = alpha11 / alpha12;
			double alpha21 = (p.y - b.y);
			double alpha22 = (a.y - b.y);
			double alpha2 = alpha21 / alpha22;

			if (Math.abs((alpha1 - alpha2)) < EPSYLON) {
				/*
				System.out.println(a.x+" "+b.x+" "+p.x);
				System.out.println(alpha1);
				System.out.println(alpha2);
				*/
				out = true;
			}
		}

		else if (a.x == b.x) {
			if (p.x == b.x && p.y < a.y && p.y > b.y) {
				out = true;
			}
		}

		else if (a.y == b.y) {
			if (p.y == b.y && p.x > a.x && p.x < b.x) {
				out = true;
			}
		}
		return out;
	}

	/**
	 * La funzione elimina i punti dominati dal vettore passato
	 * @param vertices      è un vettore di NewPoint
	 * @return              un vettore di NewPoint contenente i vertici non dominati
	 */
	public Vector<newPoint> DomRemove2D(Vector<newPoint> vertices) {
		//Vector out = vertices;

		for (int i = 0; i < vertices.size(); i++) {
			boolean t = false;
			double x1 = vertices.get(i).getX();
			double y1 = vertices.get(i).getY();
			for (int j = 0; j < vertices.size(); j++) {

				double x2 = vertices.get(j).getX();
				double y2 = vertices.get(j).getY();
				if (x1 < x2 && y1 < y2) {
					t = true;
					break;
				}
			}
			if (t) {
				vertices.removeElementAt(i);
// necessary x not skip the next point given the resize. of the vector
				i--;
			}
		}
		return vertices;
	}

	/**
* The method adds the axis projections to a vertex vector
* @param vertices is the vector of NewPoint passed
* @return a vector of NewPoint containing initial vertices + projections
	 */
	public Vector<newPoint> LExplode2D(Vector<newPoint> vertices) {
		Vector<newPoint> lout = new Vector<newPoint>();
		lout.addElement(new newPoint(0, 0));
		for (int i = 0; i < vertices.size(); i++) {
			lout.addElement(new newPoint((int) (vertices.get(i).getX()), ((int) vertices.get(i).getY())));
			lout.addElement(new newPoint(0, ((int) vertices.get(i).getY())));
			lout.addElement(new newPoint(((int) vertices.get(i).getX()), 0));
		}
		return lout;
	}

	/**
* The method checks if a 2D vertex is present in a vertex vector
* @param a the passed newPoint
* @param vertices the array of newPoints to check
* @return true if it is present
	 */
	public boolean VPresent(newPoint a, Vector<newPoint> vertices) {
		boolean out = false;
		for (int i = 0; i < vertices.size(); i++) {
			if (a.x == vertices.get(i).x && a.y == vertices.get(i).y) {
				out = true;
				break;
			}
		}
		return out;
	}

	public Vector<newPoint> RemoveCollinear(Vector<newPoint> vertices) {
		Vector<newPoint> out = new Vector<newPoint>(vertices);
		int ilast = vertices.size() - 1;
		newPoint first = out.get(0);
		// Attention here below out.get(0) -> out.get(1) ??
		newPoint second = out.get(1);
		newPoint last = out.lastElement();
		newPoint prelast = out.get(ilast - 1);

		if (last.x == prelast.x && last.y == 0) {
			out.remove(out.lastElement());
		}

		if (first.y == second.y && first.x == 0) {
			out.removeElementAt(0);
		}

		return out;
	}

}
