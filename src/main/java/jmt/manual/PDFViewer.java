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

package jmt.manual;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.rendering.PDFRenderer;

import jmt.framework.gui.components.JMTFrame;
import jmt.gui.common.Defaults;

/**
 * <p>Title: PDF Viewer</p>
 * <p>Description: This class opens a view to a specified chapter of the JMT
 * manual with navigation.</p>
 *
 * @author Lulai Zhu
 * Date: 16-08-2018
 * Time: 18.00.00
 */
public class PDFViewer extends JMTFrame {

	private static final long serialVersionUID = 1L;

	private static final String MANUAL_PATH = "manuals" + File.separator + "manual.pdf";
	private static final float IMAGE_SCALE = Toolkit.getDefaultToolkit().getScreenResolution() / 64.0f;

	private PDDocument document;
	private PDDocumentCatalog catalog;
	private PDOutlineItem chapter;

	private int startPageIndex;
	private int endPageIndex;

	private JScrollBar pagePaneBar;

	public PDFViewer(String title, ChapterIdentifier identifier) throws Exception {
		try {
			setTitle(title);
			openManual();
			findChapter(identifier);
			initComponents();
			closeManual();
		} catch (Exception ex) {
			closeManual();
			throw ex;
		}
	}

	private void openManual() throws IOException {
		File file = new File(MANUAL_PATH);
		if (!file.isFile()) {
			file = new File(Defaults.getWorkingPath(), MANUAL_PATH);
		}
		if (!file.isFile()) {
			throw new IOException("Cannot find JMT manual.pdf file. Please put it in "
					+ file.getParentFile().getAbsolutePath() + ".");
		}
		try {
			Class.forName("sun.java2d.cmm.kcms.KcmsServiceProvider");
			System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
		} catch (ClassNotFoundException ex) {
			// Do nothing
		}
		document = PDDocument.load(file);
		catalog = document.getDocumentCatalog();
	}

	private void findChapter(ChapterIdentifier identifier) throws IOException {
		PDDocumentOutline outline = catalog.getDocumentOutline();
		Iterable<PDOutlineItem> children = outline.children();
		Iterator<PDOutlineItem> it = children.iterator();
		while (it.hasNext()) {
			PDOutlineItem item = it.next();
			if (item.getTitle().startsWith(identifier.getPrefix())) {
				chapter = item;
				startPageIndex = retrievePageIndex(item);
				if (it.hasNext()) {
					item = it.next();
					endPageIndex = retrievePageIndex(item);
				} else {
					endPageIndex = document.getNumberOfPages();
				}
				return;
			}
		}
		throw new IOException("No chapter in JMT manual.pdf file is prefixed with "
				+ identifier.getPrefix() + ".");
	}

	private void initComponents() throws IOException {
		JTree bookmarkTree = new JTree();
		bookmarkTree.setModel(createBookmarkTreeModel());
		bookmarkTree.setSelectionModel(new BookmarkTreeSelectionModel());
		bookmarkTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		bookmarkTree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				TreePath path = e.getNewLeadSelectionPath();
				if (path == null) {
					return;
				}
				BookmarkTreeNode node = (BookmarkTreeNode) path.getLastPathComponent();
				int pageOffset = node.getPageIndex() - startPageIndex;
				int pageNumber = endPageIndex - startPageIndex;
				int max = pagePaneBar.getMaximum();
				int min = pagePaneBar.getMinimum();
				int value = (int) (((double) pageOffset / pageNumber) * (max - min + 8));
				pagePaneBar.setValue(value);
			}
		});

		JScrollPane bookmarkPane = new JScrollPane();
		bookmarkPane.setViewportView(bookmarkTree);

		int pageNumber = endPageIndex - startPageIndex;
		PDFRenderer renderer = new PDFRenderer(document);
		JPanel pagePanel = new JPanel();
		pagePanel.setLayout(new GridLayout(pageNumber, 1, 0, 8));
		for (int i = 0; i < pageNumber; i++) {
			int pageIndex = i + startPageIndex;
			BufferedImage image = renderer.renderImage(pageIndex, IMAGE_SCALE);
			JLabel label = new JLabel();
			label.setIcon(new ImageIcon(image));
			label.setHorizontalAlignment(JLabel.CENTER);
			pagePanel.add(label);
		}

		JScrollPane pagePane = new JScrollPane();
		pagePane.setViewportView(pagePanel);
		pagePaneBar = pagePane.getVerticalScrollBar();
		pagePaneBar.setUnitIncrement(16);

		JSplitPane mainPanel = new JSplitPane();
		mainPanel.setDividerSize(4);
		mainPanel.setResizeWeight(0.25);
		mainPanel.setLeftComponent(bookmarkPane);
		mainPanel.setRightComponent(pagePane);
		add(mainPanel);

		centerWindow(Defaults.getAsInteger("JMTManualWidth").intValue(),
				Defaults.getAsInteger("JMTManualHeight").intValue());
		setVisible(true);
	}

	private int retrievePageIndex(PDOutlineItem item) throws IOException {
		PDActionGoTo action = (PDActionGoTo) item.getAction();
		PDNamedDestination namedDest = (PDNamedDestination) action.getDestination();
		PDPageDestination pageDest = catalog.findNamedDestinationPage(namedDest);
		return pageDest.retrievePageNumber();
	}

	private TreeModel createBookmarkTreeModel() throws IOException {
		BookmarkTreeNode root = new BookmarkTreeNode();
		root.setUserObject(chapter.getTitle());
		root.setPageIndex(retrievePageIndex(chapter));
		addDescendentBookmarkTreeNodes(chapter, root);
		return new DefaultTreeModel(root);
	}

	private void addDescendentBookmarkTreeNodes(PDOutlineItem parentItem, BookmarkTreeNode parentNode) throws IOException {
		Iterable<PDOutlineItem> children = ((PDOutlineNode) parentItem).children();
		Iterator<PDOutlineItem> it = children.iterator();
		while (it.hasNext()) {
			PDOutlineItem item = it.next();
			BookmarkTreeNode node = new BookmarkTreeNode();
			node.setUserObject(item.getTitle());
			node.setPageIndex(retrievePageIndex(item));
			addDescendentBookmarkTreeNodes(item, node);
			parentNode.add(node);
		}
	}

	private void closeManual() throws IOException {
		if (document != null) {
			document.close();
		}
	}

	@Override
	protected void doClose() {
		Dimension d = getSize();
		Defaults.set("JMTManualWidth", String.valueOf(d.width));
		Defaults.set("JMTManualHeight", String.valueOf(d.height));
		Defaults.save();
	}

	private class BookmarkTreeNode extends DefaultMutableTreeNode {

		private static final long serialVersionUID = 1L;

		private int pageIndex;

		public int getPageIndex() {
			return pageIndex;
		}

		public void setPageIndex(int value) {
			pageIndex = value;
		}

	}

	private class BookmarkTreeSelectionModel extends DefaultTreeSelectionModel {

		private static final long serialVersionUID = 1L;

		@Override
		public void setSelectionPaths(TreePath[] pPaths) {
			super.setSelectionPaths(null);
			super.setSelectionPaths(pPaths);
		}

	}

}
