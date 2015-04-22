package jmt.jmva;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import jmt.framework.gui.components.JMTDialog;
import jmt.gui.common.startScreen.GraphStartScreen;
import jmt.jmva.gui.exact.ExactWizard;
import jmt.manual.PDFViewerBuffer;
import jmt.util.PartialTitleFrameMatcher;
import jmt.util.TextButtonMatcher;
import jmt.util.TextLabelMatcher;
import jmt.util.TextMenuItemMatcher;

import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.finder.JFileChooserFinder;
import org.fest.swing.finder.JOptionPaneFinder;
import org.fest.swing.finder.WindowFinder;
import org.fest.swing.fixture.DialogFixture;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JFileChooserFixture;
import org.fest.swing.fixture.JOptionPaneFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author Piotr Tokaj
 *
 */
public class JmvaGuiTest {

	private FrameFixture jmva;
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
		window.button(new TextButtonMatcher("JMVA")).click();
		jmva = WindowFinder.findFrame(ExactWizard.class).using(window.robot);
	}

	/**
	 * Checks that, on the main JMT window, the user can press the "JMVA"
	 * button, and that this will open a new window of type @ExactWizard
	 * (which is the JMVA frame).
	 */
	@Test
	public void mainJMVAWindowDisplaysCorrectly() {
		jmva.show();
	}
	
	@Test
	public void JMVAhelpWindowDisplaysCorrectly() {
		jmva.menuItem(new TextMenuItemMatcher("JMVA help")).click();
		FrameFixture helpWindow = WindowFinder.findFrame(PDFViewerBuffer.class).using(jmva.robot);
		helpWindow.close();
	}
	
	@Test
	public void JMVAaboutWindowDisplaysCorrectly() {
		jmva.menuItem(new TextMenuItemMatcher("About JMVA...")).click();
		DialogFixture about = WindowFinder.findDialog(JMTDialog.class).using(jmva.robot);
		about.close();
	}
	
	@Test
	public void JMVAopenNewModel() {
		jmva.menuItem(new TextMenuItemMatcher("New...")).click();
		FrameFixture newJmva = WindowFinder.findFrame(ExactWizard.class).using(jmva.robot);
		newJmva.close();
	}
	
	@Test
	public void JMVAopenSimpleExampleAndResolve() throws URISyntaxException {
		jmva.menuItem(new TextMenuItemMatcher("Open...")).click();
		JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(jmva.robot);
		fileChooser.selectFile(new File(this.getClass().getResource("1cl_1stat_mm1.jmva").toURI()));
		fileChooser.approve();
		FrameFixture solutions = WindowFinder.findFrame(new PartialTitleFrameMatcher("JMVA Solutions")).using(jmva.robot);
		solutions.close();
		jmva.menuItem(new TextMenuItemMatcher("Solve")).click();
		FrameFixture solutionsAgain = WindowFinder.findFrame(new PartialTitleFrameMatcher("JMVA Solutions")).using(jmva.robot);
		solutionsAgain.close();		
	}
	
	@Test
	public void JMVAsolveSimpleExample() throws URISyntaxException {
		jmva.menuItem(new TextMenuItemMatcher("Open...")).click();
		JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(jmva.robot);
		fileChooser.selectFile(new File(this.getClass().getResource("1cl_1stat_mm1_without_solutions.jmva").toURI()));
		fileChooser.approve();
		jmva.menuItem(new TextMenuItemMatcher("Solve")).click();
		FrameFixture solutions = WindowFinder.findFrame(new PartialTitleFrameMatcher("JMVA Solutions")).using(jmva.robot);
		solutions.close();
	}
	
	@Test
	public void JMVAsolveRandomizedSimpleExample() throws URISyntaxException {
		jmva.menuItem(new TextMenuItemMatcher("Open...")).click();
		JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(jmva.robot);
		fileChooser.selectFile(new File(this.getClass().getResource("1cl_1stat_mm1_without_solutions.jmva").toURI()));
		fileChooser.approve();
		jmva.menuItem(new TextMenuItemMatcher("Randomize")).click();		
		jmva.menuItem(new TextMenuItemMatcher("Solve")).click();
		FrameFixture solutions = WindowFinder.findFrame(new PartialTitleFrameMatcher("JMVA Solutions")).using(jmva.robot);
		solutions.close();
	}
	
	@Test
	public void JMVAverifyCertainButtonsWork() throws URISyntaxException, InterruptedException {
		jmva.menuItem(new TextMenuItemMatcher("Open...")).click();
		JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(jmva.robot);
		fileChooser.selectFile(new File(this.getClass().getResource("1cl_1stat_mm1_without_solutions.jmva").toURI()));
		fileChooser.approve();
		jmva.label(new TextLabelMatcher("Service Times")).click();
		jmva.button(new TextButtonMatcher("Service Demands")).click();
		JOptionPaneFixture confirmation = JOptionPaneFinder.findOptionPane().using(jmva.robot);
		confirmation.button(new TextButtonMatcher("Yes")).click();
		jmva.label(new TextLabelMatcher("Stations")).click();
		jmva.button(new TextButtonMatcher("New Station")).click();
		jmva.menuItem(new TextMenuItemMatcher("Solve")).click();
		FrameFixture solutions = WindowFinder.findFrame(new PartialTitleFrameMatcher("JMVA Solutions")).using(jmva.robot);
		solutions.close();
	}
	
	@Test
	public void JMVAexitWithoutSaving() throws URISyntaxException {
		jmva.menuItem(new TextMenuItemMatcher("Open...")).click();
		JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(jmva.robot);
		fileChooser.selectFile(new File(this.getClass().getResource("1cl_1stat_mm1_without_solutions.jmva").toURI()));
		fileChooser.approve();
		jmva.menuItem(new TextMenuItemMatcher("Randomize")).click();
		jmva.menuItem(new TextMenuItemMatcher("Exit")).click();
		JOptionPaneFixture confirmation = JOptionPaneFinder.findOptionPane().using(jmva.robot);
		confirmation.button(new TextButtonMatcher("No")).click();
	}
	
	@Test
	public void JMVAcancelExit() throws URISyntaxException {
		jmva.menuItem(new TextMenuItemMatcher("Open...")).click();
		JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(jmva.robot);
		fileChooser.selectFile(new File(this.getClass().getResource("1cl_1stat_mm1_without_solutions.jmva").toURI()));
		fileChooser.approve();
		jmva.menuItem(new TextMenuItemMatcher("Randomize")).click();
		jmva.menuItem(new TextMenuItemMatcher("Exit")).click();
		JOptionPaneFixture confirmation = JOptionPaneFinder.findOptionPane().using(jmva.robot);
		confirmation.button(new TextButtonMatcher("Cancel")).click();
		jmva.close();
	}
	
	@Test
	public void JMVAsaveOnExit() throws URISyntaxException, IOException, InterruptedException {
		jmva.menuItem(new TextMenuItemMatcher("Open...")).click();
		JFileChooserFixture openFileChooser = JFileChooserFinder.findFileChooser().using(jmva.robot);
		openFileChooser.selectFile(new File(this.getClass().getResource("1cl_1stat_mm1_without_solutions.jmva").toURI()));
		openFileChooser.approve();
		jmva.menuItem(new TextMenuItemMatcher("Randomize")).click();
		jmva.menuItem(new TextMenuItemMatcher("Exit")).click();
		JOptionPaneFixture confirmation = JOptionPaneFinder.findOptionPane().using(jmva.robot);
		confirmation.button(new TextButtonMatcher("Yes")).click();
		JFileChooserFixture saveFileChooser = JFileChooserFinder.findFileChooser().using(jmva.robot);
		saveFileChooser.selectFile(new File(getTempFile("saveOnExit", ".jmva")));
		saveFileChooser.approve();
	}
	
	@After
	public void tearDown() {
		jmva.cleanUp();
		window.cleanUp();
	}
	
	private static String getTempFile(String name, String suffix) throws IOException {
		File file = File.createTempFile(name, suffix);
		String path = file.getAbsolutePath();
		file.delete();
		return path;
	}
}
