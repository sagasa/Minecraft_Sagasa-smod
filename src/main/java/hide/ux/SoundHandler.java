package hide.ux;

import hide.types.effects.Sound;
import hide.types.util.DataView.ViewCache;
import hide.ux.network.PacketPlaySound;
import hidemod.HideMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SoundHandler {
	/** 再生リクエストを送信 サーバーサイドで呼んでください 射撃音など遠距離まで聞こえる必要がある音に使用*/
	public static void broadcastSound(Entity e, double x, double y, double z, ViewCache<Sound> sound, boolean excepting) {
		broadcastSound(e, x, y, z, sound, excepting, (byte) 0);
	}

	/** 再生リクエストを送信 サーバーサイドで呼んでください 射撃音など遠距離まで聞こえる必要がある音に使用*/
	public static void broadcastSound(Entity e, double x, double y, double z, ViewCache<Sound> viewCache, boolean excepting,
			byte cate) {
		broadcastSound(e.world, e, viewCache.get(Sound.Name), x, y, z, viewCache.get(Sound.Range), viewCache.get(Sound.Volume), viewCache.get(Sound.Pitch),
				viewCache.get(Sound.UseDelay),
				viewCache.get(Sound.UseDecay), excepting, cate);
	}

	/** 再生リクエストを送信 サーバーサイドで呼んでください 射撃音など遠距離まで聞こえる必要がある音に使用 */
	public static void broadcastSound(World world, Entity e, String soundName, double x, double y, double z,
			float range, float vol,
			float pitch, boolean useDelay, boolean useDecay, boolean excepting, byte cate) {
		// 同じワールドのプレイヤーの距離を計算してパケットを送信
		for (EntityPlayer player : world.playerEntities) {
			double distance = new Vec3d(e.posX, e.posY, e.posZ)
					.distanceTo(new Vec3d(player.posX, player.posY, player.posZ));
			if ((!excepting || player != e) && distance < range) {
				// パケット
				HideMod.NETWORK.sendTo(
						new PacketPlaySound(e.getEntityId(), cate, soundName, x, y, z, vol, pitch, range, useDelay, useDecay),
						(EntityPlayerMP) player);
			}
		}
	}

	/**カテゴリが設定された音のキャンセルリクエスト*/
	public static void bloadcastCancel(World world, int entity, byte cate) {
		for (EntityPlayer player : world.playerEntities)
			HideMod.NETWORK.sendTo(new PacketPlaySound(entity, cate), (EntityPlayerMP) player);
	}
}
