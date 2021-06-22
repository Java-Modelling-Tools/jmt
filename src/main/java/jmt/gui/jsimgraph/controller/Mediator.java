/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.gui.jsimgraph.controller;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.*;

import jmt.commandline.Jmt;
import jmt.engine.log.LoggerParameters;
import jmt.framework.gui.components.JMTMenuBar;
import jmt.framework.gui.components.JMTToolBar;
import jmt.framework.gui.image.ImageLoader;
import jmt.framework.gui.listeners.MenuAction;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.controller.DispatcherThread;
import jmt.gui.common.controller.ModelChecker;
import jmt.gui.common.controller.PADispatcherThread;
import jmt.gui.common.definitions.*;
import jmt.gui.common.editors.DefaultsEditor;
import jmt.gui.common.panels.*;
import jmt.gui.common.panels.parametric.PAProgressWindow;
import jmt.gui.common.panels.parametric.PAResultsWindow;
import jmt.gui.common.panels.parametric.ParametricAnalysisPanel;
import jmt.gui.common.xml.ModelLoader;
import jmt.gui.common.xml.XMLWriter;
import jmt.gui.jsimgraph.DialogFactory;
import jmt.gui.jsimgraph.JGraphMod.*;
import jmt.gui.jsimgraph.UtilPoint;
import jmt.gui.jsimgraph.controller.actions.*;
import jmt.gui.jsimgraph.definitions.*;
import jmt.gui.jsimgraph.mainGui.ComponentBar;
import jmt.gui.jsimgraph.mainGui.JSIMGraphMain;
import jmt.gui.jsimgraph.panels.AutoDownloadPanel;
import jmt.gui.jsimgraph.panels.JModelProblemsWindow;
import jmt.gui.jsimgraph.panels.StationNamePanel;
import jmt.gui.jsimgraph.panels.TemplatePanel;
import jmt.gui.jsimgraph.panels.JModelClassesPanel;
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.gui.exact.ExactWizard;

import org.jgraph.JGraph;
import org.jgraph.graph.*;
import org.jgraph.plaf.basic.BasicGraphUI;

/**
 * This class maintains a reference to all the main components of the Gui,
 * in this way it is possible to divide the responsibility of the actions &
 * every object know only about of himself & the mediator.
 * Other actions are made through the mediator without knowing who will actually
 * do it.
 */
public class Mediator implements GuiInterface {

	private boolean isReleased = false;
	// making it final allows the compiler to skip code generation when false
	private GraphMouseListener mouseListener;
	// Dialog factory
	private DialogFactory dialogFactory;
	// Cell factory
	private CellFactory cellFactory;
	// Dialog Factory to display template input panels.
	private CustomizableDialogFactory customizableDialogFactory;

	private AbstractJmodelAction closeModel;
	private AbstractJmodelAction newModel;
	private AbstractJmodelAction openHelp;
	private AbstractJmodelAction openModel;
	private AbstractJmodelAction saveModel;
	private AbstractJmodelAction setConnect;
	private AbstractJmodelAction setBezierConnect;
	private AbstractJmodelAction actionSnapToGrid;
	private AbstractJmodelAction actionCut;
	private AbstractJmodelAction actionCopy;
	private AbstractJmodelAction actionPaste;
	private AbstractJmodelAction actionDelete;
	private AbstractJmodelAction actionCancelEdgeDrawing;
	private AbstractJmodelAction setOptions;
	private AbstractJmodelAction setSelect;
	private AbstractJmodelAction simulate;
	private AbstractJmodelAction solveAnalitic;
	private AbstractJmodelAction solveApp;
	private AbstractJmodelAction editUserClasses;
	private AbstractJmodelAction editMeasures;
	private AbstractJmodelAction switchToExactSolver;
	private AbstractJmodelAction exit;
	private AbstractJmodelAction editDefaults;
	private AbstractJmodelAction saveModelAs;
	private AbstractJmodelAction pauseSimulation;
	private AbstractJmodelAction stopSimulation;
	private AbstractJmodelAction editSimParams;
	private AbstractJmodelAction showResults;

	private AbstractJmodelAction useTemplate;

	private AbstractJmodelAction about;
	private AbstractJmodelAction addBlockingRegion;
	private AbstractJmodelAction takeScreenShot;
	private AbstractJmodelAction actionRotate;
    private AbstractJmodelAction actionRotateLeft;
    private AbstractJmodelAction actionRotateRight;
	private AbstractJmodelAction actionSetRight;
	private AbstractJmodelAction editUndo;
	private AbstractJmodelAction editRedo;

	private AbstractJmodelAction actionBezierEdgeAddControlPoint;
	private AbstractJmodelAction actionBezierEdgeAddTangents;
	private AbstractJmodelAction actionBezierEdgeBreakEdge;
	private AbstractJmodelAction actionBezierEdgeRemoveControlPoint;
	private AbstractJmodelAction actionBezierEdgeSelectControlPoint;
	private AbstractJmodelAction actionBezierEdgeUnlockTangents;

	private AbstractJmodelAction downloadDefaultTemplates;

	private JmtJGraph graph;
	private JSIMGraphMain mainWindow;

	protected Object[] cells;
	protected Map cellsAttr;

	private Cursor cursor;
	private Cursor oldCursor;

	private JSimGraphModel model;
	private JFrame resultsWindow;
	private JmtClipboard clipboard;
	private DispatcherThread dispatcher = null;
	private ModelLoader modelLoader = new ModelLoader(ModelLoader.JMODEL, ModelLoader.JMODEL_SAVE);
	private JMTToolBar componentBar;

	private ModelChecker mc;
	private JModelProblemsWindow pw;
	private PADispatcherThread batchThread;
	private PAProgressWindow progressWindow;
	private AbstractJmodelAction editPAParams;

	private JmtOverlapping overlapping;

	private BezierModificationToolsPanel bezierEditingToolBox;
	private BezierConnectionHelperBar BezierConnectionHelperBar;

	private JmtEdgeBreakagesIterator edgeBreakagesIterator;

	public Mediator(final JmtJGraph graph, JSIMGraphMain mainWindow) {
		this.mainWindow = mainWindow;
		dialogFactory = new DialogFactory(mainWindow);
		cellFactory = new CellFactory(this);
		customizableDialogFactory = new CustomizableDialogFactory(mainWindow);

		if (Defaults.getAsBoolean("showDefaultTemplates")) {
			int result = JOptionPane.showConfirmDialog(mainWindow, "Would you like to download some recommended templates for JSIMGraph?");
			if (result == JOptionPane.YES_OPTION) {
				downloadDefaultTemplates();
			}
			if (result == JOptionPane.NO_OPTION) {
				Defaults.set("showDefaultTemplates", "false");
				Defaults.save();
			}
		}

		this.graph = graph;
		closeModel = new CloseModel(this);
		newModel = new NewModel(this);
		openHelp = new OpenHelp(this);
		openModel = new OpenModel(this);
		saveModel = new SaveModel(this);
		setConnect = new SetConnectState(this);
		setBezierConnect = new SetBezierConnectState(this);
		actionCut = new ActionCut(this);
		actionCopy = new ActionCopy(this);
		actionPaste = new ActionPaste(this);
		actionDelete = new ActionDelete(this);
		actionCancelEdgeDrawing = new ActionCancelEdgeDrawing(this);
		setOptions = new SetOptions(this);
		setSelect = new SetSelectState(this);
		simulate = new Simulate(this);

		solveAnalitic = new SolveAnalytic(this);
		solveApp = new SolveApprox(this);

		undoManager = new GraphUndoManager();
		undoProxy = new UndoManagerProxy(undoManager);
		editUndo = new ActionUndo(this, undoManager);
		editRedo = new ActionRedo(this, undoManager);

		pauseSimulation = new PauseSimulation(this);
		stopSimulation = new StopSimulation(this);
		exit = new Exit(this);
		clipboard = new JmtClipboard(this);
		editDefaults = new EditDefaults(this);
		saveModelAs = new SaveModelAs(this);
		editSimParams = new EditSimParams(this);
		editPAParams = new EditPAParams(this);
		showResults = new ShowResults(this);

		useTemplate = new UseTemplate(this);
		downloadDefaultTemplates = new DownloadDefaultTemplates(this);

		about = new About(this);
		addBlockingRegion = new AddBlockingRegion(this);

		actionSetRight = new ActionSetRight(this);
		takeScreenShot = new TakeScreenShot(this);

		actionRotate = new ActionRotate(this);
        actionRotateLeft = new ActionRotateLeft(this);
        actionRotateRight = new ActionRotateRight(this);
		overlapping = new JmtOverlapping(this);

		actionSnapToGrid = new ActionSnapToGrid(this);

		editUserClasses = new EditUserClasses(this);
		editMeasures = new EditMeasures(this);
		switchToExactSolver = new SwitchToExactSolver(this);
		// Initialize new Component bar
		componentBar = new ComponentBar(this);


		actionBezierEdgeAddControlPoint = new ActionBezierEdgeAddControlPoint(this);
		actionBezierEdgeAddTangents = new ActionBezierEdgeAddTangents(this);
		actionBezierEdgeBreakEdge = new ActionBezierEdgeBreakEdge(this);
		actionBezierEdgeRemoveControlPoint = new ActionBezierEdgeRemoveControlPoint(this);
		actionBezierEdgeSelectControlPoint = new ActionBezierEdgeSelectControlPoint(this);
		actionBezierEdgeUnlockTangents = new ActionBezierEdgeUnlockTangents(this);

		bezierEditingToolBox = new BezierModificationToolsPanel(mainWindow, this);
		BezierConnectionHelperBar = new BezierConnectionHelperBar(mainWindow, this);

		edgeBreakagesIterator = new JmtEdgeBreakagesIterator();

	}
	
	

	/**
	 * Creates a toolbar to be displayed in main window.
	 * @return created toolbar.
	 */
	public JMTToolBar createToolbar() {
		JMTToolBar toolbar = new JMTToolBar(JMTImageLoader.getImageLoader());
		// Builds an array with all actions to be put in the toolbar
		AbstractJmodelAction[] actions = new AbstractJmodelAction[] { newModel, openModel, saveModel, null,
				actionCut, actionCopy, actionPaste, actionDelete, null, editUserClasses, editMeasures, editSimParams, editPAParams, null,
				switchToExactSolver, null, simulate, pauseSimulation, stopSimulation, showResults, null, editDefaults, openHelp, null, useTemplate};
		toolbar.populateToolbar(actions);
		return toolbar;
	}


	/**
	 * Creates a menu to be displayed in main window.
	 * @return created menu.
	 */
	public JMTMenuBar createMenu() {
		JMTMenuBar menu = new JMTMenuBar(JMTImageLoader.getImageLoader());

		// File menu
		MenuAction action = new MenuAction("File", new AbstractJmodelAction[] { newModel, openModel, saveModel, saveModelAs, closeModel, null, exit });
		menu.addMenu(action);

		// Edit menu
		action = new MenuAction("Edit", new AbstractJmodelAction[] { actionCut, actionCopy, actionPaste, actionDelete, actionCancelEdgeDrawing, null, takeScreenShot});
		menu.addMenu(action);

		// Define menu
		action = new MenuAction("Define", new AbstractJmodelAction[] { editUserClasses, editMeasures, editSimParams, editPAParams, null, editDefaults });
		menu.addMenu(action);

		// Solve menu
		action = new MenuAction("Solve", new AbstractJmodelAction[] { simulate, pauseSimulation, stopSimulation, null, switchToExactSolver, null, showResults });
		menu.addMenu(action);

		// Help menu
		action = new MenuAction("Help", new AbstractJmodelAction[] { openHelp, null, about });
		menu.addMenu(action);

		return menu;
	}

	public JMTToolBar getComponentBar() {
		return componentBar;
	}

	public void setMouseListener(GraphMouseListener mouseListener) {
		this.mouseListener = mouseListener;
	}

	public GraphMouseListener getMouseListener() {
		return mouseListener;
	}

	private File openedArchive;

	public AbstractJmodelAction getEditUserClasses() {
		return editUserClasses;
	}

	public AbstractJmodelAction getExit() {
		return exit;
	}

	public JmodelClassDefinition getClassDefinition() {
		return model;
	}

	public JmodelStationDefinition getStationDefinition() {
		return model;
	}

	public SimulationDefinition getSimulationDefinition() {
		return model;
	}

	public JmodelBlockingRegionDefinition getBlockingRegionDefinition() {
		return model;
	}

	public void enableAddBlockingRegion(boolean state) {
		addBlockingRegion.setEnabled(state);
	}

	public void enableRotateAction(boolean state) {
		actionRotate.setEnabled(state);
	}

    public void enableRotateLeftAction(boolean state) {
        actionRotateLeft.setEnabled(state);
    }

    public void enableRotateRightAction(boolean state) {
        actionRotateRight.setEnabled(state);
    }

	public void enableSetRight(boolean state) {
		actionSetRight.setEnabled(state);
	}

	public AbstractJmodelAction getAddBlockingRegion() {
		return addBlockingRegion;
	}

	public AbstractJmodelAction getRotate() {
		return actionRotate;
	}

    public AbstractJmodelAction getRotateLeft() {
        return actionRotateLeft;
    }

    public AbstractJmodelAction getRotateRight() {
        return actionRotateRight;
    }

	public AbstractJmodelAction getSetRight() {
		return actionSetRight;
	}

	public AbstractJmodelAction getPauseSimulation() {
		return pauseSimulation;
	}

	public AbstractJmodelAction getStopSimulation() {
		return stopSimulation;
	}

	public AbstractJmodelAction getEditDefaults() {
		return editDefaults;
	}

	public AbstractJmodelAction getEditSimParams() {
		return editSimParams;
	}

	public AbstractJmodelAction getEditPAParams() {
		return editPAParams;
	}

	public AbstractJmodelAction getShowResults() {
		return showResults;
	}

	public AbstractJmodelAction getAbout() {
		return about;
	}

	public AbstractJmodelAction getBezierEdgeAddControlPoint() {
		return actionBezierEdgeAddControlPoint;
	}

	public AbstractJmodelAction getBezierEdgeAddTangents() {
		return actionBezierEdgeAddTangents;
	}

	public AbstractJmodelAction getBezierEdgeBreakEdge() {
		return actionBezierEdgeBreakEdge;
	}

	public AbstractJmodelAction getBezierEdgeRemoveControlPoint() {
		return actionBezierEdgeRemoveControlPoint;
	}

	public AbstractJmodelAction getBezierEdgeSelectControlPoint() {
		return actionBezierEdgeSelectControlPoint;
	}

	public AbstractJmodelAction getBezierEdgeUnlockTangents() {
		return actionBezierEdgeUnlockTangents;
	}

	public AbstractJmodelAction getCancelEdgeDrawing() {
		return actionCancelEdgeDrawing;
	}



	private GraphUndoManager undoManager;
	private UndoManagerProxy undoProxy;

	public void undo() {
		undoManager.undo();
	}

	public void redo() {
		undoManager.redo();
	}

	public void enableUndoAction(boolean state) {
		editUndo.setEnabled(state);
	}

	public void enableRedoAction(boolean state) {
		editRedo.setEnabled(state);
	}

	public AbstractJmodelAction getUndoAction() {
		return editUndo;
	}

	public AbstractJmodelAction getRedoAction() {
		return editRedo;
	}

	public GraphUndoManager getUndoManager() {
		return undoManager;
	}

	public void setUndoManager(GraphUndoManager um) {
		undoManager = um;
	}

	public void setConnectState() {
		setSelect.setEnabled(true);
		oldCursor = cursor;
		cursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
		setGraphCursor(cursor);
//		System.out.println("Mediator -> setConnectState");
		mouseListener.setConnectState();
	}

	public void setBezierConnectState() {
		setSelect.setEnabled(true);
		oldCursor = cursor;
		cursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
		setGraphCursor(cursor);
//		System.out.println("Mediator -> setBezierConnectState");
		mouseListener.setBezierConnectState();
	}


	public void setInsertState(String className) {
		setSelect.setEnabled(true);
		mouseListener.setInsertState(className);
	}

	public void setSelectState() {
		setSelect.setEnabled(true);
		mouseListener.setSelectState();
	}

	public void activateSelect() {
		setSelect.setEnabled(true);
		componentBar.clickButton(setSelect);
	}

	public void enableCutAction(boolean state) {
		actionCut.setEnabled(state);
	}

	public void enableCopyAction(boolean state) {
		actionCopy.setEnabled(state);
	}

	public void enablePasteAction(boolean state) {
		actionPaste.setEnabled(state);
	}

	public void enableDeleteAction(boolean state) {
		actionDelete.setEnabled(state);
	}

	public void enableSnapToGridAction(boolean state) {
		actionSnapToGrid.setEnabled(state);
	}
	public AbstractJmodelAction getTakeScreenShot() {
		return takeScreenShot;
	}

	public void setHandle(CellHandle handle) {
		this.mouseListener.setHandle(handle);
	}

	public AbstractJmodelAction getOpenHelp() {
		return openHelp;
	}

	public AbstractJmodelAction getNewModel() {
		return newModel;
	}

	public AbstractJmodelAction getOpenModel() {
		return openModel;
	}

	public AbstractJmodelAction getSaveModel() {
		return saveModel;
	}

	public AbstractJmodelAction getSaveModelAs() {
		return saveModelAs;
	}

	public AbstractJmodelAction getCloseModel() {
		return closeModel;
	}

	public AbstractJmodelAction getSetConnect() { return setConnect; }

	public AbstractJmodelAction getSetBezierConnect() { return setBezierConnect; }

	public AbstractJmodelAction getSnapToGrid() {
		return actionSnapToGrid;
	}

	public AbstractJmodelAction getCutAction() {
		return actionCut;
	}

	public AbstractJmodelAction getCopyAction() {
		return actionCopy;
	}

	public AbstractJmodelAction getPasteAction() {
		return actionPaste;
	}

	public AbstractJmodelAction getDeleteAction() {
		return actionDelete;
	}

	public AbstractJmodelAction getSetOptions() {
		return setOptions;
	}

