package jmt.gui.jsimgraph.JGraphMod;

import java.util.*;
/**
 * <p>Title: JmtEdgeBreakagesIterator</p>
 * <p>Description: Stores the identifiers for broken arcs </p>
 *
 * @author Emma Bortone
 *         Date: 13-feb-2020
 *         Time: 13.33.00
 *
 */
public class JmtEdgeBreakagesIterator {


    private HashMap<Object,HashMap<Object, List<Integer>>> actualLetter;
    private List<String> letters = Arrays.asList("A", "B", "C", "D", "E","F","G","H","I","J","K","L","M","N","O","P","Q",
            "R","S","T","U","V","W","X","Y","Z"," ");
    private List<Boolean> isAvailableIndex;

    public JmtEdgeBreakagesIterator(){
        actualLetter = new HashMap<Object,HashMap<Object, List<Integer>>>();
        int n = letters.size();
        Boolean[] isAvailable = new Boolean[n];
        //Assign all the elements of boolean array to true
        Arrays.fill(isAvailable,true);
        isAvailableIndex = new ArrayList<Boolean>();
        Collections.addAll(isAvailableIndex, isAvailable);
    }

    /**
     * Returns the identifier letter for a given connection and a given breakage in that connection
     * @param sourceKey : key of the source station of the connection
     * @param targetKey : key of the target station of the connection
     * @param breakIndex : index of the breakage in this connection
     * @return identifier letter for a given connection and a given breakage in that connection
     */
    public String getLetter(Object sourceKey,Object targetKey,int breakIndex){
        while(!isAttributed(sourceKey,targetKey,breakIndex)) {
            attributeNewLetter(sourceKey,targetKey);
        }
        return (letters.get(actualLetter.get(sourceKey).get(targetKey).get(breakIndex)));
    }

    /**
     * Assigns a new letter to a given connection
     * @param sourceKey : key of the source station of the connection
     * @param targetKey : key of the target station of the connection
     */
    private void attributeNewLetter(Object sourceKey, Object targetKey) {
        int nextLetter = findNextLetter();
        if(!actualLetter.keySet().contains(sourceKey)){
            actualLetter.put(sourceKey,new HashMap<Object, List<Integer>>());
        }
        if(!actualLetter.get(sourceKey).keySet().contains(targetKey)){
           actualLetter.get(sourceKey).put(targetKey,new ArrayList<Integer>());
        }
        actualLetter.get(sourceKey).get(targetKey).add(nextLetter);
        isAvailableIndex.set(nextLetter,false);
    }

    /**
     * @return the next available letter
     */
    private int findNextLetter() {
        int n = isAvailableIndex.size();
        for(int i =0;i <n; i++){
            if(isAvailableIndex.get(i)){
                return i;
            }
        }
        return(n-1);
    }

    /**
     *
     * @param sourceKey : key of the source station of the connection
     * @param targetKey : key of the target station of the connection
     * @param breakIndex : index of the breakage in this connection
     * @return true if the specific connection and breakage already has a letter assigned to it
     *         false otherwise
     */
    private boolean isAttributed(Object sourceKey, Object targetKey, int breakIndex) {
        if(actualLetter.keySet().contains(sourceKey)){
            if(actualLetter.get(sourceKey).keySet().contains(targetKey)){
                return actualLetter.get(sourceKey).get(targetKey).size() > breakIndex;
            }
        }
        return false;
    }

    /**
     * Removes one attributed letter for the connection identified by sourceKey and targetKey
     * @param sourceKey : key of the source station of the connection
     * @param targetKey : key of the target station of the connection
     */
    public void removeLast(Object sourceKey,Object targetKey){
        if(actualLetter.keySet().contains(sourceKey)){
            if(actualLetter.get(sourceKey).keySet().contains(targetKey)){
                int size = actualLetter.get(sourceKey).get(targetKey).size();
                if(size >0){
                   int indexLetter = actualLetter.get(sourceKey).get(targetKey).get(size-1);
                   actualLetter.get(sourceKey).get(targetKey).remove(size-1);
                   isAvailableIndex.set(indexLetter,true);
               }
            }
        }
    }

    /**
     * @param sourceKey : key of the source station of the connection
     * @param targetKey : key of the target station of the connection
     * @return the number of identifiers attributed to the connection identified by sourceKey and targetKey
     */
    public int getNumberAttributedTo(Object sourceKey,Object targetKey){
        int size=0;
        if(actualLetter.keySet().contains(sourceKey)){
            if(actualLetter.get(sourceKey).keySet().contains(targetKey)){
                size = actualLetter.get(sourceKey).get(targetKey).size();

            }
        }
        return(size);
    }

    /**
     *Removes all the attributed letters for the connection identified by sourceKey and targetKey
     * @param sourceKey : key of the source station of the connection
     * @param targetKey : key of the target station of the connection
     */
    public void removeAll(Object sourceKey,Object targetKey){
        while(getNumberAttributedTo(sourceKey,targetKey)>0){
            removeLast(sourceKey,targetKey);
        }
    }
}
