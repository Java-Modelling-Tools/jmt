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

package jmt.gui.jsimgraph.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.xml.sax.SAXException;

import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.xml.TemplateFileOperation;
import jmt.gui.jsimgraph.template.AddingPanelData;
import jmt.gui.jsimgraph.template.TemplateConstants;
import jmt.gui.jsimgraph.template.TemplateData;
import jmt.gui.common.RootNode;
import jmt.gui.common.TreeTableNode;

/**
 * @author S Jiang
 * 
 */
public class TemplateAddingPanel extends WizardPanel implements TemplateConstants {

	private static final long serialVersionUID = 1L;

	private static final String CONNECTION_ERROR = "Connection error";
	private static final String NO_DESC_PAGE = "NoDescriptionPage.html";
	private static final String ERROR_PAGE = "ErrorPage.html";
	private static final String BLANK_PAGE = "BlankPage.html";
	private static final String MAIN_PAGE = "MainPage.html";

	private TemplatePanel templatePanel;
	private AddingPanelData data;
	private JLabel status;
	private JComboBox<String> siteList;
	private AvailableTreeTable availableTreeTable;

	private JButton siteUpdate;
	private JButton siteAdd;
	private JButton tempDownload;
	private JButton close;
	private String indexFile;

	private JPanel parent;
	private JScrollPane tablePane;
	private JScrollPane desPane;
	private JEditorPane pagePane;
	private File[] templates;

	private boolean index_init_error = false;

	public TemplateAddingPanel(TemplatePanel templatePanel) {
		data = new AddingPanelData();
		this.templatePanel = templatePanel;
		parent = this;
		initComponents();
	}

