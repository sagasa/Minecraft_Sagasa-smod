package hide.guns.data;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nonnull;

import hide.types.items.MagazineData;
import pack.PackData;

/** 装填済みのマガジン管理用 変更通知機能付き */
public class LoadedMagazine {
	private List<Magazine> magazineList = new CopyOnWriteArrayList<>();

	/** 単体のマガジン */
	public static class Magazine {
		public static final Magazine EMPTY_MAG = new Magazine(null, 0);
		public String name;
		public int num;

		public Magazine(String name, int num) {
			this.name = name;
			this.num = num;
		}

		@Override
		public String toString() {
			return "[name=" + name + ",num=" + num + "]";
		}

		public boolean magEquals(Magazine obj) {
			return obj != null && name.equals(obj.name) && num == obj.num;
		}
	}

	/** 次に撃つ弾を取得 */
	public MagazineData getNextBullet() {
		Magazine mag = getNextMagazine(true);
		if (mag == null) {
			return null;
		}
		return PackData.getBulletData(mag.name);
	}

	/** 次に撃つ弾を取得 消費する */
	public MagazineData useNextBullet() {
		Magazine mag = getNextMagazine(true);
		if (mag == null) {
			return null;
		}
		return PackData.getBulletData(mag.name);
	}

	/** 初めの弾が入ったマガジンを返す */
	private Magazine getNextMagazine(boolean use) {
		int last = magazineList.size() - 1;
		if (last < 0)
			return null;
		if (magazineList.get(last).num > 0) {
			// 存在しない弾の場合の例外処理
			if (PackData.getBulletData(magazineList.get(last).name) == null) {
				magazineList.remove(last);
				return getNextMagazine(use);
			}
			// 弾の消費処理
			Magazine mag = magazineList.get(last);
			mag.num--;
			if (magazineList.size() > 0 && mag.num <= 0 && PackData.getBulletData(mag.name).get(MagazineData.MagazineBreak)) {
				magazineList.remove(last);
			}
			return mag;
		}
		if (magazineList.get(last).num <= 0 && PackData.getBulletData(magazineList.get(last).name).get(MagazineData.MagazineBreak)) {
			// System.out.println("Size "+ magazineList.size());
			magazineList.remove(last);
		}
		return null;
	}

	public void removeEmpty() {
		Iterator<Magazine> itr = magazineList.iterator();
		while (itr.hasNext()) {
			Magazine mag = itr.next();
			if (PackData.getBulletData(mag.name) == null || mag.num <= 0)
				itr.remove();
		}
	}

	public void addMagazinetoFast(@Nonnull Magazine mag) {
		magazineList.add(0, mag);
	}

	public void addMagazinetoLast(@Nonnull Magazine mag) {
		magazineList.add(mag);
	}

	public List<Magazine> getList() {
		return magazineList;
	}

	@Override
	public String toString() {
		return magazineList.toString();
	}

	public boolean magEquals(LoadedMagazine loadmag) {
		if (loadmag != null && magazineList.size() == loadmag.magazineList.size()) {
			for (int i = 0; i < magazineList.size(); i++)
				if (!magazineList.get(i).magEquals(loadmag.magazineList.get(i)))
					return false;
			return true;
		}
		return false;
	}

	/** 今の残弾を返す */
	public int getLoadedNum() {
		int num = 0;
		for (Magazine magazine : magazineList) {
			if (magazine != null) {
				num += magazine.num;
			}
		}
		return num;
	}

	/**最小のマガジン装填率のマガジンを返す ぜんぶFullならnull*/
	public Magazine getMinMag() {
		//最小のマガジンを検出
		float min = 1f;
		Magazine minMag = null;
		Iterator<Magazine> itr = magazineList.iterator();
		while (itr.hasNext()) {
			Magazine mag = itr.next();
			MagazineData magData = PackData.getBulletData(mag.name);
			//存在しないマガジンなら排出
			if (magData == null) {
				itr.remove();
			} else {
				float dia = mag.num / (float) magData.get(MagazineData.MagazineSize);
				if (dia < min) {
					min = dia;
					minMag = mag;
				}
			}
		}
		return minMag;
	}
}