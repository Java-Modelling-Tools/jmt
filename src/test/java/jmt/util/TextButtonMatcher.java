package jmt.util;

import javax.swing.JButton;

import org.fest.swing.core.GenericTypeMatcher;

public class TextButtonMatcher extends GenericTypeMatcher<JButton> {

	private String textToMatch;

	public TextButtonMatcher(String textToMatch) {
		super(JButton.class);
		this.textToMatch = textToMatch;
	}
	
	@Override
	protected boolean isMatching(JButton button) {
		return textToMatch.equals(button.getText());
	}

}
