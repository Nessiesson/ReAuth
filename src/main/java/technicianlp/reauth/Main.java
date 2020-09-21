package technicianlp.reauth;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.UUID;

@Mod(modid = "reauth", name = "ReAuth", version = "3.6.1", guiFactory = "technicianlp.reauth.GuiFactory", canBeDeactivated = true, clientSideOnly = true, acceptedMinecraftVersions = "[1.12]")
public class Main {
	protected static final Logger log = LogManager.getLogger("ReAuth");
	protected static Configuration config;
	protected static boolean offlineModeEnabled;

	public static void loadConfig() {
		final int numAccounts = Math.min(config.get("accounts", Configuration.CATEGORY_GENERAL, 0).getInt(), 999); // who has more than 1000 accounts anyway?
		for (int accNo = 0; accNo < numAccounts; ++accNo) {
			final String user = config.get("username." + accNo, Configuration.CATEGORY_GENERAL, "").getString();
			if (user == null) {
				continue;
			}

			final String pass = config.get("password." + accNo, Configuration.CATEGORY_GENERAL, "").getString();
			final char[] pw = pass == null ? null : pass.toCharArray();
			final String uuidStr = config.get("uuid." + accNo, Configuration.CATEGORY_GENERAL, "").getString();
			final UUID uuid = uuidStr == null ? null : UUID.fromString(uuidStr);
			final String displayName = config.get("displayName." + accNo, Configuration.CATEGORY_GENERAL, "").getString();
			Secure.accounts.put(user, new Secure.Account(user, pw, uuid, displayName));
		}

		Main.offlineModeEnabled = config.get(Configuration.CATEGORY_GENERAL, "offlineModeEnabled", false, "Enables play-offline button").getBoolean();
		GuiHandler.enabled = config.get(Configuration.CATEGORY_GENERAL, "validatorEnabled", true, "Disables the Session Validator").getBoolean();
		GuiHandler.bold = config.get(Configuration.CATEGORY_GENERAL, "validatorBold", true, "If the Session-Validator look weird disable this").getBoolean();
		Main.config.save();
	}

	@Mod.EventHandler
	public void preInit(final FMLPreInitializationEvent evt) {
		MinecraftForge.EVENT_BUS.register(this);
		//Moved ReAuth config out of /config
		final File config = new File(Minecraft.getMinecraft().gameDir, ".ReAuth.cfg");
		//new one missing; old one there -> move the file
		if (evt.getSuggestedConfigurationFile().exists() && !config.exists()) {
			//noinspection ResultOfMethodCallIgnored
			evt.getSuggestedConfigurationFile().renameTo(config);
		}

		//initialize config
		Main.config = new Configuration(config);
		Main.loadConfig();
	}

	@SubscribeEvent
	public void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent evt) {
		if (evt.getModID().equals("reauth")) {
			Main.loadConfig();
		}
	}
}
