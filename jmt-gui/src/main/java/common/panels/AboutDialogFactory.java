/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package common.panels;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jmt.framework.gui.components.JMTDialog;
import common.CommonConstants;
import common.JMTImageLoader;
import static common.CommonConstants.*;

/**
 * Factory creating the about dialog objects.
 */
/**
 * Factory creating the about dialog objects.
 */
/**
 * Factory creating the about dialog objects.
 */
/**
 * Factory creating the about dialog objects.
 */
public class AboutDialogFactory {

	public enum Company {
		POLIMI, ICL, UNIVAQ;

		public JComponent getLogo() {
			switch (this) {
				case POLIMI:
					JLabel logo = new JLabel("<html><body><b>DEIB - Politecnico di Milano</b></body></html>");
					logo.setHorizontalTextPosition(SwingConstants.TRAILING);
					logo.setVerticalTextPosition(SwingConstants.CENTER);
					logo.setIconTextGap(10);
					logo.setIcon(JMTImageLoader.loadImage("logo", new Dimension((int)(70 * CommonConstants.widthScaling), (int)(70 * CommonConstants.heightScaling))));
					return logo;
				case ICL:
					return new JLabel(JMTImageLoader.loadImage("logo_icl", new Dimension(-1, 40)));
				case UNIVAQ:
					JLabel logoUnivaq = new JLabel("<html><body><b>DISIM - Universita dell'Aquila</b></body></html>");
					logoUnivaq.setHorizontalTextPosition(SwingConstants.TRAILING);
					logoUnivaq.setVerticalTextPosition(SwingConstants.CENTER);
					logoUnivaq.setIconTextGap(10);
					logoUnivaq.setIcon(JMTImageLoader.loadImage("logo_spencer", new Dimension((int)(70 * CommonConstants.widthScaling), (int)(70 * CommonConstants.heightScaling))));
					return logoUnivaq;
				default:
					return null;
			}
		}
	}

	private static final Contributors[] JMVA = {
			new Contributors(Company.POLIMI, "Serazzi (Unit Leader) Giuseppe", "Bertoli Marco", "Cerotti Davide", "Conti Andrea", "Dall'Orso Federico", "Granata Federico", "Gribaudo Marco", "Omini Stefano", "Sheikhvand Kourosh"),
			new Contributors(Company.ICL, "Casale (Unit Leader) Giuliano", "Makaronidis Michalis", "Bradshaw John", "Chugh Abhimanyu", "Su Haixiao", "Tokaj Piotr", "Wai Hong Ong", "Zakon Alexander", "Pahnke Ben"),
			new Contributors(Company.UNIVAQ, "Cortellessa (Unit Leader) Vittorio", "Tucci Michele", "Di Pompeo Daniele", "Carlino Calogero"),
	};

	private static final Contributors[] JSIM = {
			new Contributors(Company.POLIMI, "Serazzi (Unit Leader) Giuseppe", "Bertoli Marco", "Granata Federico", "Omini Stefano", "Radaelli Francesco", "Dall'Orso Federico"),
			new Contributors(Company.ICL, "Casale (Unit Leader) Giuliano", "Salem Ahmed", "Badial Jeevan", "Davies Emyr", "Eleftheriou Anastasia", "Jiang Shuai", "Lopes Vitor", "Zhu Lulai", "Phuangjaisri Phumin", "Murali Srikrishna", "Vaibhav Krishnakumar", "Ng Lok Sen Rosemary", "Kodiak Conrad", "Li Zelun", "Wang Nathan", "Roman Andrei", "Pahnke Ben", "Choy Ashton", "Zhu Jerry"),
			new Contributors(Company.UNIVAQ, "Cortellessa (Unit Leader) Vittorio", "Tucci Michele", "Di Pompeo Daniele", "Carlino Calogero"),
	};

