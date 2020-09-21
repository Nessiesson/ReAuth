package technicianlp.reauth;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

import java.util.Set;

public final class GuiFactory implements IModGuiFactory {
	public final void initialize(final Minecraft minecraftInstance) {
	}

	@Override
	public final boolean hasConfigGui() {
		return true;
	}

	@Override
	public final GuiScreen createConfigGui(final GuiScreen parentScreen) {
		return new ConfigGUI(parentScreen);
	}

	@Override
	public final Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		return null;
	}
}
