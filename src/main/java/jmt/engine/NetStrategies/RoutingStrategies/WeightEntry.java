package jmt.engine.NetStrategies.RoutingStrategies;

public class WeightEntry {

    private Object value;
    private Integer weight;

    public WeightEntry(String value, Integer weight) {
        this.value = value;
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public Object getValue() {
        return value;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public void setValue(Object value) {
        this.value = value;
    }

}
