package jmt.gui;

import jmt.gui.common.startScreen.GraphStartScreen;

import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MainTest {

	private FrameFixture window;

	@Before
	public void setUp() {
		window = new FrameFixture(
				GuiActionRunner.execute(new GuiQuery<GraphStartScreen>() {
					protected GraphStartScreen executeInEDT() {
						return new GraphStartScreen();
					}
				}));
	}

	@Test
	public void mainWindowDisplaysCorrectly() {
		window.show();
	}

	@After
	public void tearDown() {
		window.cleanUp();
	}
}
