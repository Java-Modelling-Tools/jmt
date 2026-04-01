package jmt.gui.common.xml.sim.serializer;

import jmt.gui.common.definitions.CommonModel;

/**
 * Carries the read-only context needed by serializers during the model-to-DTO
 * conversion phase, giving each serializer access to the source {@link CommonModel}.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
public class WritingContext {
    public final CommonModel model;

    public WritingContext(CommonModel model) {
        this.model = model;
    }
}