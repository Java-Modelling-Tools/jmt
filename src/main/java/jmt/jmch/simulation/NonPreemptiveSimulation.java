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

import java.util.Arrays;
import java.util.List;

import jmt.jmch.Constants;

/**
 * NonPreemptive Simulation class
 * It has a list with all its subclasses
 * 
 * @author Lorenzo Torri
 * Date: 30-may-2024
 * Time: 14.13
 */
public abstract class NonPreemptiveSimulation implements Simulation{
    private static final List<String> algorithms = Arrays.asList(FCFS.NAME, LCFS.NAME, SJF.NAME, LJF.NAME);

    public static List<String> getAlgorithms() {
        return algorithms;
    }

    public SimulationType getType(){
        return SimulationType.NON_PREEMPTIVE;
    }
}

/*
 * NON PREEMPTIVE algorithms
 * - FCFS
 * - LCFS
 * - SJF
 * - LJF
 */
class FCFS extends NonPreemptiveSimulation{
    protected static final String NAME = Constants.FCFS; 

    @Override
    public String getName() {
        return Constants.FCFS;
    }

    @Override
    public String getDescription() {
        return Constants.FCFS_DESCRIPTION;
    }    
}

class LCFS extends NonPreemptiveSimulation{
    protected static final String NAME = Constants.LCFS;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return Constants.LCFS_DESCRIPTION;
    }
}

class SJF extends NonPreemptiveSimulation{
    protected static final String NAME = Constants.SJF;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return Constants.SJF_DESCRIPTION;
    }
}

class LJF extends NonPreemptiveSimulation{
    protected static final String NAME = Constants.LJF;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return Constants.LJF_DESCRIPTION;
    }
}
