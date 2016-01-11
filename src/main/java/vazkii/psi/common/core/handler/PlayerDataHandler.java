/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Psi Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 * 
 * Psi is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 * 
 * File Created @ [10/01/2016, 23:21:21 (GMT)]
 */
package vazkii.psi.common.core.handler;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;

public class PlayerDataHandler {

	private static HashMap<Integer, PlayerData> playerData = new HashMap();
	
	private static final String DATA_TAG = "PsiData";

	public static PlayerData get(EntityPlayer player) {
		int key = getKey(player);
		if(!playerData.containsKey(key))
			playerData.put(key, new PlayerData(player));
		
		return playerData.get(key);
	}
	
	public static void cleanup() {
		List<Integer> remove = new ArrayList();
		
		for(int i : playerData.keySet()) {
			PlayerData d = playerData.get(i);
			if(d.playerWR.get() == null)
				remove.add(i);
		}
		
		for(int i : remove)
			playerData.remove(i);
	}
	
	private static int getKey(EntityPlayer player) {
		return player.hashCode() << 1 + (player.worldObj.isRemote ? 1 : 0);
	}
	
	public static NBTTagCompound getDataCompoundForPlayer(EntityPlayer player) {
		NBTTagCompound forgeData = player.getEntityData();
		if(!forgeData.hasKey(EntityPlayer.PERSISTED_NBT_TAG))
			forgeData.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());

		NBTTagCompound persistentData = forgeData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
		if(!persistentData.hasKey(DATA_TAG))
			persistentData.setTag(DATA_TAG, new NBTTagCompound());

		return persistentData.getCompoundTag(DATA_TAG);
	}

	public static class PlayerData {

		private static final String TAG_LEVEL = "level";
		private static final String TAG_AVAILABLE_PSI = "availablePsi";
		private static final String TAG_REGEN_CD = "regenCd";

		public int level;
		public int availablePsi;
		public int regenCooldown;

		public final List<Deduction> deductions = new ArrayList();
		public final WeakReference<EntityPlayer> playerWR;
		private final boolean client;
		
		public PlayerData(EntityPlayer player) {
			playerWR = new WeakReference(player);
			client = player.worldObj.isRemote;
			
			load();
		}

		public void tick() {
			level = 1; // TODO Debug
			
			if(regenCooldown == 0) {
				int max = getTotalPsi();
				if(availablePsi < max && regenCooldown == 0) {
					availablePsi = Math.min(max, availablePsi + getRegenPerTick());
					save();
				}
			} else {
				regenCooldown--;
				save();
			}
			
			List<Deduction> remove = new ArrayList();
			for(Deduction d : deductions) {
				if(d.invalid)
					remove.add(d);
				else d.tick();
			}
			deductions.removeAll(remove);
		}

		public void deductPsi(int psi, int cd, boolean sync) {
			int currentPsi = availablePsi;
			availablePsi -= psi;
			if(regenCooldown < cd)
				regenCooldown = cd;
			
			if(availablePsi < 0) {
				int overflow = -availablePsi;
				availablePsi = 0;
				
				// TODO Use CAD batteries
				
				float dmg = (float) overflow / 50;
				if(!client) {
					EntityPlayer player = playerWR.get();
					if(player != null)
						player.attackEntityFrom(DamageSource.magic, dmg); // TODO better DS
				}
			}

			if(sync) {
				if(client)
					addDeduction(currentPsi, psi, cd);
				else {
					// TODO Sync
				}
			}
			
			save(); 
		}
		
		public void addDeduction(int current, int deduct, int cd) {
			if(deduct > current)
				deduct = current;
			if(deduct < 0)
				deduct = 0;
			
			if(deduct == 0)
				return;
			
			deductions.add(new Deduction(current, deduct, 20));
		}
		
		public int getTotalPsi() {
			return level * 200;
		}

		public int getRegenPerTick() {
			return level;
		}

		public void save() {
			if(!client) {
				EntityPlayer player = playerWR.get();

				if(player != null) {
					NBTTagCompound cmp = getDataCompoundForPlayer(player);
					cmp.setInteger(TAG_LEVEL, level);
					cmp.setInteger(TAG_AVAILABLE_PSI, availablePsi);
					cmp.setInteger(TAG_REGEN_CD, regenCooldown);	
				}
			}
		}

		public void load() {
			if(!client) {
				EntityPlayer player = playerWR.get();

				if(player != null) {
					NBTTagCompound cmp = getDataCompoundForPlayer(player);
					level = cmp.getInteger(TAG_LEVEL);
					availablePsi = cmp.getInteger(TAG_AVAILABLE_PSI);
					regenCooldown = cmp.getInteger(TAG_REGEN_CD);
				}
			}
		}
		
		public static class Deduction {
			
			public final int current; 
			public final int deduct; 
			public final int cd;
			
			public int elapsed;
			
			public boolean invalid;
			
			public Deduction(int current, int deduct, int cd) {
				this.current = current;
				this.deduct = deduct;
				this.cd = cd;
			}
			
			public void tick() {
				elapsed++;
				
				if(elapsed >= cd)
					invalid = true;
			}
			
			public float getPercentile(float partTicks) {
				return 1F - Math.min(1F, (elapsed + partTicks) / cd);
			}
		}
		
	}
}