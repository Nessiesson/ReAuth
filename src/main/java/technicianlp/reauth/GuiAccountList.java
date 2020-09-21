package technicianlp.reauth;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class GuiAccountList extends GuiScreen {
	private final static int BUTTON_WIDTH = 308;
	private final GuiScreen parentScreen;
	private GuiButton loginButton;
	private Secure.Account selectedAccount = null;
	private GuiSlotAccounts accountList;

	protected GuiAccountList(final GuiScreen parentScreen) {
		this.parentScreen = parentScreen;
	}

	@Override
	public final void initGui() {
		super.initGui();

		final int start = this.width / 2 - BUTTON_WIDTH / 2;
		final int halfWidth = BUTTON_WIDTH / 2 - 4;
		final int thirdWidth = BUTTON_WIDTH / 3 - 4;
		this.addButton(this.loginButton = new GuiButton(0, start, this.height - 50, halfWidth, 20, "Login"));
		this.addButton(new GuiButton(1, start + BUTTON_WIDTH - halfWidth, this.height - 50, halfWidth, 20, "Add Account"));
		final GuiButton editButton = new GuiButton(2, start, this.height - 27, thirdWidth, 20, "Edit account");
		final GuiButton removeButton = new GuiButton(3, this.width / 2 - thirdWidth / 2, this.height - 27, thirdWidth, 20, "Remove account");
		final GuiButton cancelButton = new GuiButton(4, start + BUTTON_WIDTH - thirdWidth, this.height - 27, thirdWidth, 20, I18n.format("gui.cancel"));
		this.addButton(editButton);
		this.addButton(removeButton);
		this.addButton(cancelButton);
		if (Secure.accounts.isEmpty()) {
			this.loginButton.enabled = false;
			editButton.enabled = false;
			removeButton.enabled = false;
		} else {
			this.selectedAccount = Secure.accounts.values().iterator().next();
		}

		this.accountList = new GuiSlotAccounts(this.mc, this.width, this.height, 50, this.height - 60, 38);
		Secure.initSkinStuff();
	}

	@Override
	public final void handleMouseInput() throws IOException {
		super.handleMouseInput();
		this.accountList.handleMouseInput();
	}

	@Override
	public final void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
		this.accountList.drawScreen(mouseX, mouseY, partialTicks);
		super.drawScreen(mouseX, mouseY, partialTicks);
		this.drawCenteredString(this.fontRenderer, "Account List", this.width / 2, 10, 0xffffff);
	}

	@Override
	protected final void actionPerformed(final GuiButton button) {
		switch (button.id) {
			case 0:
				if (this.selectedAccount.getPassword() == null) {
					mc.displayGuiScreen(new GuiLogin(parentScreen, this, this.selectedAccount));
				} else {
					try {
						Secure.login(this.selectedAccount.getUsername(), this.selectedAccount.getPassword(), true);
						this.mc.displayGuiScreen(this.parentScreen);
					} catch (AuthenticationException | IllegalAccessException e) {
						this.mc.displayGuiScreen(new GuiErrorScreen("ReAuth", "Authentication Failed"));
					}
				}

				break;
			case 1:
				mc.displayGuiScreen(new GuiLogin(this.parentScreen, this, this.selectedAccount));
				break;
			case 2:
				Secure.accounts.remove(this.selectedAccount.getUsername());
				if (Secure.accounts.isEmpty()) {
					this.mc.displayGuiScreen(this.parentScreen);
				} else {
					this.selectedAccount = Secure.accounts.values().iterator().next();
				}

				Main.loadConfig();
				break;
			case 4:
				this.mc.displayGuiScreen(this.parentScreen);
				break;
		}
	}

	private final class GuiSlotAccounts extends GuiSlot {
		public GuiSlotAccounts(final Minecraft mc, final int width, final int height, final int top, final int bottom, final int slotHeight) {
			super(mc, width, height, top, bottom, slotHeight);
		}

		@Override
		protected final int getSize() {
			return Secure.accounts.size();
		}

		@Override
		protected final void elementClicked(final int slotIndex, final boolean isDoubleClick, final int mouseX, final int mouseY) {
			int i = 0;
			for (Secure.Account account : Secure.accounts.values()) {
				if (i++ == slotIndex) {
					selectedAccount = account;
					break;
				}
			}

			if (isDoubleClick) {
				GuiAccountList.this.actionPerformed(loginButton);
			}
		}

		@Override
		protected final boolean isSelected(final int slotIndex) {
			int i = 0;
			for (Secure.Account account : Secure.accounts.values()) {
				if (i == slotIndex)
					return selectedAccount.equals(account);
				i++;
			}
			return false;
		}

		@Override
		protected final void drawBackground() {
			drawDefaultBackground();
		}

		@Override
		protected final void drawSlot(final int slotIndex, final int xPos, final int yPos, final int heightIn, final int mouseXIn, final int mouseYIn, final float partialTicks) {
			Secure.Account account = null;
			int i = 0;
			for (Secure.Account a : Secure.accounts.values()) {
				if (i == slotIndex) {
					account = a;
					break;
				}
				i++;
			}

			if (account == null) {
				return;
			}

			drawString(this.mc.fontRenderer, account.getDisplayName(), xPos + 50, yPos + 7, 0xffffff);
			drawString(this.mc.fontRenderer, account.getUsername(), xPos + 50, yPos + 19, 0x777777);

			GameProfile gameProfile = new GameProfile(account.getUuid(), account.getDisplayName());
			if (account.getLastQuery() + 600000 < System.currentTimeMillis()) {
				if (!gameProfile.getProperties().containsKey("textures") || !gameProfile.isComplete()) {
					gameProfile = TileEntitySkull.updateGameProfile(gameProfile);
					if (account.getUuid() == null) {
						account.setUuid(gameProfile.getId());
						Main.loadConfig();
					}

					account.setLastQuery(System.currentTimeMillis());
				}
			}

			final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> profiles = Minecraft.getMinecraft().getSkinManager().loadSkinFromCache(gameProfile);
			final ResourceLocation skin;
			if (profiles.containsKey(MinecraftProfileTexture.Type.SKIN)) {
				skin = Minecraft.getMinecraft().getSkinManager().loadSkin(profiles.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
			} else {
				final UUID id = EntityPlayer.getUUID(gameProfile);
				skin = DefaultPlayerSkin.getDefaultSkin(id);
			}

			Minecraft.getMinecraft().getTextureManager().bindTexture(skin);
			drawScaledCustomSizeModalRect(xPos + 1, yPos + 1, 8, 8, 8, 8, 32, 32, 64, 64);
		}
	}
}
