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

import jmt.engine.jaba.Hull.ConvexHullException;
import jmt.engine.jaba.Hull.Vertex;

/**
 * Created by IntelliJ IDEA.
 * User: Andrea Zanzottera
 * Date: 1-ago-2005
 * Time: 10.30.07
 */
public class Calc {

// I instantiate the ViewResults class to print the results on the screen
//ViewResults vres = new ViewResults();
// I initialize the vector that will then be passed by the faces3d function
Vector<newFace> faces = new Vector<newFace>();
// I initialize the vector of sides to join for the calcusideson
Vector<Object> sides = new Vector<Object>();
// Initialize the vector containing the sectors where 3 stations saturate simultaneously
Vector<Object> triangles = new Vector<Object>();
// I initialize the vector containing the sectors where saturate 2 stations at the same time
Vector<Object> sect2station = new Vector<Object>();
// Initialize the vector containing the sectors where saturates 1 station
Vector<Object> sect1station = new Vector<Object>();
// I duplicate the vector of vertices to do some final checks
Vector<Vertex> original3D = new Vector<Vertex>();

	/**
* Passing a vector of newPoint containing the D of two classes the method returns
* a Sector2D Vector with information about popusideson mix and which stations
* saturate for that mix.
*
* @param vertices2D
* @return a Vector of Sector2D
	 */
	public Vector<Sector2D> Calc2D(Vector<newPoint> vertices2D) {
		Vector<newPoint> original2D = new Vector<newPoint>(vertices2D);
		Util2d util = new Util2d();

// Add the projections
vertices2D = util.LExplode2D(vertices2D);

//I remove the redundant points (OTHERWISE GRAHAMSCAN CRASHES!!!)
Vector<newPoint> verticesnew = new Vector<newPoint>();
for (int i = 0; i < vertices2D.size(); i++) {
if (!util.VPresent(vertices2D.get(i), verticesnew)) {
verticesnew.addElement(vertices2D.get(i));
}
}
vertices2D = verticesnew;

// I remove the dominated points
vertices2D = util.DomRemove2D(vertices2D);

// Add the origin to actually make the polygon convex
// It's because the Graham Scan was designed for each case
vertices2D.addElement(new newPoint(0, 0));

// Run the doGraham function which takes a vector of as a parameter
// newPoint and returns the lines vector of newPoint
grahamScan gra = new grahamScan();
vertices2D = gra.doGraham(vertices2D);

//to check the projections better: case (10,4) - (10,0)
// you have to make sure that a point is discarded anyway if it is collinear
//I remove the projections v1.5
Vector<newPoint> verticesnp = new Vector<newPoint>();
for (int i = 0; i < vertices2D.size(); i++) {
if (util.VPresent(vertices2D.get(i), original2D)) {
verticesnp.addElement(vertices2D.get(i));

}
}
// I eliminate the projections in case they are collinear to other points
// For example (10.4) - (10.0) or (0.5) - (5.5)
if (verticesnp.size() > 1) {
verticesnp = util.RemoveCollinear(verticesnp);
}

//Mapping the remaining points
		Beta2D b2d = new Beta2D();
		Vector<Sector2D> sector = new Vector<Sector2D>(); // è il vettore con le Beta
		sector = b2d.BetaVector(verticesnp);

		//System.out.println(sector.size());

		/*
//Add the "diagonal" collinear points inserting them in the right place
if (verticesnp.size()>1 && original2D.size()>1)
{
for (int i=0;i<(verticesnp.size()-1);i++)
{
for (int j=0;j<original2D.size();j++)
{
// a vertex is good when it's not already included,
// is collinear to the 2 of the sector
// its x is greater than the first v in the sector
// its x is less than the second v of the sector

		if (verticesnp.contains(original2D.get(j))==false &&
		util.Collinear((newPoint)verticesnp.get(i),(newPoint)verticesnp.get(i+1),(newPoint)original2D.get(j)) &&
		(((newPoint)verticesnp.get(i)).getX()<((newPoint)original2D.get(j)).getX()) &&
		(((newPoint)verticesnp.get(i+1)).getX()>((newPoint)original2D.get(j)).getX())
		)
		{
		verticesnp.insertElementAt(original2D.get(j),i);
		//i--;
		}
		}
		}
		}
		*/

		Vector<Sector2D> res = new Vector<Sector2D>();

if (verticesnp.size() > 1) {
// I associate the stations to the sectors
res = b2d.StatAss(sector);

/*
for (int i=0;i<sector.size();i++)
System.out.println(sector.get(i));
*/

// Adding collinear points
for (int i = 0; i < res.size(); i++) //for all sectors
{
for (int j = 0; j < original2D.size(); j++) //for all original points
{
// Initial Horizontal Collinear
if (i == 0 && (res.get(i).getP1()).getX() != 0) {
res.get(i).addCollinearFirst(res.get(i), original2D.get(j));
}

// Diagonal hills
res.get(i).addCollinear(res.get(i), original2D.get(j));

// Collinear on the vertical side
if (i == res.size() - 1 && (res.get(i).getP1()).getY() != 0) {
res.get(i).addCollinearLast(res.get(i), original2D.get(j));
}

}
}
} else
// If saturates only one station
{
res.addElement(new Sector2D(1, 0, 0, 1, verticesnp.get(0)));
}

return res;
}
	public Vector<Object> Calc2D(Vector<newPoint> vertices2D, String[] stationNames, String[] classNames) {
		Vector<newPoint> original2D = new Vector<newPoint>(vertices2D);
		Util2d util = new Util2d();

//create the vector with the stations, it is used to associate name with coordinates
Vector<Station2D> stations = new Vector<Station2D>();
for (int i = 0; i < vertices2D.size(); i++) {
Station2D stat = new Station2D(vertices2D.get(i), stationNames[i]);
stations.addElement(stat);
}
// Add the projections
vertices2D = util.LExplode2D(vertices2D);

//I remove the redundant points (OTHERWISE GRAHAMSCAN CRASHA!!!)
Vector<newPoint> verticesnew = new Vector<newPoint>();
for (int i = 0; i < vertices2D.size(); i++) {
if (!util.VPresent(vertices2D.get(i), verticesnew)) {
verticesnew.addElement(vertices2D.get(i));
}
}
vertices2D = verticesnew;

// I remove the dominated points
vertices2D = util.DomRemove2D(vertices2D);

// Add the origin to actually make the polygon convex
// It's because the Graham Scan was designed for each case
vertices2D.addElement(new newPoint(0, 0));

// Run the doGraham function which takes a vector of as a parameter
// newPoint and returns the lines vector of newPoint

		grahamScan gra = new grahamScan();
		vertices2D = gra.doGraham(vertices2D);

// todo check is the new one (and it doesn't work!)
/*
newGraham gra = new newGraham();
vertices2D=gra.GrahamScan(vertices2D);
*/

//to check the projections better: case (10,4) - (10,0)
// you have to make sure that a point is discarded anyway if it is collinear
//I remove the projections v1.5
Vector<newPoint> verticesnp = new Vector<newPoint>();
for (int i = 0; i < vertices2D.size(); i++) {
if (util.VPresent(vertices2D.get(i), original2D)) {
verticesnp.addElement(vertices2D.get(i));

}
}
// I eliminate the projections in case they are collinear to other points
// For example (10.4) - (10.0) or (0.5) - (5.5)
if (verticesnp.size() > 1) {
verticesnp = util.RemoveCollinear(verticesnp);
}

//Mapping the remaining points
Beta2D b2d = new Beta2D();
Vector<Sector2D> sector = new Vector<Sector2D>(); // is the array with the Betas
sector = b2d.BetaVector(verticesnp);

//System.out.println(sector.size());

/*
//Add the "diagonal" collinear points inserting them in the right place
if (verticesnp.size()>1 && original2D.size()>1)
{
for (int i=0;i<(verticesnp.size()-1);i++)
{
for (int j=0;j<original2D.size();j++)
{
// a vertex is good when it's not already included,
// is collinear to the 2 of the sector
// its x is greater than the first v in the sector
// its x is less than the second v of the sector

if (verticesnp.contains(original2D.get(j))==false &&
util.Collinear((newPoint)verticesnp.get(i),(newPoint)verticesnp.get(i+1),(newPoint)original2D.get(j)) &&
(((newPoint)verticesnp.get(i)).getX()<((newPoint)original2D.get(j)).getX()) &&
(((newPoint)verticesnp.get(i+1)).getX()>((newPoint)original2D.get(j)).getX())
)
{
verticesnp.insertElementAt(original2D.get(j),i);
//i--;
}
}
}
}
*/
		Vector<Sector2D> res = new Vector<Sector2D>();

		if (verticesnp.size() > 1) {
	// I associate the stations to the sectors
res = b2d.StatAss(sector);

/*
for (int i=0;i<sector.size();i++)
System.out.println(sector.get(i));
*/

// Adding collinear points
for (int i = 0; i < res.size(); i++) //for all sectors
{
for (int j = 0; j < original2D.size(); j++) //for all original points
{
// Initial Horizontal Collinear
if (i == 0 && (res.get(i).getP1()).getX() != 0) {
res.get(i).addCollinearFirst(res.get(i), original2D.get(j));
}

// Diagonal hills
res.get(i).addCollinear(res.get(i), original2D.get(j));

// Collinear on the vertical side
if (i == res.size() - 1 && (res.get(i).getP1()).getY() != 0) {
res.get(i).addCollinearLast(res.get(i), original2D.get(j));
}

}
}
} else
// If saturates only one station
		{
			res.addElement(new Sector2D(1, 0, 0, 1, verticesnp.get(0)));
		}

		Vector<Object> finalres = new Vector<Object>();

		for (int i = 0; i < res.size(); i++) {
			FinalSect2D fs = new FinalSect2D(res.get(i), stations, classNames);
			finalres.addElement(fs);
		}

		return finalres;
	}
/**
* 3 CLASS CASE
*
*/
public Vector<Object> Calc3D(Vector<Vertex> vertices) throws ConvexHullException {
Vector<Object> out = new Vector<Object>();
// Vertices are contained in vertices (it changes with calculations) and original
// The faces of the CHull are in faces
// The triangular sectors in which 3 stations saturate are in triangles
// The quadrangular sectors in which 2 stations saturate are in sect2station

		original3D = vertices;

		Vector<newPoint> vertices2Dxy = new Vector<newPoint>();
		Vector<newPoint> vertices2Dxz = new Vector<newPoint>();
		Vector<newPoint> vertices2Dyz = new Vector<newPoint>();

		Faces3D faces3d = new Faces3D();
// I instantiate the Beta3D class
Beta3D b3d = new Beta3D();

// I instantiate the Segment3D class
Segment3D seg = new Segment3D();

// Initialize the name of the pdf file that will be created
String name = "noname";

//------------------------------------- START ---------- ------------------------------------------//

// If there is only one station or only one is dominant it ends here
OneDominator od = new OneDominator();
		od.setVertices(vertices);
		if (od.IsOneDominator()) {
			sect1station = b3d.OneDominator(od.getDominator());

			out.addElement(faces);
			out.addElement(original3D);
			out.addElement(triangles);
			out.addElement(sect2station);
			out.addElement(sect1station);
			out.addElement(name);

			return out;
		}

		if (vertices.size() == 1) {
			sect1station = b3d.OneDominator(vertices.get(0));

			out.addElement(faces);
			out.addElement(original3D);
			out.addElement(triangles);
			out.addElement(sect2station);
			out.addElement(sect1station);
			out.addElement(name);

			return out;
		}

//------------------------------------- 3D PHASE --------- --------------------------------------//

// Check that there are at least 3 stations before doing the CHull
if (vertices.size() > 2) {

// I explode the vector of the vertices also creating the projections
vertices = faces3d.LExplode3D(vertices);

// I call the Hull3D method of the faces3d class which returns me
// the faces of the polygon already marking the coplanar ones
faces = faces3d.Hull3D(vertices);

// Check that there is at least one face
if (faces.size() > 0) {
// I remove the faces containing projections
faces = faces3d.RemoveP(faces, original3D);

// Association of Betas with triangular faces
triangles = b3d.BetaTriangles(faces);

// I generate the sides to join
sides = seg.CreateLt(triangles);

// Join of the triangles
if (triangles.size() > 1) {
sect2station = b3d.JoinTriangles(triangles);
}

// Remove the sides used by the join and add the created ones
sides = seg.FixLtFromJoin(sect2station, sides);

//------------------------------------- PHASE 2D --------- -------------------------------------------//

}// if (faces.size()>0)
}//if (vertices.size()>2)
//else System.out.println("there are not enough vertices");
		vertices2Dxy = faces3d.VertexRemoveZ(original3D);
		vertices2Dxz = faces3d.VertexRemoveY(original3D);
		vertices2Dyz = faces3d.VertexRemoveX(original3D);

		Vector<Sector2D> resxy = new Vector<Sector2D>();
		Vector<Sector2D> resxz = new Vector<Sector2D>();
		Vector<Sector2D> resyz = new Vector<Sector2D>();

Calc calculation2d = new Calc();

// Generate the side of the triangle with beta3=0
resxy = calculation2d.Calc2D(vertices2Dxy);

// Generate the side of the triangle with beta2=0
resxz = calculation2d.Calc2D(vertices2Dxz);

// Generate the side of the triangle with beta1=0
resyz = calculation2d.Calc2D(vertices2Dyz);

// I differentiate the procedure in case there is a sector with 3 saturating stations at the same time.
// from which to start to join the segments to the side of the triangle or not.

		//SIDES XY
		Vector<Vector<Object>> newresxy = new Vector<Vector<Object>>();
		Vector<Object> sidesxy = new Vector<Object>();
		sidesxy = seg.FixLtFrom2Dxy(resxy);

		//SIDES XZ
		Vector<Vector<Object>> newresxz = new Vector<Vector<Object>>();
		Vector<Object> sidesxz = new Vector<Object>();
		sidesxz = seg.FixLtFrom2Dxz(resxz);

		//SIDES YZ
		Vector<Vector<Object>> newresyz = new Vector<Vector<Object>>();
		Vector<Object> sidesyz = new Vector<Object>();
		sidesyz = seg.FixLtFrom2Dyz(resyz);

		if (faces.size() > 0) {
			newresxy = b3d.Join2Statxy(sidesxy, sides, sect2station);
			sect2station = newresxy.get(0);
			sides = newresxy.get(1);
			sidesxy = newresxy.get(2);

			newresxz = b3d.Join2Statxz(sidesxz, sides, sect2station);
			sect2station = newresxz.get(0);
			sides = newresxz.get(1);
			sidesxz = newresxz.get(2);

			newresyz = b3d.Join2Statyz(sidesyz, sides, sect2station);
			sect2station = newresyz.get(0);
			sides = newresyz.get(1);
			sidesyz = newresyz.get(2);

/*
// Print the sectors in which 2 stations are saturated
System.out.println("Number of sectors in which 2 concurrent stations saturate: "+sect2station.size());
vres.ViewRes3D2Stat(sect2station,original3D);
*/

//------------------------------ 1 STATION ---------------- ----------------------------------------
sect1station = b3d.Join1Staz(sidesxy, sidesxz, sidesyz, sides, sect1station);

sect1station = b3d.Join1Staz(sidesxy, sidesxz, sidesyz, sides, sect1station);

//------------------------------ COMPLANAR FACES ---------------- ---------------------------------

// Check if there are coplanar faces
if (faces3d.ExistsComplanar(faces)) {
//System.out.println("COMPLANAR FACES EXIST");
triangles = b3d.JoinComplanars(triangles);
//System.out.println(triangles.size());
sect2station = b3d.DeleteFake(sect2station);
}
//todo create method that adds more coplanar stations
// (i.e. those that despite being coplanar are discarded by cHull)

}// if faces.size()>0

//------------------------------ IN CASE THERE ARE NO FACES ----------- -------------------------

else //(if faces.size==0)
{
Vector<Vector<Object>> resN31 = new Vector<Vector<Object>>();
//System.out.println("There are no faces");

// Search for sectors in which 2 stations are full
			resN31 = b3d.Join2StazN3(sidesxy, sidesxz, sidesyz, sect1station, sect2station);

			//System.out.println("resN31.size(): "+resN31.size());

			sides = resN31.get(0);
			sidesxy = resN31.get(1);
			sidesxz = resN31.get(2);
			sidesyz = resN31.get(3);
			sect1station = resN31.get(4);
			sect2station = resN31.get(5);

			sect1station = b3d.Join1Staz(sidesxy, sidesxz, sidesyz, sides, sect1station);
			sect1station = b3d.Join1Staz(sidesxy, sidesxz, sidesyz, sides, sect1station);

		}

		/*
		//Generate results
		out.addElement(faces);
		out.addElement(original3D);
		out.addElement(triangles);
		out.addElement(sect2station);
		out.addElement(sect1station);
		out.addElement(name);
		*/

		Mapping3D map = new Mapping3D();
		map.RemapAllSectors(triangles);
		map.RemapAllSectors(sect2station);
		map.RemapAllSectors(sect1station);

		Vector<Object> allres = new Vector<Object>();
		for (int i = 0; i < sect1station.size(); i++) {
			allres.addElement(sect1station.get(i));
		}
		for (int i = 0; i < sect2station.size(); i++) {
			allres.addElement(sect2station.get(i));
		}
		for (int i = 0; i < triangles.size(); i++) {
			allres.addElement(triangles.get(i));
		}

		return allres;
		//return out;
	}

