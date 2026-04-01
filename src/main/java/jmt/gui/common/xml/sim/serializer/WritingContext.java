/**
 * Carries the read-only context needed by serializers during the model-to-DTO
 * conversion phase, giving each serializer access to the source {@link jmt.gui.common.definitions.CommonModel}.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.sim.serializer;

import jmt.gui.common.definitions.CommonModel;

public class WritingContext {
    public final CommonModel model;

    public WritingContext(CommonModel model) {
        this.model = model;
    }
}