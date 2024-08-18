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
 * Common Interface to all the simulations.
 * 
 * @author Lorenzo Torri
 * Date: 30-may-2024
 * Time: 13.23
 */


/*
 * Little Explanation on how this folder 'Simulation' is composed
 * The superclass is Simulation.
 * For each type of Simulation: Non-Preemptive, Preemptive... there are different abstract class.
 * For each abstract class there are subclasses that represent the possible algorithms
 */
public interface Simulation {
    public String getName();

    public String getDescription();

    public SimulationType getType();
}
