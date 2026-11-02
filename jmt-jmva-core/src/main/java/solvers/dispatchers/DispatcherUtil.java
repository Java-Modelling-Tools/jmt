package solvers.dispatchers;

import analytical.ExactConstants;
import jmt.common.exception.SolverException;

/**
 * Utilities of the JMVA solver dispatchers.
 */
/**
 * Utilities of the JMVA solver dispatchers.
 */
/**
 * Utilities of the JMVA solver dispatchers.
 */
/**
 * Utilities of the JMVA solver dispatchers.
 */
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
