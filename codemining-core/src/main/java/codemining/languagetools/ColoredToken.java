package codemining.languagetools;

import java.awt.Color;

/**
 * Struct class representing a colored token.
 * 
 */
public final class ColoredToken {
	public Color fontColor;
	public final Color bgColor;
	public final String token;
	public String extraStyle;

	/**
	 * Construct with default bgColor white.
	 * 
	 * @param token
	 * @param fontColor
	 */
	public ColoredToken(final String token, final Color fontColor) {
		this.token = token;
		this.fontColor = fontColor;
		bgColor = Color.WHITE;
		extraStyle = "";
	}

	public ColoredToken(final String token, final Color fontColor,
			final Color bgColor, final String extraStyle) {
		this.token = token;
		this.fontColor = fontColor;
		this.bgColor = bgColor;
		this.extraStyle = extraStyle;
	}

	public void setColor(final Color fontColor) {
		this.fontColor = fontColor;
	}

	public void setStyle(final String extraStyle) {
		this.extraStyle = extraStyle;
	}

}