	public Vector<Object> Calc3D(Vector<Vertex> vertices, String[] stationNames, String[] classNames) throws ConvexHullException {
		Vector<Object> out = new Vector<Object>();
// Vertices are contained in vertices (it changes with calculations) and original
// The faces of the CHull are in faces
// The triangular sectors in which 3 stations saturate are in triangles
// The quadrangular sectors in which 2 stations saturate are in sect2station

Vector<Station3D> stations = new Vector<Station3D>();

// Create an array with the stations associated with their names
		for (int i = 0; i < vertices.size(); i++) {
			stations.add(new Station3D(stationNames[i], vertices.get(i)));
		}

		original3D = vertices;

		Vector<newPoint> vertices2Dxy = new Vector<newPoint>();
		Vector<newPoint> vertices2Dxz = new Vector<newPoint>();
		Vector<newPoint> vertices2Dyz = new Vector<newPoint>();

		Faces3D faces3d = new Faces3D();
// I instantiate the Beta3D class
Beta3D b3d = new Beta3D();

// I instantiate the Segment3D class
Segment3D seg = new Segment3D();

// Initialize the name of the pdf file that will be created
String name = "noname";

//------------------------------------- START ---------- ------------------------------------------//

// If there is only one station or only one is dominant it ends here
		OneDominator od = new OneDominator();
		od.setVertices(vertices);
		if (od.IsOneDominator()) {
			sect1station = b3d.OneDominator(od.getDominator());

			//out.addElement(faces);
			out.addElement(original3D);
			//out.addElement(triangles);
			//out.addElement(sect2station);
			out.addElement(sect1station);
			//out.addElement(name);
			Mapping3D map = new Mapping3D();
			//map.RemapAllSectors(triangles);
			//map.RemapAllSectors(sect2station);
			map.RemapAllSectors(sect1station);

			Vector<Object> allres = new Vector<Object>();
			for (int i = 0; i < sect1station.size(); i++) {
				allres.addElement(sect1station.get(i));
			}/*
			for (int i = 0; i<sect2station.size();i++) {
				allres.addElement((Sector3D) sect2station.get(i));
			}
			for (int i = 0; i<triangles.size();i++) {
				allres.addElement((Sector3D) triangles.get(i));
			}*/

			//System.out.println("allres.size() = "+allres.size());
			for (int i = 0; i < allres.size(); i++) {
				((Sector3D) allres.get(i)).givename(stations);
				((Sector3D) allres.get(i)).setClassNames(classNames);
			}

			return allres;
//todo the name and all!
//returnout;
		}

		if (vertices.size() == 1) {
			sect1station = b3d.OneDominator(vertices.get(0));

			out.addElement(faces);
			out.addElement(original3D);
			out.addElement(triangles);
			out.addElement(sect2station);
			out.addElement(sect1station);
			out.addElement(name);

			return out;
		}

		//------------------------------------- PHASE 3D   -----------------------------------------------//

// Check that there are at least 3 stations before doing the CHull
if (vertices.size() > 2) {

// I explode the vector of the vertices also creating the projections
vertices = faces3d.LExplode3D(vertices);

// I call the Hull3D method of the faces3d class which returns me
// the faces of the polygon already marking the coplanar ones
faces = faces3d.Hull3D(vertices);

// Check that there is at least one face
if (faces.size() > 0) {
// I remove the faces containing projections
faces = faces3d.RemoveP(faces, original3D);

// Association of Betas with triangular faces
triangles = b3d. BetaTriangles(faces);

// I generate the sides to join
sides = seg.CreateLt(triangles);

// Join of the triangles
if (triangles.size() > 1) {
sect2station = b3d.JoinTriangles(triangles);
}

// I remove the sides used by the join and add the created ones
				sides = seg.FixLtFromJoin(sect2station, sides);

				//------------------------------------- PHASE 2D ----------------------------------------------------//

			}// if (faces.size()>0)
		}//if (vertices.size()>2)
		//else System.out.println("not enough vertices");

		vertices2Dxy = faces3d.VertexRemoveZ(original3D);
		vertices2Dxz = faces3d.VertexRemoveY(original3D);
		vertices2Dyz = faces3d.VertexRemoveX(original3D);

		Vector<Sector2D> resxy = new Vector<Sector2D>();
		Vector<Sector2D> resxz = new Vector<Sector2D>();
		Vector<Sector2D> resyz = new Vector<Sector2D>();

		Calc calculation2d = new Calc();

// Generate the side of the triangle with beta3=0
		resxy = calculation2d.Calc2D(vertices2Dxy);

// Generate the side of the triangle with beta2=0
		resxz = calculation2d.Calc2D(vertices2Dxz);

// Generate the side of the triangle with beta1=0
		resyz = calculation2d.Calc2D(vertices2Dyz);

// I differentiate the procedure in case there is a sector with 3 saturating stations at the same time.
// from which to start to join the segments to the side of the triangle or not.

		//SIDES XY
		Vector<Vector<Object>> newresxy = new Vector<Vector<Object>>();
		Vector<Object> sidesxy = new Vector<Object>();
		sidesxy = seg.FixLtFrom2Dxy(resxy);

		//SIDES XZ
		Vector<Vector<Object>> newresxz = new Vector<Vector<Object>>();
		Vector<Object> sidesxz = new Vector<Object>();
		sidesxz = seg.FixLtFrom2Dxz(resxz);

		//SIDES YZ
		Vector<Vector<Object>> newresyz = new Vector<Vector<Object>>();
		Vector<Object> sidesyz = new Vector<Object>();
		sidesyz = seg.FixLtFrom2Dyz(resyz);

		if (faces.size() > 0) {
			newresxy = b3d.Join2Statxy(sidesxy, sides, sect2station);
			sect2station = newresxy.get(0);
			sides = newresxy.get(1);
			sidesxy = newresxy.get(2);

			newresxz = b3d.Join2Statxz(sidesxz, sides, sect2station);
			sect2station = newresxz.get(0);
			sides = newresxz.get(1);
			sidesxz = newresxz.get(2);

			newresyz = b3d.Join2Statyz(sidesyz, sides, sect2station);
			sect2station = newresyz.get(0);
			sides = newresyz.get(1);
			sidesyz = newresyz.get(2);

			/*
// Print the sectors in which 2 stations are saturated
System.out.println("Number of sectors in which 2 concurrent stations saturate: "+sect2station.size());
vres.ViewRes3D2Stat(sect2station,original3D);
*/

//------------------------------ 1 STATION ---------------- ----------------------------------------
			sect1station = b3d.Join1Staz(sidesxy, sidesxz, sidesyz, sides, sect1station);

			sect1station = b3d.Join1Staz(sidesxy, sidesxz, sidesyz, sides, sect1station);

//------------------------------ COMPLANAR FACES ---------------- ---------------------------------

// Check if there are coplanar faces
			if (faces3d.ExistsComplanar(faces)) {
				//System.out.println("ESISTONO FACCE COMPLANARI");
				triangles = b3d.JoinComplanars(triangles);
				//System.out.println(triangles.size());
				sect2station = b3d.DeleteFake(sect2station);
			}
//todo create method that adds more coplanar stations
// (i.e. those that despite being coplanar are discarded by cHull)
		}// if faces.size()>0

//------------------------------ CASE THERE ARE NO FACES ----------- -------------------------

		else //(if faces.size==0)
		{
			Vector<Vector<Object>> resN31 = new Vector<Vector<Object>>();
//System.out.println("There are no faces");

			// Search sectors where 2 stations saturate
			resN31 = b3d.Join2StazN3(sidesxy, sidesxz, sidesyz, sect1station, sect2station);

			//System.out.println("resN31.size(): "+resN31.size());

			sides = resN31.get(0);
			sidesxy = resN31.get(1);
			sidesxz = resN31.get(2);
			sidesyz = resN31.get(3);
			sect1station = resN31.get(4);
			sect2station = resN31.get(5);

			sect1station = b3d.Join1Staz(sidesxy, sidesxz, sidesyz, sides, sect1station);
			sect1station = b3d.Join1Staz(sidesxy, sidesxz, sidesyz, sides, sect1station);

		}

//------------------------------ MAPPING IN THE 2 DIMENSIONS OF THE BETA SPACE ----------- --------

		Mapping3D map = new Mapping3D();
		if (faces.size() > 0) {
			map.RemapAllSectors(triangles);
		}
		map.RemapAllSectors(sect2station);
		map.RemapAllSectors(sect1station);

		Vector<Object> allres = new Vector<Object>();
		for (int i = 0; i < sect1station.size(); i++) {
			allres.addElement(sect1station.get(i));
		}
		for (int i = 0; i < sect2station.size(); i++) {
			allres.addElement(sect2station.get(i));
		}
		if (faces.size() > 0) {
			for (int i = 0; i < triangles.size(); i++) {
				allres.addElement(triangles.get(i));
			}
		}

		//System.out.println("allres.size() = "+allres.size());
		for (int i = 0; i < allres.size(); i++) {
			((Sector3D) allres.get(i)).givename(stations);
			((Sector3D) allres.get(i)).setClassNames(classNames);
		}

		return allres;
		//return out;
	}

// Methods for getting the results
	public Vector<newFace> getFaces() {
		return faces;
	}

	public Vector<Object> gettriangles() {
		return triangles;
	}

	public Vector<Object> getsect1station() {
		return sect1station;
	}

	public Vector<Object> getsect2station() {
		return sect2station;
	}

}
