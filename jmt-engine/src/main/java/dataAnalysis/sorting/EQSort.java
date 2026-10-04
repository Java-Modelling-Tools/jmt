package dataAnalysis.sorting;

//
// Modified by Lev Shereshevsky (leo@aeriesoft.ru) 98/03/20
//

/*
 * @(#)EQSort.java     1.0 95/06/26 Jim Boritz
 *
 * Copyright (c) 1995 UBC Microsystems, Inc. All Rights Reserved.
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
 * 19 Feb 1996: Fixed to avoid infinite loop discovered by Paul Haberli.
 *              Misbehaviour expressed when the pivot element was not unique.
 *              -Jason Harrison
 *
 * 21 Jun 1996: Modified code based on comments from Paul Haeberli, and
 *              Peter Schweizer (Peter.Schweizer@mni.fh-giessen.de).
 *              Used Daeron Meyer's (daeron@geom.umn.edu) code for the
 *              new pivoting code. - Jason Harrison
 *
 * 09 Jan 1998: Another set of bug fixes by Thomas Everth (everth@wave.co.nz)
 *              and John Brzustowski (jbrzusto@gpu.srv.ualberta.ca).
 */

/**
 * An enhanced quick sort demonstration algorithm.
 * SortAlgorithm.java, Thu Oct 27 10:32:35 1994
 *
 * @author Jim Boritz
 * @version     1.0, 26 Jun 1995
 */
public class EQSort implements SortAlgorithm {

    private void brute(double[] array, int lo, int hi) {
        if ((hi - lo) == 1) {
            if (array[hi] < array[lo]) {
                double temp = array[lo];
                array[lo] = array[hi];
                array[hi] = temp;
            }
        }
        if ((hi - lo) == 2) {
            int pmin = array[lo] < array[lo + 1] ? lo : lo + 1;
            pmin = array[pmin] < array[lo + 2] ? pmin : lo + 2;
            if (pmin != lo) {
                double temp = array[lo];
                array[lo] = array[pmin];
                array[pmin] = temp;
            }
            brute(array, lo + 1, hi);
        }
        if ((hi - lo) == 3) {
            int pmin = array[lo] < array[lo + 1] ? lo : lo + 1;
            pmin = array[pmin] < array[lo + 2] ? pmin : lo + 2;
            pmin = array[pmin] < array[lo + 3] ? pmin : lo + 3;
            if (pmin != lo) {
                double temp = array[lo];
                array[lo] = array[pmin];
                array[pmin] = temp;
            }
            int pmax = array[hi] > array[hi - 1] ? hi : hi - 1;
            pmax = array[pmax] > array[hi - 2] ? pmax : hi - 2;
            if (pmax != hi) {
                double temp = array[hi];
                array[hi] = array[pmax];
                array[pmax] = temp;
            }
            brute(array, lo + 1, hi - 1);
        }
    }

    private void sort(double[] array, int lo0, int hi0) {
        int lo = lo0;
        int hi = hi0;
        if ((hi - lo) <= 3) {
            brute(array, lo, hi);
            return;
        }

        /*
         *  Pick a pivot and move it out of the way
         */
        double pivot = array[(lo + hi) / 2];
        array[(lo + hi) / 2] = array[hi];
        array[hi] = pivot;

        while (lo < hi) {
            /*
             *  Search forward from array[lo] until an element is found that
             *  is greater than the pivot or lo >= hi
             */
            while ((array[lo] <= pivot) && lo < hi) {
                lo++;
            }

            /*
             *  Search backward from array[hi] until element is found that
             *  is less than the pivot, or hi <= lo
             */
            while ((pivot <= array[hi]) && lo < hi) {
                hi--;
            }

            /*
             *  Swap elements array[lo] and array[hi]
             */
            if (lo < hi) {
                double temp = array[lo];
                array[lo] = array[hi];
                array[hi] = temp;
            }
        }

        /*
         *  Put the median in the "center" of the list
         */
        array[hi0] = array[hi];
        array[hi] = pivot;

        /*
         *  Recursive calls, elements array[lo0] to array[lo-1] are less than or
         *  equal to pivot, elements array[hi+1] to array[hi0] are greater than
         *  pivot.
         */
        sort(array, lo0, lo - 1);
        sort(array, hi + 1, hi0);
    }

    @Override
    public void sort(double[] data) {
        if (data != null && data.length > 1) {
            sort(data, 0, data.length - 1);
        }
    }
}