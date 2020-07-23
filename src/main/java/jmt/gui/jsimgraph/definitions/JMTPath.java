package jmt.gui.jsimgraph.definitions;

import java.util.ArrayList;
/**
 * <p>Title: JMTPath </p>
 * <p>Description: data structure used to define the shape of a Bezier connection</p>
 *
 * @author Emma Bortone
 * Date: 2020
 *
 */
public class JMTPath {
    private static final long serialVersionUID = 1L;
    protected ArrayList<JMTArc> arcs  ;

    public JMTPath(ArrayList<JMTArc> arcs) {
        this.arcs = arcs;
    }

    public JMTPath(JMTPath original) {
        this.arcs = new ArrayList<JMTArc>();
        for(int i =0;i<original.arcs.size();i++){
            arcs.add(new JMTArc(original.getArc(i)));
        }
    }

    public ArrayList<JMTArc> getArcs() {
        return arcs;
    }

    public int getArcsNb(){
        return arcs.size();
    }

    public JMTArc getArc(int i){
        return arcs.get(i);
    }

    public void setArcs(ArrayList<JMTArc> arcs){
        this.arcs = arcs;
    }


}
