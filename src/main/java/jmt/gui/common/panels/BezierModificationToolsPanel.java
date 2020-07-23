package jmt.gui.common.panels;

import jmt.framework.gui.components.JMTDialog;
import jmt.framework.gui.components.JMTToolBar;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.jsimgraph.controller.Mediator;
import jmt.gui.jsimgraph.controller.actions.ActionDelete;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class BezierModificationToolsPanel extends JPanel {
    protected JToolBar toolBar;
    protected JMTDialog dialog;
    private Frame mainWindow;
    private Mediator m;

    public BezierModificationToolsPanel(Frame mainWindow, Mediator m) {
        super(new BorderLayout());
        this.mainWindow=mainWindow;
        this.m=m;

        toolBar = new JMTToolBar(JMTImageLoader.getImageLoader());
        ((JMTToolBar) toolBar).addGenericButton(m.getBezierEdgeSelectControlPoint());
        toolBar.addSeparator(new Dimension(5,0));
        ((JMTToolBar) toolBar).addGenericButton(m.getBezierEdgeAddControlPoint());
        toolBar.addSeparator(new Dimension(5,0));
        ((JMTToolBar) toolBar).addGenericButton(m.getBezierEdgeRemoveControlPoint());
        toolBar.addSeparator(new Dimension(5,0));
        ((JMTToolBar) toolBar).addGenericButton(m.getBezierEdgeAddTangents());
        toolBar.addSeparator(new Dimension(5,0));
        ((JMTToolBar) toolBar).addGenericButton(m.getBezierEdgeUnlockTangents());
        toolBar.addSeparator(new Dimension(5,0));
        ((JMTToolBar) toolBar).addGenericButton(m.getBezierEdgeBreakEdge());

        dialog = new JMTDialog(mainWindow, false);
        dialog.setFocusableWindowState(false);
        dialog.setFocusable(false);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(toolBar, BorderLayout.CENTER);
        dialog.setTitle("Arc editing tool box");
        dialog.centerWindow(255, 95);
    }


    public JMTDialog getDialog() {
        return(dialog);
    }

    public JMTToolBar getToolBar() {
        return (JMTToolBar) toolBar;
    }
}


