package jmt.util;

import javax.swing.JMenuItem;

import org.fest.swing.core.GenericTypeMatcher;

public class TextMenuItemMatcher extends GenericTypeMatcher<JMenuItem> {
	
	private String textToMatch;

	public TextMenuItemMatcher(String textToMatch) {
		super(JMenuItem.class);
		this.textToMatch = textToMatch;
	}
	
	@Override
	protected boolean isMatching(JMenuItem item) {
		return textToMatch.equals(item.getText());
	}

}