	public AbstractJmodelAction getSetSelect() {
		return setSelect;
	}

	public AbstractJmodelAction getSimulate() {
		return simulate;
	}

	public AbstractJmodelAction getSolveAnalitic() {
		return solveAnalitic;
	}

	public AbstractJmodelAction getSolveApp() {
		return solveApp;
	}

	/**
	 * Gets cell factory to create new graph cells
	 * @return cell factory
	 */
	public CellFactory getCellFactory() {
		return cellFactory;
	}

	public void newModel() {
		if (checkForSave("<html>Save changes before creating a new model?</html>")) {
			return;
		}
		resetMouseState();
		graph = new JmtJGraph(this);
		graph.setModel(new DefaultGraphModel());

		graph.getGraphLayoutCache().setFactory(new JmtDefaultCellViewFactory(this) {
			private static final long serialVersionUID = 1L;

			protected EdgeView createEdgeView(Object cell) {
				if (cell instanceof Edge || cell instanceof JmtEdge) {
					return new JmtEdgeView(cell, mediator);
				} else {
					return new JmtEdgeView(cell, mediator);
				}
			}
		});

		// Sets the cloneable flag to 'false'
		graph.setCloneable(false);
		graph.setGridSize(20);
		graph.setGridVisible(true);

		graph.addMouseListener(mouseListener);
		graph.addMouseMotionListener(mouseListener);

		undoProxy.discardAllEdits();
		graph.getModel().addUndoableEditListener(undoProxy);

		// Instantiates a new JMODELModel data structure to store the entire model
		model = new JSimGraphModel();

		mainWindow.setGraph(graph);
		closeModel.setEnabled(true);
		saveModel.setEnabled(true);
		saveModelAs.setEnabled(true);
		editMeasures.setEnabled(true);

		useTemplate.setEnabled(true);

		// Show only insert options on ComponentBar
		componentBar.clearButtonGroupSelection(0);
		componentBar.enableButtonGroup(0, true);


		// Disables show results button and measure definition, until simulation
		showResults.setSelected(false);
		showResults.setEnabled(false);
		if (resultsWindow != null) {
			resultsWindow.dispose();
		}
		resultsWindow = null;

		// Disables cut/copy/delete (leave paste enabled as clipboard is not flushed)
		enableCutAction(false);
		enableCopyAction(false);
		enableDeleteAction(false);

		// Enable snap to grid
		enableSnapToGridAction(true);

		// Disables creation of blocking region
		enableAddBlockingRegion(false);
		setConnect.setEnabled(false);
		setBezierConnect.setEnabled(false);
		setSelect.setEnabled(false);
		actionRotate.setEnabled(false);
		actionSetRight.setEnabled(false);

		// Enable the action to perform editing user classes
		editUserClasses.setEnabled(true);
		switchToExactSolver.setEnabled(true);
		// Enables the button to start simulation
		simulate.setEnabled(true);
		editSimParams.setEnabled(true);
		editPAParams.setEnabled(true);
		takeScreenShot.setEnabled(true);
		openedArchive = null;
		mainWindow.updateTitle(null);
		// Free same resources by forcing a garbage collection
		System.gc();


	}

	/**
	 * Opens a model from a data file.
	 * <br> Author: Bertoli Marco
	 */
	public void openModel(File file) {
		isReleased = true;
		if (checkForSave("<html>Save changes before opening a saved model?</html>")) {
			return;
		}
		if (animationHolder != null) {
			animationHolder.stop();
		}
		JSimGraphModel tmpmodel = new JSimGraphModel();
		int state = modelLoader.loadModel(tmpmodel, mainWindow, file);
		if (state == ModelLoader.SUCCESS || state == ModelLoader.WARNING) {
			resetMouseState();
			// Avoid checkForSave again...
			if (model != null) {
				model.resetSaveState();
			}
			newModel();
			// At this point loading was successful, so substitutes old model with loaded one
			model = tmpmodel;
			this.populateGraph();
			setSelect.setEnabled(true);
			componentBar.clickButton(setSelect);
			openedArchive = modelLoader.getSelectedFile();
			mainWindow.updateTitle(openedArchive.getName());
			// Removes selection
			graph.clearSelection();
			// If model contains results, enable Results Window
			if (model.containsSimulationResults()) {
				if (model.isParametricAnalysisEnabled()) {
					this.setResultsWindow(new PAResultsWindow(model, openedArchive.getName()));
					showResults.setEnabled(true);
				} else {
					this.setResultsWindow(new ResultsWindow(model, openedArchive.getName()));
					showResults.setEnabled(true);
				}
			}
			model.resetSaveState();
			System.gc();
		} else if (state == ModelLoader.FAILURE) {
			showErrorMessage(modelLoader.getFailureMotivation());
		}
		// Shows warnings if any
		if (state == ModelLoader.WARNING) {
			new WarningWindow(modelLoader.getLastWarnings(), mainWindow, modelLoader.getInputFileFormat(), CommonConstants.JSIM).show();
		}
	}

	public void closeModel() {
		// Checks if there's an old graph to save
		if (checkForSave("<html>Save changes before closing?</html>")) {
			return;
		}
		resetMouseState();

		// clear undo history
		graph.getModel().removeUndoableEditListener(undoProxy);
		undoProxy.discardAllEdits();
	
		mainWindow.removeGraph();
		graph = null;
		closeModel.setEnabled(false);
		saveModel.setEnabled(false);
		editMeasures.setEnabled(false);
		saveModelAs.setEnabled(false);
		componentBar.clearButtonGroupSelection(0);
		componentBar.enableButtonGroup(0, false);
		setConnect.setEnabled(false);
		setBezierConnect.setEnabled(false);
		actionCut.setEnabled(false);
		actionCopy.setEnabled(false);
		actionPaste.setEnabled(false);
		actionDelete.setEnabled(false);
		actionRotate.setEnabled(false);
		actionSetRight.setEnabled(false);
		actionSnapToGrid.setEnabled(false);

		setSelect.setEnabled(false);
		simulate.setEnabled(false);
		solveAnalitic.setEnabled(false);
		solveApp.setEnabled(false);
		editUserClasses.setEnabled(false);
		editMeasures.setEnabled(false);
		switchToExactSolver.setEnabled(false);
		// Disables the button to start simulation
		simulate.setEnabled(false);
		editSimParams.setEnabled(false);

		useTemplate.setEnabled(false);

		editPAParams.setEnabled(false);
		takeScreenShot.setEnabled(false);
		// Disables show results button and measure definition
		showResults.setSelected(false);
		showResults.setEnabled(false);
		if (resultsWindow != null) {
			resultsWindow.dispose();
		}
		resultsWindow = null;
		openedArchive = null;
		model = new JSimGraphModel();
		mainWindow.updateTitle(null);
		// Free same resources by forcing a garbage collection
		System.gc();
	}

	/** Inserts a new cell (vertex) in the desired point into the graph.
	 *
	 * @param newCell the new cell
	 * @param pt point in absolute coordinates in the
	 */
	public void InsertCell(Point2D pt, JmtCell newCell) {
		pt = graph.snap(pt);
		Object[] arg = new Object[] { newCell };
		graph.getModel().insert(arg, newCell.setAttributes(pt, graph), null, null, null);

		//snap the cell to the grid (will work only if the grid is enabled)
		this.snapCellByPort(newCell);

		// Puts new cell on back to go under blocking regions
		graph.getModel().toBack(new Object[] { newCell });
		newCell.resetParent();
		setConnect.setEnabled(true);
		setBezierConnect.setEnabled(true);
		actionSetRight.setEnabled(!(model.hasConnectionShape()));

	}

	/** Set the state of mouse listener to select & passes the event to the
	 * listener as if press event is generated.
	 *
	 * @param e
	 */
	public void selectAt(MouseEvent e) {
		activateSelect();
		mouseListener.mousePressed(e);
	}

	/**
	 * Determines whether this component is enabled. An enabled component
	 * can respond to user input and generate events. Components are
	 * enabled initially by default. A component may be enabled or disabled by
	 * calling its <code>setEnabled</code> method.
	 * @return <code>true</code> if the component is enabled,
	 * 		<code>false</code> otherwise
	 * @since JDK1.0
	 */
	public boolean isGraphEnabled() {
		return graph.isEnabled();
	}

	public void graphRequestFocus() {
		graph.requestFocus();
	}

	public int getTolerance() {
		return graph.getTolerance();
	}

	public Rectangle2D fromScreen(Rectangle2D r) {
		return graph.fromScreen(r);
	}

	public Point2D fromScreen(Point2D p) {
		return graph.fromScreen(p);
	}

	/**
	 * Returns this graph's graphics context, which lets you draw
	 * on a component. Use this method get a <code>Graphics</code> object and
	 * then invoke operations on that object to draw on the component.
	 * @return this components graphics context
	 */
	public Graphics2D getGraphGraphics() {
		return (Graphics2D) graph.getGraphics();
	}

	public CellView getNextViewAt(CellView current, double x, double y) {
		return graph.getNextViewAt(current, x, y);
	}

	/**
	 * Returning true signifies the marquee handler has precedence over
	 * other handlers, and is receiving subsequent mouse events.
	 */
	public boolean isForceMarqueeEvent(MouseEvent e) {
		return ((JmtGraphUI) graph.getUI()).isForceMarqueeEvent(e);
	}

	/**
	 * Returns the number of clicks for editing of the graph to start.
	 */
	public int getEditClickCount() {
		return graph.getEditClickCount();
	}

	/**
	 * Returning true signifies a mouse event on the cell should toggle
	 * the selection of only the cell under mouse.
	 */
	public boolean isToggleSelectionEvent(MouseEvent e) {
		return ((JmtGraphUI) graph.getUI()).isToggleSelectionEvent(e);
	}

	/**
	 * Returns true if the cell is currently selected.
	 * @param cell an object identifying a cell
	 * @return true if the cell is selected
	 */
	public boolean isCellSelected(Object cell) {
		return graph.isCellSelected(cell);
	}

	/**
	 * Messaged to update the selection based on a MouseEvent over a
	 * particular cell. If the event is a toggle selection event, the
	 * cell is either selected, or deselected. Otherwise the cell is
	 * selected.
	 */
	public void selectCellForEvent(Object cell, MouseEvent e) {
		((JmtGraphUI) graph.getUI()).selectCellForEvent(cell, e);
	}

	/**
	 * Scroll the graph for an event at <code>p</code>.
	 */
	public void autoscroll(Point p) {
		BasicGraphUI.autoscroll(graph, p);
	}

	/**
	 * Gets the cursor set in the graph. If the graph does
	 * not have a cursor set, the cursor of its parent is returned.
	 * If no cursor is set in the entire hierarchy,
	 * <code>Cursor.DEFAULT_CURSOR</code> is returned.
	 */
	public Cursor getGraphCursor() {
		return graph.getCursor();
	}

	/**
	 * Sets graph cursor
	 * @param cursor to be set
	 */
	public void setGraphCursor(Cursor cursor) {
		graph.setCursor(cursor);
	}

	/**
	 * Returns true if the graph is being edited.  The item that is being
	 * edited can be returned by getEditingCell().
	 */
	public boolean isGraphEditing() {
		return graph.getUI().isEditing(graph);
	}

	/**
	 * Returns the given point applied to the grid.
	 * @param p a point in screen coordinates.
	 * @return the same point applied to the grid.
	 */
	public Point2D snap(Point2D p) {
		return graph.snap(p);
	}

	/**
	 * Upscale the given point in place, i.e.
	 * using the given instance.
	 * @param p the point to be upscaled
	 * @return the upscaled point instance
	 */
	public Point2D toScreen(Point2D p) {
		return graph.toScreen(p);
	}

	/**
	 * Gets the background color of graph.
	 * @return this component's background color; if this component does
	 * 		not have a background color,
	 *		the background color of its parent is returned
	 */
	public Color getGraphBackground() {
		return graph.getBackground();
	}

	/**
	 * Returns the current marquee color of the graph.
	 */
	public Color getGraphMarqueeColor() {
		return graph.getMarqueeColor();
	}

	public void connect(Point2D start, Point2D current, PortView inPort, PortView outPort) {
		Point2D p = fromScreen(start);
		Point2D p2 = fromScreen(current);
		if (inPort != null && outPort != null) {
			ArrayList<Point2D> list = new ArrayList<Point2D>();
			list.add(p);
			list.add(p2);
			Map map = new Hashtable();
			GraphConstants.setPoints(map, list);
			GraphConstants.setRouting(map, JmtGraphConstants.ROUTING_JMT);
			GraphConstants.setLineEnd(map, GraphConstants.ARROW_CLASSIC);
			GraphConstants.setEndFill(map, true);
			GraphConstants.setConnectable(map, false);
			GraphConstants.setDisconnectable(map, false);
			Map<Object, Map> viewMap = new Hashtable<Object, Map>();
			// Adds connection into underlying data structure
			Object sourceKey = ((CellComponent) ((JmtCell) ((OutputPort) (outPort.getCell())).getUserObject()).getUserObject()).getKey();
			Object targetKey = ((CellComponent) ((JmtCell) ((InputPort) (inPort.getCell())).getUserObject()).getUserObject()).getKey();
			JmtEdge connection = new JmtEdge(sourceKey, targetKey, this);
			viewMap.put(connection, map);
			Object[] insert = new Object[] { connection };
			ConnectionSet cs = new ConnectionSet();
			cs.connect(connection, outPort.getCell(), true);
			cs.connect(connection, inPort.getCell(), false);

			// if previous JmtEdge exists and erase it
			Set edges= ((OutputPort) (outPort.getCell())).getEdges();
			Iterator<JmtEdge> itr = edges.iterator();
			JmtEdge e = null;
			for (int i=0; itr.hasNext(); i++){
				e = itr.next();
				if (e.getTargetKey()==targetKey) {
					this.deleteJmtEdge(e);
				}
			}

			// Visualizes connection only if it can be created into data structure
			if(model.areConnectable(sourceKey,targetKey)){
				model.setConnected(sourceKey, targetKey, true);
				graph.getModel().insert(insert, viewMap, cs, null, null);

				//A Non-Bezier connection has been created so the attribute free of the cells is set to false
				JmtCell source = (JmtCell) ((DefaultPort) connection.getSource()).getParent();
				JmtCell target = (JmtCell) ((DefaultPort) connection.getTarget()).getParent();

				//if the source or target station were previously rotated by any angle, creating a non-bezier edge will
				// snap their rotation at zero, so we also need to rotate the edges accordingly
				// If they were not rotate, we rotate the edge by an angle zero so we do nothing
				rotateEndEdges(source,  Math.toRadians(-source.getRotationAngle()));
				rotateEndEdges(target,  Math.toRadians(-target.getRotationAngle()));

				source.setFreeRotationAllowed(false);
				target.setFreeRotationAllowed(false);
				this.loadImage(source);
				this.loadImage(target);
				this.toggleFreeRotationButtons();
//				System.out.println("Mediator - > connect (model.hasConnectionShape())"+(model.hasConnectionShape()));
				actionSetRight.setEnabled(!(model.hasConnectionShape()));
			}
		}
	}

	public void connectBezier( Point2D start, Point2D current, JMTPath path, PortView inPort, PortView outPort) {
		Point2D p = fromScreen(start);
		Point2D p2 = fromScreen(current);
		if (inPort != null && outPort != null) {
			// Adds connection into underlying data structure
            Object sourceKey = ((CellComponent) ((JmtCell) ((OutputPort) (outPort.getCell())).getUserObject()).getUserObject()).getKey();
            Object targetKey = ((CellComponent) ((JmtCell) ((InputPort) (inPort.getCell())).getUserObject()).getUserObject()).getKey();

			ArrayList<Point2D> list = new ArrayList<Point2D>();
			list.add(p);
			list.add(p2);
			Map map = new Hashtable();
			GraphConstants.setPoints(map, list);
			GraphConstants.setRouting(map, JmtGraphConstants.ROUTING_BEZIER_JMT);
			GraphConstants.setLineEnd(map, GraphConstants.ARROW_CLASSIC);
			GraphConstants.setEndFill(map, true);
			GraphConstants.setConnectable(map, false);
			GraphConstants.setDisconnectable(map, false);

			Map<Object, Map> viewMap = new Hashtable<Object, Map>();
			// Adds connection into underlying data structure
			JmtEdge connection = new JmtEdge(sourceKey, targetKey, true, this);

			viewMap.put(connection, map);
			Object[] insert = new Object[] { connection };
			ConnectionSet cs = new ConnectionSet();
			cs.connect(connection, outPort.getCell(), true);
			cs.connect(connection, inPort.getCell(), false);


            // if previous JmtEdge exists and erase it
			Set edges= ((OutputPort) (outPort.getCell())).getEdges();
			Iterator<JmtEdge> itr = edges.iterator();
			JmtEdge e = null;
			for (int i=0; itr.hasNext(); i++){
				e = itr.next();
				if (e.getTargetKey()==targetKey) {
					this.deleteJmtEdge(e);

				}
			}
			if (model.areConnectable(sourceKey,targetKey)){
				model.setConnected(sourceKey, targetKey, true);
				model.setConnectionShape(sourceKey,targetKey,path);
				graph.getModel().insert(insert, viewMap, cs, null, null);
				editConnectionLabels(sourceKey,targetKey, start, current,connection);

				//test if the cell is only linked to Bezier edges to set the value of the attribute AuthorisedFreeRotation
				JmtCell source = (JmtCell) ((DefaultPort) connection.getSource()).getParent();
				JmtCell target = (JmtCell) ((DefaultPort) connection.getTarget()).getParent();
				source.setFreeRotationAllowed(this.isLinkedOnlyToBezierEdges(source));
				target.setFreeRotationAllowed(this.isLinkedOnlyToBezierEdges(target));
				this.loadImage(source);
				this.loadImage(target);
				this.toggleFreeRotationButtons();
				actionSetRight.setEnabled(false);

				Object[] edges2 = ((OutputPort) (outPort.getCell())).getEdges().toArray();
				Object[] edges3 = ((InputPort) (inPort.getCell())).getEdges().toArray();
				toFront(edges2);
				toFront(edges3);
			}

		}
	}


