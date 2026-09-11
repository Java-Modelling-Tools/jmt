package jwat.filters;

import jwat.Observation;

public interface FilterOnVariable {
	public boolean isMatching(Observation o, int pos);
}
