/**
 * This class is a Data Transfer Object (DTO) that represents a user class in the simulation model.
 * User classes are used to differentiate jobs and apply different behaviors or priorities.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.sim.dto;

import java.util.Objects;

public class UserClassDescriptor {

    private String name;
    private String type;
    private String priority;
    private String referenceSource;
    private String softDeadline;
    private String customers;
    private String cacheMissClass;
    private String cacheHitClass;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getReferenceSource() {
        return referenceSource;
    }

    public void setReferenceSource(String referenceSource) {
        this.referenceSource = referenceSource;
    }

    public String getSoftDeadline() {
        return softDeadline;
    }

    public void setSoftDeadline(String softDeadline) {
        this.softDeadline = softDeadline;
    }

    public String getCustomers() {
        return customers;
    }

    public void setCustomers(String customers) {
        this.customers = customers;
    }

    public String getCacheMissClass() {
        return cacheMissClass;
    }

    public void setCacheMissClass(String cacheMissClass) {
        this.cacheMissClass = cacheMissClass;
    }

    public String getCacheHitClass() {
        return cacheHitClass;
    }

    public void setCacheHitClass(String cacheHitClass) {
        this.cacheHitClass = cacheHitClass;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserClassDescriptor that = (UserClassDescriptor) o;
        return Objects.equals(name, that.name) && Objects.equals(type, that.type) && Objects.equals(priority, that.priority) && Objects.equals(referenceSource, that.referenceSource) && Objects.equals(softDeadline, that.softDeadline) && Objects.equals(customers, that.customers) && Objects.equals(cacheMissClass, that.cacheMissClass) && Objects.equals(cacheHitClass, that.cacheHitClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, priority, referenceSource, softDeadline, customers, cacheMissClass, cacheHitClass);
    }
}
