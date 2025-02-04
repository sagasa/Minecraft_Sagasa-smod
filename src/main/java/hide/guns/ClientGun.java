package hide.guns;

import handler.client.HideViewHandler;
import helper.HideMath;
import hide.guns.data.LoadedMagazine;
import hide.guns.gui.RecoilHandler;
import hide.guns.network.PacketShoot;
import hide.types.guns.GunFireMode;
import hide.types.guns.ProjectileData;
import hide.types.items.MagazineData;
import hide.ux.HideSoundManager;
import hidemod.HideMod;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ClientGun extends CommonGun {

	public ClientGun(EnumHand hand) {
		super(hand);
	}

	private double X;
	private double Y;
	private double Z;
	private float Yaw;
	private float Pitch;
	private double oldX;
	private double oldY;
	private double oldZ;
	private float oldYaw;
	private float oldPitch;

	/** 弾の出現点を設定 */
	public ClientGun setPos(double x, double y, double z) {
		//System.out.println(x+" "+y+" "+z + " "+System.currentTimeMillis()+" "+hand);
		oldX = X;
		oldY = Y;
		oldZ = Z;
		X = x;
		Y = y;
		Z = z;
		return this;
	}

	/** 弾の向きを設定 */
	public ClientGun setRotate(float yaw, float pitch) {
		oldYaw = Yaw;
		oldPitch = Pitch;
		Yaw = yaw;
		Pitch = pitch;
		return this;
	}

	private long lastTime = -1;
	private int shootDelay = 0;
	private int shootNum = 0;
	private boolean stopShoot = false;

	/** このTickで射撃可能かどうか */
	public boolean canShoot() {
		return magazine.getLoadedNum() > 0 && !stopShoot && shootDelay <= 0 & shootNum <= 0;
	}

	private float completionTick = 0;

	public void stopShoot() {
		stopShoot=true;
	}

	/** 50Hzのアップデート処理プレイヤー以外はこのメゾットを使わない */
	@SideOnly(Side.CLIENT)
	public void gunUpdate(boolean trigger, float completion) {
		completionTick = completion;

		if (!isGun())
			return;

		if (lastTime == -1)
			lastTime = Minecraft.getSystemTime();
		if (0 < shootDelay)
			shootDelay -= Minecraft.getSystemTime() - lastTime;
		lastTime = Minecraft.getSystemTime();
		if (!trigger)
			stopShoot = false;
		GunFireMode firemode = getFireMode();

		if (firemode == GunFireMode.SEMIAUTO && !stopShoot && shootDelay <= 0 && trigger) {
			if (shootDelay < 0) {
				shootDelay = 0;
			}
			shoot(MillistoTick(shootDelay));
			shootDelay += RPMtoMillis(dataView.get(ProjectileData.RPM));
			stopShoot = true;
		} else if (firemode == GunFireMode.FULLAUTO && !stopShoot && shootDelay <= 0 && trigger) {
			while (shootDelay <= 0 && !stopShoot) {
				shoot(MillistoTick(shootDelay));
				shootDelay += RPMtoMillis(dataView.get(ProjectileData.RPM));
			}
		} else if (firemode == GunFireMode.BURST && !stopShoot) {
			// 射撃開始
			if (trigger && shootNum == -1 && shootDelay <= 0 && !stopShoot) {
				shootNum = dataView.get(ProjectileData.BurstCount);
			}
			while (shootNum > 0 && shootDelay <= 0 && !stopShoot) {
				shoot(MillistoTick(shootDelay));
				shootDelay += RPMtoMillis(dataView.get(ProjectileData.BurstRPM));
				shootNum--;
			}
			if (shootNum == 0) {
				stopShoot = true;
				shootNum = -1;
				shootDelay += RPMtoMillis(dataView.get(ProjectileData.RPM));
			}
			if (stopShoot) {
				shootNum = -1;
			}

		} else if (firemode == GunFireMode.MINIGUN && !stopShoot && shootDelay <= 0 && trigger) {
			while (shootDelay <= 0 && !stopShoot) {
				shoot(MillistoTick(shootDelay));
				shootDelay += RPMtoMillis(dataView.get(ProjectileData.RPM));
			}
		}
	}

	/** 射撃リクエスト */
	private void shoot(float offset) {
		MagazineData bullet = magazine.useNextBullet();
		if (bullet != null) {
			// クライアントなら
			Minecraft mc = Minecraft.getMinecraft();
			boolean isADS = HideViewHandler.isADS;

			offset += completionTick;

			HideSoundManager.playSound(mc.player, 0, 0, 0, dataView.getData(ProjectileData.SoundShoot));

			RecoilHandler.addRecoil(dataView.getView(), hand);
			double x = HideMath.completion(oldX, X, offset);
			double y = HideMath.completion(oldY, Y, offset);
			double z = HideMath.completion(oldZ, Z, offset);
			float yaw = HideMath.completion(oldYaw, Yaw, offset);
			float pitch = HideMath.completion(oldPitch, Pitch, offset);

			HideMod.NETWORK.sendToServer(
					new PacketShoot(isADS, offset, x, y, z, yaw, pitch, hand == EnumHand.MAIN_HAND));

		} else {
			stopShoot = true;
		}
	}

	public void syncMag() {
		if (!isGun())
			return;
		magazine = HideGunNBT.GUN_MAGAZINES.get(gun);
	}

	public void setMagazine(LoadedMagazine magazine) {
		this.magazine = magazine;
	}

	@Override
	public void tickUpdate() {
		if (!isGun())
			return;

		//LoadedMagazine mag = HideNBT.getGunLoadedMagazines(gun.getGunTag());
		//if (magazine.getList().size() != mag.getList().size())
		//	magazine = mag;

	}

	@Override
	protected void updateData() {
		if (!isGun())
			return;
		syncMag();
		shootDelay = HideGunNBT.GUN_SHOOTDELAY.get(gun);
	}

}
