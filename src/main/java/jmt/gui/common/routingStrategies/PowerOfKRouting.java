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

package jmt.gui.common.routingStrategies;

import java.util.Map;

/**
 * Power of K Routing Strategy
 * @author Vaibhav Krishnakumar
 */
public class PowerOfKRouting extends RoutingStrategy {

    private static final String POK_DESC = "k random stations are chosen from those connected to the current one. Jobs are routed to the station with the smallest queue length amongst these k (number of jobs, current value).";
    private static final String POK_WITH_MEM_DESC = "k random stations are chosen from those connected to the current one. Their queue lengths are recorded in a local \"memory\" array. Jobs are routed to the node with the smallest queue length in the memory (number of jobs).";

    // Set default value of k to 2
    private Integer k = 2;
    // Set default memory disabled
    private Boolean withMemory = false;

    public PowerOfKRouting() {
        updateDescription();
    }

    private void updateDescription() {
        description = withMemory ? POK_WITH_MEM_DESC : POK_DESC;
    }

    @Override
    public String getName() {
        return "Power of k";
    }

    @Override
    public Map<Object, Double> getValues() {
        return null;
    }

    @Override
    public PowerOfKRouting clone() {
        PowerOfKRouting pr = new PowerOfKRouting();
        pr.setK(k);
        pr.setWithMemory(withMemory);
        return pr;
    }

    @Override
    public String getClassPath() {
        return "jmt.engine.NetStrategies.RoutingStrategies.PowerOfKRoutingStrategy";
    }

    /**
     * Returns true if the routing strategy is dependent on the state of
     * the model
     * @return true if the routing strategy is dependent on the state of
     * the model
     *
     * Author: Francesco D'Aquino
     */
    @Override
    public boolean isModelStateDependent() {
        return true;
    }

    @Override
    public void addStation(Object stationKey) {
    }

    @Override
    public void removeStation(Object stationKey) {
    }

    public Integer getK() {
        return k;
    }

    public void setK(Integer k) {
        this.k = k;
    }

    public Boolean isWithMemory() {
        return withMemory;
    }

    public void setWithMemory(Boolean withMemory) {
        this.withMemory = withMemory;
        updateDescription();
    }
}
