package jmt.gui.common.xml.mapper;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.dto.UserClassDescriptor;

public class ClassesMapper implements ModelElementMapper {

    @Override
    public void map(SimDescriptor dto, MappingContext context) {
        CommonModel model = context.getModel();

        if (dto.getUserClasses() == null) {
            return;
        }

        // Scan all elements (UserClassDescriptor)
        for (UserClassDescriptor currclass : dto.getUserClasses()) {
            String name = currclass.getName();

            // Determines the class type: "open" or "closed"
            int type = "open".equals(currclass.getType()) ?
                    CommonConstants.CLASS_TYPE_OPEN : CommonConstants.CLASS_TYPE_CLOSED;

            // Set default parameters (since they are not mandatory)
            Integer priority = Defaults.getAsInteger("classPriority");
            Double softDeadline = Defaults.getAsDouble("classSoftDeadline");
            Integer customers = Defaults.getAsInteger("classPopulation");

            // Try parsing the actual attributes (if present and not empty)
            String tmpCustomers = currclass.getCustomers();
            if (tmpCustomers != null && !tmpCustomers.isEmpty()) {
                customers = Integer.valueOf(tmpCustomers);
            }

            String tmpPriority = currclass.getPriority();
            if (tmpPriority != null && !tmpPriority.isEmpty()) {
                priority = Integer.valueOf(tmpPriority);
            }

            String tmpSoftDeadline = currclass.getSoftDeadline();
            if (tmpSoftDeadline != null && !tmpSoftDeadline.isEmpty()) {
                softDeadline = Double.valueOf(tmpSoftDeadline);
            }

            // Adds the User Class to the CommonModel.
            // Note: the distribution will be set later.
            Object key = model.addClass(name, type, priority, softDeadline, customers,
                    Defaults.getAsNewInstance("classDistribution"));

            // Stores the reference source in the context, it will be used in Post-Processing
            context.refStations.put(key, currclass.getReferenceSource());

            // Populates the map that associates the class name with its generated key
            context.classes.put(name, key);
        }
    }
}