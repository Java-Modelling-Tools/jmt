package dataAnalysis.sorting;

//
// Modified by Lev Shereshevsky (leo@aeriesoft.ru) 98/03/20
//

/*
 * @(#)MergeSort.java  1.0 95/06/23 Jason Harrison
 *
 * Copyright (c) 1995 University of British Columbia
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies. Please refer to the file "copyright.html"
 * for further important copyright and licensing information.
 *
 * UBC MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. UBC SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

/**
 * A merge sort demonstration algorithm.
 * SortAlgorithm.java, Thu Oct 27 10:32:35 1994
 *
 * @author Jason Harrison@cs.ubc.ca
 * @version 1.1, 12 Jan 1998
 */
public class MergeSort implements SortAlgorithm {

    /**
     * pre :  0 &lt;= left &lt;= right &lt;= array.length
     * <p>
     * post :  elements of array[left..right] are rearranged so that
     * <p>
     * array[left] &lt;= array[left + 1] &lt;= ... &lt;= array[right]
     */
    private static void sort(double[] array, int left, int right) {
        if (left < right) {
            /* split the data into two halves */
            int mid = (left + right) / 2;

            /* recursively sort each half */
            sort(array, left, mid);
            sort(array, mid + 1, right);

            /* ``merge'' the two halves */
            merge(array, left, mid, right);
        }
    }

    /**
     * pre :  (i)   0 &lt;= left &lt;= mid &lt; right &lt;= array.length-1
     * (ii)  array[left] &lt;= array[left+1] &lt;= ... &lt;= array[mid]
     * (iii) array[mid+1] &lt;= array[mid+2] &lt;= ... &lt;= array[right]
     * <p>
     * post :  elements of array[left..right] are rearranged so that
     * <p>
     * array[left] &lt;= array[left+1] &lt;= ... &lt;= array[right]
     */
    private static void merge(double[] array, int left, int mid, int right) {
        int size = right - left + 1;
        double[] copy = new double[size];

        /* copy data to be merged into array ``copy'' */
        for (int i = 0; i < size; i++) {
            copy[i] = array[left + i];
        }

        int copyMid = mid - left; // index of middle element in ``copy''
        int copyEnd = right - left; // index of last element in ``copy''

        int i = 0;
        int j = copyMid + 1;
        int k = left;

        while (i <= copyMid && j <= copyEnd) {
            /* invariant:
             *   array[left] <= array[left+1] <= ... <= array[k] <= min(copy[i], copy[j])
             */
            if (copy[i] < copy[j]) {
                array[k++] = copy[i++];
            } else {
                array[k++] = copy[j++];
            }
        }

        // copy left over stuff
        int a0, aN;
        if (i > copyMid) {
            a0 = j;
            aN = copyEnd;
        } else {
            a0 = i;
            aN = copyMid;
        }

        /* array[k-1] <= copy[a0] <= copy[a0+1] <= ... <= copy[aN] */

        // copy rest
        for (int l = a0; l <= aN; l++) {
            array[k++] = copy[l];
        }
    }

    @Override
    public void sort(double[] data) {
        if (data != null && data.length > 1) {
            sort(data, 0, data.length - 1);
        }
    }

}
