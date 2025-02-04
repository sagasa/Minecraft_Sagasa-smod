package helper;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.math.RayTraceResult;
import org.la4j.LinearAlgebra;
import org.la4j.Matrix;
import org.la4j.Vector;

import net.minecraft.util.math.Vec3d;

public class HideCollisionDetector {

	float soutaiItiX = 0, soutaiItiY = 0, soutaiItiZ = 0, soutaiYaw = 0, soutaiPitch = 0;

	public List<Vec3d> collisionVec = new ArrayList<>();

	HideCollisionDetector(List<Vec3d> model) {
		collisionVec = model;
	}

	public void isHit(List<RayTraceResult> list, Vec3d startv, Vec3d endv) {
		for (int n = 0; n < collisionVec.size() / 3; n++) {
			//rayのベクトルを取得
			Vec3d ray = endv.subtract(startv);
			//コリジョンの2辺のベクトルを取得
			Vec3d vecA = new Vec3d(collisionVec.get(3 * n + 1).x - collisionVec.get(3 * n).x, collisionVec.get(3 * n + 1).y - collisionVec.get(3 * n).y, collisionVec.get(3 * n + 1).z - collisionVec.get(3 * n).z);
			Vec3d vecB = new Vec3d(collisionVec.get(3 * n + 2).x - collisionVec.get(3 * n).x, collisionVec.get(3 * n + 2).y - collisionVec.get(3 * n).y, collisionVec.get(3 * n + 2).z - collisionVec.get(3 * n).z);
			//AとBの外積を取得
			Vec3d normalVec = HideMathHelper.cross3dProduct(vecA, vecB);
			//単位ベクトル化
			Vec3d unitVec = HideMathHelper.normalize(normalVec);
			Vec3d origin = new Vec3d(collisionVec.get(3 * n).x, collisionVec.get(3 * n).y, collisionVec.get(3 * n).z);
			Vec3d fromCollisionSurfaceToStartVec = new Vec3d(startv.x - collisionVec.get(3 * n).x, startv.y - collisionVec.get(3 * n).y, startv.z - collisionVec.get(3 * n).z);
			if (HideMathHelper.innerProduct3d(ray, unitVec) != 0 && canIntersect(startv, endv, normalVec, origin)) {
				//コリジョンに届くrayを生成
				float t = -HideMathHelper.innerProduct3d(fromCollisionSurfaceToStartVec, unitVec) / HideMathHelper.innerProduct3d(ray, unitVec);
				if (t >= 0 && t <= 1) {
					//交点の座標を取得
					Vec3d crossingVec = ray.scale(t).add(startv);
					Matrix rayPosVec;
					Matrix ABi;
					if (Matrix.from2DArray(new double[][] { { vecA.x, vecB.x }, { vecA.y, vecB.y } }).determinant() != 0) {
						rayPosVec = Matrix.from2DArray(new double[][] { { crossingVec.x - collisionVec.get(3 * n).x, crossingVec.y - collisionVec.get(3 * n).y } }).transpose();
						ABi = Matrix.from2DArray(new double[][] { { vecA.x, vecB.x }, { vecA.y, vecB.y } }).withInverter(LinearAlgebra.INVERTER).inverse();
					} else if (Matrix.from2DArray(new double[][] { { vecA.y, vecB.y }, { vecA.z, vecB.z } }).determinant() != 0) {
						rayPosVec = Matrix.from2DArray(new double[][] { { crossingVec.y - collisionVec.get(3 * n).y, crossingVec.z - collisionVec.get(3 * n).z } }).transpose();
						ABi = Matrix.from2DArray(new double[][] { { vecA.y, vecB.y }, { vecA.z, vecB.z } }).withInverter(LinearAlgebra.INVERTER).inverse();
					} else if (Matrix.from2DArray(new double[][] { { vecA.z, vecB.z }, { vecA.x, vecB.x } }).determinant() != 0) {
						rayPosVec = Matrix.from2DArray(new double[][] { { crossingVec.z - collisionVec.get(3 * n).z, crossingVec.x - collisionVec.get(3 * n).x } }).transpose();
						ABi = Matrix.from2DArray(new double[][] { { vecA.z, vecB.z }, { vecA.x, vecB.x } }).withInverter(LinearAlgebra.INVERTER).inverse();
					} else {
						continue;
					}
					//vecAとvecBを基底としたときの成分が正かつ足して1以下なら衝突
					//Vec3d R = ABi.multiply(rayPosVec).getColumn(0);
					Vector R = ABi.multiply(rayPosVec).getColumn(0);
					if (R.get(0) >= 0 && R.get(1) >= 0 && R.get(0) + R.get(1) < 1) {
						System.out.println("GetCrossingVector! " + crossingVec);

						break;
					}
				}
			}
		}
	}

	public void isProximity(List<RayTraceResult> list, Vec3d startv, Vec3d endv, float radius) {
		for (int n = 0; n < collisionVec.size() / 3; n++) {
			Vec3d vecPos = new Vec3d(0, 0, 0);
			for (int m = 0; m < 3; m++) {
				vecPos.add(collisionVec.get(3 * n + m)).scale(0.3333333F);
			}
			Vec3d vec = vecPos.subtract(startv);
			Vec3d ray = endv.subtract(startv);
			Vec3d rayProjection = ray.scale(HideMathHelper.innerProduct3d(vec, ray) / ray.lengthSquared());
			Vec3d shortestToCollision = vec.subtract(rayProjection);
			if (shortestToCollision.lengthVector() - HideMathHelper.getDistance(collisionVec.get(3 * n), vecPos) < radius) {
				//do something
			}
		}
	}

	private boolean canIntersect(Vec3d startv, Vec3d endv, Vec3d normal, Vec3d origin) {
		return (HideMathHelper.innerProduct3d(normal, startv) - HideMathHelper.innerProduct3d(normal, origin)) * (HideMathHelper.innerProduct3d(normal, endv) - HideMathHelper.innerProduct3d(normal, origin)) <= 0;
	}
}