	/**
	 * Creates a connection between given source and target JmtCells
	 * @param source source cell
	 * @param target target cell
	 * @return created component or null if connection between source and target cannot be created
	 *
	 * Author: Bertoli Marco
	 */
	public JmtEdge connect(JmtCell source, JmtCell target) {
		return connect(source, target, false);
	}

	/**
	 * Creates a connection between given source and target JmtCells
	 * @param source source cell
	 * @param target target cell
	 * @param forced true if connection must be shown also if could not be created into data structure.
	 * @return created component or null if connection between source and target cannot be created
	 *
	 * Author: Bertoli Marco
     *
     * Modified by Emma Bortone to add the BEZIER ROUTING
	 */
	public JmtEdge connect(JmtCell source, JmtCell target, boolean forced) {
		// If one of parameter is null, returns null
		if (source == null || target == null) {
			return null;
		}
		// Retrieves source and target keys to create connection
		Object sourceKey = ((CellComponent) source.getUserObject()).getKey();
		Object targetKey = ((CellComponent) target.getUserObject()).getKey();
		// Initializes correct layout for routing edges
		Map map = new Hashtable();
		JmtEdge connection;
        if (this.getModel().hasConnectionShape(sourceKey,targetKey)){
            GraphConstants.setRouting(map, JmtGraphConstants.ROUTING_BEZIER_JMT);
            connection = new JmtEdge(sourceKey, targetKey, true,this);
            setConnectionLabels(map, sourceKey,targetKey, getPortOutPosition(source), getPortInPosition(target));
        }
		else {
            GraphConstants.setRouting(map, JmtGraphConstants.ROUTING_JMT);
			connection = new JmtEdge(sourceKey, targetKey, this);
        }
		GraphConstants.setLineEnd(map, GraphConstants.ARROW_CLASSIC);
		GraphConstants.setEndFill(map, true);
		GraphConstants.setConnectable(map, false);
		GraphConstants.setDisconnectable(map, false);

		Map<Object, Map> viewMap = new Hashtable<Object, Map>();
		viewMap.put(connection, map);
		Object[] insert = new Object[] { connection };
		ConnectionSet cs = new ConnectionSet();
		// Finds sourcePort
		Iterator it;
		it = source.getChildren().iterator();
		DefaultPort tmpPort, sourcePort, targetPort;
		sourcePort = null;
		while (it.hasNext()) {
			tmpPort = (DefaultPort) it.next();
			if (tmpPort instanceof OutputPort) {
				sourcePort = tmpPort;
			}
		}
		// Finds targetPort
		it = target.getChildren().iterator();
		targetPort = null;
		while (it.hasNext()) {
			tmpPort = (DefaultPort) it.next();
			if (tmpPort instanceof InputPort) {
				targetPort = tmpPort;
			}
		}
		if (sourcePort != null && targetPort != null) {
			cs.connect(connection, sourcePort, true);
			cs.connect(connection, targetPort, false);
			// Adds connection to the graph only if it can be created into data structure
			if (model.setConnected(sourceKey, targetKey, true) || forced) {
				graph.getModel().insert(insert, viewMap, cs, null, null);

                //test if the cell is only linked to Bezier edges to set the value of the attribute AuthorisedFreeRotation
				source.setFreeRotationAllowed(this.isLinkedOnlyToBezierEdges(source));
				target.setFreeRotationAllowed(this.isLinkedOnlyToBezierEdges(target));
                this.loadImage(source);
                this.loadImage(target);
				actionSetRight.setEnabled(!(model.hasConnectionShape()));
				return connection;

			}
		}
		return null;
	}

	/**
	 * repaints the graph component
	 */
	public void graphRepaint() {
		graph.repaint();
	}

	/**
	 * Returns the parent of <I>child</I> in the model.
	 * <I>child</I> must be a node previously obtained from
	 * this data source. This returns null if <i>child</i> is
	 * a root in the model.
	 *
	 * @param   child  a node in the graph, obtained from this data source
	 * @return  the parent of <I>child</I>
	 */
	public Object getParent(Object child) {
		return graph.getModel().getParent(child);
	}

	/**
	 * Gets the first portView of the input port of the cell at position
	 * @param x
	 * @param y
	 * @return portView of the input port
	 */
	public PortView getInPortViewAt(int x, int y) {
		return (PortView) graph.getGraphLayoutCache().getMapping(graph.getInPortAt(x, y), false);
	}

	/**
	 * Gets the first portView of the output port of the cell at position
	 * @param x
	 * @param y
	 * @return portView of the output port
	 */
	public PortView getOutPortViewAt(int x, int y) {
		return (PortView) graph.getGraphLayoutCache().getMapping(graph.getOutPortAt(x, y), false);
	}

	/**
	 * Tells if a given cell exists in the graph
	 * @param cell
	 * @return true iff the cell exists
	 */
	public boolean containsCell(Object cell) {
		return graph.getModel().contains(cell);
	}

	/**
	 * Returns if a given cell is visible in the graph
	 * @param cell
	 * @return true iff the cell is visible
	 */
	public boolean isCellVisible(Object cell) {
		return ((JmtGraphUI) graph.getUI()).getGraphLayoutCache().isVisible(cell);
	}

	/**
	 * Returns the views for the specified array of cells. Returned
	 * array may contain null pointers if the respective cell is not
	 * mapped in this view and <code>create</code> is <code>false</code>.
	 */
	public CellView getViewOfCell(Object cell, boolean create) {
		return ((JmtGraphUI) graph.getUI()).getGraphLayoutCache().getMapping(cell, create);
	}

	/**
	 * Selects the specified cell and initiates editing.
	 * The edit-attempt fails if the <code>CellEditor</code>
	 * does not allow
	 * editing for the specified item.
	 */
	public void startEditingAtCell(Object cell) {

		graph.startEditingAtCell(cell);
		if ((cell != null) && (cell instanceof JmtCell)) {
			JmtCell jcell = (JmtCell) cell;
			showStationParameterPanel(((CellComponent) jcell.getUserObject()).getKey(),
					StationParameterPanel.INPUT_SECTION, null);
			// Updates cell dimensions if name was changed too much...
			Hashtable<Object, Map> nest = new Hashtable<Object, Map>();
			Dimension cellDimension = jcell.getSize(graph);
			Map attr = jcell.getAttributes();
			Rectangle2D oldBounds = GraphConstants.getBounds(attr);
			if (oldBounds.getWidth() != cellDimension.getWidth()) {
				GraphConstants.setBounds(attr, new Rectangle2D.Double(oldBounds.getX(), oldBounds.getY(),
						cellDimension.getWidth(), cellDimension.getHeight()));
				nest.put(cell, attr);
				jcell.updatePortPositions(nest, GraphConstants.getIcon(attr), cellDimension);
				graph.getGraphLayoutCache().edit(nest);
			}
		}
		// Blocking region editing
		else if ((cell != null) && (cell instanceof BlockingRegion)) {
			showBlockingRegionParameterPanel(((BlockingRegion) cell).getKey());
		}
	}

	public void startEditingAtAbstractCell(Object cell) {
		graph.startEditingAtCell(cell);
		if ((cell != null) && (cell instanceof JmtCell)) {
			JmtCell jcell = (JmtCell) cell;
			showStationParameterPanel(((CellComponent) jcell.getUserObject()).getKey(),
					StationParameterPanel.INPUT_SECTION, null);
		}
		// Blocking region editing
		else if ((cell != null) && (cell instanceof BlockingRegion)) {
			showBlockingRegionParameterPanel(((BlockingRegion) cell).getKey());
		}
	}

	public void showStationParameterPanel(Object stationKey, int section, Object classKey) {
		StationParameterPanel stationPanel = new StationParameterPanel(model, model, stationKey);
		stationPanel.add(new StationNamePanel(model, stationKey), BorderLayout.NORTH);
		stationPanel.selectSectionPanel(section, classKey);
		dialogFactory.getDialog(stationPanel, "Editing " + model.getStationName(stationKey) + " Properties...");
	}

	public void showBlockingRegionParameterPanel(Object regionKey) {
		BlockingRegionParameterPanel regionPanel = new BlockingRegionParameterPanel(model, model, regionKey);
		dialogFactory.getDialog(regionPanel, "Editing " + model.getRegionName(regionKey) + " Properties...");
	}

	public void showBezierEditingPanel() {
//		System.out.println("Mediator -> showBezierEditingPanel !");
		bezierEditingToolBox.getDialog().setVisible(true);
		bezierEditingToolBox.getToolBar().clickButton(actionBezierEdgeSelectControlPoint);
	}

	public void hideBezierEditingPanel() {
		if (mouseListener.getCurrentState() == mouseListener.bezierSelect){
//			System.out.println("Mediator -> hideBezierEditingPanel !");
			bezierEditingToolBox.getDialog().setVisible(false);
		}
	}

	/**
	 * Cuts all the selected stations, connections and blocking regions.
	 */
	public void cutSelection() {
		clipboard.cut(graph.getDescendants(graph.getSelectionCells()));
	}

	/**
	 * Copies all the selected stations, connections and blocking regions.
	 */
	public void copySelection() {
		clipboard.copy(graph.getDescendants(graph.getSelectionCells()));
	}

	/**
	 * Pastes all the cut/copied stations, connections and blocking regions.
	 */
	public void pasteSelection() {
		clipboard.paste();
		// If more than one stations are present enables link button
		if (graph.getModel().getRootCount() > 1) {
			setConnect.setEnabled(true);
			setBezierConnect.setEnabled(true);
		}
		// If one station is present show select button
		if (graph.getModel().getRootCount() >= 1) {
			activateSelect();
		}
	}

	/**
	 * Deletes all the selected stations, connections and blocking regions.
	 */
	public void deleteSelection() {
		delete(graph.getSelectionCells());
	}

	/**
	 * Deletes the given elements from graph.
	 */
	public void delete(Object[] cells) {
		GraphModel graphModel = graph.getModel();
		// Set of the edges from/to removed stations
		Set<Object> edges = new HashSet<Object>();
		// Set of the ports on removed stations
		Set<Object> ports = new HashSet<Object>();
		// Set of the stations in removed blocking regions
		Set<Object> children = new HashSet<Object>();
		// Set of the blocking regions with deleted stations
		Set<Object> regions = new HashSet<Object>();
		// List of the cells connected to the deleted edges
		HashSet<JmtCell> connectedCells = new HashSet<>();
		for (Object cell : cells) {
			if (cell instanceof JmtCell) {
				for (Object c : ((JmtCell) cell).getChildren()) {
					if (c instanceof DefaultPort) {
						edges.addAll(((DefaultPort) c).getEdges());
						ports.add(c);
					}
				}
				Object p = ((JmtCell) cell).getParent();
				if (p instanceof BlockingRegion) {
					regions.add(p);
				}
			} else if (cell instanceof BlockingRegion) {
				for (Object c : ((BlockingRegion) cell).getChildren()) {
					if (c instanceof JmtCell) {
						children.add(c);
					}
				}
			} else if (cell instanceof JmtEdge) { // Add to the list connectedCells the cells connected to the deleted edges
				JmtCell source = (JmtCell) ((DefaultPort) ((JmtEdge) cell).getSource()).getParent();
				JmtCell target = (JmtCell) ((DefaultPort) ((JmtEdge) cell).getTarget()).getParent();
				connectedCells.add(source);
				connectedCells.add(target);
			}
		}
		for (Object edge : edges) { // Add to the list connectedCells the cells connected to the additional deleted edges
			JmtCell source = (JmtCell) ((DefaultPort) ((JmtEdge) edge).getSource()).getParent();
			JmtCell target = (JmtCell) ((DefaultPort) ((JmtEdge) edge).getTarget()).getParent();
			connectedCells.add(source);
			connectedCells.add(target);
		}

		// Removes edges from graph
		graphModel.remove(edges.toArray());
		// Removes elements from graph
		graphModel.remove(cells);
		// Removes ports from graph
		graphModel.remove(ports.toArray());
		// Resets parents of the stations
		for (Object c : children) {
			((JmtCell) c).resetParent();
		}

		// Removes elements from data structure
		for (Object cell : cells) {
			if (cell instanceof JmtCell) {
				if(connectedCells.contains(cell)){ // removes from the list connectedCells the cells taht will be deleted
					connectedCells.remove(cell);
				}
				model.deleteStation(((CellComponent) ((JmtCell) cell).getUserObject()).getKey());
			} else if (cell instanceof JmtEdge) {
				model.setConnected(((JmtEdge) cell).getSourceKey(), ((JmtEdge) cell).getTargetKey(), false);
				if (model.hasConnectionShape(((JmtEdge) cell).getSourceKey(), ((JmtEdge) cell).getTargetKey())) {
                    model.deleteConnectionShape(((JmtEdge) cell).getSourceKey(), ((JmtEdge) cell).getTargetKey());
					edgeBreakagesIterator.removeAll(((JmtEdge) cell).getSourceKey(), ((JmtEdge) cell).getTargetKey());
                }
			} else if (cell instanceof BlockingRegion) {
				model.deleteBlockingRegion(((BlockingRegion) cell).getKey());
			}
		}

		//update the attribute authorisedFreeRotation of the cells that were connected to a deleted edge
		for (JmtCell connectedCell : connectedCells) {
			connectedCell.setFreeRotationAllowed(this.isLinkedOnlyToBezierEdges(connectedCell));
		}


		// Checks for empty blocking regions
		Iterator<Object> it = regions.iterator();
		while (it.hasNext()) {
			if (!((BlockingRegion) it.next()).getChildren().isEmpty()) {
				it.remove();
			}
		}
		// Removes empty blocking regions from graph and data structure
		graphModel.remove(regions.toArray());
		for (Object r : regions) {
			model.deleteBlockingRegion(((BlockingRegion) r).getKey());
		}

		// Disables the buttons if nothing remains
		if (graphModel.getRootCount() == 0) {
			componentBar.clearButtonGroupSelection(0);
			setConnect.setEnabled(false);
			setBezierConnect.setEnabled(false);
			setSelect.setEnabled(false);
			actionSetRight.setEnabled(false);
			actionRotateLeft.setEnabled(false);
            actionRotateRight.setEnabled(false);
		}

		// Change graphMouseListener state to select state if current state is bezierEdgeModification
		this.mouseListener.cellDeleted();
	}

	/**
	 * This function deletes a jmtEdge without disconnecting the stations
	 * this is used before replacing that edge with another.
	 */
	public void deleteJmtEdge(JmtEdge edge){
		GraphModel graphModel = graph.getModel();
		HashSet<JmtCell> connectedCells = new HashSet<>();
		JmtCell source = (JmtCell) ((DefaultPort) edge.getSource()).getParent();
		JmtCell target = (JmtCell) ((DefaultPort) edge.getTarget()).getParent();
		connectedCells.add(source);
		connectedCells.add(target);

		Object[] objectToDelete = new Object[] { edge };
		// Removes elements from graph
		graphModel.remove(objectToDelete);
		// Removes elements from data structure

		if (model.hasConnectionShape(edge.getSourceKey(), edge.getTargetKey())) {
			model.deleteConnectionShape(edge.getSourceKey(), edge.getTargetKey());
			edgeBreakagesIterator.removeAll(edge.getSourceKey(), edge.getTargetKey());
		}
	}

	/**
	 * Displays an error message in the panel that is responsible to make the
	 * user understand why a certain operation is not valid.
	 *
	 * @param message error to be displayed.
	 */
	public void displayGraphErrMsg(String message) {
		System.out.println("message = " + message);
	}

	/**
	 * Shows the panel that opens when the right button is clicked.
	 *
	 * @param p point where the right button is clicked on the graph
	 */
	public void showOPanel(Point p) {
		return;
	}

	/**
	 * Saves the current model into current file if exists, otherwise calls saveModelAs()
	 * Author: Bertoli Marco
	 */
	public void saveModel() {
		if (openedArchive == null) {
			saveModelAs();
			return;
		}
		// Updates station positions into data structure
		updateStationPositions();
		int status = modelLoader.saveModel(model, mainWindow, openedArchive);
		switch (status) {
		case ModelLoader.SUCCESS:
			model.resetSaveState();
			mainWindow.updateTitle(openedArchive.getName());
			if (resultsWindow instanceof ResultsWindow) {
				((ResultsWindow) resultsWindow).updateTitle(openedArchive.getName());
			}
			if (resultsWindow instanceof PAResultsWindow) {
				((PAResultsWindow) resultsWindow).updateTitle(openedArchive.getName());
			}
			break;
		case ModelLoader.FAILURE:
			showErrorMessage(modelLoader.getFailureMotivation());
			break;
		}
	}

	/**
	 * Saves the current model into a user specified file.
	 * Author: Bertoli Marco
	 */
	public void saveModelAs() {
		// Updates station positions into data structure
		updateStationPositions();
		int status = modelLoader.saveModel(model, mainWindow, null);
		switch (status) {
		case ModelLoader.SUCCESS:
			model.resetSaveState();
			openedArchive = modelLoader.getSelectedFile();
			mainWindow.updateTitle(openedArchive.getName());
			if (resultsWindow instanceof ResultsWindow) {
				((ResultsWindow) resultsWindow).updateTitle(openedArchive.getName());
			}
			if (resultsWindow instanceof PAResultsWindow) {
				((PAResultsWindow) resultsWindow).updateTitle(openedArchive.getName());
			}
			break;
		case ModelLoader.FAILURE:
			showErrorMessage(modelLoader.getFailureMotivation());
			break;
		}
	}

	/**
	 * Updates station positions into data structure to reflect the one shown on jgraph
	 * window. This method is called before saving model.
	 * Author: Bertoli Marco
	 */
	public void updateStationPositions() {
		Object key;
		Object[] cells = graph.getDescendants(graph.getRoots());
		for (Object cell : cells) {
			if (cell instanceof JmtCell) {
				JmtCell jcell = (JmtCell) cell;
				key = ((CellComponent) jcell.getUserObject()).getKey();
				// Sets cell coordinate into data structure
				model.setStationPosition(key, new JMTPoint(getCellCoordinates(jcell), !jcell.isLeftInputCell(), ((JmtCell) cell).getRotationAngle()));
			}
		}
	}

