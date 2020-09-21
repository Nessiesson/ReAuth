package technicianlp.reauth;

import com.google.common.base.Charsets;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.Session;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.io.File;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class Secure {
	/**
	 * Username/email -> password map
	 */
	protected static final Map<String, Account> accounts = new LinkedHashMap<>();
	/**
	 * Mojang authentificationservice
	 */
	private static final YggdrasilAuthenticationService yas = new YggdrasilAuthenticationService(Minecraft.getMinecraft().getProxy(), UUID.randomUUID().toString());
	private static final YggdrasilUserAuthentication yua = (YggdrasilUserAuthentication) yas.createUserAuthentication(Agent.MINECRAFT);
	private static final YggdrasilMinecraftSessionService ymss = (YggdrasilMinecraftSessionService) yas.createMinecraftSessionService();
	private static String offlineUsername = "";

	public static void initSkinStuff() {
		final GameProfileRepository gpr = yas.createProfileRepository();
		final PlayerProfileCache ppc = new PlayerProfileCache(gpr, new File(Minecraft.getMinecraft().gameDir, MinecraftServer.USER_CACHE_FILE.getName()));
		TileEntitySkull.setProfileCache(ppc);
		TileEntitySkull.setSessionService(ymss);
	}

	/**
	 * Logs you in; replaces the Session in your client; and saves to config
	 */
	static void login(final String user, char[] pw, final boolean savePassToConfig) throws AuthenticationException, IllegalAccessException {
		/* set credentials */
		Secure.yua.setUsername(user);
		Secure.yua.setPassword(new String(pw));

		/* login */
		Secure.yua.logIn();

		Main.log.info("Login successful!");

		/* put together the new Session with the auth-data */
		final String username = Secure.yua.getSelectedProfile().getName();
		final UUID uuid = Secure.yua.getSelectedProfile().getId();
		final String uuidStr = UUIDTypeAdapter.fromUUID(uuid);
		final String access = Secure.yua.getAuthenticatedToken();
		final String type = Secure.yua.getUserType().getName();
		SessionUtil.set(new Session(username, uuidStr, access, type));

		/* logout to discard the credentials in the object */
		Secure.yua.logOut();

		/* save username and password to config */
		Secure.accounts.put(user, new Account(user, savePassToConfig ? pw : null, uuid, username));

		Main.config.save();
	}

	static void offlineMode(final String username) throws IllegalAccessException {
		/* Create offline uuid */
		final UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(Charsets.UTF_8));
		SessionUtil.set(new Session(username, uuid.toString(), "invalid", "legacy"));
		Main.log.info("Offline Username set!");
		Secure.offlineUsername = username;
	}

	/**
	 * checks online if the session is valid
	 */
	static boolean SessionValid() {
		try {
			final GameProfile gp = SessionUtil.get().getProfile();
			final String token = SessionUtil.get().getToken();
			final String id = UUID.randomUUID().toString();

			Secure.ymss.joinServer(gp, token, id);
			if (Secure.ymss.hasJoinedServer(gp, id, null).isComplete()) {
				Main.log.info("Session validation successful");
				return true;
			}
		} catch (Exception e) {
			Main.log.info("Session validation failed: " + e.getMessage());
			return false;
		}

		Main.log.info("Session validation failed!");
		return false;
	}

	private static final class SessionUtil {
		/**
		 * as the Session field in Minecraft.class is final we have to access it
		 * via reflection
		 */
		private static final Field sessionField = ObfuscationReflectionHelper.findField(Minecraft.class, "field_71449_j");

		private static Session get() {
			return Minecraft.getMinecraft().getSession();
		}

		static void set(final Session s) throws IllegalArgumentException, IllegalAccessException {
			SessionUtil.sessionField.set(Minecraft.getMinecraft(), s);
			GuiHandler.invalidateStatus();
		}
	}

	protected static class Account {
		private final String username;
		private final char[] password;
		private final String displayName;
		private long lastQuery = 0;
		private UUID uuid;

		Account(final String username, final char[] password, final UUID uuid, final String displayName) {
			this.username = username;
			this.password = password;
			this.uuid = uuid;
			this.displayName = displayName;
		}

		String getUsername() {
			return username;
		}

		char[] getPassword() {
			return password;
		}

		UUID getUuid() {
			return uuid;
		}

		public void setUuid(final UUID uuid) {
			this.uuid = uuid;
		}

		String getDisplayName() {
			return displayName;
		}

		public long getLastQuery() {
			return lastQuery;
		}

		public void setLastQuery(final long lastQuery) {
			this.lastQuery = lastQuery;
		}
	}
}
