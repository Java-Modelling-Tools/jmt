package jmt.jmva.analytical.solvers;

import java.util.*;
import java.lang.Math;

public class Util {
    /**
     * Generates all populations needed for MVA to compute indices for the required population array
     * @param nDist - final population array
     * @return an array of population arrays
     */
    public static int[][] generatePopulations(int[] nDist) {
        // number of classes
        int C = nDist.length;
        // total number of customers
        int N = 0;
        // number of population states
        int numPops = 1;
        for (int i = 0; i < C; i++) {
            N += nDist[i];
            numPops *= (nDist[i] + 1);
        }
        // disregard the 0 vector population, hence numPops-1
        int[][] populations = new int[numPops][C];
        List<int[]> pops = new ArrayList<>();
        pops.add(new int[C]);
        // population that is generated
        int[] pop;
        // copy of a generated population
        int[] popCopy;
        int idx = 0;
        for (int i = 0; i < N; i++) {
            // Add all valid populations of size i
            int j = pops.size();
            while (j > 0) {
                pop = pops.get(0);
                pops.remove(0);
                j--;
                for (int k = 0; k < C; k++) {
                    pop[k]++;
                    if (validPopulation(pop, nDist) && !isInList(pops, pop)) {
                        popCopy = Arrays.copyOf(pop,pop.length);
                        pops.add(popCopy);
                        assert (idx < numPops) :
                            "Population index out of bounds";
                        populations[idx] = popCopy;
                        idx++;
                    }
                    pop[k]--;
                }
            }
        }
        return sortPopulations(populations);
    }

    /**
     * Sorts population arrays in increasing order from right to left
     * @param populations - an array of population arrays to be sorted
     * @return sorted array of population arrays
     */
    private static int[][] sortPopulations(int[][] populations) {
        Arrays.sort(populations, new java.util.Comparator<int[]>() {
            public int compare(int[] a, int[] b) {
                assert a.length == b.length : "Arrays have different sizes";
                for (int i = a.length - 1; i >= 0; i--) {
                    if (a[i] < b[i]) {
                        return -1;
                    } else if (a[i] > b[i]) {
                        return 1;
                    }
                }
                return 0;
            }
        });
        return populations;
    }

    /**
     * Checks is a population array is valid, i.e. population of each class is
     * smaller or equal to the population of each class in the final population array
     * @param pop - population array to be checked
     * @param nDist - final population array (population distribution)
     * @return true if pop is valid, false otherwise
     */
    private static boolean validPopulation(int[] pop, int[] nDist) {
        assert nDist.length == pop.length :
                "Population number of classes is different " +
                        "from the actual number of classes";
        for (int i = 0; i < pop.length; i++)
            if (pop[i] > nDist[i]) return false;

        return true;
    }

    /**
     * Checks if a given array is in a given list
     * @param list - list of integer arrays
     * @param candidate - integer array to be checked
     * @return true if candidate is in list, false otherwise
     */
    private static boolean isInList(final List<int[]> list, final int[] candidate){

        for(final int[] item : list){
            if(Arrays.equals(item, candidate)){
                return true;
            }
        }
        return false;
    }

    /**
     * Prints to console population arrays
     * @param pops - an array of population arrays
     */
    private static void printPops(int[][] pops) {
        for (int i = 0; i < pops.length; i++) {
            printPop(pops[i]);
        }
    }

    /**
     * Prints to console the population array
     * @param pop - population array
     */
    private static void printPop(int[] pop) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < pop.length; i++) {
            sb.append(pop[i]);
            sb.append(", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(')');
        System.out.println(sb.toString());
    }

    /**
     * Computes a hash code for a population array
     * @param pop - population array
     * @param max - maximum population in the final population array
     * @return an integer hash code of pop
     */
    public static int popHashCode(int[] pop, int max) {
        // TODO: deal with integer overflow
        int hash = 0;
        for (int i = 0; i < pop.length-1; i++) {
            hash += (pow(max+1, i) * pop[i]);
        }
        return hash;
    }


    /**
     * Computes an integer raised to a power of another integer
     * @param a
     * @param b
     * @return a ^ b
     */
    public static int pow(int a, int b) {
        int re = 1;
        while (b > 0) {
            if ((b & 1) == 1) {
                re *= a;
            }
            b >>= 1;
            a *= a;
        }
        return re;
    }


    public static void main(String[] args) {
        int[][] pops = generatePopulations(new int[]{2,2});
        for (int i = 0; i < pops.length; i++) {
            System.out.println(popHashCode(pops[i], 2));
        }

    }

}
