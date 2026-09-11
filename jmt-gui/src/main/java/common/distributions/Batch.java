/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package common.distributions;

import common.JMTImageLoader;

import javax.swing.*;

/**
 * <p>Title: Batched Distribution</p>
 * <p>Description: Batched distribution data structure</p>
 *
 * @author Jerry Zhu
 *         Date: 07-March-2025
 */
public class Batch extends Distribution {

    /**
     * Constructs a new Batched Distribution
     */
    public Batch() {
        super("Batch", "random.Batch", "random.BatchPar", "Batch distribution");
        hasMean = false;
        hasC = false;
        isNestable = false;
        setValueChecker(new ValueChecker() {
            public boolean checkValue(Object value) {
                if (parameters[0].getValue() == null) {
                    precondition = "Base distribution has to be defined.";
                    return false;
                }
                return true;
            }
        });
    }

    /**
     * Used to set parameters of this distribution
     * @return distribution parameters
     */
    @Override
    protected Parameter[] setParameters() {
        // Creates parameter array
        Parameter[] parameters = new Parameter[4];

        parameters[0] = new Parameter(
                "BaseDistribution",
                "Base distribution",
                Distribution.class,
                null,
                true
        );
        parameters[1] = new Parameter(
                "BatchSizeDistribution",
                "Batch size distribution",
                Distribution.class,
                null,
                true
        );
        parameters[2] = new Parameter(
                "FixedBatchSize",
                "Fixed batch size",
                Integer.class,
                1,
                true
        );
        parameters[3] = new Parameter(
                "IsFixedBatchSize",
                "Whether the distribution uses a fixed batch size or a batch size distribution",
                Boolean.class,
                true,
                true
        );
        return parameters;
    }

    /**
     * Sets illustrating figure in distribution panel
     * @return illustrating figure
     */
    @Override
    protected ImageIcon setImage() {
        return JMTImageLoader.loadImage("Batch");
    }

    /**
     * Returns this distribution's short description
     * @return distribution's short description
     */
    @Override
    public String toString() {
        if (parameters[3].getValue().equals(true)) {
            String base = parameters[0].getValue().toString();
            String size = parameters[2].getValue().toString();
            return "batch(" + base + ",[" + size + "," + size + "])";
        } else {
            String base = parameters[0].getValue().toString();
            String size = parameters[1].getValue().toString();
            return "batch(" + base + "," + size + ")";
        }
    }
}