	/**
	 * Uses information retrieved from data structure to recreate graph structure.
	 * This method has to be called after loading a model.
	 * <br>Author: Bertoli Marco
	 */
	public void populateGraph() {
		Object[] stations = model.getStationKeys().toArray();
		HashMap<Object, JmtCell> cells = new HashMap<Object, JmtCell>();
		JmtCell cell;

		// Variables for auto-placement. Currently items are placed on a grid...
		// Need to be improved!!!
		int count = 0;
		int X = 150; // distance on the X axis
		int Y = 50; // distance on the Y axis
		int X0 = 50;
		int Y0 = 15;
		int colCount = (graph.getHeight() - 2 * Y0) / Y;
		boolean containPosition = true;
		// Shows stations
		for (Object station : stations) {
			cell = cellFactory.createStationCell(station);
			JMTPoint position = model.getStationPosition(station);
			// If position is not present, auto-position this station
			while (position == null) {
				containPosition = false;
				JMTPoint tmp = new JMTPoint(X0 + X * (count / colCount), Y0 + Y * (count % colCount), false, 0);
				if (!overlapCells(tmp, cell)) {
					position = tmp;
				}
				count++;
			}
			InsertCell(position, cell);

			//Set attributes of the cell that modify the icon (mirrored, rotation angle)
			//Here the attribute isFreeRotationAllowed will be set to True, it will be actualized if a connection with
			// a non-Bezier arc is found.
			cell.setIconModifiers(position.isRotate(),true, position.getAngle());
			loadImage(cell);

			cells.put(station, cell);
		}
		Vector<Object> forwardConnections;
		// Shows connections
		for (Object station : stations) {
			forwardConnections = model.getForwardConnections(station);

			for (int j = 0; j < forwardConnections.size(); j++) {
				// Forces connection as it is already present into data structure
				connect(cells.get(station), cells.get(forwardConnections.get(j)), true);
			}
		}

		// Now adds blocking regions
		Vector<Object> regions = model.getRegionKeys();
		for (int i = 0; i < regions.size(); i++) {
			Object key = regions.get(i);
			Set<JmtCell> regionStation = new HashSet<JmtCell>();
			Iterator<Object> stationKeys = model.getBlockingRegionStations(key).iterator();
			while (stationKeys.hasNext()) {
				regionStation.add(cells.get(stationKeys.next()));
			}
			// Adds cells to blocking region
			addCellsToBlockingRegion(key, regionStation.toArray());
		}

		// whether the Position is Null, the application call the reposition's method
		if (!containPosition) {
			adjustGraph();
		}
		graphRepaint();
		graph.getGraphLayoutCache().reload();
	}

	/**
	 * @return the system <code>JGraph</code>.
	 */
	public JGraph getGraph() {
		return graph;
	}

	/**
	 * Launches the <code>UserClass</code> editor.
	 */
	public void editUserClasses() {
		dialogFactory.getDialog(new JModelClassesPanel(model, model), "Define customer classes",
				Defaults.getAsInteger("JSIMClassDefWindowWidth").intValue(),
				Defaults.getAsInteger("JSIMClassDefWindowHeight").intValue(),
				true, "JSIMClassDefWindowWidth", "JSIMClassDefWindowHeight");
	}

	/**
	 * Enables or not the <code>UserClass</code> editor function.
	 */
	public void enableEditUserClasses(boolean state) {
		editUserClasses.setEnabled(state);
	}

