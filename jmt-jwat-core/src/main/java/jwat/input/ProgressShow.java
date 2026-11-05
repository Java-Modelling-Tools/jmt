package jwat.input;

/**
 * Progress display interface of the JWAT time consuming operations.
 */
/**
 * Progress display interface of the JWAT time consuming operations.
 */
/**
 * Progress display interface of the JWAT time consuming operations.
 */
/**
 * Progress display interface of the JWAT time consuming operations.
 */
public interface ProgressShow {

	public void eventUpdate(int value, String txt);

	public boolean isCanceled();

	public void closeView();

	public void initShow(int maxValue);

	public int getStep();
}
