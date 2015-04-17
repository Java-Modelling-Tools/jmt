package jmt.gui;

import jmt.gui.common.startScreen.GraphStartScreen;
import jmt.gui.exact.ExactWizard;
import jmt.util.TextButtonMatcher;

import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.finder.WindowFinder;
import org.fest.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JmvaGuiTest {

	private FrameFixture window;

	@Before
	public void setUp() {
		window = new FrameFixture(
				GuiActionRunner.execute(new GuiQuery<GraphStartScreen>() {
					protected GraphStartScreen executeInEDT() {
						return new GraphStartScreen();
					}
				}));
		window.show();
	}

	@Test
	public void mainJMVAWindowDisplaysCorrectly() {
		window.button(new TextButtonMatcher("JMVA")).click();
		FrameFixture jmva = WindowFinder.findFrame(ExactWizard.class).using(window.robot);
		jmva.show();
	}
	
	@After
	public void tearDown() {
		window.cleanUp();
	}
}