	private static final Contributors[] JMODEL = {
			new Contributors(Company.POLIMI, "Serazzi (Unit Leader) Giuseppe", "Bertoli Marco", "Bortone Emma", "Cazzoli Mattia", "D'Aquino Francesco", "Granata Federico", "Omini Stefano", "Radaelli Francesco", "Das Ashanka", "Spicuglia Sebastiano", "Gribaudo Marco", "Canakoglu Arif", "Tucci Francesco", "Rosmini Simone"),
			new Contributors(Company.ICL, "Casale (Unit Leader) Giuliano", "Salem Ahmed", "Badial Jeevan", "Davies Emyr", "Eleftheriou Anastasia", "Jiang Shuai", "Lopes Vitor", "Zhu Lulai", "Phuangjaisri Phumin", "Murali Srikrishna", "Vaibhav Krishnakumar", "Ng Lok Sen Rosemary", "Kodiak Conrad", "Li Zelun", "Wang Nathan", "Roman Andrei", "Pahnke Ben", "Choy Ashton", "Gao Xinyu", "Zhu Jerry"),
			new Contributors(Company.UNIVAQ, "Cortellessa (Unit Leader) Vittorio", "Tucci Michele", "Di Pompeo Daniele", "Carlino Calogero"),
	};

	private static final Contributors[] JABA = {
			new Contributors(Company.POLIMI, "Serazzi (Unit Leader) Giuseppe", "Bertoli Marco", "Zanzottera Andrea", "Gimondi Carlo", "Spicuglia Sebastiano"),
			new Contributors(Company.UNIVAQ, "Cortellessa (Unit Leader) Vittorio", "Tucci Michele", "Di Pompeo Daniele", "Carlino Calogero"),
	};

	private static final Contributors[] JMCH = {
			new Contributors(Company.POLIMI, "Serazzi (Unit Leader) Giuseppe", "Canakoglu Arif", "Di Mauro Ernesto", "Torri Lorenzo"),
			new Contributors(Company.UNIVAQ, "Cortellessa (Unit Leader) Vittorio", "Tucci Michele", "Di Pompeo Daniele", "Carlino Calogero"),
	};

	private static final Contributors[] JWAT = {
			new Contributors(Company.POLIMI, "Serazzi (Unit Leader) Giuseppe", "Brambilla Davide", "Fumagalli Claudio"),
			new Contributors(Company.UNIVAQ, "Cortellessa (Unit Leader) Vittorio", "Tucci Michele", "Di Pompeo Daniele", "Carlino Calogero"),
	};

	private static final Contributors[] JMT;

	static {
		List<Contributors> allContributors = new ArrayList<AboutDialogFactory.Contributors>();
		allContributors.addAll(Arrays.asList(JMVA));
		allContributors.addAll(Arrays.asList(JSIM));
		allContributors.addAll(Arrays.asList(JMODEL));
		allContributors.addAll(Arrays.asList(JABA));
		allContributors.addAll(Arrays.asList(JMCH));
		allContributors.addAll(Arrays.asList(JWAT));
		Map<Company, Set<String>> m = new EnumMap<Company, Set<String>>(Company.class);
		for (Contributors c : allContributors) {
			Set<String> set = m.get(c.getCompany());
			if (set == null) {
				set = new HashSet<String>();
				m.put(c.getCompany(), set);
			}
			set.addAll(c.getNames());
		}
		JMT = new Contributors[m.size()];
		int idx = 0;
		for (Company c : m.keySet()) {
			Set<String> names = m.get(c);
			JMT[idx] = new Contributors(c, names.toArray(new String[names.size()]));
			idx++;
		}
	}

	private static final long AUTOCLOSE_TIMEOUT = 2500;
	private static boolean autoJMVAshown = false;