	/**
	 * This function will put selected cells in place avoiding overlapping with other cells
	 * in graph window
	 * <br>
	 * Author: Bertoli Marco
	 * Heavily modified by Giuseppe De Cicco & Fabio Granara
	 *
	 */
	public void putSelectedCellsInGoodPlace(Object[] cells, Integer[] X, Integer[] Y) {
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] instanceof JmtCell) {
				putCellInGoodPlace((JmtCell) cells[i], X[i].intValue(), Y[i].intValue(), true);
			}
			if (cells[i] instanceof BlockingRegion) {
				Object[] tmp = new Object[1];
				tmp[0] = cells[i];
				Object[] children = graph.getDescendants(tmp);
				for (Object element : children) {
					if (element instanceof JmtCell) {
						putCellInGoodPlace((JmtCell) element, -1, -1, false);
					}
				}
			}
		}
		graph.getGraphLayoutCache().reload();
		sp = 0;
	}

	/**
	 * This function will put given cell in place avoiding overlapping with other cells
	 * in graph window
	 * <br>
	 * Author: Bertoli Marco
	 * Heavily modified by Giuseppe De Cicco & Fabio Granara
	 * @param cell Identifier of the cell to be moved
	 */

	int resetOverLapping = 0;
	int  sp = 0;

	public void putCellInGoodPlace(JmtCell cell, int x, int y, boolean flag) {
		if (sp > 9) {
			sp = 0;
		}
		int oldPointX = 0;
		int oldPointY = 0;
		boolean inGroup = false;

		if (flag) {
			Rectangle bounds = GraphConstants.getBounds(cell.getAttributes()).getBounds();
			Rectangle bounds2 = new Rectangle((int) bounds.getX() - 20, (int) bounds.getY(), (int) bounds.getWidth() + 38, (int) bounds.getHeight());

			oldPointX = x;
			oldPointY = y;

			// check if a cell isInGroup
			if (isInGroup(cell)) {
				inGroup = true;
			}

			// Avoids negative starting point
			if (bounds.getX() < 20) {
				bounds.setLocation(20, (int) bounds.getY());
			}
			if (bounds.getY() < 0) {
				bounds.setLocation((int) bounds.getX(), 0);
			}

			Object[] overlapping = graph.getDescendants(graph.getRoots(bounds2));

			Point2D zero = new Point(20, 0);
			resetOverLapping = 0;
			while (overlapping.length > 0) {
				// Moves bounds until it does not overlap with anything
				Point2D last = (Point2D) zero.clone();

				for (int j = 0; j < overlapping.length; j++) {
					resetOverLapping++;
					// resetOverLapping is inserted for an abnormal behavior, 
					// if you disable this variable you can see that the tool
					// stops and "for cycle" will be repeated infinite times
					if (resetOverLapping > 50) {
						bounds.setLocation(new Point(oldPointX, oldPointY));
						GraphConstants.setBounds(cell.getAttributes(), bounds);
						resetOverLapping = 0;
						return;
					}

					// Puts last to last corner of overlapping cells
					if (overlapping[j] instanceof JmtCell && overlapping[j] != cell && inGroup) {
						Rectangle2D b2 = GraphConstants.getBounds(((JmtCell) overlapping[j]).getAttributes());
						if (b2.intersects(bounds)) {
							if (b2.getMaxX() > last.getX()) {
								last.setLocation(b2.getMaxX(), last.getY());
							}
							if (b2.getMaxY() > last.getY()) {
								last.setLocation(last.getX(), b2.getMaxY());
							}
						}
						last.setLocation(new Point((int) (last.getX() + .5), (int) (last.getY() + .5)));
					}

					int numberOfChild = cell.getChildCount();

					if (!inGroup && overlapping[j] instanceof JmtCell && overlapping[j] != cell) {
						Rectangle2D b = GraphConstants.getBounds(((JmtCell) overlapping[j]).getAttributes());
						// Consider only rectangles that intersects with given bound
						if (b.intersects(bounds2)) {
							last.setLocation(new Point(oldPointX, oldPointY));
						}
					}
					/**Part to modified by Emma Bortone to excluse bezier curves**/
					if (overlapping[j] instanceof JmtEdge
							&& overlapping[j] != cell
							&& !isInGroup(overlapping[j])
							&& ((JmtEdge) overlapping[j]).intersects((EdgeView) (graph.getGraphLayoutCache()).getMapping(overlapping[j], false),
									GraphConstants.getBounds(cell.getAttributes()))
							&& ((JmtEdge) overlapping[j]).getSource() != cell.getChildAt(0)
							&& (!((JmtEdge) overlapping[j]).getIsBezier())
					       ){
						/** **/
						boolean access = false;
						boolean access2 = false;
						if (cell instanceof SourceCell
								&& ((JmtEdge) overlapping[j]).intersects((EdgeView) (graph.getGraphLayoutCache()).getMapping(overlapping[j], false),
										GraphConstants.getBounds(((SourceCell) cell).getAttributes()))) {
							if (((JmtEdge) overlapping[j]).getSource() != cell.getChildAt(0)) {
								ArrayList<Point2D> intersectionPoints = ((JmtEdge) overlapping[j]).getIntersectionVertexPoint();
								Point2D tmp = (intersectionPoints.get(0));
								Rectangle2D cellBound = GraphConstants.getBounds(((SourceCell) cell).getAttributes());
								double vertexMaxX = (int) cellBound.getMaxX();
								double vertexMaxY = (int) cellBound.getMaxY();
								double vertexHeight = (int) cellBound.getHeight();
								double vertexWidth = (int) cellBound.getWidth();
								boolean upperSideIntersaction = ((JmtEdge) overlapping[j]).getUpperSideIntersaction();
								boolean lowerSideIntersaction = ((JmtEdge) overlapping[j]).getLowerSideIntersaction();
								boolean leftSideIntersaction = ((JmtEdge) overlapping[j]).getLeftSideIntersaction();
								boolean rightSideIntersaction = ((JmtEdge) overlapping[j]).getRightSideIntersaction();
								if (upperSideIntersaction && lowerSideIntersaction) {
									int valoreIntermedio = ((int) vertexMaxX - (int) (vertexWidth / 2));
									if ((int) tmp.getX() < valoreIntermedio) {
										Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
												false, false, true, false);
										bounds.setLocation(newPosition);
									} else {
										Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
												false, false, false, true);
										bounds.setLocation(newPosition);
									}
								} else if (leftSideIntersaction && rightSideIntersaction) {
									int valoreIntermedio = ((int) vertexMaxY - (int) (vertexHeight / 2));
									if ((int) tmp.getY() < valoreIntermedio) {
										Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
												false, true, false, false);
										Point newPosition2 = new Point(newPosition.x, newPosition.y + sp);
										bounds.setLocation(newPosition2);
										sp = sp + 2;
									} else {
										Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
												true, false, false, false);
										bounds.setLocation(newPosition);
									}
								} else if (upperSideIntersaction && rightSideIntersaction) {
									Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp, false,
											false, false, true);
									bounds.setLocation(newPosition);
								} else if (upperSideIntersaction && leftSideIntersaction) {
									Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp, false,
											false, true, false);
									bounds.setLocation(newPosition);
								} else if (lowerSideIntersaction && rightSideIntersaction) {
									Point2D tmp1 = (intersectionPoints.get(1));
									Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp1, false,
											false, false, true);
									bounds.setLocation(newPosition);
								} else if (lowerSideIntersaction && leftSideIntersaction) {
									Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp, false,
											false, true, false);
									bounds.setLocation(newPosition);
								}
								access = true;
							}
						}

						if (cell instanceof SinkCell) {
							if (((JmtEdge) overlapping[j]).getTarget() != cell.getChildAt(0)) {
								if (((JmtEdge) overlapping[j]).intersects((EdgeView) (graph.getGraphLayoutCache()).getMapping(overlapping[j], false),
										GraphConstants.getBounds(((SinkCell) cell).getAttributes()))) {
									ArrayList<Point2D> intersectionPoints = ((JmtEdge) overlapping[j]).getIntersectionVertexPoint();
									Point2D tmp = (intersectionPoints.get(0));
									Rectangle2D cellBound = GraphConstants.getBounds(((SinkCell) cell).getAttributes());
									double vertexMaxX = (int) cellBound.getMaxX();
									double vertexMaxY = (int) cellBound.getMaxY();
									double vertexHeight = (int) cellBound.getHeight();
									double vertexWidth = (int) cellBound.getWidth();
									boolean upperSideIntersaction = ((JmtEdge) overlapping[j]).getUpperSideIntersaction();
									boolean lowerSideIntersaction = ((JmtEdge) overlapping[j]).getLowerSideIntersaction();
									boolean leftSideIntersaction = ((JmtEdge) overlapping[j]).getLeftSideIntersaction();
									boolean rightSideIntersaction = ((JmtEdge) overlapping[j]).getRightSideIntersaction();
									if (upperSideIntersaction && lowerSideIntersaction) {
										int valoreIntermedio = ((int) vertexMaxX - (int) (vertexWidth / 2));
										if ((int) tmp.getX() < valoreIntermedio) {
											Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
													false, false, true, false);
											bounds.setLocation(newPosition);
										} else {
											Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
													false, false, false, true);
											bounds.setLocation(newPosition);
										}
									} else if (leftSideIntersaction && rightSideIntersaction) {
										int valoreIntermedio = ((int) vertexMaxY - (int) (vertexHeight / 2));
										if ((int) tmp.getY() < valoreIntermedio) {
											Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
													false, true, false, false);
											Point newPosition2 = new Point(newPosition.x, newPosition.y + sp);
											bounds.setLocation(newPosition2);
											sp = sp + 3;
										} else {
											Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
													true, false, false, false);
											bounds.setLocation(newPosition);
										}
									} else if (upperSideIntersaction && rightSideIntersaction) {
										Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
												false, false, false, true);
										bounds.setLocation(newPosition);
									} else if (upperSideIntersaction && leftSideIntersaction) {
										Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
												false, false, true, false);
										bounds.setLocation(newPosition);
									} else if (lowerSideIntersaction && rightSideIntersaction) {
										Point2D tmp1 = (intersectionPoints.get(1));
										Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp1,
												false, false, false, true);
										bounds.setLocation(newPosition);
									} else if (lowerSideIntersaction && leftSideIntersaction) {
										Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
												false, false, true, false);
										bounds.setLocation(newPosition);
									}
									access2 = true;
								}
							}
						}

						if (!isInGroup(overlapping[j]) && !access && !access2 && overlapping[j] instanceof JmtEdge) {
							if ((numberOfChild == 2)
									&& ((JmtEdge) overlapping[j]).getSource() != cell.getChildAt(0)
									&& ((JmtEdge) overlapping[j]).getSource() != cell.getChildAt(1)
									&& ((JmtEdge) overlapping[j]).getTarget() != cell.getChildAt(0)
									&& ((JmtEdge) overlapping[j]).getTarget() != cell.getChildAt(1)
									&& ((JmtEdge) overlapping[j]).intersects((EdgeView) (graph.getGraphLayoutCache()).getMapping(overlapping[j],
											false), GraphConstants.getBounds(cell.getAttributes()))) {
								access = access2 = false;
								ArrayList<Point2D> intersectionPoints = ((JmtEdge) overlapping[j]).getIntersectionVertexPoint();
								if ((intersectionPoints == null) || intersectionPoints.size() <= 0) {
									bounds.setLocation(new Point(oldPointX, oldPointY));
									GraphConstants.setBounds(cell.getAttributes(), bounds);
									resetOverLapping = 0;
									return;
								} else {
									Point2D tmp = (intersectionPoints.get(0));
									Rectangle2D cellBound = GraphConstants.getBounds(cell.getAttributes());
									double vertexMaxX = (int) cellBound.getMaxX();
									double vertexMaxY = (int) cellBound.getMaxY();
									double vertexHeight = (int) cellBound.getHeight();
									double vertexWidth = (int) cellBound.getWidth();
									boolean upperSideIntersaction = ((JmtEdge) overlapping[j]).getUpperSideIntersaction();
									boolean lowerSideIntersaction = ((JmtEdge) overlapping[j]).getLowerSideIntersaction();
									boolean leftSideIntersaction = ((JmtEdge) overlapping[j]).getLeftSideIntersaction();
									boolean rightSideIntersaction = ((JmtEdge) overlapping[j]).getRightSideIntersaction();
									if (upperSideIntersaction && lowerSideIntersaction) {
										int valoreIntermedio = ((int) vertexMaxX - (int) (vertexWidth / 2));
										if ((int) tmp.getX() < valoreIntermedio) {
											Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
													false, false, true, false);
											bounds.setLocation(newPosition);
										} else {
											Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
													false, false, false, true);
											bounds.setLocation(newPosition);
										}
									} else if (leftSideIntersaction && rightSideIntersaction) {
										int valoreIntermedio = ((int) vertexMaxY - (int) (vertexHeight / 2));
										if ((int) tmp.getY() < valoreIntermedio) {
											Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
													false, true, false, false);
											Point newPosition2 = new Point(newPosition.x, newPosition.y + sp);
											bounds.setLocation(newPosition2);
											sp = sp + 3;
										} else {
											Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
													true, false, false, false);
											bounds.setLocation(newPosition);
										}
									} else if (upperSideIntersaction && rightSideIntersaction) {
										Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
												false, false, false, true);
										bounds.setLocation(newPosition);
									} else if (upperSideIntersaction && leftSideIntersaction) {
										Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
												false, false, true, false);
										bounds.setLocation(newPosition);
									} else if (lowerSideIntersaction && rightSideIntersaction) {
										Point2D tmp1 = (intersectionPoints.get(1));
										Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp1,
												false, false, false, true);
										bounds.setLocation(newPosition);
									} else if (lowerSideIntersaction && leftSideIntersaction) {
										Point newPosition = (this.overlapping).findFreePosition(((JmtEdge) overlapping[j]), cell, cellBound, tmp,
												false, false, true, false);
										bounds.setLocation(newPosition);
									}
								}
							}
						}
					}
				}
				if (last.equals(zero)) {
					break;
				}
				bounds.setLocation(new Point((int) (last.getX()), (int) (last.getY())));
				overlapping = graph.getDescendants(graph.getRoots(bounds));
			}

			GraphConstants.setBounds(cell.getAttributes(), bounds);
		}
		else {
			Rectangle bounds = GraphConstants.getBounds(cell.getAttributes()).getBounds();
			if (isInGroup(cell)) {
				inGroup = true;
			}

			// Avoids negative starting point
			if (bounds.getX() < 20) {
				bounds.setLocation(20, (int) bounds.getY());
			}
			if (bounds.getY() < 0) {
				bounds.setLocation((int) bounds.getX(), 0);
			}
			Object[] overlapping = graph.getDescendants(graph.getRoots(bounds));

			if (overlapping == null) {
				return;
			}

			Point2D zero = new Point(20, 0);
			while (overlapping.length > 0) {
				Point2D last = (Point2D) zero.clone();
				for (Object element : overlapping) {
					if (element instanceof JmtCell && element != cell && inGroup) {
						Rectangle2D b2 = GraphConstants.getBounds(((JmtCell) element).getAttributes());
						if (b2.intersects(bounds)) {
							if (b2.getMaxX() > last.getX()) {
								last.setLocation(b2.getMaxX(), last.getY());
							}
							if (b2.getMaxY() > last.getY()) {
								last.setLocation(last.getX(), b2.getMaxY());
							}
							last.setLocation(new Point((int) (last.getX() + .5), (int) (last.getY() + .5)));
						}
					}
					if (!inGroup && element instanceof JmtCell && element != cell) {
						last.setLocation(new Point((int) (last.getX() + .5), (int) (last.getY() + .5)));
					}
					int numberOfChild = cell.getChildCount();
					if (isInGroup(element) && element instanceof JmtEdge) {
						if ((numberOfChild == 2)
								&& ((JmtEdge) element).getSource() != cell.getChildAt(0)
								&& ((JmtEdge) element).getSource() != cell.getChildAt(1)
								&& ((JmtEdge) element).getTarget() != cell.getChildAt(0)
								&& ((JmtEdge) element).getTarget() != cell.getChildAt(1)
								&& ((JmtEdge) element).intersects((EdgeView) (graph.getGraphLayoutCache()).getMapping(element, false), GraphConstants
										.getBounds(cell.getAttributes()))) {
							;
						}
						//Rectangle2D b2 = GraphConstants.getBounds(((JmtEdge) element).getAttributes());
						//if (b2.intersects(bounds)) {
						//	;
						//}

						last.setLocation(new Point((int) (last.getX() + .5), (int) (last.getY() + .5)));
					}
				}

				if (last.equals(zero)) {
					break;
				}
				bounds.setLocation(new Point((int) (last.getX()), (int) (last.getY())));
				overlapping = graph.getDescendants(graph.getRoots(bounds));
			}

			GraphConstants.setBounds(cell.getAttributes(), bounds);
		}
	}

	/**
	 * Retrieves the location of the given cell.
	 * @param cell The given cell
	 * @return The cell location
	 */
	public Point2D getCellCoordinates(JmtCell cell) {
		Rectangle2D bounds = GraphConstants.getBounds(cell.getAttributes());
		return new Point2D.Double(bounds.getMinX(), bounds.getMinY());
	}

	/**
	 * Checks whether the given cell overlaps an existing cell with its bounds.
	 * @param p The point where the given cell will be inserted.
	 * @param cell The given cell.
	 * @return <code>true</code> - whether there's an overlapping situation.
	 */
	public boolean overlapCells(Point2D p, JmtCell cell) {
		return overlapCells(p, cell.getSize(graph));
	}

	/**
	 * Checks whether the given region overlaps an existing cell with its bounds.
	 * This method is used to control overlapping before inserting new cell into the
	 * Jgraph.
	 * @param p The point where the given cell will be inserted.
	 * @param d The dimensions of cell to be inserted
	 * @return <code>true</code> - whether there's an overlapping situation.
	 *
	 * Author: Bertoli Marco
	 *
	 * Heavily Modified by Giuseppe De Cicco & Fabio Granara
	 */
	public boolean overlapCells(Point2D p, Dimension d) {
		Rectangle r = new Rectangle2D.Double(p.getX(), p.getY(), d.getWidth(), d.getHeight()).getBounds();
		Object[] cells = graph.getRoots(r);
		for (Object cell : cells) {
			if (cell instanceof JmtEdge ) {
				if (!((JmtEdge) cell).getIsBezier()) {
					if (((JmtEdge) cell).intersects((EdgeView) (graph.getGraphLayoutCache()).getMapping(cell, false), r)) {
						return true;
					}
				}
			}
			if (cell instanceof JmtCell || cell instanceof BlockingRegion) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Generates the xml to send to simulation engine.
	 *
	 * Author: Bertoli Marco
	 *
	 * Modified by Francesco D'Aquino
	 * Modified by Michael Fercu (Logger,2008,0.7.4)
	 */
	public void startSimulation() {
		if (animationHolder != null ) {
			animationHolder.stop();
		}
		// if simulation is not in pause state
		if (!stopSimulation.isEnabled()) {
			// Asks for confirmation before overwriting previous simulation data
			if (model.containsSimulationResults()) {
				// Find frame to show confirm dialog
				Component parent = mainWindow;
				if (resultsWindow != null && resultsWindow.isFocused()) {
					parent = resultsWindow;
				}

				int resultValue = JOptionPane.showConfirmDialog(parent, "This operation will overwrite old simulation results. "
						+ "Continue anyway?", "JMT - Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (resultValue == JOptionPane.NO_OPTION) {
					return;
				}
			}
			// Asks for confirmation if a logger-file exists (and has existing data) [MF08 0.7.4 (Marco Bertoli)]
			String[] ln = model.getLoggerNameList();
			String ln2 = "";
			if (ln != null) {
				if (model.getLoggingGlbParameter("autoAppend").equalsIgnoreCase(new Integer(LoggerParameters.LOGGER_AR_ASK).toString())) {
					if (ln.length > 0) {
						// Cache the absolute log-path
						String logabspath;
						if (model.getLoggingGlbParameter("path").equalsIgnoreCase("") || (model.getLoggingGlbParameter("path").equalsIgnoreCase("."))) {
							logabspath = new File("").getAbsolutePath() + File.separator;
						} else {
							logabspath = new File(model.getLoggingGlbParameter("path")).getAbsolutePath() + File.separator;
						}

						// Find if the logfiles have data in them:
						try {
							//Code to remove duplicate file names from the list to obtain a unique list of File names.
							Arrays.sort(ln);
							int k = 0;
							for (int i = 0; i < ln.length; i++) {
								if (i > 0 && ln[i].equals(ln[i -1])) {
									continue;
								}
								ln[k++] = ln[i];
							}
							String[] unique = new String[k];
							System.arraycopy(ln, 0, unique, 0, k);

							for (String element : unique) {
								// if the files have data, print what will be overwritten
								if (new File(logabspath + element).length() > 0) {
									ln2 = ln2 + element + ", ";
								}
							}
							// remove the trailing comma
							if (ln2 != "") {
								ln2 = ln2.substring(0, ln2.length() - 2);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}

						if (ln2 != "") {
							// Find frame to show dialog
							Component parent = mainWindow;
							if (resultsWindow != null && resultsWindow.isFocused()) {
								parent = resultsWindow;
							}

							int resultValue = JOptionPane.showConfirmDialog(parent, "This operation will modify the following logfile(s): " + ln2
									+ ". " + "Continue anyway?", "JMT - Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
							if (resultValue == JOptionPane.NO_OPTION) {
								return;
							}
						}
					}
				}
			}
			// Correct eventual problems on preloading for closed classes
			model.manageJobs();
			mc = new ModelChecker(model, model, model, model, false);
			pw = new JModelProblemsWindow(mainWindow, mc, this);
			pw.setModal(true);
			if (!mc.isEverythingOkNormal()) {
				pw.show();
			}
			if (mc.isEverythingOkNormal() || ((!mc.isEverythingOkNormal()) && (pw.continued()))) {
				if (!model.isParametricAnalysisEnabled()) {
					try {
						// Removes previous ResultsWindow
						if (resultsWindow != null) {
							resultsWindow.dispose();
							showResults.setEnabled(false);
						}
						File temp = File.createTempFile("~JModelSimulation", ".xml");
						temp.deleteOnExit();
						XMLWriter.writeXML(temp, model);
						// Creates results data structure
						String logCSVDelimiter = model.getLoggingGlbParameter("delim");
						String logDecimalSeparator = model.getLoggingGlbParameter("decimalSeparator");
						model.setSimulationResults(new ResultsModel(model.getPollingInterval().doubleValue(), logCSVDelimiter, logDecimalSeparator));
						showResults.setEnabled(true);
						dispatcher = new DispatcherThread(this, model);
						dispatcher.startSimulation(temp);
					} catch (Exception e) {
						handleException(e);
					}
				} else {
					// Removes previous ResultsWindow
					showResults.setEnabled(false);
					if (resultsWindow != null) {
						resultsWindow.dispose();
					}
					if (progressWindow == null) {
						progressWindow = new PAProgressWindow(mainWindow, simulate, pauseSimulation, stopSimulation, model.getParametricAnalysisModel());
					}
					batchThread = new PADispatcherThread(this, model, progressWindow);
					changeSimActionsState(false, true, true);
					progressWindow.initialize(model.getParametricAnalysisModel().getNumberOfSteps());
					progressWindow.start();
					progressWindow.show();
					batchThread.start();
				}
			}
		} else {
			if (!model.isParametricAnalysisEnabled()) {
				dispatcher.restartSimulation();
			} else {
				batchThread.restartSimulation();
			}
		}
	}

	/**
	 * Stops current simulation, aborting all measures
	 *
	 * Author: Bertoli Marco
	 */
	public void stopSimulation() {
		if (stopSimulation.isEnabled()) {
			if (!model.isParametricAnalysisEnabled()) {
				dispatcher.stopSimulation();
			} else {
				batchThread.stopSimulation();
			}
		}
	}

	/**
	 * Pauses current simulation
	 *
	 * Author: Bertoli Marco
	 *
	 * Modified by Francesco D'Aquino
	 */
	public void pauseSimulation() {
		if (!model.isParametricAnalysisEnabled()) {
			dispatcher.pauseSimulation();
		} else {
			batchThread.pauseSimulation();
		}
	}

	/**
	 * Changes simulation action status. This method is called by DispatcherThread.
	 * @param start state for start action
	 * @param pause state for pause action
	 * @param stop state for stop action
	 */
	public void changeSimActionsState(boolean start, boolean pause, boolean stop) {
		simulate.setEnabled(start);
		stopSimulation.setEnabled(stop);
		pauseSimulation.setEnabled(pause);
	}

	/**
	 * Launches the <code>Measure</code> editor.
	 * Author: Bertoli Marco
	 */
	public void editMeasures() {
		dialogFactory.getDialog(new MeasurePanel(model, model, model), "Define performance indices",
				Defaults.getAsInteger("JSIMMeasuresDefWindowWidth").intValue(),
				Defaults.getAsInteger("JSIMMeasuresDefWindowHeight").intValue(),
				true, "JSIMMeasuresDefWindowWidth", "JSIMMeasuresDefWindowHeight");
	}

	/**
	 * @return A reference to the action <code>EditMeasures</code>.
	 */
	public AbstractJmodelAction getEditMeasures() {
		return editMeasures;
	}

	/**
	 * Checks if there's an old graph to save. This methods is called when creates/closes/opens a graph.
	 * @param msg The message to display.
	 * @return <code>true</code> - whether the user accepts to save the graph, or he cancels the current action.
	 */
	public boolean checkForSave(String msg) {
		// Checks if there's an old graph to save
		if (model != null && model.toBeSaved()) {
			int resultValue = JOptionPane.showConfirmDialog(mainWindow, msg, "JSIMgraph - Warning", JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE);
			if (resultValue == JOptionPane.YES_OPTION) {
				saveModel();
				return true;
			}
			if (resultValue == JOptionPane.CANCEL_OPTION) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return A reference to the action <code>SwitchToExactSolver</code>.
	 */
	public AbstractJmodelAction getSwitchToWizard() {
		return switchToExactSolver;
	}

	public Cursor getOldCursor() {
		return oldCursor;
	}

	public void setOldCursor(Cursor oldCursor) {
		this.oldCursor = oldCursor;
	}

	public void setCursor(Cursor cursor) {
		oldCursor = this.cursor;
		this.cursor = cursor;
		setGraphCursor(cursor);
	}

	/**
	 * Sends an exit signal to main window
	 */
	public void exit() {
		mainWindow.dispatchEvent(new WindowEvent(mainWindow, WindowEvent.WINDOW_CLOSING));
	}

	/**
	 * Shows a DefaultEditor to edit Defaults parameters
	 */
	public void showDefaultsEditor() {
		DefaultsEditor.getInstance(mainWindow, DefaultsEditor.JMODEL).show();
	}

	/**
	 * Used to reset mouseListener to default (to avoid File_Save / File_New
	 * operations while in inserting mode)
	 */
	public void resetMouseState() {
		mouseListener.setDefaultState();
		mouseListener.setFocus(null);
		mouseListener.setCell(null);
		componentBar.clearButtonGroupSelection(0);
		if (graph != null) {
			graph.clearSelection();
			setGraphCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Returns true iff specified cell is editable. This is used by <code>SelectState</code>
	 * to check if editor has to be showed upon double click event.
	 * @param cell specified cell
	 * @return true iff cell is editable
	 */
	public boolean isCellEditable(Object cell) {
		return cell instanceof JmtCell || cell instanceof BlockingRegion;
	}

	/**
	 * Shows a panel with caught exception
	 * @param e exception to be shown
	 */
	public void handleException(Exception e) {
		e.printStackTrace();
		showErrorMessage(e.getMessage());
	}

	/**
	 * Shows a panel with an error message
	 * @param message specified error message
	 */
	public synchronized void showErrorMessage(String message) {
		Component parent = mainWindow;
		if (resultsWindow != null && resultsWindow.hasFocus()) {
			parent = resultsWindow;
		}
		JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Switch current model to JMVA exact solver
	 */
	public void toJMVA() {
		mc = new ModelChecker(model, model, model, model, true);
		pw = new JModelProblemsWindow(mainWindow, mc, this);
		pw.setModal(true);
		if (!mc.isEverythingOkToJMVA()) {
			pw.show();
		}
		if (mc.isEverythingOkToJMVA() || ((!mc.isEverythingOkToJMVA()) && (pw.continued()))) {
			if (checkForSave("<html>Save changes before switching?</html>")) {
				return;
			}
			ExactModel output = new ExactModel();
			List<String> res = ModelConverter.convertJSIMtoJMVA(model, output);
			ExactWizard jmva = new ExactWizard(output);
			// If problems are found, shows warnings
			if (res.size() > 0) {
				new WarningWindow(res, jmva, CommonConstants.JSIM, CommonConstants.JMVA).show();
			}
		}
	}

	/**
	 * Called when EditSimParams action is triggered
	 */
	public void editSimulationParameters() {
		dialogFactory.getDialog(new SimulationPanel(model, model, model, this), "Define simulation parameters",
				Defaults.getAsInteger("JSIMSimulationDefWindowWidth").intValue(),
				Defaults.getAsInteger("JSIMSimulationDefWindowHeight").intValue(),
				true, "JSIMSimulationDefWindowWidth", "JSIMSimulationDefWindowHeight");
	}

	/**
	 * Called when EditPAParams action is triggered
	 */
	public void editPAParameters() {
		dialogFactory.getDialog(new ParametricAnalysisPanel(model, model, model, this), "Define What-if analysis parameters",
				Defaults.getAsInteger("JSIMPADefWindowWidth").intValue(),
				Defaults.getAsInteger("JSIMPADefWindowHeight").intValue(),
				true, "JSIMPADefWindowWidth", "JSIMPADefWindowHeight");
	}

	/**
	 * Called when UseTemplate action is triggered
	 */
	public void editTemplate() {
		customizableDialogFactory.getDialog(700, 300, new TemplatePanel(this), "Add/Use templates");
	}

	/**
	 * Sets resultWindow to be shown. This method is used by pollerThread
	 * @param rsw window to be set as current ResultsWindow
	 */
	public void setResultsWindow(JFrame rsw) {
		this.resultsWindow = rsw;
		if (rsw instanceof ResultsWindow) {
			// Sets action for toolbar buttons
			((ResultsWindow) rsw).addButtonActions(simulate, pauseSimulation, stopSimulation);
		} else {
			showResults.setEnabled(true);
		}
		// Adds a listener that will unselect Show results button upon results window closing
		rsw.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				showResults.setSelected(false);
			}
		});
	}

	/**
	 * Called when showResults action is triggered
	 * @param selected Tells if show results button is selected or not
	 *
	 * Modified by Francesco D'Aquino
	 */
	public synchronized void showResultsWindow(boolean selected) {
		if (selected) {
			if (resultsWindow != null) {
				resultsWindow.show();
			}
		} else {
			if (resultsWindow != null) {
				resultsWindow.hide();
			}
		}
	}

	/**
	 * Shows results window and forces show results button to be selected
	 */
	public void showResultsWindow() {
		showResults.setSelected(true);
		showResultsWindow(true);
	}

	/**
	 * Returns current ResultsWindow
	 * @return current ResultsWindow or null if none was created
	 */
	public JFrame getResultsWindow() {
		return resultsWindow;
	}

	/**
	 * Returns current PAProgressWindow
	 * @return current PAProgressWindow or null if none was created
	 */
	public PAProgressWindow getPAProgressWindow() {
		return progressWindow;
	}

	/**
	 * Shows about window
	 */
	public void about() {
		AboutDialogFactory.showJMODEL(mainWindow);
	}

	/**
	 * Tells if something is selected into graph window
	 * @return true if something is selected
	 */
	public boolean isSomethingSelected() {
		return graph.getSelectionCell() != null;
	}

	/**
	 * Takes a screenshot of current jgraph. Shows a dialog to select image type and name
	 */
	public void takeScreenShot() {
		graph.clearSelection();
		graph.showScreenShotDialog();
	}

	/**
	 *  Shows the panel to solve a problem
	 */
	public void showRelatedPanel(int problemType, int problemSubType, Object relatedStation, Object relatedClass) {
		if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.NO_CLASS_ERROR)) {
			editUserClasses();
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.NO_STATION_ERROR)) {
			JOptionPane.showMessageDialog(null, "Please add at least one station other than a source or sink.\n",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.NO_MEASURE_ERROR)) {
			editMeasures();
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.OPEN_CLASS_BUT_NO_SOURCE_ERROR)) {
			JOptionPane.showMessageDialog(null, "An open class is defined but no sources or transitions. Please add a source or transition.\n",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.OPEN_CLASS_BUT_NO_SINK_ERROR)) {
			JOptionPane.showMessageDialog(null, "An open class is defined but no sinks or transitions. Please add a sink or transition.\n",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.SOURCE_WITHOUT_OPEN_CLASS_ERROR)) {
			editUserClasses();
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.SINK_BUT_NO_OPEN_CLASS_ERROR)) {
			editUserClasses();
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.NO_REFERENCE_STATION_ERROR)) {
			editUserClasses();
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.NO_ESSENTIAL_LINK_ERROR)) {
			String stationType = model.getStationType(relatedStation);
			String stationName = model.getStationName(relatedStation);
			String message = null;
			if (stationType.equals(CommonConstants.STATION_TYPE_SINK)) {
				message = stationName + " is not backward linked. Please add a backward link.\n";
			} else if (stationType.equals(CommonConstants.STATION_TYPE_TRANSITION)) {
				message = stationName + " is not forward or backward linked. Please add a forward or backward link.\n";
			} else {
				message = stationName + " is not forward linked. Please add a forward link.\n";
			}
			JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.INVALID_MEASURE_ERROR)) {
			editMeasures();
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.DUPLICATE_MEASURE_ERROR)) {
			int k = JOptionPane.showConfirmDialog(null, "Delete all redundant performance indices?\n",
					"Redundant performance indices found", JOptionPane.ERROR_MESSAGE);
			if (k == JOptionPane.YES_OPTION) {
				mc.deleteRedundantMeasures();
			}
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.INCONSISTENT_QUEUE_STRATEGY_ERROR)) {
			showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.UNPREDICTABLE_SERVICE_ERROR)) {
			showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.JOIN_WITHOUT_FORK_ERROR)) {
			JOptionPane.showMessageDialog(null, "A join is found but no forks. Please remove all joins or add a fork.\n",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.BLOCKING_REGION_CAPACITY_OVERLOAD_ERROR)) {
			editSimulationParameters();
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.BLOCKING_REGION_MEMORY_OVERLOAD_ERROR)) {
			editSimulationParameters();
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.CLASS_SWITCH_INVALID_MATRIX_ERROR)) {
			showStationParameterPanel(relatedStation, StationParameterPanel.SERVICE_SECTION, null);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.CLASS_SWITCH_REFERENCE_STATION_ERROR)) {
			editUserClasses();
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.ZERO_GUARD_STRATEGY_ERROR)) {
			showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, relatedClass);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.SEMAPHORE_NOT_BETWEEN_FORK_JOIN_ERROR)) {
			String stationName = model.getStationName(relatedStation);
			JOptionPane.showMessageDialog(null, stationName + " is not located between fork/join. This topology is not allowed.\n",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.SCALER_NOT_BETWEEN_FORK_JOIN_ERROR)) {
			String stationName = model.getStationName(relatedStation);
			JOptionPane.showMessageDialog(null, stationName + " is not located between fork/join. This topology is not allowed.\n",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
        else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.IMPATIENCE_ENABLED_IN_STATION_BETWEEN_FORK_JOIN_ERROR)) {
			showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.TRANSITION_INFINITE_ENABLING_DEGREE_ERROR)) {
			showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
		}
		// used only in JMVA conversion
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.BCMP_DIFFERENT_QUEUE_STRATEGIES_WARNING)) {
			String stationName = model.getStationName(relatedStation);
			int k = JOptionPane.showConfirmDialog(null, "According to BCMP theorem hypothesis, each server must have the same queue strategy\n"
					+ "for each class, but mixed queue strategies are found at " + stationName + ".\nDo you want to edit the queue strategies of "
					+ stationName + "?\n\n", "Mixed queue strategies found", JOptionPane.WARNING_MESSAGE);
			if (k == JOptionPane.OK_OPTION) {
				showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
			}
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.TRANSITION_INFINITE_ENABLING_DEGREE_ERROR)) {
			showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
		}
		// used only in JMVA conversion
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.BCMP_FCFS_DIFFERENT_SERVICE_STRATEGIES_WARNING)) {
			String stationName = model.getStationName(relatedStation);
			int k = JOptionPane.showConfirmDialog(null, "According to BCMP theorem hypothesis, each FCFS server must have the same service\n"
					+ "strategy for each class, but mixed service strategies (i.e. both the load dependent and\nindependent) are found at "
					+ stationName + ".\nDo you want to edit the service strategies of " + stationName + "?\n\n", "Mixed service strategies found",
					JOptionPane.WARNING_MESSAGE);
			if (k == JOptionPane.OK_OPTION) {
				showStationParameterPanel(relatedStation, StationParameterPanel.SERVICE_SECTION, null);
			}
		}
		// used only in JMVA conversion
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.BCMP_FCFS_NON_EXPONENTIAL_DISTRIBUTION_WARNING)) {
			String stationName = model.getStationName(relatedStation);
			int k = JOptionPane.showConfirmDialog(null, "According to BCMP theorem hypothesis, the service time distributions of each FCFS\n"
					+ "server must be exponential, but a non exponential distribution is found at " + stationName + ".\nDo you want to edit "
					+ " the service time distributions of " + stationName + "?\n\n", "Non exponential distribution found", JOptionPane.WARNING_MESSAGE);
			if (k == JOptionPane.OK_OPTION) {
				showStationParameterPanel(relatedStation, StationParameterPanel.SERVICE_SECTION, null);
			}
		}
		// used only in JMVA conversion
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.BCMP_FCFS_DIFFERENT_SERVICE_TIMES_WARNING)) {
			String stationName = model.getStationName(relatedStation);
			int k = JOptionPane.showConfirmDialog(null, "According to BCMP theorem hypothesis, each FCFS server must have the same mean service\n"
					+ "time for each class. If the service strategies are load dependent, the mean service time in each\nrange has to be the same "
					+ "for each class.\nDo you want to edit the mean service times of " + stationName + "?\n\n", "Mixed mean service times found",
					JOptionPane.WARNING_MESSAGE);
			if (k == JOptionPane.OK_OPTION) {
				showStationParameterPanel(relatedStation, StationParameterPanel.SERVICE_SECTION, null);
			}
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.NO_OPTIONAL_LINK_WARNING)) {
			String stationName = model.getStationName(relatedStation);
			JOptionPane.showMessageDialog(null, stationName + " is not backward linked. Please check the topology.\n", "Warning",
					JOptionPane.WARNING_MESSAGE);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.FORK_WITHOUT_JOIN_WARNING)) {
			JOptionPane.showMessageDialog(null, "A fork is found but no joins. Please check the topology.\n", "Warning",
					JOptionPane.WARNING_MESSAGE);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.PARAMETRIC_ANALYSIS_MODEL_MODIFIED_WARNING)) {
			int k = JOptionPane.showConfirmDialog(null, "The parametric analysis model previously defined becomes inconsistent with the simulation\n"
					+ "model. It will be automatically modified when simulation is started.\nDo you want to autocorrect and check the parametric "
					+ "analysis model?\n\n", "Inconsistent parametric analysis model", JOptionPane.WARNING_MESSAGE);
			if (k == JOptionPane.OK_OPTION) {
				model.getParametricAnalysisModel().checkCorrectness(true);
				editPAParameters();
			}
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.PARAMETRIC_ANALYSIS_NOT_AVAILABLE_WARNING)) {
			int k = JOptionPane.showConfirmDialog(null, "A parametric analysis model was defined, but no parametric analysis is currently available\n"
					+ "since the simulation model is changed. It is only possible to execute normal simulation.\nDo you want to continue anyway?\n\n",
					"Parametric analysis not available", JOptionPane.WARNING_MESSAGE);
			if (k == JOptionPane.OK_OPTION) {
				model.setParametricAnalysisEnabled(false);
				model.setParametricAnalysisModel(null);
			}
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.ZERO_POPULATION_WARNING)) {
			editUserClasses();
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.ZERO_TOTAL_POPULATION_WARNING)) {
			editUserClasses();
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.CLASS_SWITCH_BETWEEN_FORK_JOIN_WARNING)) {
			String stationName = model.getStationName(relatedStation);
			JOptionPane.showMessageDialog(null, stationName + " seems to be located between fork/join. The switched tasks may fail to merge\n"
					+ "at the join with a Guard strategy.\n", "Warning", JOptionPane.WARNING_MESSAGE);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.SCHEDULING_SAME_PRIORITY_WARNING)) {
			showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.TRANSITION_BETWEEN_FORK_JOIN_WARNING)) {
			String stationName = model.getStationName(relatedStation);
			JOptionPane.showMessageDialog(null, stationName + " seems to be located between fork/join. On any firing of a transition between\n"
					+ "fork/join, the number of output tasks for each class should be equal to the number of input\ntasks for that class.\n",
					"Warning", JOptionPane.WARNING_MESSAGE);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.TRANSITION_CONSTANT_ENABLING_DEGREE_WARNING)) {
			showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.TRANSITION_INVALID_INPUT_CONDITION_WARNING)) {
			showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.TRANSITION_NO_FIRING_OUTCOME_WARNING)) {
			showStationParameterPanel(relatedStation, StationParameterPanel.OUTPUT_SECTION, null);
		}
	}

	/**
	 * Used to discover if the instance can display simulation animation
	 *
	 * @return true if the instance can display simulation animation
	 */
	public boolean isAnimationDisplayable() {
		return true;
	}

	/**
	 * Gets the Dimension of a specified cell
	 * @param cell
	 * @return the cell Dimension
	 */
	public Rectangle2D getCellDimension(JmtCell cell) {
		return GraphConstants.getBounds(cell.getAttributes());
	}

	/**
	 * Adds given JmtCells to a freshly created blocking region. This method will not modify
	 * data structure and is used during load operation
	 */
	public void addCellsToBlockingRegion(Object regionKey, Object[] cells) {
		BlockingRegion bl = new BlockingRegion(this, regionKey);
		bl.addStations(cells);
	}

	/**
	 * Adds a new blocking region that contains selected cells,
	 * if this does not overlap with existing one
	 */
	public void addSelectionToNewBlockingRegion() {
		Object[] cells = graph.getSelectionCells();
		// Data structure to hold all selected stations and their search's key
		HashMap<Object, Object> stations = new HashMap<Object, Object>();
		boolean canBeAdded = true;
		Object regionKey = model.addBlockingRegion();
		for (Object cell : cells) {
			if (cell instanceof JmtCell) {
				Object stationKey = ((CellComponent) ((JmtCell) cell).getUserObject()).getKey();
				if (!model.canRegionStationBeAdded(regionKey, stationKey)) {
					canBeAdded = false;
					break;
				} else {
					stations.put(stationKey, cell);
				}
			} else if (cell instanceof BlockingRegion) {
				// A blocking region cannot overlap another one
				canBeAdded = false;
				break;
			}
		}
		// If blocking region can be added, adds it to graph window, otherwise deletes it
		if (canBeAdded && stations.size() > 0) {
			// Adds stations to blocking region into data structure
			for (Object stationKey : stations.keySet()) {
				model.addRegionStation(regionKey, stationKey);
			}
			addCellsToBlockingRegion(regionKey, stations.values().toArray());
		} else {
			model.deleteBlockingRegion(regionKey);
		}
	}

	/**
	 * This method is used to reflect drag in and out a blocking region on data
	 * structure and to move dragged cells to background to use transparency of
	 * blocking region over them
	 */
	public void handlesBlockingRegionDrag() {
		Object[] cells = graph.getSelectionCells();
		HashSet<Object> putBack = new HashSet<Object>();
		for (Object c : cells) {
			if (c instanceof JmtCell && ((JmtCell) c).parentChanged()) {
				// This cell was moved in, out or between blocking regions
				JmtCell cell = (JmtCell) c;
				Object key = ((CellComponent) cell.getUserObject()).getKey();
				Object oldRegionKey, newRegionKey;
				if (!(cell.getParent() instanceof BlockingRegion)) {
					// Object removed from blocking region
					putBack.add(cell);
					oldRegionKey = ((BlockingRegion) cell.getPrevParent()).getKey();
					model.removeRegionStation(oldRegionKey, key);
					// If region is empty, deletes region
					if (model.getBlockingRegionStations(oldRegionKey).size() == 0) {
						model.deleteBlockingRegion(oldRegionKey);
					}
					// Allow adding of removed objects to a new blocking region
					enableAddBlockingRegion(true);
				} else if (cell.getPrevParent() instanceof BlockingRegion) {
					// Object changed blocking region
					oldRegionKey = ((BlockingRegion) cell.getPrevParent()).getKey();
					model.removeRegionStation(oldRegionKey, key);
					// If region is empty, deletes region
					if (model.getBlockingRegionStations(oldRegionKey).size() == 0) {
						model.deleteBlockingRegion(oldRegionKey);
					}
					newRegionKey = ((BlockingRegion) cell.getParent()).getKey();
					model.addRegionStation(newRegionKey, key);
				} else {
					// Object added to a blocking region
					newRegionKey = ((BlockingRegion) cell.getParent()).getKey();
					if (!model.addRegionStation(newRegionKey, key)) {
						// object cannot be added to blocking region (for example it is a source)
						cell.removeFromParent();
						graph.getModel().insert(new Object[] { cell }, null, null, null, null);
						putBack.add(cell);
					}
					// Does not allow adding of selected objects to a new blocking region
					enableAddBlockingRegion(false);
				}
				// Resets parent for this cell
				cell.resetParent();
			}
			// Avoids insertion of an edge to a blocking region
			else if (c instanceof JmtEdge) {
				JmtEdge edge = (JmtEdge) c;
				if (edge.getParent() != null) {
					edge.removeFromParent();
					graph.getModel().insert(new Object[] { edge }, null, null, null, null);
					putBack.add(edge);
				}
			}
			// Avoids insertion of a blocking region to another
			else if (c instanceof BlockingRegion) {
				BlockingRegion region = (BlockingRegion) c;
				if (region.getParent() != null) {
					region.removeFromParent();
					graph.getModel().insert(new Object[] { region }, null, null, null, null);
					putBack.add(region);
				}
			}
		}
		// Puts cells removed from blocking regions to back
		graph.getModel().toBack(putBack.toArray());
	}

	/**
	 * Method that rotate components
	 *
	 * author Giuseppe De Cicco & Fabio Granara
	 */
	public void rotateComponent(Object[] cells) {
		if (cells == null) {
			cells = graph.getSelectionCells();
		}
		for (Object cell : cells) {
			if ((cell instanceof BlockingRegion) || cell instanceof JmtEdge) {
				continue;
			}
			JmtCell current = (JmtCell) cell;
			current.setLeftInputCell(!current.isLeftInputCell());

			rotateEndEdges(current,Math.toRadians(180));
			loadImage(current);

			Object key =((CellComponent) (current).getUserObject()).getKey();
			if (model.hasConnectionShape(key,key)){
				RotateSelfLoopEdge(model.getConnectionShape(key,key), current);
				loadImage(current);
			}
		}
		avoidOverlappingCell(cells);
	}

	public boolean isInGroup(Object cell) {
		Object[] celgru = null;
		Object[] celless = null;
		cells = graph.getDescendants(graph.getRoots());
		if (cells.length > 0) {
			for (Object cell2 : cells) {
				if (cell2 instanceof BlockingRegion) {
					celgru = new Object[1];
					celgru[0] = cell2;
					celless = graph.getDescendants(celgru);
					for (Object celles : celless) {
						if (celles.equals(cell)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public void avoidOverlappingCell(Object[] cells2) {
		overlapping.avoidOverlappingCell(cells2);
		graphRepaint();
		graph.getGraphLayoutCache().reload();
	}


	public void forceAdjustGraph() {
		if(mouseListener.getCurrentState() == mouseListener.bezierconnect){
			((BezierConnectState) mouseListener.getCurrentState()).initializeState();
		}

		Object[] cells = graph.getDescendants(graph.getRoots());
		Rectangle[] cellsBounds = new Rectangle[cells.length];
		boolean[] cellsRotate = new boolean[cells.length];
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] instanceof JmtCell) {
				JmtCell cell = (JmtCell) cells[i];
				cellsBounds[i] = GraphConstants.getBounds(cell.getAttributes()).getBounds();
				cellsRotate[i] = !cell.isLeftInputCell();
			}
		}

		adjustGraph();

		for (int i = 0; i < cells.length; i++) {
			if (cells[i] instanceof JmtCell) {
				JmtCell cell = (JmtCell) cells[i];
				GraphConstants.setBounds(cell.getAttributes(), cellsBounds[i]);
				if (cellsRotate[i]) {
					rotateComponent(new Object[] { cell });
				}
			}
		}
		graphRepaint();
		graph.getGraphLayoutCache().reload();
		avoidOverlappingCell(cells);
	}


	public void tryAdjustGraph(boolean needConfirm) {
		if(mouseListener.getCurrentState() == mouseListener.bezierconnect){
			((BezierConnectState) mouseListener.getCurrentState()).initializeState();
		}

		Object[] cells = graph.getDescendants(graph.getRoots());
		Rectangle[] cellsBounds = new Rectangle[cells.length];
		boolean[] cellsRotate = new boolean[cells.length];
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] instanceof JmtCell) {
				JmtCell cell = (JmtCell) cells[i];
				cellsBounds[i] = GraphConstants.getBounds(cell.getAttributes()).getBounds();
				cellsRotate[i] = !cell.isLeftInputCell();
			}
		}

		adjustGraph();

		if (needConfirm) {
			int resultValue = JOptionPane.showConfirmDialog(mainWindow, "Keep the new layout?",
					"Optimize the layout", JOptionPane.YES_NO_OPTION);
			if (resultValue == JOptionPane.NO_OPTION) {
				for (int i = 0; i < cells.length; i++) {
					if (cells[i] instanceof JmtCell) {
						JmtCell cell = (JmtCell) cells[i];
						GraphConstants.setBounds(cell.getAttributes(), cellsBounds[i]);
						if (cellsRotate[i]) {
							rotateComponent(new Object[] { cell });
						}
					}
				}
				graphRepaint();
				graph.getGraphLayoutCache().reload();
				avoidOverlappingCell(cells);
			}
		}
	}

	int e = -1;
	int x = 25;
	int y = 90;
	int widthMax = 0;
	int heightMax = 0;
	private boolean flag = false;
	private boolean flag1 = false;
	private boolean flag2 = false;
	private boolean inRepositionSons = false;
	private Thread animationHolder = null;

	public void adjustGraph() {
		Object[] cells;

		int inMin = 100;
		int inMax = 0;

		List<Object> min = new ArrayList<Object>();
		List<Object> max = new ArrayList<Object>();

		cells = graph.getDescendants(graph.getRoots());

		for (int i = 0; i < cells.length; i++) {
			if (cells[i] instanceof JmtCell) {
				Rectangle bounds = GraphConstants.getBounds(((JmtCell) cells[i]).getAttributes()).getBounds();
				if (bounds.getWidth() > widthMax) {
					widthMax = (int) bounds.getWidth();
				}
				if (bounds.getHeight() > heightMax) {
					heightMax = (int) bounds.getHeight();
				}
				if (!((JmtCell) cells[i]).isLeftInputCell()) {
					rotateComponent(new Object[] { cells[i] });
				}
			}
		}

		boolean sourceIn = false;
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] instanceof JmtCell) {
				((JmtCell) cells[i]).in = (DefaultGraphModel.getIncomingEdges(graph.getModel(), cells[i])).length;
				if (((JmtCell) cells[i]).in < inMin) {
					inMin = ((JmtCell) cells[i]).in;
				}
				if (((JmtCell) cells[i]).in > inMax) {
					inMax = ((JmtCell) cells[i]).in;
				}
			}
		}

		boolean projectClose = true;
		for (Object cell : cells) {
			if (cell instanceof JmtCell && !sourceIn) {
				if (((JmtCell) cell).in == 0) {
					projectClose = false;
				}
			}
		}

		boolean serverWithZeroIn = false;
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] instanceof JmtCell) {
				if (((JmtCell) cells[i]).in == 0) {
					serverWithZeroIn = true;
				}
				if (!min.contains(cells[i])) {
					if (((JmtCell) cells[i]).in == inMin) {
						min.add(cells[i]);
					} else if (((JmtCell) cells[i]).in == inMax) {
						max.add(cells[i]);
					}
				}
			}
		}

		if (!sourceIn && projectClose && !serverWithZeroIn) {
			int tmpMax = 0;
			JmtCell tmpCell = null;
			for (Object cell : cells) {
				if (cell instanceof JmtCell) {
					int tmpIn = (((JmtCell) cell).in);
					if (tmpMax < tmpIn) {
						tmpMax = tmpIn;
						tmpCell = ((JmtCell) cell);
					}
				}
			}
			min = new ArrayList<Object>();
			min.add(tmpCell);
		}

		int widthMaxMin = 0;
		for (int w = 0; w < min.size(); w++) {
			Rectangle bounds = GraphConstants.getBounds(((JmtCell) min.get(w)).getAttributes()).getBounds();
			if (bounds.getWidth() > widthMaxMin) {
				widthMaxMin = (int) bounds.getWidth();
			}
		}

		for (int q = 0; q < min.size(); q++) {
			Rectangle bounds = GraphConstants.getBounds(((JmtCell) min.get(q)).getAttributes()).getBounds();
			x = widthMaxMin / 2 + 25 - (int) (bounds.getWidth() / 2);
			searchNext((JmtCell) min.get(q));

			e += ((JmtCell) min.get(q)).sons;

			flag1 = false;
			flag2 = false;
		}

		min = new ArrayList<Object>();
		for (int w2 = 0; w2 < cells.length; w2++) {
			if (cells[w2] instanceof JmtCell) {
				if (!((JmtCell) cells[w2]).seen) {
					min.add(cells[w2]);
				}
			}
		}
		flag1 = false;

		for (int q = 0; q < min.size(); q++) {
			searchNext((JmtCell) min.get(q));
			flag1 = false;
		}

		flag1 = false;
		flag2 = false;
		e = -1;
		x = 25;
		y = 90;
		widthMax = 0;
		heightMax = 0;
		for (int z = 0; z < cells.length; z++) {
			if (cells[z] instanceof JmtCell) {
				((JmtCell) cells[z]).sons = 1;
				((JmtCell) cells[z]).seen = false;
			}
		}

		graphRepaint();
		graph.getGraphLayoutCache().reload();
		avoidOverlappingCell(cells);
	}

	private int searchNext(JmtCell prev) {
		Rectangle boundspadre = GraphConstants.getBounds((prev).getAttributes()).getBounds();
		Object[] listEdges = null;
		GraphModel graphmodel = graph.getModel();
		List<Object> next = new ArrayList<Object>();

		if (!flag1 && !prev.seen) {
			if (!flag2) {
				boundspadre.setLocation(x, y + ((e + 1) * (42 + heightMax)) - (int) (boundspadre.getHeight() / 2) + 30);
			} else {
				boundspadre.setLocation(x - (int) (boundspadre.getWidth() / 2), y + ((e + 1) * (42 + heightMax))
						- (int) (boundspadre.getHeight() / 2) + 30);
			}

			GraphConstants.setBounds(prev.getAttributes(), boundspadre);
			x = (int) boundspadre.getCenterX() + widthMax + 50;
			prev.seen = true;
			flag2 = true;
		}

		listEdges = DefaultGraphModel.getOutgoingEdges(graphmodel, prev);
		Vector<Object> listEdgestmp = new Vector<Object>();
		for (Object listEdge : listEdges) {
			JmtCell qq = (JmtCell) (graphmodel.getParent(graphmodel.getTarget(listEdge)));
			if (!(qq).seen) {
				listEdgestmp.add(listEdge);
			}
		}
		listEdges = listEdgestmp.toArray();

		int numTarget = listEdges.length;
		if (numTarget == 0) {
			return 1;
		}

		for (int k = 0; k < numTarget; k++) {
			next.add((graphmodel.getParent(graphmodel.getTarget(listEdges[k]))));
		}

		int j = 1;
		if (!inRepositionSons && !((JmtCell) next.get(0)).seen) {
			j = searchNext((JmtCell) next.get(0));
		} else if (inRepositionSons && !((JmtCell) next.get(0)).seen) {
			Rectangle bounds = GraphConstants.getBounds(((JmtCell) next.get(0)).getAttributes()).getBounds();
			bounds.setLocation((int) (boundspadre.getCenterX()) + widthMax + 50 - (int) (bounds.getWidth() / 2),
					(int) boundspadre.getCenterY() - (int) (bounds.getHeight() / 2));
			GraphConstants.setBounds(((JmtCell) next.get(0)).getAttributes(), bounds);

			((JmtCell) next.get(0)).seen = true;
			j = searchNext((JmtCell) next.get(0));
		}

		if (numTarget > 0) {
			if (!flag) {
				repositionSons(prev, next, j - 1, 1);
			} else {
				repositionSons(prev, next, -1, 0);
			}
			flag = false;
		}

		(prev).sons = 0;
		for (int w = 0; w < numTarget; w++) {
			prev.sons += ((JmtCell) next.get(w)).sons;
		}

		return prev.sons;
	}

	private void repositionSons(JmtCell padre, List<Object> sons, int numero, int cont) {
		inRepositionSons = true;
		Object[] listEdges = null;
		GraphModel graphmodel = graph.getModel();

		flag1 = true;

		int j = 0;
		Rectangle boundspadre = GraphConstants.getBounds(padre.getAttributes()).getBounds();
		int w = boundspadre.y + ((heightMax + 35) * (numero + 1)) - 38;

		for (int i = cont; i < sons.size(); i++) {
			if (!((JmtCell) sons.get(i)).seen) {
				Rectangle bounds = GraphConstants.getBounds(((JmtCell) sons.get(i)).getAttributes()).getBounds();
				bounds.setLocation((int) (boundspadre.getCenterX()) + widthMax + 50 - (int) (bounds.getWidth() / 2),
						w - (int) (bounds.getHeight() / 2) + 80);
				GraphConstants.setBounds(((JmtCell) sons.get(i)).getAttributes(), bounds);

				((JmtCell) sons.get(i)).seen = true;
				listEdges = DefaultGraphModel.getOutgoingEdges(graphmodel, sons.get(i));

				if (listEdges.length > 0) {
					flag = true;
					j = searchNext((JmtCell) sons.get(i));
					inRepositionSons = true;
				}

				if (j > 0) {
					j = j - 1;
				}

				listEdges = null;
			}

			w = w + (heightMax + ((heightMax + 15) * j) + 30);
			j = 0;
		}

		inRepositionSons = false;
	}

	public void setIsReleased(boolean state) {
		isReleased = state;
	}

	public boolean getIsReleased() {
		return isReleased;
	}

	public void zoomIn() {
		graph.setScale(graph.getScale() * 1.25);
	}

	public void zoomOut() {
		graph.setScale(graph.getScale() / 1.25);
	}

	@Override
	public void setAnimationHolder(Thread thread) {
		animationHolder = thread;
	}

	public JSimGraphModel getModel() {
		return model;
	}

	public JSIMGraphMain getMainWindow() {
		return this.mainWindow;
	}

	public void downloadDefaultTemplates() {
		customizableDialogFactory.getDialog(520, 200, new AutoDownloadPanel(), "Downloading...");
	}

	// used to find the bottom bound of canvas
	public double getBound() {
		graph.getGraphLayoutCache().reload();
		int nCells = graph.getModel().getRootCount();
		double Y = 10.0;
		for (int i = 0; i < nCells; i++) {
			double temp = graph.getCellBounds(graph.getModel().getRootAt(i)).getMaxY();
			if (Y < temp) {
				Y = temp;
			}
		}
		return Y;
	}

	@Override
	public String getFileName() {
		if (openedArchive != null) {
			return openedArchive.getName();
		} else {
			return null;
		}
	}


	/**
	 * Toggle enableGrid in the JGraph object
	 *
	 * if the grid was previously enabled, the grid is now disabled.
	 * if the grid was previously disabled, the grid is now enabled.
	 *
	 * @author Emma Bortone
	 * Date: April 2020
	 */
	public void toggleGrid() {
		if (this.getGraph().isGridEnabled()) {
//			System.out.println("disable grid");
			this.getGraph().setGridEnabled(false); // disables the grid
			this.getGraph().setGridMode(0); // displays dots in the background
			this.getGraph().setGridColor(Color.BLACK);
		}
		else {
//			System.out.println("enable grid");
			this.getGraph().setGridEnabled(true); // enables the grid
			this.getGraph().setGridMode(2); // displays a grid in the background
			this.getGraph().setGridColor(Color.LIGHT_GRAY);

		}
	}

	/**
	 * If the Grid is enabled, return the offset separating a given point from the grid
	 * Otherwise return (0, 0)
	 *
	 * @param  originalPoint Point2D
	 * @return offset Point2D
	 *
	 * @author Emma Bortone
	 * Date: April 2020
	 */
	private Point2D offsetForGridSnap(Point2D originalPoint){
		Point2D snappedPoint = this.snap(new Point2D.Double(originalPoint.getX(),originalPoint.getY()));
		Point2D offset= new Point2D.Double(snappedPoint.getX()-originalPoint.getX(),snappedPoint.getY()-originalPoint.getY());
		return offset;
	}

	/**
	 * Moves a JmtCell by a given offset
	 * @param  offset Point2D
	 * @param  cell JmtCell
	 *
	 * @author Emma Bortone
	 * Date: April 2020
	 */
	private void moveCellByOffset(JmtCell cell, Point2D offset){
		Rectangle2D bounds = GraphConstants.getBounds(cell.getAttributes()); // bounds of the cell
		Point2D topLeftPoint = new Point2D.Double(bounds.getMinX(), bounds.getMinY()); // Top left point of the cell
		// newTopLeftPoint = topLeftPoint+ offset
		Point2D newTopLeftPoint = new Point2D.Double(topLeftPoint.getX()+offset.getX(), topLeftPoint.getY()+offset.getY());
		Rectangle newBounds = new Rectangle((int) newTopLeftPoint.getX(), (int) newTopLeftPoint.getY() , (int) bounds.getWidth(), (int) bounds.getHeight());
		GraphConstants.setBounds(cell.getAttributes(), newBounds);
	}


	/**
	 * Return the position of the input port of a JmtCell if it exists,
	 * otherwise returns null
	 * @param  cell JmtCell
	 * @return position of input port Point2D
	 *
	 * @author Emma Bortone
	 * Date: April 2020
	 */
	private Point2D getPortInPosition(JmtCell cell){
		Rectangle2D rett = GraphConstants.getBounds( cell.getAttributes());
		PortView port = this.getInPortViewAt((int) rett.getCenterX(), (int) rett.getCenterY());
		if (port!= null){
			return  port.getLocation();
		}
		return null;
	}

	/**
	 * Return the position of the output port of a JmtCell if it exists,
	 * otherwise returns null
	 * @param  cell JmtCell
	 * @return position of output port Point2D
	 *
	 * @author Emma Bortone
	 * Date: April 2020
	 */
	private Point2D getPortOutPosition(JmtCell cell){
		Rectangle2D rett = GraphConstants.getBounds( cell.getAttributes());
		PortView port = this.getOutPortViewAt((int) rett.getCenterX(), (int) rett.getCenterY());
		if (port!= null){
			return port.getLocation();
		}
		return null;
	}

	/**
	 * If the grid is enabled, moves a JmtCell so that it one of its ports is snapped to the grid.
	 * Returns true if the cell was moved, false otherwise
	 *
	 * First we try to snap the input port, if it doesn't exists (ex :source)  we snap the output port
	 *
	 * @param  cell JmtCell
	 * @return true if the cell was moved,
	 * 	       otherwise returns false
	 *
	 * @author Emma Bortone
	 * Date: April 2020
	 */
	public boolean snapCellByPort(JmtCell cell){
		Point2D offset;
		Point2D inPortPoint = this.getPortInPosition( cell);
		Point2D outPortPoint = this.getPortOutPosition( cell);

		if (inPortPoint != null) {
			// offset = distance separating the inPort from the grid
			offset = this.offsetForGridSnap(inPortPoint);
		}
		// If there no input port (ex :source) the cell is snapped by its output port
		else {
			//offset = distance separating the outPort from the grid
			offset = this.offsetForGridSnap(outPortPoint);
		}
		//If the offset between the snapped Port point and the original Port point is superior to 1, the cell is moved
		//If the grid is not enabled, the offset will be zero and the cell won't be moved
		if ((Math.abs(offset.getX()) >=1) || (Math.abs(offset.getY()) >=1)){ //
			this.moveCellByOffset(cell, offset);
			return true ;
		}
		else {
			return false;
		}
	};

    /**
     * Returns true if the cell is connected only with Bezier edges,
     * otherwise returns false
     *
     *
     * @param  cell JmtCell
     * @return true if the cell if the cell is connected only with Bezier edges,
     * 	       otherwise returns false
     *
     * @author Emma Bortone
     * Date: April 2020
     */
	private boolean isLinkedOnlyToBezierEdges(JmtCell cell){
        Integer childIndex;
        List children= cell.getChildren();
        Set edges = new HashSet();
        boolean isOnlyLinkedByBezierEdges = true;

        // all the edges connected to this cell are put in the HashSet edges
        for (childIndex=0; childIndex<cell.getChildCount(); childIndex++ ){
            if (children.get(childIndex) instanceof  DefaultPort){
                   edges.addAll(((DefaultPort)children.get(childIndex)).getEdges());
            }
        }

        // For each edge connected to the cell, we check if it a bezier edge
        //If we find one NON Bezier edge, the loop break and the function returns false
        Iterator<JmtEdge> itr = edges.iterator();
        JmtEdge e = null;
        for (int i=0; itr.hasNext(); i++){
            e = itr.next();
            if (!e.getIsBezier()){
                isOnlyLinkedByBezierEdges=false;
                break;
            }
        }
        return isOnlyLinkedByBezierEdges;
    }


    /**
     * Enables or Disable the RotateLeft and RotateRight buttons
     *
     * If the selected cells contain only connection of Bezier type, then the rotation buttons are enabled
     * If the selected cells contain at least one connection of NON Bezier type, then the rotation buttons are disabled
     *
     *
     * @author Emma Bortone
     * Date: April 2020
     */
	private void toggleFreeRotationButtons(){
        //find all the selected cells
        boolean SelectionLinkedOnlyToBezierEdges =true;
        Object[] cells = graph.getSelectionCells();
        for (Object cell : cells) {
            if (cell instanceof JmtCell) {

            	 if (!(((JmtCell) cell).isFreeRotationAllowed())){
                    SelectionLinkedOnlyToBezierEdges = false;
                    break;
                }
            }
        }
        if (SelectionLinkedOnlyToBezierEdges){
            //If all the selected cells are linked only by Bezier edges, the buttons are enabled
            this.enableRotateLeftAction(true);
            this.enableRotateRightAction(true);
        }
        else{
            // If the selected cells contain at least one connection of NON Bezier type, the rotation buttons are disabled
            this.enableRotateLeftAction(false);
            this.enableRotateRightAction(false);
        }
    }


	/**
	 * Set the angle of rotation in the attributes of the rotated cells
	 * Load the rotated image
	 *
	 * @param cells list of cells to rotate left
	 *
	 * @author Emma Bortone
	 * Date: April 2020
	 */
    public void rotateLeft(Object[] cells) {
		if (cells == null) {
			cells = graph.getSelectionCells();
		}
		for (Object cell : cells) {
			if ((cell instanceof BlockingRegion) || cell instanceof JmtEdge) {
				continue;
			}
			JmtCell current = (JmtCell) cell;

			Double previousAngle= current.getRotationAngle();
			Double angleModification = 45.0;
			Double newAngle= (previousAngle - angleModification) % (360);
			current.setRotationAngle(newAngle);
			rotateEndEdges(current,Math.toRadians(-angleModification));
			loadImage(current);


			Object key =((CellComponent) (current).getUserObject()).getKey();
			if (model.hasConnectionShape(key,key)){
				RotateSelfLoopEdge(model.getConnectionShape(key,key), current);
				loadImage(current);
			}
		}
	}

	/**
	 * Set the angle of rotation in the attributes of the rotated cells
	 * Load the rotated image
	 *
	 * @param cells list of cells to rotate left
	 *
	 * @author Emma Bortone
	 * Date: April 2020
	 */
	public void rotateRight(Object[] cells) {
		if (cells == null) {
			cells = graph.getSelectionCells();
		}
		for (Object cell : cells) {
			if ((cell instanceof BlockingRegion) || cell instanceof JmtEdge) {
				continue;
			}
			JmtCell current = (JmtCell) cell;

			Double previousAngle= current.getRotationAngle();
			Double angleModification = 45.0;
			Double newAngle= (previousAngle + angleModification) % (360);
			current.setRotationAngle(newAngle);
			rotateEndEdges(current,Math.toRadians(angleModification));
			loadImage(current);

			Object key =((CellComponent) (current).getUserObject()).getKey();
			if (model.hasConnectionShape(key,key)){
				RotateSelfLoopEdge(model.getConnectionShape(key,key), current);
				loadImage(current);
			}

		}
	}

	/**
	 * Modify the points of the path (self loop path) of a cell that is rotated
	 * @param path path of the cell loop
	 * @param cell station that presents a self loop that needs to be rotated
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
	private void RotateSelfLoopEdge(JMTPath path, JmtCell cell){
		ArrayList pointsList = new ArrayList<JMTPoint>();
		ArrayList<JMTArc> arcs = new ArrayList<JMTArc>();
		pointsList.add( new Point2D.Double(0, 0));
		pointsList.add( new Point2D.Double(0, 0));
		double xOffset, yOffset, rotationAngle;
		Point2D.Double point1,  point2, point3, point4, inputPort_location, outputPort_location;
		inputPort_location = (Point2D.Double) getPortInPosition(cell);
		outputPort_location = (Point2D.Double)  getPortOutPosition(cell);

		double height =inputPort_location.getY()-outputPort_location.getY();
		double width = inputPort_location.getX()-outputPort_location.getX();
		rotationAngle = ((!(cell.isLeftInputCell())) ? (cell.getRotationAngle()+180.0) : cell.getRotationAngle());
		xOffset = ((width >= 0) ? -20 : 20);
		yOffset = ((height >= 0) ? 20 : -20);
		if (Math.abs(height) <= 1.0){
			yOffset = 40.0;
		}
		if (Math.abs(width) <= 1.0){
			xOffset = 40.0;
		}
		//If station is in vertical position
		if (rotationAngle==-90.0 || rotationAngle==90.0 ||rotationAngle==270.0 || rotationAngle==-270.0){
			point1 = new Point2D.Double(0.0, -yOffset);
			point2= new Point2D.Double(xOffset, point1.getY());
			point3= new Point2D.Double(point2.getX(),height+yOffset);
			point4= new Point2D.Double(0.0,point3.getY());
		}
		else {
			point1 = new Point2D.Double(xOffset, 0.0);
			point2= new Point2D.Double(point1.getX(), height+yOffset);
			point3= new Point2D.Double(width-xOffset, point2.getY());
			point4= new Point2D.Double(point3.getX(), height);
		}
		arcs.add(new JMTArc(new Point2D.Double(0, 0), pointsList, point1));
		arcs.add(new JMTArc(point1, pointsList, point2));
		arcs.add(new JMTArc(point2, pointsList, point3));
		arcs.add(new JMTArc(point3, pointsList, point4));
		arcs.add(new JMTArc(point4, pointsList,  new Point2D.Double(0, 0)));
		path.setArcs(arcs);
	}

	/**
	 * When a station is rotated, the positions of the end of its edges (if different from the position
	 * of the port) need to be rotated by the same rotation
	 * @param angle angle by which the cell was rotated (in radians)
	 * @param cell station that has been rotated
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
	private void rotateEndEdges(JmtCell cell, double angle){
		Set<Object> edges = new HashSet<Object>();
		for (Object c : (cell).getChildren()) {
			if (c instanceof DefaultPort) {
				edges.addAll(((DefaultPort) c).getEdges());
			}
		}
		for (Object e : edges){
			if (model.hasConnectionShape(((JmtEdge) e).getSourceKey() ,((JmtEdge) e).getTargetKey())){
				JMTPath path = (model.getConnectionShape(((JmtEdge) e).getSourceKey() ,((JmtEdge) e).getTargetKey()));
				// The target points must be moved
				if (((JmtEdge) e).getTargetKey() ==  ((CellComponent) cell.getUserObject()).getKey()){
					Point2D initialTargetPosition = path.getArc(path.getArcsNb()-1).getTarget();
					Point2D newTargetPosition = newEndPosition( initialTargetPosition, angle);
					path.getArc(path.getArcsNb()-1).setTarget(newTargetPosition);
				}
				// The input point must be moved
				else {
					Point2D initialSourcePosition = path.getArc(0).getSource();
					Point2D newSourcePosition= newEndPosition( initialSourcePosition,  angle);
					path.getArc(0).setSource(newSourcePosition);
				}
			}
		}

	}

	/**
	 * This function calculates the new position of the end of the edge after a rotation by the angle in argument
	 * @param initialPosition initial position of the end of the edge
	 * @param angle angle by which the cell is rotated (in radians)
	 * @return The new position of the end edge
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
	private Point2D newEndPosition(Point2D initialPosition, double angle){
		Point2D newPosition = new Point2D.Double(0.0,0.0);
		double norm =Math.sqrt( Math.pow(initialPosition.getX(),2)+Math.pow(initialPosition.getY(),2));
		if (norm > 10e-6){
			double yPos = 1;
			if(initialPosition.getY() <0.0){
				yPos = -1;
			}
			double angle1 = Math.acos(  (initialPosition.getX())/norm) * yPos;
			double angle2 = angle1+angle;
			 newPosition = new Point2D.Double(norm*Math.cos(angle2),norm*Math.sin(angle2));
		}
		return newPosition;
	}


	/**
	 * This function interrupt the drawing of an Edge
	 * For example if the user realised that he made a mistake
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
	public void cancelEdgeDrawing(){
		if(mouseListener.getCurrentState()==mouseListener.bezierconnect){
			((BezierConnectState) mouseListener.getCurrentState()).initializeState();
		}
	}



	/**
	 * This function load the image of a station applying the right modifications (rotation or mirror)
	 *
	 * @author Emma Bortone
	 * Date: April 2020
	 */
	public void loadImage (JmtCell cell) {
		String iconName = cell.getIcon();
		Map<Object, Map> nested = new Hashtable<Object, Map>();
		Map attributeMap = new Hashtable();
		ImageIcon icon;
		boolean mirrored = !(cell.isLeftInputCell());
		boolean rotated = (cell.isFreeRotationAllowed());

		if(mirrored & rotated){
			icon = JMTImageLoader.loadImage(iconName, ImageLoader.MODIFIER_MIRROR_AND_ROTATE,  cell.getRotationAngle());
		}
		else if(mirrored){
			icon = JMTImageLoader.loadImage(iconName, ImageLoader.MODIFIER_MIRROR);
		}
		else if(rotated){
			icon = JMTImageLoader.loadImage(iconName, ImageLoader.MODIFIER_ROTATE, cell.getRotationAngle());
		}
		else{
			icon = JMTImageLoader.loadImage(iconName);
		}
		GraphConstants.setIcon(attributeMap, icon);
		nested.put(cell, attributeMap);
		cell.updateCellSize(icon, graph);
		cell.updatePortPositions(nested, icon, cell.getSize(graph));
		graph.getGraphLayoutCache().edit(nested);
	}

	/**
	 * This function shows the helper bar for drawing Bezier Edges
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
	public void BezierConnectshowHelperBar(){
		BezierConnectionHelperBar.setVisible(true);
	}

	/**
	 * This function hides the helper bar for drawing Bezier Edges
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
	public void BezierConnecthideHelperBar(){
		BezierConnectionHelperBar.setVisible(false);
	}

	/**
	 * @return the helper bar for the creation of Bezier Edges
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
	public BezierConnectionHelperBar getBezierConnectionHelperBar() {
		return (BezierConnectionHelperBar);
	}

	/**
	 * Update the shape of a connection when the source station is moved
	 * The goal is to update the shape in order to maintain constant the absolute position of all the control points
	 * of the connection shape except for the first arc.
	 * @param offsetByCell
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
	void updateBezierPath(HashMap<Object, Point2D> offsetByCell){
		for (Object sourceKey : offsetByCell.keySet()) {
			HashMap<Object, JMTPath> pathByTarget = model.getConnectionShapesFromSource(sourceKey);
			if (pathByTarget !=null){
				for (Object targetKey : pathByTarget.keySet()) {
					if ( !(offsetByCell.keySet().contains(targetKey))){ //if the target wasn't moved

						JMTPath path = model.getConnectionShape(sourceKey,targetKey);
						Point2D offset = offsetByCell.get(sourceKey);
						Integer n = path.getArcsNb();

						Point2D newSourceArc;
						Point2D newTargetArc;

						if (n>1){
							//for first arc
							newTargetArc = UtilPoint.subtractPoints(path.getArc(0).getTarget(),offset);
							path.getArc(0).setTarget(newTargetArc);

							//arcs in the middle
							for (int i = 1; i < n-1; i++) {
								newSourceArc = UtilPoint.subtractPoints(path.getArc(i).getSource(),offset);
								newTargetArc = UtilPoint.subtractPoints(path.getArc(i).getTarget(),offset);
								path.getArc(i).setSource(newSourceArc);
								path.getArc(i).setTarget(newTargetArc);
							}

							//for last arc
							newSourceArc = UtilPoint.subtractPoints(path.getArc(n-1).getSource(),offset);
							path.getArc(n-1).setSource(newSourceArc);
						}

					}
				}
			}

		}
	}

	/**
	 * This function updates the labels of all Bezier edges connected to a cell after the cell was moved
	 * The labels identify the broken arcs
	 * @param cells : the cells that were moved
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
    public void updateLabelAfterMovingCell(Object[] cells){
        ArrayList<JmtEdge> edges = new ArrayList<>();
        if (cells != null) {
//            System.out.println("mediator -> updateLabelAfterMovingCell"+cells);
            for (Object cell : cells) {
                if (cell instanceof JmtCell) {
                    for (Object c : ((JmtCell) cell).getChildren()) {
                        if (c instanceof DefaultPort) {
                            edges.addAll(((DefaultPort) c).getEdges());
                        }
                    }
                } else if (cell instanceof BlockingRegion) {
                    for (Object c : ((BlockingRegion) cell).getChildren()) {
                        if (c instanceof JmtCell) {
                            for (Object c1 : ((JmtCell) cell).getChildren()) {
                                if (c1 instanceof DefaultPort) {
                                    edges.addAll(((DefaultPort) c).getEdges());

                                }
                            }
                        }
                    }
                }
            }
            for (Object edge : edges) {
                Object sourcekey = ((JmtEdge) edge).getSourceKey();
                Object targetkey = ((JmtEdge) edge).getTargetKey();
                if (model.hasConnectionShape(sourcekey, targetkey)) {
                    Point2D source =(Point2D) ((PortView) ((EdgeView) getViewOfCell(edge, false)).getSource()).getLocation();
                    Point2D target = (Point2D) ((PortView) ((EdgeView) getViewOfCell(edge, false)).getTarget()).getLocation();
                    editConnectionLabels( sourcekey,targetkey,  source,  target, (JmtEdge) edge);
                }
            }
        }
    };

	/**
	 * Sets the labels for Bezier connections
	 * A connection needs a labels if it contains one or several broken arcs. The labels are positioned on each side
	 * of the breakage to identify how they are connected
	 * @param map : attribute map
	 * @param sourceKey key of the station at the source of the edge
	 * @param targetKey key of the station at the target of the edge
	 * @param source position of the output port of the source station
	 * @param target position of the input port of the target station
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
	private void setConnectionLabels(Map map, Object sourceKey, Object targetKey, Point2D source, Point2D target){
        //ADDING LABEL TO BROKEN ARCS
        // for all the arcs
		if (model.hasConnectionShape(sourceKey,targetKey)) {
			JMTPath path = model.getConnectionShape(sourceKey,targetKey);
			int n = path.getArcsNb();
			ArrayList<Object> labels = new ArrayList<>();
			ArrayList<Point2D> labelPositions = new ArrayList<Point2D>();
			Point2D sourceArc,targetArc;
			Point2D[] listPoints = getAbsolutePoints( source,  target, sourceKey, targetKey);
			double lengthPath = lengthPathToPoint(Arrays.asList(listPoints),4 * (n-1) + 3);
			int indexBreak = 0;
			for (int i = 1; i < n; i++) {
				sourceArc =  listPoints[4 * i];
				targetArc =  listPoints[4 * (i-1) + 3];

				if (!UtilPoint.equalsWithTolerance(sourceArc,targetArc)){ //if edge is broken
					double lengthPathToSource =lengthPathToPoint(Arrays.asList(listPoints),4 * i);
					double lengthPathToTarget =lengthPathToPoint(Arrays.asList(listPoints),4 * (i-1) + 3);

					String letter = edgeBreakagesIterator.getLetter(sourceKey,targetKey,indexBreak);
					labels.add(letter);
					labels.add(letter);
					labelPositions.add(new Point2D.Double(GraphConstants.PERMILLE*(lengthPathToSource/lengthPath),10.0));
					labelPositions.add(new Point2D.Double(GraphConstants.PERMILLE*(lengthPathToTarget/lengthPath),10.0));
					indexBreak ++;
				}

			}
			while(indexBreak < edgeBreakagesIterator.getNumberAttributedTo(sourceKey,targetKey)) {
				// RemoveLast
				edgeBreakagesIterator.removeLast(sourceKey,targetKey);
			}
			Point2D[] simpleArray = new Point2D[ labelPositions.size() ];
			labelPositions.toArray( simpleArray );
			GraphConstants.setExtraLabelPositions(map, simpleArray);
			GraphConstants.setExtraLabels(map, labels.toArray());
		}


	}


	/**
	 *  Edit the labels the given connection
	 * 	A connection needs a labels if it contains one or several broken arcs. The labels are positioned on each side
	 *  of the breakage to identify how they are connected
	 * @param sourceKey key of the station at the source of the edge
	 * @param targetKey key of the station at the target of the edge
	 * @param source position of the output port of the source station
	 * @param target position of the input port of the target station
	 * @param edge edge whose labels needs to be edited
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
	public void editConnectionLabels(Object sourceKey, Object targetKey, Point2D source, Point2D target, JmtEdge edge){
        Map nested = new Hashtable();
		Map map = new Hashtable();

		setConnectionLabels(map,  sourceKey, targetKey,  source,  target);
        nested.put(edge, map);
        graph.getGraphLayoutCache().edit(nested, null, null, null);
    }

	/**
	 * Returns a list of the points constituting a path in absolute
	 * @param sourceKey key of the station at the source of the edge
	 * @param targetKey key of the station at the target of the edge
	 * @param source position of the output port of the source station
	 * @param target position of the input port of the target station
	 * @return the list of point in absolute
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
    private Point2D[] getAbsolutePoints(Point2D source, Point2D target, Object sourceKey, Object targetKey){
        ArrayList<Point2D> points = new ArrayList<>();
		JMTPath path = model.getConnectionShape(sourceKey,targetKey);
		Point2D sourceArc, controlPoint0, controlPoint1, targetArc;

		for (int i=0; i<path.getArcsNb()-1;i++){
            sourceArc = UtilPoint.addPoints(source,path.getArc(i).getSource());
            controlPoint0 = UtilPoint.addPoints(sourceArc,path.getArc(i).getArcPoints().get(0));
            targetArc = UtilPoint.addPoints(source,path.getArc(i).getTarget());
            controlPoint1 = UtilPoint.addPoints(targetArc,path.getArc(i).getArcPoints().get(1));
            points.add(sourceArc);
            points.add(controlPoint0);
            points.add(controlPoint1);
            points.add(targetArc);
        }
        sourceArc = UtilPoint.addPoints(source,path.getArc(path.getArcsNb()-1).getSource());
        controlPoint0 = UtilPoint.addPoints(sourceArc,path.getArc(path.getArcsNb()-1).getArcPoints().get(0));
        targetArc = UtilPoint.addPoints(target,path.getArc(path.getArcsNb()-1).getTarget());
        controlPoint1 = UtilPoint.addPoints(targetArc,path.getArc(path.getArcsNb()-1).getArcPoints().get(1));
        points.add(sourceArc);
        points.add(controlPoint0);
        points.add(controlPoint1);
        points.add(targetArc);

        Point2D[] listPoints = new Point2D[ points.size() ];
        points.toArray( listPoints );
        return listPoints;

    }

	/**
	 * Returns the length of a path constituted by the list of points from the start to the specific index
	 * @param points list of points
	 * @param pointIndex index of the end point
	 * @return length of a path from the start to the specific index
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
    private double lengthPathToPoint(List points, int pointIndex){
        double length=0.0;
        for (int i=0; i<pointIndex; i++ ){
            length=length+Math.sqrt(Math.pow(((Point2D)points.get(i+1)).getX()-((Point2D) points.get(i)).getX(),2)+
                    Math.pow(((Point2D) points.get(i+1)).getY()-((Point2D) points.get(i)).getY(),2));
        }
        return length;
    }



	// Brings the Specified Cells to Front
	public void toFront(Object[] c) {
		if (c != null && c.length > 0) {
			graph.getGraphLayoutCache().toFront(c);
		}
	}

}
