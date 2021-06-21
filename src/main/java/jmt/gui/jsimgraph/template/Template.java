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

package jmt.gui.jsimgraph.template;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.panels.CustomizableDialogFactory;
import jmt.gui.jsimgraph.JGraphMod.CellFactory;
import jmt.gui.jsimgraph.JGraphMod.JmtCell;
import jmt.gui.jsimgraph.controller.JmtClipboard;
import jmt.gui.jsimgraph.controller.Mediator;
import jmt.gui.jsimgraph.definitions.JMTPoint;
import jmt.gui.jsimgraph.mainGui.JSIMGraphMain;
import jmt.gui.jsimgraph.template.ITemplate;

/**
 * @author S Jiang
 * 
 */
public class Template extends WizardPanel implements ITemplate, CommonConstants {

	private static final long serialVersionUID = 1L;

	private Mediator mediator;

	JSpinner number;

	private JmtCell fork;
	private JmtCell join;
	private JmtCell server;

	private JPanel parent;

	public Template(Mediator m) {
		mediator = m;
		parent = this;
		this.initComponents();
	}

	public void initComponents() {
		//------------------build the GUI components of your template here------------------//

		initCells();
		JPanel picPane = new JPanel();
		picPane.setBorder(new TitledBorder(new EtchedBorder(), "Illustration"));
		try {
			BufferedImage img = ImageIO.read(new File("/home/js/JMT-Refactory/template-sample-code/pictures/TemplateIllustration.png"));
			Image resizedImage = 
					img.getScaledInstance(300, 200, Image.SCALE_SMOOTH);
			ImageIcon icon = new ImageIcon(resizedImage);

			JLabel pLabel = new JLabel(icon);
			picPane.add(pLabel);
		} catch (Exception e) {

		}
		JLabel label = new JLabel("Number of service center: ");
		number = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
		label.setLabelFor(number);

		JPanel structurePane = new JPanel();
		structurePane.setBorder(new TitledBorder(new EtchedBorder(), "Structure"));
		structurePane.add(label);
		structurePane.add(number);

		JButton editFork = new JButton("Edit Fork");
		JButton editServer = new JButton("Edit Service Center");
		JButton editJoin = new JButton("Edit Join");

		editFork.setPreferredSize(new Dimension(150,25));
		editServer.setPreferredSize(new Dimension(150,25));
		editJoin.setPreferredSize(new Dimension(150,25));

		editFork.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				mediator.startEditingAtAbstractCell(fork);
			}
		});

		editServer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				mediator.startEditingAtAbstractCell(server);
			}
		});

		editJoin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				mediator.startEditingAtAbstractCell(join);
			}
		});

		JPanel parameterPane = new JPanel();
		parameterPane.setBorder(new TitledBorder(new EtchedBorder(), "Parameter"));
		parameterPane.add(editFork);
		parameterPane.add(editServer);
		parameterPane.add(editJoin);

		JPanel centerPane = new JPanel(new GridLayout(2,1));
		centerPane.add(structurePane);
		centerPane.add(parameterPane);

		JButton create = new JButton("Insert");

		create.setPreferredSize(new Dimension(100,25));

		create.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				createModel();
				SwingUtilities.getWindowAncestor(parent).setVisible(false);
			}
		});

		JPanel bottoms = new JPanel();
		bottoms.add(create);
		this.setLayout(new BorderLayout());
		this.add(picPane, BorderLayout.NORTH);
		this.add(centerPane, BorderLayout.CENTER);
		this.add(bottoms, BorderLayout.SOUTH);

		//------------------------------------END------------------------------------------//
	}

	public void initCells() {
		CellFactory cf = new CellFactory(mediator);
		fork = cf.createCell("ForkCell");
		server = cf.createCell("ServerCell");
		join = cf.createCell("JoinCell");
	}

	@Override
	public CommonModel createModel() {
		mediator.setSelectState();
		double YBound = mediator.getBound();
		int num = (Integer)number.getValue();

		//-----------------Adding cells to mediator----------------------//
		mediator.InsertCell(new JMTPoint(50, YBound + 50 * num, false), fork);
		mediator.InsertCell(new JMTPoint(300, YBound + 50 * num, false), join);
		mediator.InsertCell(new JMTPoint(150, YBound + 50, false), server);

		mediator.connect(fork, server);
		mediator.connect(server, join);

		JmtClipboard clipboard = new JmtClipboard(mediator);

		for (int i = 1; i < num; i++) {
			clipboard.copyCell(server);
			JmtCell sub = clipboard.pasteCell(new JMTPoint(150, YBound + 50 + 100 * i, false));

			mediator.connect(fork, sub);
			mediator.connect(sub, join);
		}

		CommonModel cm = mediator.getModel();
		return cm;
	}

	@Override
	public void showDialog(JSIMGraphMain mainWindow) {
		CustomizableDialogFactory templateDialogFactory = new CustomizableDialogFactory(mainWindow);
		//change size/name of your template dialog here
		//width, height, ..., title
		templateDialogFactory.getDialog(500, 420, this, "Parallel Model Template");
	}

	//change the name of your template
	@Override
	public String getName() {
		return "Parallel Model Template";
	}

	public static void main(String[] args) {
		return;
	}

}
