/**
 * This class is responsible for mapping {@link UserClassDescriptor} objects from the DTO to the {@link CommonModel}.
 * It iterates through the user classes defined in the {@link SimDescriptor} and adds them to the model.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.mapper;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.dto.UserClassDescriptor;

public class ClassesMapper implements ModelElementMapper {

    @Override
    public void mapDtoToCommonModel(SimDescriptor dto, MappingContext context) {
        if (dto.getUserClasses() == null) {
            return;
        }

        for (UserClassDescriptor currclass : dto.getUserClasses()) {
            mapUserClass(currclass, context);
        }
    }

    @Override
    public void mapCommonModelToDto(SimDescriptor dto, MappingContext context) {
        // TODO: implement logic to map common model to dto
    }

    private void mapUserClass(UserClassDescriptor currclass, MappingContext context) {
        CommonModel model = context.getModel();
        String name = currclass.getName();

        // Determines the class type: "open" or "closed"
        int type = "open".equals(currclass.getType()) ?
                CommonConstants.CLASS_TYPE_OPEN : CommonConstants.CLASS_TYPE_CLOSED;

        // Try parsing the actual attributes (if present and not empty), otherwise use defaults
        Integer priority = getInt(currclass.getPriority(), Defaults.getAsInteger("classPriority"));
        Double softDeadline = getDouble(currclass.getSoftDeadline(), Defaults.getAsDouble("classSoftDeadline"));
        Integer customers = getInt(currclass.getCustomers(), Defaults.getAsInteger("classPopulation"));

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