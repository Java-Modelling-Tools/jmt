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

        // Scansiona tutti gli elementi (UserClassDescriptor)
        for (UserClassDescriptor currclass : dto.getUserClasses()) {
            String name = currclass.getName();

            // Determina il tipo di classe: "open" o "closed"
            int type = "open".equals(currclass.getType()) ?
                    CommonConstants.CLASS_TYPE_OPEN : CommonConstants.CLASS_TYPE_CLOSED;

            // Imposta i parametri di default (poiché non sono obbligatori)
            Integer priority = Defaults.getAsInteger("classPriority");
            Double softDeadline = Defaults.getAsDouble("classSoftDeadline");
            Integer customers = Defaults.getAsInteger("classPopulation");

            // Tenta il parsing degli attributi effettivi (se presenti e non vuoti)
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

            // Aggiunge la User Class al CommonModel.
            // Nota: la distribuzione verrà settata più tardi.
            Object key = model.addClass(name, type, priority, softDeadline, customers,
                    Defaults.getAsNewInstance("classDistribution"));

            // Memorizza la sorgente di riferimento nel context, verrà usata nel Post-Processing
            context.refStations.put(key, currclass.getReferenceSource());

            // Popola la mappa che associa il nome della classe alla sua chiave generata
            context.classes.put(name, key);
        }
    }
}