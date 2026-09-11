package jwat;

import jwat.MatrixObservations;

public interface JWatModel {
	public void resetModel();

	public MatrixObservations getMatrix();

	public void setMatrix(MatrixObservations matrix);
}
