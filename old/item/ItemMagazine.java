package item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.realmsclient.gui.ChatFormatting;

import helper.NBTWrapper;
import hideMod.PackData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import types.items.MagazineData;

public class ItemMagazine extends Item {

	public static Map<String, ItemMagazine> INSTANCE_MAP = new HashMap<>();

	public MagazineData MagazineData;

	// ========================================================================
	// 登録
	public ItemMagazine(MagazineData data) {
		super();
		this.setCreativeTab(CreativeTabs.COMBAT);
		String name = data.ITEM_SHORTNAME;
		this.setUnlocalizedName(name);
		this.setRegistryName(name);
		this.setMaxStackSize(data.ITEM_STACK_SIZE);
		this.MagazineData = data;
		INSTANCE_MAP.put(name, this);
	}

	/** アイテムスタックを作成 残弾指定 */
	public static ItemStack makeMagazine(String name, int ammoNum) {
		return NBTWrapper.setMagazineBulletNum(makeMagazine(name), ammoNum);
	}

	/** アイテムスタック作成時に呼ばれる これの中でNBTを設定する */
	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
		setBulletNBT(stack);
		return super.initCapabilities(stack, nbt);
	}

	/** アイテムスタックを作成 */
	public static ItemStack makeMagazine(String name) {
		if (PackData.MAGAZINE_DATA_MAP.containsKey(name)) {
			ItemStack stack = new ItemStack(INSTANCE_MAP.get(name));
			stack.setTagCompound(new NBTTagCompound());
			return setBulletNBT(stack);
		}
		return null;
	}

	/** どのような状態からでも有効なNBTを書き込む */
	public static ItemStack setBulletNBT(ItemStack item) {
		if (!(item.getItem() instanceof ItemMagazine)) {
			return item;
		}
		MagazineData data = getMagazineData(item);
		NBTWrapper.setMagazineBulletNum(item, data.MAGAZINE_SIZE);
		return item;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return getMagazineData(stack).ITEM_DISPLAYNAME;
	}

	// =========================================================
	// 更新 便利機能
	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		return getMagazineSize(stack) > getBulletNum(stack);
	}

	@Override
	public int getDamage(ItemStack stack) {
		return getMagazineSize(stack) - getBulletNum(stack);
	}

	@Override
	public int getMaxDamage(ItemStack stack) {
		return getMagazineSize(stack);
	}

	@Override
	public boolean isDamaged(ItemStack stack) {
		return false;
	}

	public static boolean isMagazine(ItemStack item, String str) {
		if (item != null && item.getItem() instanceof ItemMagazine
				&& ItemMagazine.getMagazineData(item).ITEM_SHORTNAME.equals(str)) {
			return true;
		}
		return false;
	}

	public static boolean isMagazine(ItemStack item, String str, int size) {
		if (item != null && item.getItem() instanceof ItemMagazine
				&& ItemMagazine.getMagazineData(item).ITEM_SHORTNAME.equals(str)
				&& NBTWrapper.getMagazineBulletNum(item) == size) {
			return true;
		}
		return false;
	}

	/** 残弾数取得 */
	public static int getBulletNum(ItemStack stack) {
		return NBTWrapper.getMagazineBulletNum(stack);
	}

	/** 残弾数書き込み */
	public static ItemStack setBulletNum(ItemStack stack, int num) {
		NBTWrapper.setMagazineBulletNum(stack, num);
		return stack;
	}

	/** 装弾数取得 */
	public static int getMagazineSize(ItemStack stack) {
		return getMagazineData(stack).MAGAZINE_SIZE;
	}

	/** アップデート 表示更新など */
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
		if (!stack.hasTagCompound()) {
			return;
		}
		tooltip.add(ChatFormatting.GRAY + "Ammo : " + getBulletNum(stack) + "/" + getMagazineSize(stack));
	}

	/** 表示名取得 */
	public static String getMagazineName(String name) {
		MagazineData data = PackData.MAGAZINE_DATA_MAP.get(name);
		return data == null ? "None" : data.ITEM_DISPLAYNAME;
	}

	/** BulletData取得 */
	public static MagazineData getMagazineData(String name) {
		return PackData.MAGAZINE_DATA_MAP.get(name);
	}

	/** BulletData取得 */
	public static MagazineData getMagazineData(ItemStack item) {
		if (!(item.getItem() instanceof ItemMagazine)) {
			return null;
		}
		return ((ItemMagazine) item.getItem()).MagazineData;
	}

	/** その名前の弾は存在するか */
	public static boolean isMagazineExist(String name) {
		return PackData.MAGAZINE_DATA_MAP.containsKey(name);
	}
}
