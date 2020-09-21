package technicianlp.reauth;

import com.mojang.authlib.exceptions.AuthenticationException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;

final class GuiLogin extends GuiScreen {
	private final Secure.Account startingAccount;
	private final GuiScreen successPrevScreen;
	private final GuiScreen failPrevScreen;
	private GuiTextField username;
	private GuiPasswordField pw;
	private GuiButton login;
	private GuiCheckBox save;
	private int basey;
	private String message = "";

	GuiLogin(final GuiScreen successPrevScreen, final GuiScreen failPrevScreen, final Secure.Account startingAccount) {
		this.mc = Minecraft.getMinecraft();
		this.fontRenderer = mc.fontRenderer;
		this.successPrevScreen = successPrevScreen;
		this.failPrevScreen = failPrevScreen;
		this.startingAccount = startingAccount;
	}

	@Override
	protected void actionPerformed(final GuiButton b) {
		switch (b.id) {
			case 0:
			case 3:
				if (this.login() || this.playOffline()) {
					this.mc.displayGuiScreen(this.successPrevScreen);
				}

				break;
			case 1:
				this.mc.displayGuiScreen(this.failPrevScreen);
				break;
			case 4:
				this.mc.displayGuiScreen(new ConfigGUI(this));
				break;
		}

	}

	@Override
	public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
		this.drawDefaultBackground();
		this.drawCenteredString(this.fontRenderer, "Username/E-Mail:", this.width / 2, this.basey, Color.WHITE.getRGB());
		this.drawCenteredString(this.fontRenderer, "Password:", this.width / 2, this.basey + 45, Color.WHITE.getRGB());
		if (!(this.message == null || this.message.isEmpty())) {
			this.drawCenteredString(this.fontRenderer, this.message, this.width / 2, this.basey - 15, 0xFFFFFF);
		}

		this.username.drawTextBox();
		this.pw.drawTextBox();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		this.username.drawTextBox();
		this.pw.drawTextBox();
	}

	@Override
	public void initGui() {
		super.initGui();
		Keyboard.enableRepeatEvents(true);

		this.basey = this.height / 2 - 110 / 2;
		this.username = new GuiTextField(0, this.fontRenderer, this.width / 2 - 155, this.basey + 15, 2 * 155, 20);
		this.username.setMaxStringLength(512);
		if (this.startingAccount != null) {
			this.username.setText(this.startingAccount.getUsername());
		}

		this.username.setFocused(true);
		this.pw = new GuiPasswordField(this.fontRenderer, this.width / 2 - 155, this.basey + 60, 2 * 155, 20);
		if (this.startingAccount != null)
			this.pw.setPassword(this.startingAccount.getPassword());

		this.save = new GuiCheckBox(2, this.width / 2 - 155, this.basey + 85, "Save Password to Config (WARNING: SECURITY RISK!)", false);
		this.buttonList.add(this.save);

		this.login = new GuiButton(0, this.width / 2 - 155, this.basey + 105, 153, 20, "Login");
		final GuiButton cancel = new GuiButton(1, this.width / 2 + 2, this.basey + 105, 155, 20, "Cancel");
		this.buttonList.add(this.login);
		this.buttonList.add(cancel);
		if (Main.offlineModeEnabled) {
			final GuiButton offline = new GuiButton(3, this.width / 2 - 50, this.basey + 105, 100, 20, "Play Offline");
			this.buttonList.add(offline);
		}
	}

	@Override
	protected void keyTyped(final char c, final int k) throws IOException {
		super.keyTyped(c, k);
		this.username.textboxKeyTyped(c, k);
		this.pw.textboxKeyTyped(c, k);
		if (k == Keyboard.KEY_TAB) {
			this.username.setFocused(!this.username.isFocused());
			this.pw.setFocused(!this.pw.isFocused());
		} else if (k == Keyboard.KEY_RETURN) {
			if (this.username.isFocused()) {
				this.username.setFocused(false);
				this.pw.setFocused(true);
			} else if (this.pw.isFocused()) {
				this.actionPerformed(this.login);
			}
		}
	}

	@Override
	protected void mouseClicked(final int x, final int y, final int b) throws IOException {
		super.mouseClicked(x, y, b);
		this.username.mouseClicked(x, y, b);
		this.pw.mouseClicked(x, y, b);
	}

	/**
	 * used as an interface between this and the secure class
	 * <p>
	 * returns whether the login was successful
	 */
	private boolean login() {
		try {
			Secure.login(this.username.getText(), this.pw.getPW(), this.save.isChecked());
			this.message = TextFormatting.GREEN + "Login successful!";
			Main.log.info("Login successful!");
			return true;
		} catch (AuthenticationException e) {
			this.message = TextFormatting.DARK_RED + "Login failed: " + e.getMessage();
			Main.log.error("Login failed:", e);
			return false;
		} catch (Exception e) {
			this.message = TextFormatting.DARK_RED + "Error: Something went wrong!";
			Main.log.error("Error:", e);
			return false;
		}
	}

	/**
	 * sets the name for playing offline
	 */
	private boolean playOffline() {
		final String username = this.username.getText();
		if (!(username.length() >= 2 && username.length() <= 16)) {
			this.message = TextFormatting.DARK_RED + "Error: Username needs a length between 2 and 16.";
			return false;
		}
		if (!username.matches("[A-Za-z0-9_]{2,16}")) {
			this.message = TextFormatting.DARK_RED + "Error: Username has to be alphanumerical.";
			return false;
		}
		try {
			Secure.offlineMode(username);
			return true;
		} catch (Exception e) {
			this.message = TextFormatting.DARK_RED + "Error: Something went wrong!";
			Main.log.error("Error:", e);
			return false;
		}
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		this.pw.setPassword(new char[0]);
		Keyboard.enableRepeatEvents(false);
	}
}
