package network;

import guns.Gun;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/** プレイヤーによるクライアントからの射撃リクエスト */
public class PacketShoot implements IMessage, IMessageHandler<PacketShoot, IMessage> {

	double x;
	double y;
	double z;
	float yaw;
	float pitch;
	float offset;
	boolean isADS;
	long uid;
	double worldTime;

	public PacketShoot() {
	}

	public PacketShoot(boolean isADS, float offset, double x, double y, double z, float yaw, float pitch, long hideID) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.offset = offset;
		this.isADS = isADS;
		this.uid = hideID;
		this.worldTime = Minecraft.getMinecraft().player.world.getTotalWorldTime();
	}

	@Override // ByteBufからデータを読み取る。
	public void fromBytes(ByteBuf buf) {
		x = buf.readDouble();
		y = buf.readDouble();
		z = buf.readDouble();
		yaw = buf.readFloat();
		pitch = buf.readFloat();
		offset = buf.readFloat();
		isADS = buf.readBoolean();
		worldTime = buf.readDouble();
		uid = buf.readLong();
	}

	@Override // ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf) {
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(yaw);
		buf.writeFloat(pitch);
		buf.writeFloat(offset);
		buf.writeBoolean(isADS);
		buf.writeDouble(worldTime);
		buf.writeLong(uid);
	}

	// 受信イベント
	@Override // IMessageHandlerのメソッド
	public IMessage onMessage(PacketShoot m, MessageContext ctx) {
		// クライアントへ送った際に、EntityPlayerインスタンスはこのように取れる。
		// EntityPlayer player =
		// SamplePacketMod.proxy.getEntityPlayerInstance();
		// サーバーへ送った際に、EntityPlayerインスタンス（EntityPlayerMPインスタンス）はこのように取れる。
		// EntityPlayer Player = ctx.getServerHandler().playerEntity;
		// System.out.println(ctx.side);
		if (ctx.side == Side.SERVER) {
			ctx.getServerHandler().player.getServer().addScheduledTask(new Runnable() {
				public void run() {
					processMessage(m);
				}
				private void processMessage(PacketShoot m) {
					EntityPlayer player = ctx.getServerHandler().player;
					double lag = player.world.getTotalWorldTime() - m.worldTime;
					lag = lag < 0 ? 0 : lag;
					//	System.out.println("lag = " + lag);
					// System.out.println("射撃パケット受信" + (m.offset + (float) lag));
					Gun.shoot(player, m.uid, m.offset + (float) lag, m.isADS, m.x, m.y, m.z, m.yaw, m.pitch);
				}
			});
		}
		return null;
	}
}
