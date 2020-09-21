package technicianlp.reauth;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;

final class GuiPasswordField extends GuiTextField {
	private char[] password = new char[0];

	GuiPasswordField(final FontRenderer renderer, final int posX, final int posY, final int x, final int y) {
		super(1, renderer, posX, posY, x, y);
		this.setMaxStringLength(512);
	}

	protected final char[] getPW() {
		final char[] pw = new char[this.password.length];
		System.arraycopy(this.password, 0, pw, 0, this.password.length);
		return pw;
	}

	public final boolean textboxKeyTyped(final char typedChar, final int keyCode) {
		if (!this.isFocused() || GuiScreen.isKeyComboCtrlC(keyCode) || GuiScreen.isKeyComboCtrlX(keyCode)) {
			return false; // Prevent Cut/Copy
		}

		if (GuiScreen.isKeyComboCtrlA(keyCode) || GuiScreen.isKeyComboCtrlV(keyCode)) {
			return super.textboxKeyTyped(typedChar, keyCode); // combos handled by super
		}

		switch (keyCode) {
			case Keyboard.KEY_BACK: // backspace
			case Keyboard.KEY_DELETE:
			case Keyboard.KEY_HOME: // jump keys?
			case Keyboard.KEY_END:
			case Keyboard.KEY_LEFT: // arrowkey
			case Keyboard.KEY_RIGHT:
				return super.textboxKeyTyped(typedChar, keyCode); // special keys handled by super
			default:
				if (isAllowedCharacter(typedChar)) {
					this.writeText(Character.toString(typedChar));
					return true;
				}
				return false;
		}
	}

	public final void writeText(final String rawInput) {
		final int selStart = Math.min(this.getCursorPosition(), this.getSelectionEnd());
		final int selEnd = Math.max(this.getCursorPosition(), this.getSelectionEnd());

		final char[] input = filterAllowedCharacters(rawInput).toCharArray();
		final char[] newPW = new char[selStart + this.password.length - selEnd + input.length];

		if (this.password.length != 0 && selStart > 0) {
			System.arraycopy(this.password, 0, newPW, 0, Math.min(selStart, this.password.length));
		}

		System.arraycopy(input, 0, newPW, selStart, input.length);
		final int l = input.length;
		if (this.password.length != 0 && selEnd < l) {
			System.arraycopy(this.password, selEnd, newPW, selStart + l, this.password.length - selEnd);
		}

		setPassword(newPW);
		Arrays.fill(newPW, 'f');
		this.moveCursorBy(selStart - this.getSelectionEnd() + l);
	}

	@Override
	public final void deleteFromCursor(final int num) {
		if (this.password.length == 0) {
			return;
		}

		if (this.getSelectionEnd() != this.getCursorPosition()) {
			this.writeText("");
		} else {
			final boolean direction = num < 0;
			final int start = direction ? Math.max(this.getCursorPosition() + num, 0) : this.getCursorPosition();
			final int end = direction ? this.getCursorPosition() : Math.min(this.getCursorPosition() + num, this.password.length);
			final char[] newPW = new char[start + this.password.length - end];
			if (start >= 0) {
				System.arraycopy(this.password, 0, newPW, 0, start);
			}

			if (end < this.password.length) {
				System.arraycopy(this.password, end, newPW, start, this.password.length - end);
			}

			this.setPassword(newPW);
			Arrays.fill(newPW, 'f');
			if (direction) {
				this.moveCursorBy(num);
			}
		}
	}

	protected final void setPassword(final char[] password) {
		if (this.password == null)
			this.password = new char[0];
		Arrays.fill(this.password, 'f');
		this.password = new char[this.password.length];
		System.arraycopy(this.password, 0, this.password, 0, this.password.length);
		this.updateText();
	}

	@Override
	public final void setText(final String text) {
		this.setPassword(text.toCharArray());
		this.updateText();
	}

	private void updateText() {
		final char[] chars = new char[this.password.length];
		Arrays.fill(chars, '\u25CF');
		super.setText(new String(chars));
	}

	/**
	 * Allow SectionSign to be input into the field
	 */
	private boolean isAllowedCharacter(final int character) {
		return character == 0xa7 || ChatAllowedCharacters.isAllowedCharacter((char) character);
	}

	/**
	 * Modified version of {@link ChatAllowedCharacters#filterAllowedCharacters(String)}
	 */
	private String filterAllowedCharacters(final String input) {
		StringBuilder stringbuilder = new StringBuilder();
		input.chars().filter(this::isAllowedCharacter).forEach(i -> stringbuilder.append((char) i));
		return stringbuilder.toString();
	}
}
