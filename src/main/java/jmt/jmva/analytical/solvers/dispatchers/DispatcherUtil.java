package jmt.jmva.analytical.solvers.dispatchers;

import jmt.common.exception.SolverException;
import jmt.jmva.analytical.ExactConstants;

public class DispatcherUtil {

    public static void fail(String message, Throwable t) throws SolverException {
        if (ExactConstants.DEBUG && t != null) {
            t.printStackTrace();
        }
        StringBuffer s = new StringBuffer(message);
        if (t != null) {
            s.append("\n");
            s.append(t.toString());
        }

        throw new SolverException(s.toString(), t);
    }

}
