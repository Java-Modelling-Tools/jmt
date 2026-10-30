package jsimgraph.controller.actions;

import static org.junit.Assert.*;

import java.io.File;

import javax.swing.JCheckBox;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import common.panels.DebugSolverPanel;
import jmt.framework.gui.listeners.AbstractJMTAction;
import log.SimEventTracer;

/**
 * Tests for Visual Debug Tracer mode activation and JAR requirement.
 */
public class VisualDebugTracerTest {

	@Before
	public void setUp() {
		SimEventTracer.disable();
		SimEventTracer.IS_RUNNING_FROM_JAR_OVERRIDE = null;
	}

	@After
	public void tearDown() {
		SimEventTracer.disable();
		SimEventTracer.IS_RUNNING_FROM_JAR_OVERRIDE = null;
		File dir = new File("traces");
		if (dir.exists() && dir.isDirectory()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File f : files) {
					f.delete();
				}
			}
			dir.delete();
		}
	}

	@Test
	public void testActionProperties() {
		VisualDebugTracer action = new VisualDebugTracer(null);
		assertEquals("Visual Debug Tracer Mode", action.getValue(javax.swing.Action.NAME));
		assertTrue("Action must be selectable (checkbox item)",
				Boolean.TRUE.equals(action.getValue(AbstractJMTAction.SELECTABLE)));
		assertFalse("Action should start unselected when tracer is disabled", action.isSelected());
	}

	@Test
	public void testDebugSolverPanelJarRequirement() {
		DebugSolverPanel panel = new DebugSolverPanel(null);
		JCheckBox checkBox = panel.getVisualTracerCheckBox();
		assertNotNull("Checkbox must be present", checkBox);

		// 1. Without JAR: checkbox disabled, explanatory text
		SimEventTracer.IS_RUNNING_FROM_JAR_OVERRIDE = false;
		panel.updateVisualTracerState();
		assertFalse("Without JAR, checkbox must be disabled", checkBox.isEnabled());
		assertFalse("Without JAR, checkbox must not be selected", checkBox.isSelected());
		assertTrue("Text should indicate JAR requirement",
				checkBox.getText().contains("requires packaged JMT JAR"));

		// 2. With JAR: checkbox enabled, logs to traces/ folder text
		SimEventTracer.IS_RUNNING_FROM_JAR_OVERRIDE = true;
		panel.updateVisualTracerState();
		assertTrue("With JAR, checkbox must be enabled", checkBox.isEnabled());
		assertTrue("Text should indicate logging to traces/ folder",
				checkBox.getText().contains("logs to traces/ folder"));
	}

	@Test
	public void testJarRequirementValidation() {
		SimEventTracer.IS_RUNNING_FROM_JAR_OVERRIDE = false;
		assertFalse("Should not be running from JAR when override is false", SimEventTracer.isRunningFromJar());
		assertFalse("enableVisualDebugTracer(true) must fail when not running from JAR",
				SimEventTracer.enableVisualDebugTracer(true));
		assertFalse("Tracer must not be enabled", SimEventTracer.isEnabled());

		SimEventTracer.IS_RUNNING_FROM_JAR_OVERRIDE = true;
		assertTrue("Should be running from JAR when override is true", SimEventTracer.isRunningFromJar());
		assertTrue("enableVisualDebugTracer(true) must succeed when running from JAR",
				SimEventTracer.enableVisualDebugTracer(true));
		assertTrue("Tracer must be enabled", SimEventTracer.isEnabled());
	}
	@Test
	public void testDebugSolverPanelWithVmOptionInIde() {
		String oldVisual = System.getProperty("jmt.trace.visual");
		try {
			SimEventTracer.disable();
			SimEventTracer.IS_RUNNING_FROM_JAR_OVERRIDE = false;
			System.setProperty("jmt.trace.visual", "true");

			DebugSolverPanel panel = new DebugSolverPanel(null);
			JCheckBox checkBox = panel.getVisualTracerCheckBox();
			panel.updateVisualTracerState();

			assertTrue("With VM option in IDE, checkbox must be enabled", checkBox.isEnabled());
			assertTrue("With VM option in IDE, checkbox must be selected", checkBox.isSelected());
			assertTrue("Text should indicate active via VM option",
					checkBox.getText().contains("active via VM option"));
		} finally {
			SimEventTracer.disable();
			SimEventTracer.IS_RUNNING_FROM_JAR_OVERRIDE = null;
			if (oldVisual != null) System.setProperty("jmt.trace.visual", oldVisual);
			else System.clearProperty("jmt.trace.visual");
		}
	}
}