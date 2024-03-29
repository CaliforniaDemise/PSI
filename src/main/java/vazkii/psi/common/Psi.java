/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Psi Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: http://psi.vazkii.us/license.php
 *
 * File Created @ [08/01/2016, 21:19:53 (GMT)]
 */
package vazkii.psi.common;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import vazkii.psi.common.command.CommandPsiLearn;
import vazkii.psi.common.command.CommandPsiUnlearn;
import vazkii.psi.common.core.proxy.CommonProxy;
import vazkii.psi.common.lib.LibMisc;

@SuppressWarnings("unused")
@Mod(modid = LibMisc.MOD_ID, name = "Psi Unhinged", version = LibMisc.VERSION, guiFactory = "vazkii.psi.client.core.proxy.GuiFactory", dependencies = "required-after:autoreglib")
public class Psi {

	@Instance(LibMisc.MOD_ID)
	public static Psi instance;
	
	public static boolean magical;

	@SidedProxy(serverSide = "vazkii.psi.common.core.proxy.CommonProxy", clientSide = "vazkii.psi.client.core.proxy.ClientProxy")
	public static CommonProxy proxy;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		magical = Loader.isModLoaded("magipsi");
		proxy.preInit(event);
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.init(event);
	}

	@EventHandler
	public void serverStartingEvent(FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandPsiLearn());
		event.registerServerCommand(new CommandPsiUnlearn());
	}

}
