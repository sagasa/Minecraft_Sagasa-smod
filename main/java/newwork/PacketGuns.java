package newwork;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import entity.EntityBullet;
import handler.PacketHandler;
import handler.PlayerHandler;
import helper.NBTWrapper;
import helper.ParseByteArray;
import hideMod.PackLoader;
import io.netty.buffer.ByteBuf;
import item.ItemGun;
import item.ItemMagazine;
import net.minecraft.block.BlockStone;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAnvilBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import scala.actors.threadpool.Arrays;
import types.BulletData;
import types.BulletData.BulletDataList;
import types.GunData;
import types.GunData.GunDataList;
import types.GunFireMode;
import types.LoadedMagazine;

/** 送受信両用 */
public class PacketGuns implements IMessage, IMessageHandler<PacketGuns, IMessage> {

	byte mode;
	static final byte GUN_SHOOT = 0;
	static final byte GUN_RELOAD_REQ = 1;
	static final byte GUN_RELOAD_REPLY = 2;
	static final byte GUN_MODE = 3;
	// 射撃パケット
	float Yaw;
	float Pitch;
	GunData gunData;
	BulletData bulletData;
	// リロードパケット
	String bulletName;
	int bulletAmount;
	//
	byte ReloadQueueID;
	int bulletNum;
	// モードパケット
	GunFireMode fireMode;
	//NBTアップデート
	byte slot;
	LoadedMagazine[] NBT_loadedMagazines;
	String NBT_UsingBulletName;
	int NBT_ShootDelay;
	int NBT_ReloadProgress;
	String NBT_fireMode;

	public PacketGuns() {
	}

	/** 射撃パケット 視線とプレイヤーインスタンス弾の名前のセット */
	public PacketGuns(GunData data, float yaw, float pitch) {
		this.mode = GUN_SHOOT;
		this.Yaw = yaw;
		this.Pitch = pitch;
		this.gunData = data;
	}

	/** リロードリクエスト 弾の登録名 カレントスロット 要求量 */
	public PacketGuns(String name,byte queue,int amount) {
		this.mode = GUN_RELOAD_REQ;
		this.bulletAmount = amount;
		this.bulletName = name;
		this.ReloadQueueID = queue;
	}
	/** リロード確定 弾の登録名
	 * @param bulletNum 弾のリクエストの量
	 * @param queue キューの元のカレントスロット*/
	public PacketGuns(int bulletNum,byte queue) {
		this.mode = GUN_RELOAD_REPLY;
		this.bulletNum = bulletNum;
		this.ReloadQueueID = queue;
	}


	@Override // ByteBufからデータを読み取る。
	public void fromBytes(ByteBuf buf) {
		this.mode = buf.readByte();
		if (mode == GUN_SHOOT) {
			this.Yaw = buf.readFloat();
			this.Pitch = buf.readFloat();
			byte length = buf.readByte();
			this.gunData = PackLoader.GUN_DATA_MAP.get(buf.readBytes(length).toString(Charset.forName("UTF-8")));
		}
		if (mode == GUN_RELOAD_REQ) {
			byte length = buf.readByte();
			this.bulletName = buf.readBytes(length).toString(Charset.forName("UTF-8"));
			this.ReloadQueueID = buf.readByte();
			this.bulletAmount = buf.readInt();
		}
		if (mode == GUN_RELOAD_REPLY) {
			this.bulletNum = buf.readInt();
			this.ReloadQueueID = buf.readByte();
		}
		if (mode == GUN_MODE) {

		}
	}

