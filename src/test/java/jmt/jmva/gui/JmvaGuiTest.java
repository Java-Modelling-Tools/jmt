package jmt.jmva.gui;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import jmt.framework.gui.components.JMTDialog;
import jmt.gui.common.startScreen.GraphStartScreen;
import jmt.jmva.gui.exact.ExactWizard;
import jmt.manual.PDFViewerBuffer;
import jmt.util.PartialTitleFrameMatcher;
import jmt.util.ShortDescriptionButtonMatcher;
import jmt.util.TextButtonMatcher;
import jmt.util.TextLabelMatcher;
import jmt.util.TextMenuItemMatcher;

import org.fest.swing.core.KeyPressInfo;
import org.fest.swing.data.TableCell;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.finder.JFileChooserFinder;
import org.fest.swing.finder.JOptionPaneFinder;
import org.fest.swing.finder.WindowFinder;
import org.fest.swing.fixture.DialogFixture;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JFileChooserFixture;
import org.fest.swing.fixture.JOptionPaneFixture;
import org.fest.swing.fixture.JTableFixture;
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
		window.button(new ShortDescriptionButtonMatcher(GraphStartScreen.JMVA_SHORT_DESCRIPTION)).click();
		jmva = WindowFinder.findFrame(ExactWizard.class).using(window.robot);
	}

	/**
	 * Checks that, on the main JMT window, the user can press the "JMVA"
	 * button, and that this will open a new window of type @ExactWizard
	 * (which is the JMVA frame).
	 */
	@Test
	public void JMVAmainWindowDisplaysCorrectly() {
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
	
	/**
	 * Checks that opening a pre-existing file which already contains solutions
	 * shows a window with these solutions, and that it is possible to close
	 * this window, press "Solve" and have the window show up again.
	 * 
	 * @throws URISyntaxException
	 */
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
	
	/**
	 * Checks that opening a pre-existing file which already contains solutions
	 * shows a window with these solutions, and that it is possible to close
	 * this window, press "Solve" and have the window show up again.
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void JMVAopenWhatIfExampleAndResolve() throws URISyntaxException {
		jmva.menuItem(new TextMenuItemMatcher("Open...")).click();
		JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(jmva.robot);
		fileChooser.selectFile(new File(this.getClass().getResource("2cl_3stat_whatif_servtime.jmva").toURI()));
		fileChooser.approve();
		FrameFixture solutions = WindowFinder.findFrame(new PartialTitleFrameMatcher("JMVA Solutions")).using(jmva.robot);
		solutions.close();
		jmva.menuItem(new TextMenuItemMatcher("Solve")).click();
		FrameFixture solutionsAgain = WindowFinder.findFrame(new PartialTitleFrameMatcher("JMVA Solutions")).using(jmva.robot);
		solutionsAgain.close();		
	}
	/**
	 * Loads a previously saved file with a model but no solutions,
	 * and solves it.
	 * 
	 * @throws URISyntaxException
	 */
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
	
	/**
	 * Loads a previously saved file with a model but no solutions,
	 * randomises the values, and solves it. 
	 * 
	 * @throws URISyntaxException
	 */
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
	
	/**
	 * Opens a previously saved file and changes some parameters
	 * before solving it.
	 * 
	 * @throws URISyntaxException
	 * @throws InterruptedException
	 */
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
		jmva.button(new TextButtonMatcher("Service Times and Visits")).click();		
		jmva.label(new TextLabelMatcher("Stations")).click();
		jmva.button(new TextButtonMatcher("New Station")).click();
		JTableFixture stationsTable = jmva.table("StationTable");
		stationsTable.cell(TableCell.row(0).column(2)).click();
		jmva.label(new TextLabelMatcher("Classes")).click();
		jmva.button(new TextButtonMatcher("New Class")).click();
		JTableFixture classesTable = jmva.table("ClassTable");
		classesTable.enterValue(TableCell.row(1).column(2), "1");
		jmva.menuItem(new TextMenuItemMatcher("Solve")).click();
		FrameFixture solutions = WindowFinder.findFrame(new PartialTitleFrameMatcher("JMVA Solutions")).using(jmva.robot);
		solutions.close();
		classesTable.cell(TableCell.row(0).column(4)).click();
	}
	
	/**
	 * Creates 3 additional classes (on top the existing 1) and deletes three
	 * of them by highlighting them and pressing the 'delete' button.
	 * Verifies the classes were deleted.
	 * Then does a similar thing with stations
	 */
	@Test
	public void JMVAdeletingManyStationsAndClasses() {
		jmva.label(new TextLabelMatcher("Classes")).click();
		jmva.button(new TextButtonMatcher("New Class")).click();
		jmva.button(new TextButtonMatcher("New Class")).click();
		jmva.button(new TextButtonMatcher("New Class")).click();
		JTableFixture classesTable = jmva.table("ClassTable");
		classesTable.selectRows(1, 2, 3);
		classesTable.pressAndReleaseKey(KeyPressInfo.keyCode(127));
		assertEquals(1, classesTable.component().getRowCount());
		jmva.label(new TextLabelMatcher("Stations")).click();
		jmva.button(new TextButtonMatcher("New Station")).click();
		jmva.button(new TextButtonMatcher("New Station")).click();
		jmva.button(new TextButtonMatcher("New Station")).click();
		JTableFixture stationsTable = jmva.table("StationTable");
		stationsTable.selectRows(0, 1, 2);
		stationsTable.pressAndReleaseKey(KeyPressInfo.keyCode(127));
		assertEquals(1, stationsTable.component().getRowCount());
	}
	
	/**
	 * Checks that a dialog confirming that you want to exit without saving
	 * shows up correctly.
	 * 
	 * @throws URISyntaxException
	 */
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
