package common.definitions;

import java.awt.Cursor;

/**
 * Minimal interface for GUI controllers that can change a graph cursor.
 * Implemented by jsimgraph.controller.Mediator in jmt-jsimg-gui.
 * Allows DispatcherThread (in jmt-gui) to set cursor on graph editor
 * contexts without a compile-time dependency on Mediator.
 */
public interface GraphCursorController {
	void setGraphCursor(Cursor cursor);
}
