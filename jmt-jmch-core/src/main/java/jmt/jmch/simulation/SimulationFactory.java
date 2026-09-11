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

/**
 * Class that resambles the Factory pattern.
 * Create a simulation based on the type and the algorithm 
 * 
 * @author Lorenzo Torri
 * Date: 30-may-2024
 * Time: 14.24
 */
public class SimulationFactory {
    public static Simulation createSimulation(SimulationType type, String algorithm){
        switch(type){
            case NON_PREEMPTIVE:
                switch(algorithm){
                    case "FCFS":
                        return new FCFS();
                    case "LCFS":
                        return new LCFS();
                    case "SJF":
                        return new SJF();
                    case "LJF":
                        return new LJF();
                }
                break;
            case PREEMPTIVE:
                break;
            case PROCESSOR_SHARING:
                switch(algorithm){
                    case "PS":
                        return new PS();
                }
                break;
            case ROUTING:
                switch(algorithm){
                    case "RR":
                        return new RR();
                    case "PROBABILITIES":
                        return new PROBABILISTIC();
                    case "JSQ":
                        return new JSQ();
                }
                break;
            default:
                break;
            
        }
        return null;
    }
}