	@Override // ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf) {
		buf.writeByte(mode);
		if (mode == GUN_SHOOT) {
			buf.writeFloat(Yaw);
			buf.writeFloat(Pitch);
			// 長さと一緒に文字列を送る
			String gunDataName = gunData.getDataString(GunDataList.SHORT_NAME);
			buf.writeByte(gunDataName.length());
			buf.writeBytes(gunDataName.getBytes());
			/*
			 * String bulletDataName =
			 * bulletData.getDataString(BulletDataList.SHORT_NAME);
			 * buf.writeByte(bulletDataName.length());
			 * buf.writeBytes(bulletDataName.getBytes());
			 */
		}
		if (mode == GUN_RELOAD_REQ) {
			buf.writeByte(bulletName.length());
			buf.writeBytes(bulletName.getBytes());
			buf.writeByte(ReloadQueueID);
			buf.writeInt(bulletAmount);
		}
		if (mode == GUN_RELOAD_REPLY) {
			buf.writeInt(bulletNum);
			buf.writeByte(ReloadQueueID);
		}
	}

	// 受信イベント
	@Override // IMessageHandlerのメソッド
	public IMessage onMessage(final PacketGuns m, final MessageContext ctx) {
		// クライアントへ送った際に、EntityPlayerインスタンスはこのように取れる。
		// EntityPlayer player =
		// SamplePacketMod.proxy.getEntityPlayerInstance();
		// サーバーへ送った際に、EntityPlayerインスタンス（EntityPlayerMPインスタンス）はこのように取れる。
		// EntityPlayer Player = ctx.getServerHandler().playerEntity;
		// System.out.println(ctx.side);
		if (ctx.side == Side.SERVER) {
			ctx.getServerHandler().playerEntity.getServerForPlayer().addScheduledTask(new Runnable() {
				public void run() {
					processMessage(m);
				}
				private void processMessage(PacketGuns m) {
					// 射撃
					EntityPlayer Player = ctx.getServerHandler().playerEntity;
					if (m.mode == GUN_SHOOT) {
						ItemStack item = Player.inventory.getCurrentItem();
						if(ItemGun.isGun(item)){
							// MBT書き換え
							LoadedMagazine[] magazines = NBTWrapper.getGunLoadedMagazines(item);
							for (int i = 0; i < magazines.length; i++) {
								LoadedMagazine magazine = magazines[i];
								// 1つ消費する
								if (magazine != null && magazine.num > 0) {
									String name = magazine.name;
									magazine.num--;
									if (magazine.num <= 0) {
										magazine = null;
									}
									magazines[i] = magazine;
									break;
								}
							}
							NBTWrapper.setGunLoadedMagazines(item, magazines);
							// 弾を発射
							EntityBullet bullet = new EntityBullet(Player.worldObj, Player, m.gunData, m.Yaw, m.Pitch);
							Player.worldObj.spawnEntityInWorld(bullet);
						}
					}
					if (m.mode == GUN_RELOAD_REQ) {
						int num = ItemMagazine.ReloadItem(Player, m.bulletName,m.bulletAmount);
						int amount = num;
						ItemStack item = Player.inventory.getCurrentItem();
						if(ItemGun.isGun(item)){
							// MBT書き換え
							LoadedMagazine[] magazines = NBTWrapper.getGunLoadedMagazines(item);
							for (int i = 0; i < magazines.length; i++) {
								LoadedMagazine magazine = magazines[i];
								// 入ってなければ追加
								if (magazine == null) {
									magazines[i] = new LoadedMagazine(m.bulletName, amount);
									break;
								}
								int num2 = ItemMagazine.getBulletData(magazine.name).getDataInt(BulletDataList.MAGAZINE_SIZE) - magazine.num;
								if (num2 > 0 && magazine.name.equals(m.bulletName)) {
									magazines[i].num += amount;
									break;
								}
							}
							NBTWrapper.setGunLoadedMagazines(item, magazines);
							//Player.inventory.mainInventory[Player.inventory.currentItem] = item;
							// 返信
							PacketHandler.INSTANCE.sendTo(new PacketGuns(num,m.ReloadQueueID), (EntityPlayerMP) Player);
						}
					}
				}
			});
		}else{
			if (m.mode == GUN_RELOAD_REPLY) {
				PlayerHandler.reloadEnd(m.bulletNum,m.ReloadQueueID);
			}
		}

		return null;
	}
}
