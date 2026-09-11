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
 * ProcessorSharing Simulation class
 * 
 * @author Lorenzo Torri
 * Date: 30-may-2024
 * Time: 14.20
 */
public abstract class ProcessorSharingSimulation implements Simulation {
    
    public SimulationType getType(){
        return SimulationType.PROCESSOR_SHARING;
    }
}


/*
 * PROCESSOR SHARING algorithms
 * - PS
 */
class PS extends ProcessorSharingSimulation{

    @Override
    public String getName() {
        return Constants.PS;
    }

    @Override
    public String getDescription() {
        return Constants.PS_DESCRIPTION;
    }
    
}
