package technicianlp.reauth;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.awt.*;

@Mod.EventBusSubscriber(modid = "reauth", value = Side.CLIENT)
public final class GuiHandler {
	private static final int buttonId = 17325;

	/**
	 * Cache the Status for 5 Minutes
	 */
	private static final CachedProperty<ValidationStatus> status = new CachedProperty<>(1000 * 60 * 5, ValidationStatus.Unknown);
	protected static boolean enabled = true;
	protected static boolean bold = true;
	private static Thread validator;

	@SubscribeEvent
	public static void open(final GuiScreenEvent.InitGuiEvent.Post e) {
		if (e.getGui() instanceof GuiMultiplayer) {
			e.getButtonList().add(new GuiButton(buttonId, 5, 5, 100, 20, "Re-Login"));
			if (enabled && !status.check()) {
				if (validator != null) {
					validator.interrupt();
				}

				validator = new Thread(() -> status.set(Secure.SessionValid() ? ValidationStatus.Valid : ValidationStatus.Invalid), "Session-Validator");
				validator.setDaemon(true);
				validator.start();
			}
		} else if (e.getGui() instanceof GuiMainMenu) {
			// Support for Custom Main Menu (add button outside of viewport)
			e.getButtonList().add(new GuiButton(buttonId, -50, -50, 20, 20, "ReAuth"));
		}
	}

	@SubscribeEvent
	public static void draw(final GuiScreenEvent.DrawScreenEvent.Post e) {
		if (enabled && e.getGui() instanceof GuiMultiplayer) {
			e.getGui().drawString(e.getGui().mc.fontRenderer, "Online:", 110, 10, 0xFFFFFFFF);
			final ValidationStatus state = status.get();
			e.getGui().drawString(e.getGui().mc.fontRenderer, (bold ? ChatFormatting.BOLD : "") + state.text, 145, 10, state.color);
		}
	}

	@SubscribeEvent
	public static void action(final GuiScreenEvent.ActionPerformedEvent.Post e) {
		if ((e.getGui() instanceof GuiMainMenu || e.getGui() instanceof GuiMultiplayer) && e.getButton().id == buttonId) {
			Minecraft.getMinecraft().displayGuiScreen(new GuiAccountList(Minecraft.getMinecraft().currentScreen));
		}
	}

	@SubscribeEvent
	public static void action(final GuiScreenEvent.ActionPerformedEvent.Pre e) {
		if (enabled && e.getGui() instanceof GuiMultiplayer && e.getButton().id == 8 && GuiScreen.isShiftKeyDown()) {
			status.invalidate();
		}
	}

	protected static void invalidateStatus() {
		status.invalidate();
	}

	private enum ValidationStatus {
		Unknown("?", Color.GRAY.getRGB()), Valid("\u2714", Color.GREEN.getRGB()), Invalid("\u2718", Color.RED.getRGB());

		private final String text;
		private final int color;

		ValidationStatus(final String text, final int color) {
			this.text = text;
			this.color = color;
		}
	}
}