	protected static JMTDialog createDialog(Window owner, AboutDialogPanel panel, boolean autoclose) {
		final JMTDialog dialog;
		if (owner == null) {
			dialog = new JMTDialog();
		} else if (owner instanceof Dialog) {
			dialog = new JMTDialog((Dialog) owner, true);
		} else if (owner instanceof Frame) {
			dialog = new JMTDialog((Frame) owner, true);
		} else {
			dialog = new JMTDialog();
		}
		dialog.setTitle(panel.getDialogTitle());
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(panel, BorderLayout.CENTER);

		JButton exit = new JButton();
		exit.setText("Close");
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.close();
			}
		});

		JPanel bottom = new JPanel();
		bottom.add(exit);
		dialog.getContentPane().add(bottom, BorderLayout.SOUTH);
		dialog.centerWindow(CommonConstants.MAX_GUI_WIDTH_ABOUT, CommonConstants.MAX_GUI_HEIGHT_ABOUT);

		if (autoclose) {
			ExecutorService es = Executors.newSingleThreadExecutor();
			es.execute(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(AUTOCLOSE_TIMEOUT);
						dialog.close();
					} catch (InterruptedException ex) {
						// Nothing to do
					}
				}
			});
			es.shutdown();
		}

		return dialog;
	}

	public static void showJMVA(Window owner, boolean autoclose) {
		if (autoclose && autoJMVAshown) {
			return;
		}
		autoJMVAshown = true;
		AboutDialogPanel panel = new AboutDialogPanel();
		panel.setTitles("About JMVA", "JMVA", IMG_JMVAICON);
		panel.setNames(JMVA);
		createDialog(owner, panel, autoclose).setVisible(true);
	}

	public static void showJSIM(Window owner) {
		AboutDialogPanel panel = new AboutDialogPanel();
		panel.setTitles("About JSIMwiz", "JSIM<em>wiz</em>", IMG_JSIMICON);
		panel.setNames(JSIM);
		createDialog(owner, panel, false).setVisible(true);
	}

	public static void showJMODEL(Window owner) {
		AboutDialogPanel panel = new AboutDialogPanel();
		panel.setTitles("About JSIMgraph", "JSIM<em>graph</em>", IMG_JMODELICON);
		panel.setNames(JMODEL);
		createDialog(owner, panel, false).setVisible(true);
	}

	public static void showJABA(Window owner) {
		AboutDialogPanel panel = new AboutDialogPanel();
		panel.setTitles("About JABA", "JABA", IMG_JABAICON);
		panel.setNames(JABA);
		createDialog(owner, panel, false).setVisible(true);
	}

	public static void showJMCH(Window owner) {
		AboutDialogPanel panel = new AboutDialogPanel();
		panel.setTitles("About JMCH", "JMCH", IMG_JMCHICON);
		panel.setNames(JMCH);
		createDialog(owner, panel, false).setVisible(true);
	}

	public static void showJWAT(Window owner) {
		AboutDialogPanel panel = new AboutDialogPanel();
		panel.setTitles("About JWAT", "JWAT", IMG_JWATICON);
		panel.setNames(JWAT);
		createDialog(owner, panel, false).setVisible(true);
	}

	public static void showJMT(Window owner) {
		AboutDialogPanel panel = new AboutDialogPanel();
		panel.setTitles("About JMT", "Java Modelling Tools", IMG_SUITEICON);
		panel.setNames(JMT);
		createDialog(owner, panel, false).setVisible(true);
	}

	public static class Contributors {

		private Company company;
		private List<String> names;
		private List<String> processedNames;

		private Contributors(Company company, String... names) {
			this(company, Arrays.asList(names));
		}

		private Contributors(Company company, List<String> names) {
			this.company = company;
			if (!names.isEmpty()) {
				String first = names.get(0);
				List<String> sorted = new ArrayList<>(names.subList(1, names.size()));
				Collections.sort(sorted);
				List<String> finalList = new ArrayList<>();
				finalList.add(first);
				finalList.addAll(sorted);
				names = finalList;
			}
			ArrayList<String> processedNames = new ArrayList<String>(names.size());
			for (String name : names) {
				while (name.startsWith("*")) {
					name = name.substring(1);
				}
				int nameIdx = name.lastIndexOf(" ");
				if (nameIdx > 0) {
					name = name.substring(nameIdx + 1) + " " + name.substring(0, nameIdx);
				}
				processedNames.add(name);
			}
			this.names = Collections.unmodifiableList(names);
			this.processedNames = Collections.unmodifiableList(processedNames);
		}

		public Company getCompany() {
			return company;
		}

		public List<String> getNames() {
			return names;
		}

		public List<String> getProcessedNames() {
			return processedNames;
		}
	}
}
