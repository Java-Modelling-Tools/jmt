package jmt.jmva.analytical.solvers.utilities;

import java.util.Arrays;

public class MVAPopulation {

    /**
     * A wrapper class for the population array, so it can safely be used in the hash map
     */

    private final int[] population;

    public MVAPopulation(int[] population) {
        this.population = population;
    }

    public int[] getPopulation() {
        return population;
    }

    public boolean emptyClass(int clas) {
        return population[clas] == 0;
    }

    public void decClassPopulation(int clas) {
        population[clas]--;
    }

    public void decClassPopulation(int clas, int n) {
        population[clas] -= n;
    }

    public void decClassPopulation(int clas, double n) {
        population[clas] -= (int) n;
    }


    public void incClassPopulation(int clas) {
        population[clas]++;
    }

    public void incClassPopulation(int clas, int n) {
        population[clas] += n;
    }

    public void incClassPopulation(int clas, double n) {
        population[clas] += (int) n;
    }

    // Ensures the content of the array is equal
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MVAPopulation that = (MVAPopulation) o;
        return Arrays.equals(this.population, that.population);
    }

    // Hashes of the content of the array
    // Arrays default hash inherits from Object so ignores the array content
    @Override
    public int hashCode() {
        return Arrays.hashCode(population);
    }
}
