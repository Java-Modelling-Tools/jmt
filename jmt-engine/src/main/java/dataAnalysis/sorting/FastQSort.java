package dataAnalysis.sorting;

/**
 * A quick sort algorithm.
 */
public class FastQSort implements SortAlgorithm {

    /**
     * This is a generic version of C.A.R Hoare's Quick Sort
     * algorithm.  This will handle arrays that are already
     * sorted, and arrays with duplicate keys.<BR>
     *
     * If you think of a one dimensional array as going from
     * the lowest index on the left to the highest index on the right
     * then the parameters to this function are lowest index or
     * left and highest index or right.  The first time you call
     * this function it will be with the parameters 0, a.length - 1.
     *
     * @param array     a double array
     * @param left      left boundary of array partition
     * @param right     right boundary of array partition
     */
    private void quickSort(double[] array, int left, int right) {
        if (right > left) {
            double pivot = array[right];
            int i = left - 1;
            int j = right;

            for (;;) {
                while (array[++i] < pivot) {
                    // Do nothing, just increment i
                }
                while (array[--j] > pivot) {
                    // Do nothing, just decrement j
                }
                if (i >= j) {
                    break;
                }
                // swap
                double temp = array[i];
                array[i] = array[j];
                array[j] = temp;
            }

            double temp = array[i];
            array[i] = array[right];
            array[right] = temp;

            quickSort(array, left, i - 1);
            quickSort(array, i + 1, right);
        }
    }

    @Override
    public void sort(double[] array) {
        if (array != null && array.length > 1) {
            quickSort(array, 0, array.length - 1);
        }
    }
}