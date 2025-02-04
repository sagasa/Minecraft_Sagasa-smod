package hide.model.gltf;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import hide.model.impl.AccessorImpl;
import hide.model.impl.IAnimation;
import hide.model.impl.NodeImpl;

class Animation implements IAnimation {

	private List<Channel> channels;
	private List<Sampler> samplers;

	transient private float minKey;
	transient private float maxKey;

	public Animation register(GltfLoader loader) {
		for (Sampler sampler : samplers) {
			sampler.register(loader);
		}
		for (Channel channel : channels) {
			channel.sampler = samplers.get(channel.samplerIndex);
			channel.register(loader);
		}

		float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
		for (Channel channel : channels) {
			min = Math.min(channel.sampler.input.getMin()[0].floatValue(), min);
			max = Math.max(channel.sampler.input.getMax()[0].floatValue(), max);
		}
		maxKey = max;
		minKey = min;
		System.out.println(min + " " + max);

		return this;
	}

	public static class Sampler {
		@SerializedName("input")
		private int inputIndex;
		@SerializedName("output")
		private int outputIndex;

		private Interpolation interpolation;

		transient private AccessorImpl input;
		transient private AccessorImpl output;

		public void register(GltfLoader loader) {
			input = loader.getAccessor(inputIndex);
			output = loader.getAccessor(outputIndex);
		}
	}

	public static class Target {
		@SerializedName("node")
		private int nodeIndex;

		private AnimationPath path;

		transient private NodeImpl node;

		public void register(GltfLoader loader) {
			node = loader.getNode(nodeIndex);
		}
	}

	static Gson testGson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithModifiers(Modifier.STATIC).serializeNulls().create();

	public static class Channel {
		@SerializedName("sampler")
		private int samplerIndex;
		@SerializedName("target")
		private Target target;

		transient private Sampler sampler;

		float[] times;
		int elementCount;

		public void register(GltfLoader loader) {
			target.register(loader);

			AccessorImpl input = sampler.input;
			times = new float[input.getCount()];
			for (int i = 0; i < input.getCount(); i++) {
				times[i] = input.getBuffer().getFloat(input.getByteIndex(i, 0));
			}

			AccessorImpl outAccessor = sampler.output;
			elementCount = target.path == AnimationPath.weights ? outAccessor.getCount() / times.length : outAccessor.getElementType().size;
		}

		/**key以下の最大のIndex*/
		private int getIndex(float key) {
			int index = Arrays.binarySearch(times, key);
			return index >= 0 ? index : Math.max(0, -index - 2);
		}

		void apply(float key) {
			int index0 = getIndex(key);
			int index1 = Math.min(times.length - 1, index0 + 1);
			float alpha = getAlpha(key, index0);
			//System.out.println("apply "+sampler.output.getBuffer().order() +" "+index0+" "+index1+" / "+times.length);
			switch (sampler.interpolation) {
			case CUBICSPLINE:
				//TODO
				break;
			case LINEAR:
				linearInterpolator(index0, index1, alpha);
				break;
			case STEP:
				//TODO
				break;
			}

		}

		private float getAlpha(float key, int index) {
			if (key <= times[0]) {
				return 0.0f;
			}
			if (key >= times[times.length - 1]) {
				return 1.0f;
			}
			float local = key - times[index];
			float delta = times[index + 1] - times[index];
			float alpha = local / delta;
			return alpha;
		}

		float[] gen() {
			float[] value = null;
			switch (target.path) {
			case rotation:
				value = new float[4];
				break;
			case scale:
				value = new float[3];
				break;
			case translation:
				value = new float[3];
				break;
			case weights:
				value = new float[elementCount];
				break;
			}
			return value;

		}

		void set(float[] value) {
			switch (target.path) {
			case rotation:
				target.node.setRotation(value);
				break;
			case scale:
				target.node.setScale(value);
				break;
			case translation:
				target.node.setTranslation(value);
				break;
			case weights:
				target.node.setWeights(value);
				break;
			}
		}

		void linearInterpolator(int index0, int index1, float alpha) {
			float[] value = gen();
			AccessorImpl out = sampler.output;

			for (int i = 0; i < elementCount; i++) {
				float a = out.getBuffer().getFloat(out.getByteIndex(index0, i));
				float b = out.getBuffer().getFloat(out.getByteIndex(index1, i));//TODO SUS

				//System.out.println(a+" "+b+" "+out.getByteIndex(index0, i)+" "+sampler.outputIndex+" ");
				value[i] = a + alpha * (b - a);
			}
			set(value);
		}

	}

	/**0-1*/
	@Override
	public void apply(float value) {
		float key = minKey + value * (maxKey - minKey);
		for (Channel channel : channels) {
			channel.apply(key);
		}
	}

	public enum Interpolation {
		CUBICSPLINE, LINEAR, STEP;
	}

	public enum AnimationPath {
		translation, rotation, scale, weights
	}
}
