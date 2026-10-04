package common.definitions;

import common.distributions.Distribution;

public class VacationParameters {
    private Distribution vacationTimeDistribution;
    private Double bernoulliP;

    public VacationParameters() {
        this.vacationTimeDistribution = null;
        this.bernoulliP = null;
    }


    public Distribution getVacationTimeDistribution() {
        return vacationTimeDistribution;
    }

    public void setVacationTimeDistribution(Distribution vacationTimeDistribution) {
        this.vacationTimeDistribution = vacationTimeDistribution;
    }

    public Double getBernoulliP() {
        return bernoulliP;
    }

    public void setBernoulliP(Double bernoulliP) {
        this.bernoulliP = bernoulliP;
    }
}