	public void initComponents() {
		// check the essential files
		pagePane = new JEditorPane();
		pagePane.setEditable(false);

		try {
			TemplateFileOperation.parseSiteXML(data);
			File index = File.createTempFile("F_NAME_INDEX", "");
			indexFile = index.getAbsolutePath();
			TemplateFileOperation.download(DEFAULT_SITE + "/" + F_NAME_INDEX, index.getAbsolutePath(), CONN_TIMEOUT, READ_TIMEOUT);
			TemplateFileOperation.parseIndexXML(indexFile, data);
			pagePane.setPage(TemplateAddingPanel.class.getResource(MAIN_PAGE));
		} catch (Exception e) {
			try {
				index_init_error = true;
				pagePane.setPage(TemplateAddingPanel.class.getResource(ERROR_PAGE));
			} catch (IOException ex) {
				Logger.getLogger(TemplateAddingPanel.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		this.setLayout(new BorderLayout());
		availableTreeTable = new AvailableTreeTable();
		availableTreeTable.expandRow(0);
		templates = TemplateFileOperation.findTemplateFiles(TEMPLATE_FOLDER);
		status = new JLabel("");

		JLabel siteTag = new JLabel("Site:");
		siteList = new JComboBox<String>(new SiteComboBoxModel());
		siteList.setEditable(true);
		siteList.setPreferredSize(new Dimension(580, 24));
		siteTag.setLabelFor(siteList);

		siteUpdate = new JButton("Update");
		siteUpdate.setPreferredSize(new Dimension(70, 24));
		siteAdd = new JButton("Add Site");
		siteAdd.setPreferredSize(new Dimension(100, 24));

		JPanel upperPane = new JPanel();
		upperPane.add(siteTag);
		upperPane.add(siteList);
		upperPane.add(siteUpdate);
		upperPane.add(siteAdd);

		// display the current site
		int index = data.getSiteURLs().indexOf(data.getSiteURL());
		if (index != -1 && !index_init_error) {
			siteList.getEditor().setItem(data.getSiteInfos().get(index));
		} else if (!index_init_error) {
			siteList.getEditor().setItem(data.getSiteInfo());
		} else {
			siteList.getEditor().setItem(DEFAULT_SITE);
		}

		tempDownload = new JButton("Download");
		tempDownload.setPreferredSize(new Dimension(100, 24));
		tempDownload.setBackground(Color.RED);
		tempDownload.setEnabled(false);
		close = new JButton("Close");
		close.setPreferredSize(new Dimension(100, 24));

		// update the index.xml when user inputing a new site two situations:
		// 1. the site is stored in the site list
		// 2. the site is not in the site list, it's typed by user in the bar
		siteUpdate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doUpdate();
			}
		});

		siteList.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent evt) {
				if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
					doUpdate();
				}
			}
		});

		// add a new site to the site list
		siteAdd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				status.setText("creating a new site...");
				status.repaint();

				AddingSitePanel newSite = new AddingSitePanel();
				String siteName = "";
				String siteAddress = "";

				int result = JOptionPane.showConfirmDialog(parent, newSite,newSite.getPanelName(),
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (result == JOptionPane.OK_OPTION) {
					siteName = newSite.getName();
					siteAddress = newSite.getAddress();
					if (!"http://".equals(siteAddress)) {
						String siteIndexUrl = siteAddress + "/" + F_NAME_INDEX;
						// verify the site
						new Checker(siteIndexUrl, status, siteName, siteAddress).execute();
					} else {
						JOptionPane.showMessageDialog(parent, "Error: please check the URL");
						status.setText("");
						status.repaint();
					}
				} else {
					status.setText("");
					status.repaint();
				}
			}
		});

		// download the selected template
		tempDownload.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int rowIndex = availableTreeTable.getSelectedRow();
				if (rowIndex != -1) {
					TreePath path = availableTreeTable.getPathForRow(rowIndex);
					TreeTableNode node = (TreeTableNode) path.getLastPathComponent();
					if ("template".equals(node.getType())) {
						String tempURL = ((TemplateData) node.getData()[0]).getUpdateAddress();
						String tempFileName = ((TemplateData) node.getData()[0]).getFileName();

						boolean flag = false;
						for (File template : templates) {
							if (template.getName().equals(tempFileName)) {
								flag = true;
							}
						}

						int overwrite = 0;
						if (flag) {
							overwrite = JOptionPane.showConfirmDialog(parent, "Replace the existing file?", "Replace",
									JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
						}

						if (overwrite == 0) {
							new Downloader(tempURL, TEMPLATE_FOLDER + tempFileName, status, D_TYPE_TEMPLATE).execute();
						}
					}
				}
			}
		});

		close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SwingUtilities.getWindowAncestor(parent).setVisible(false);
			}
		});

		JPanel buttons = new JPanel();
		buttons.add(tempDownload);
		buttons.add(close);
		buttons.setLayout(new FlowLayout(FlowLayout.TRAILING));

		JPanel bottomPane = new JPanel();
		bottomPane.setLayout(new GridLayout(1, 2));
		bottomPane.add(status);
		bottomPane.add(buttons);

		tablePane = new JScrollPane();
		tablePane.getViewport().add(availableTreeTable);
		desPane = new JScrollPane(pagePane);

		JPanel cenPane = new JPanel();
		cenPane.setLayout(new GridLayout(2, 1));
		cenPane.add(tablePane);
		cenPane.add(desPane);

		// if user select a template, the description page of that template will be displayed
		availableTreeTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int rowIndex = availableTreeTable.getSelectedRow();
				if (rowIndex != -1) {
					TreePath path = availableTreeTable.getPathForRow(rowIndex);
					TreeTableNode node = (TreeTableNode) path.getLastPathComponent();
					if ("template".equals(node.getType())) {
						String DescriptionURL = ((TemplateData) node.getData()[0]).getDescriptionAddress();
						if (DescriptionURL != null) {
							new PageLoader(DescriptionURL).execute();
						} else {
							new PageLoader(TemplateAddingPanel.class.getResource(NO_DESC_PAGE).toString()).execute();
						}
					}
				}
			}
		});

		this.add(upperPane, BorderLayout.NORTH);
		this.add(cenPane, BorderLayout.CENTER);
		this.add(bottomPane, BorderLayout.SOUTH);
	}

	@Override
	public String getName() {
		return "Add templates";
	}

	private void doUpdate() {
		String indexURL = null;
		// prepare the index URL
		if (siteList.getSelectedIndex() != -1) {
			// in the list
			indexURL = data.getSiteURLs().get(siteList.getSelectedIndex()) + "/" + F_NAME_INDEX;
		} else {
			// not in the list
			String raw = (String) siteList.getEditor().getItem();
			String find = "http.*";
			Pattern pattern = Pattern.compile(find);
			Matcher matcher = pattern.matcher(raw);
			if (matcher.find()) {
				indexURL = matcher.group() + "/" + F_NAME_INDEX;
			}
		}

		File index;
		try {
			index = File.createTempFile("index", ".xml");
			// download the index.xml
			indexFile = index.getAbsolutePath();
			new Downloader(indexURL, index.getAbsolutePath(), status, D_TYPE_INDEX).execute();
		} catch (IOException ex) {
			Logger.getLogger(TemplateAddingPanel.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public class AvailableTreeTable extends JXTreeTable {

		private static final long serialVersionUID = 1L;

		public AvailableTreeTable() {
			super();
			setTreeTableModel(new AvailableTreeTableModel(data.getRoot()));
			sizeColumns();
			setRowHeight(CELL_HEIGHT);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);

			addTreeSelectionListener(new TreeSelectionListener() {
				@Override
				public void valueChanged(TreeSelectionEvent e) {
					TreePath path = e.getPath();
					TreeTableNode node = (TreeTableNode) path.getLastPathComponent();
					if ("template".equals(node.getType())) {
						tempDownload.setEnabled(true);
					} else {
						tempDownload.setEnabled(false);
					}
				}
			});
		}

		private void sizeColumns() {
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setPreferredWidth(((AvailableTreeTableModel) getTreeTableModel()).columnSizes[i]);
			}
		}

		@Override
		public String getToolTipText(MouseEvent e) {
			String tip = null;
			Point p = e.getPoint();
			int rowIndex = rowAtPoint(p);
			if (rowIndex != -1) {
				TreePath path = availableTreeTable.getPathForRow(rowIndex);
				TreeTableNode node = (TreeTableNode) path.getLastPathComponent();
				if ("template".equals(node.getType())) {
					tip = ((TemplateData) node.getData()[0]).getToolTip();
				}
			}
			return tip;
		}

	}

	public class AvailableTreeTableModel extends AbstractTreeTableModel {

		private String[] columnNames = new String[] { "Name", "Author", "Version", "Upload Date", "Description", "Downloaded" };
		public int[] columnSizes = new int[] { 240, 100, 60, 100, 400, 200 };

		AvailableTreeTableModel(Object root) {
			super(root);
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public String getColumnName(int colIndex) {
			return columnNames[colIndex];
		}

		public void resetRoot(Object root) {
			this.root = root;
			modelSupport.fireNewRoot();
		}

		@Override
		public Object getValueAt(Object parent, int index) {
			if (!(parent instanceof RootNode)) {
				if (("template".equals(((TreeTableNode) parent).getType()))) {
					switch (index) {
					case 0:
						return ((TemplateData) ((TreeTableNode) parent).getData()[0]).getName();
					case 1:
						return ((TemplateData) ((TreeTableNode) parent).getData()[0]).getAuthor();
					case 2:
						return ((TemplateData) ((TreeTableNode) parent).getData()[0]).getVersion();
					case 3:
						return ((TemplateData) ((TreeTableNode) parent).getData()[0]).getDate();
					case 4:
						return ((TemplateData) ((TreeTableNode) parent).getData()[0]).getShortDescription();
					case 5:
						Path temPath = Paths.get(TEMPLATE_FOLDER + ((TemplateData) ((TreeTableNode) parent).getData()[0]).getFileName());
						try {
							BasicFileAttributes view = Files.getFileAttributeView(temPath, BasicFileAttributeView.class).readAttributes();
							FileTime modifiedTime = view.lastModifiedTime();
							DateFormat df = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
							String cTime = df.format(modifiedTime.toMillis());
							return cTime;
						} catch (Exception e) {
							return "--";
						}
					default:
						return null;
					}
				} else {
					if (index < 1)
						return ((TreeTableNode) parent).getData()[index];
					else
						return null;
				}
			}
			return null;
		}

		@Override
		public Object getChild(Object parent, int index) {
			return ((TreeTableNode) parent).getChildAt(index);
		}

		@Override
		public int getChildCount(Object parent) {
			return ((TreeTableNode) parent).getChildCount();
		}

		@Override
		public int getIndexOfChild(Object parent, Object child) {
			return ((TreeTableNode) parent).getIndex((TreeTableNode) child);
		}

	}

	// site list combobox model
	public class SiteComboBoxModel extends AbstractListModel<String> implements ComboBoxModel<String> {

		private static final long serialVersionUID = 1L;

		Object selection;

		@Override
		public String getElementAt(int index) {
			return data.getSiteInfos().get(index);
		}

		@Override
		public int getSize() {
			return data.getSiteInfos().size();
		}

		@Override
		public Object getSelectedItem() {
			return selection;
		}

		@Override
		public void setSelectedItem(Object selection) {
			this.selection = selection;
		}

	}

	// verify the new web site
	public class Checker extends SwingWorker<Boolean, String> {

		String siteIndexURL;
		JLabel status;
		String siteName;
		String siteAddress;

		public Checker(String siteIndexURL, JLabel status, String siteName, String siteAddress) {
			this.siteIndexURL = siteIndexURL;
			this.status = status;
			this.siteName = siteName;
			this.siteAddress = siteAddress;
		}

		@Override
		protected Boolean doInBackground() throws Exception {
			publish("Validating the new site (time out: " + READ_TIMEOUT / 1000 + " s)");
			try {
				URL url = new URL(siteIndexURL);
				// if the InputStream can be opened, then it's verified
				URLConnection conn = url.openConnection();
				conn.setConnectTimeout(CONN_TIMEOUT);
				conn.setReadTimeout(READ_TIMEOUT);
				conn.connect();

				// write the site into sites.xml and re-parse it
				TemplateFileOperation.writeSiteXML(siteName, siteAddress);
				TemplateFileOperation.parseSiteXML(data);

				publish("has been added to the site list successfully");
				Thread.sleep(1000);
				publish("");
				return true;
			} catch (MalformedURLException e) {
				// if the URL format is invalid, shows the err msg
				publish("the URL is invalid");
				JOptionPane.showMessageDialog(parent, "Invalid URL");
				Thread.sleep(1000);
				publish("");
			} catch (IOException e) {
				// if it's unable to open the input stream, shows the err msg
				publish(CONNECTION_ERROR);
				JOptionPane.showMessageDialog(parent, CONNECTION_ERROR);
				Thread.sleep(1000);
				publish("");
			} catch (TransformerFactoryConfigurationError e) {
				e.printStackTrace();
			} finally {
				;
			}
			return false;
		}

		@Override
		protected void process(final List<String> chunks) {
			for (final String string : chunks) {
				status.setText(string);
				status.repaint();
				if (string.equals("Validating the new site (time out: " + READ_TIMEOUT / 1000 + " s)")) {
					// when validating the site, disable all the buttons and change the cursor
					disableAll();
				} else if (string.equals("has been added to the site list successfully")) {
					// refresh the site bar
					siteList.setModel(new SiteComboBoxModel());
				} else if (string.equals("")) {
					// after verification, enable all buttons
					enableAll();
				}
			}
		}

	}

	// download index.xml or templates
	public class Downloader extends SwingWorker<File, String> {

		private String type;
		private String myURL;
		private String fileName;
		private JLabel status;

		public Downloader(String myURL, String fileName, JLabel status, String type) {
			this.type = type;
			this.myURL = myURL;
			this.fileName = fileName;
			this.status = status;
		}

		@Override
		protected File doInBackground() throws Exception {
			File temp = null;
			// if downloading index.xml
			if (type.equals(D_TYPE_INDEX)) {
				try {
					// approximately 60s (?)
					publish("Connecting to the site (time out: " + READ_TIMEOUT / 1000 + " s)");
					temp = TemplateFileOperation.download(myURL, fileName, CONN_TIMEOUT, READ_TIMEOUT);
					publish("index.xml is downloaded successfully");
					// is it necessary to sleep for 100ms for showing the msg (?)
					data.clearSitesData();
					Thread.sleep(100);
					publish("Updating index");
					Thread.sleep(100);
					// I want to tell user that the index is up to date,
					// but logically it's not guaranteed within 100ms
					// I tried to put it into process function, but it
					// doesn't work
					// any suggestions(?)
					publish("index is up to date");
					Thread.sleep(1000);
					publish("");
				} catch (IOException e) {
					publish(CONNECTION_ERROR);
					JOptionPane.showMessageDialog(parent, CONNECTION_ERROR);
					publish("");
				}
			} else if (type.equals(D_TYPE_TEMPLATE)) {
				// similar to the 'index', except for the msg
				try {
					publish("Connecting to the site (time out: " + READ_TIMEOUT / 1000 + " s)");
					temp = TemplateFileOperation.download(myURL, fileName, CONN_TIMEOUT, READ_TIMEOUT);
					templates = TemplateFileOperation.findTemplateFiles(TEMPLATE_FOLDER);
					publish("template is downloaded successfully");
					Thread.sleep(100);
					publish("Updating template list");
					Thread.sleep(100);
					publish("template list is up to date");
					Thread.sleep(100);
					publish("");
				} catch (IOException e) {
					publish(CONNECTION_ERROR);
					JOptionPane.showMessageDialog(parent, CONNECTION_ERROR);
					publish("");
				}
			}
			return temp;
		}

		@Override
		protected void process(final List<String> chunks) {
			for (final String string : chunks) {
				// repaint the status first
				status.setText(string);
				status.repaint();
				if (string.equals("Connecting to the site (time out: " + READ_TIMEOUT / 1000 + " s)")) {
					disableAll();
				} else if (string.equals("Updating index")) {
					// after downloading the index.xml successfully, it's parsed
					try {
						data = new AddingPanelData();
						TemplateFileOperation.parseIndexXML(indexFile, data);
						TemplateFileOperation.parseSiteXML(data);
					} catch (SAXException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					// refresh the table
					((AvailableTreeTableModel) availableTreeTable.getTreeTableModel()).resetRoot(data.getRoot());
					availableTreeTable.expandPath(availableTreeTable.getPathForRow(0));
					siteList.setModel(new SiteComboBoxModel());
					siteList.getEditor().setItem(data.getSiteInfo());
					try {
						pagePane.setPage(TemplateAddingPanel.class.getResource(BLANK_PAGE));
					} catch (IOException ex) {
						Logger.getLogger(TemplateAddingPanel.class.getName()).log(Level.SEVERE, null, ex);
					}
				} else if (string.equals("template is downloaded successfully")) {
					availableTreeTable.revalidate();
					availableTreeTable.repaint();
				} else if (string.equals("Updating template list")) {
					// update the template panel (local templates)
					templatePanel.updateTemplates();
				} else if (string.equals(CONNECTION_ERROR)) {
					enableAll();
				} else if (string.equals("")) {
					enableAll();
				}
			}
		}
	}

	// loading the HTML page
	public class PageLoader extends SwingWorker<Boolean, String> {

		String URLString;

		public PageLoader(String URLString) {
			this.URLString = URLString;
		}

		@Override
		protected Boolean doInBackground() throws Exception {
			try {
				publish("Loading the description page...");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		protected void process(final List<String> chunks) {
			for (final String string : chunks) {
				if (string.equals("Loading the description page...")) {
					try {
						try {
							pagePane.setPage(new URL(URLString));
						} catch (Exception e) {
							pagePane.setPage(TemplateAddingPanel.class.getResource(NO_DESC_PAGE));
						}
						// repaint the page
						desPane.getViewport().revalidate();
						desPane.getViewport().repaint();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void disableAll() {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		siteAdd.setEnabled(false);
		siteUpdate.setEnabled(false);
		tempDownload.setEnabled(false);
		close.setEnabled(false);
	}

	public void enableAll() {
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		siteAdd.setEnabled(true);
		siteUpdate.setEnabled(true);
		tempDownload.setEnabled(true);
		close.setEnabled(true);
	}

}
