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

package random;

import jmt.common.exception.IncorrectDistributionParameterException;

/**
 *
 * This is the parameter that should be passed to the Batched distribution.
 *
 * <br><br>Copyright (c) 2003
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 * @author Jerry Zhu
 *
 */
public class BatchPar extends AbstractParameter implements Parameter {
    /** The base distribution */
    private Distribution base;

    /** The parameters of the parent distribution */
    private Parameter baseParameter;

    /** The batch size distribution */
    private Distribution batchSize;

    /** The parameter of the batch size distribution */
    private Parameter batchSizeParameter;

    /** The fixed batch size value, will be used instead of [batchSize] if IsFixedBatchedSize is true */
    private int fixedBatchSize;

    /** If true, the batch size is a fixed value instead of a random variable */
    private boolean isFixedBatchSize;

    /**
     * It creates a new batched parameter.
     */
    public BatchPar(
            DistributionContainer base,
            DistributionContainer batchSize,
            Integer fixedBatchSize,
            Boolean IsFixedBatchSize) throws IncorrectDistributionParameterException {
        this.base = base.getDistribution();
        this.baseParameter = base.getParameter();
        this.isFixedBatchSize = IsFixedBatchSize;
        if (isFixedBatchSize) {
            this.fixedBatchSize = fixedBatchSize;
        } else {
            this.batchSize = batchSize.getDistribution();
            this.batchSizeParameter = batchSize.getParameter();
        }
    }

    /**
     * An empty constructor.
     */
    public BatchPar() { }

    /**
     * It checks if the parameter is correct or not.
     */
    @Override
    public boolean check() {
        if (isFixedBatchSize) {
            return fixedBatchSize > 0;
        } else {
            return true;
        }
    }

    /**
     * Getters and setters
     */

    public Distribution getBase() {
        return base;
    }

    public void setBase(Distribution base) {
        this.base = base;
    }

    public Parameter getBaseParameter() {
        return baseParameter;
    }

    public void setBaseParameter(Parameter baseParameter) {
        this.baseParameter = baseParameter;
    }

    public Distribution getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Distribution batchSize) {
        this.batchSize = batchSize;
    }

    public Parameter getBatchSizeParameter() {
        return batchSizeParameter;
    }

    public void setBatchSizeParameter(Parameter batchSizeParameter) {
        this.batchSizeParameter = batchSizeParameter;
    }

    public int getFixedBatchSize() {
        return fixedBatchSize;
    }

    public void setFixedBatchSize(int fixedBatchSize) {
        this.fixedBatchSize = fixedBatchSize;
    }

    public boolean getIsFixedBatchSize() {
        return isFixedBatchSize;
    }

    public void setIsFixedBatchSize(boolean IsFixedBatchSize) {
        this.isFixedBatchSize = IsFixedBatchSize;
    }
} // end BatchedExponentialPar
