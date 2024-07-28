package jmt.gui.common.definitions;

import java.util.ArrayList;
import java.util.List;

public class ServerType {

    private int id;
    private Object serverTypeKey;
    private Object stationKey;
    private String name;
    List<Object> compatibleClassKeys;
    private int numOfServers;

    public ServerType(String name, int numOfServers, Object serverKey, Object stationKey, int id){
        this.serverTypeKey = serverKey;
        this.stationKey = stationKey;
        this.name = name;
        this.numOfServers = numOfServers;
        this.compatibleClassKeys = new ArrayList<>();
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id){
        this.id = id;
    }

    public Object getServerKey() {
        return serverTypeKey;
    }

    public Object getStationKey() {
        return stationKey;
    }

    public void incrementNumOfServers(int num){
        numOfServers += num;
    }

    public void decrementNumOfServers(int num) {
        numOfServers -= num;
    }

    public int getNumOfServers(){
        return numOfServers;
    }

    public String getName(){
        return name;
    }

    public void setNumOfServers(int numOfServers){
        this.numOfServers = numOfServers;
    }

    public void setName(String name){
        this.name = name;
    }

    public void addCompatibility(Object classKey){
        compatibleClassKeys.add(classKey);
    }

    public boolean isCompatible(Object classKey){
        return compatibleClassKeys.contains(classKey);
    }

    public void removeCompatibility(Object classKey){
        compatibleClassKeys.remove(classKey);
    }

    public List<Object> getCompatibleClassKeys(){
        return compatibleClassKeys;
    }

}
