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
package jmt.jmch.simulation;

import jmt.jmch.Constants;

/**
 * Routing Simulation class
 * 
 * @author Lorenzo Torri
 * Date: 30-may-2024
 * Time: 14.20
 */
public abstract class RoutingSimulation implements Simulation {
    public SimulationType getType(){
        return SimulationType.ROUTING;
    }
}

/*
 * ROUTING algorithms
 * - RR
 * - PROBABILISTIC
 * - JSQ
 */
class RR extends RoutingSimulation{

    @Override
    public String getName() {
        return Constants.RR;
    }

    @Override
    public String getDescription() {
        return Constants.RR_DESCRIPTION;
    }
} 

class PROBABILISTIC extends RoutingSimulation{

    @Override
    public String getName() {
        return Constants.PROBABILISTIC;
    }

    @Override
    public String getDescription() {
        return Constants.PROB_DESCRIPTION;
    }
}

class JSQ extends RoutingSimulation{

    @Override
    public String getName() {
        return Constants.JSQ;
    }

    @Override
    public String getDescription() {
        return Constants.JSQ_DESCRIPTION;
    }
}
