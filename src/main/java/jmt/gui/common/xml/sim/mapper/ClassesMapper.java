/**
 * This class is responsible for mapping {@link UserClassDescriptor} objects from the DTO to the {@link CommonModel}
 * and vice-versa. It iterates through the user classes defined in the {@link SimDescriptor} and adds them to the model,
 * or retrieves them from the model to build the DTO.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.sim.mapper;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.sim.dto.SimDescriptor;
import jmt.gui.common.xml.sim.dto.UserClassDescriptor;
import jmt.gui.common.xml.sim.serializer.WritingContext;

import java.util.List;

public class ClassesMapper implements ModelElementMapper {

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SimDescriptor dto, MappingContext context) {
        if (dto.getUserClasses() == null) {
            return;
        }

        for (UserClassDescriptor currclass : dto.getUserClasses()) {
            mapUserClass(currclass, context);
        }
    }

    private void mapUserClass(UserClassDescriptor currclass, MappingContext context) {
        CommonModel model = context.getModel();
        String name = currclass.getName();

        // Determines the class type: "open" or "closed"
        int type = XML_A_CLASS_TYPE_OPEN.equals(currclass.getType()) ?
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

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SimDescriptor dto, WritingContext context) {
        CommonModel model = context.model;
        List<Object> classKeys = model.getClassKeys();

        if (classKeys == null || classKeys.isEmpty()) {
            return;
        }

        for (Object classKey : classKeys) {
            UserClassDescriptor classDto = new UserClassDescriptor();
            mapModelClassToDto(model, classKey, classDto);
            dto.addUserClass(classDto);
        }
    }

    private void mapModelClassToDto(CommonModel model, Object classKey, UserClassDescriptor classDto) {
        // Name
        classDto.setName(model.getClassName(classKey));

        // Type & Customers (Population)
        if (model.getClassType(classKey) == CommonConstants.CLASS_TYPE_OPEN) {
            classDto.setType(XML_A_CLASS_TYPE_OPEN);
        } else {
            classDto.setType(XML_A_CLASS_TYPE_CLOSED);
            Integer population = model.getClassPopulation(classKey);
            if (population != null) {
                classDto.setCustomers(population.toString());
            }
        }

        // Priority
        Integer priority = model.getClassPriority(classKey);
        if (priority != null) {
            classDto.setPriority(priority.toString());
        }

        // Soft Deadline
        Double softDeadline = model.getClassSoftDeadline(classKey);
        if (softDeadline != null) {
            classDto.setSoftDeadline(softDeadline.toString());
        }

        // Reference Source
        Object refStationKey = model.getClassRefStation(classKey);
        String referenceSource = resolveReferenceSourceForClass(model, refStationKey);
        if (referenceSource != null) {
            classDto.setReferenceSource(referenceSource);
        }
    }

    private String resolveReferenceSourceForClass(CommonModel model, Object refStationKey) {
        if (refStationKey == null) {
            return null;
        }
        if (CommonConstants.STATION_TYPE_FORK.equals(refStationKey)
                || CommonConstants.STATION_TYPE_CLASSSWITCH.equals(refStationKey)
                || CommonConstants.STATION_TYPE_SCALER.equals(refStationKey)
                || CommonConstants.STATION_TYPE_TRANSITION.equals(refStationKey)) {
            return (String) refStationKey;
        }
        return model.getStationName(refStationKey);
    }
}