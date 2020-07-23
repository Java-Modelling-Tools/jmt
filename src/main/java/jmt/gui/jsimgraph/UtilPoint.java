package jmt.gui.jsimgraph;

import java.awt.geom.Point2D;
import java.util.List;

public class UtilPoint {

    public static Point2D subtractPoints(Point2D p1, Point2D p2){
        return new Point2D.Double(p1.getX()-p2.getX(),p1.getY()-p2.getY());
    }

    public static boolean equalsWithTolerance(Point2D p1, Point2D p2){
        return equalsWithTolerance(p1,p2,0.001);
    }

    public static boolean equalsWithTolerance(Point2D p1, Point2D p2, double tol){
        return ((Math.abs(p1.getX() - p2.getX()) < tol) && (Math.abs(p1.getY() - p2.getY()) < tol));
    }

    public static Point2D addPoints(Point2D p1, Point2D p2){
        return new Point2D.Double(p1.getX()+p2.getX(),p1.getY()+p2.getY());
    }

    public static Point2D inversePoint(Point2D point){
        return new Point2D.Double(-point.getX(),-point.getY());
    }

    public static Point2D addMultiplePoints(Point2D... listPoint){
        Point2D res= new Point2D.Double(0,0);
        for (Point2D point : listPoint){
            res=new Point2D.Double(res.getX()+point.getX(),res.getY()+point.getY());
        }
        return  res;
    }



}
