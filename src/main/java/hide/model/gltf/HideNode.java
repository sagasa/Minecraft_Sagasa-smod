package hide.model.gltf;

import static hide.model.gltf.TransformMatUtil.*;

import java.nio.FloatBuffer;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.google.gson.annotations.SerializedName;

import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SkinModel;
import hide.model.gltf.Model.HideShader;
import hide.model.gltf.animation.Skin;
import hide.model.gltf.base.Accessor;
import hide.model.gltf.base.IDisposable;
import net.minecraft.client.renderer.GlStateManager;

public class HideNode implements IDisposable {

	@SerializedName("children")
	private int[] childrenIndex = ArrayUtils.EMPTY_INT_ARRAY;
	@SerializedName("skin")
	private int skinIndex = -1;
	@SerializedName("mesh")
	private int meshIndex = -1;
	private float[] matrix;
	private float[] rotation;
	private float[] scale;
	private float[] translation;
	private float[] weights;

	transient private float[] globalMatrix;
	transient private boolean isValidLocalMat = false;
	transient private boolean isValidGlobalMat = false;
	transient private boolean isValidWeight = false;

	transient private boolean hasMesh;
	transient private boolean useWeight;
	transient private boolean useSkin;

	transient private Mesh mesh;
	transient private Skin skin;

	public Skin getSkin() {
		return skin;
	}

	transient HideNode[] children = new HideNode[0];
	transient HideNode parent = null;

	public HideNode getParent() {
		return parent;
	}

	transient public NodeModel nodeModel_;
	transient private HideShader shader;

	//skin render
	transient SkinModel skinModel_;
	transient private Accessor inverseMatrices;
	transient private FloatBuffer boneMat;

	//モーフィング
	transient private boolean hasWeight;

	public HideNode register(GltfLoader loader) {
		for (int i = 0; i < childrenIndex.length; i++) {
			HideNode child = loader.getNode(childrenIndex[i]);
			child.parent = this;
			children[i] = child;
		}

		hasMesh = meshIndex != -1;

		if (hasMesh) {
			mesh = loader.getMesh(meshIndex);
		} else
			return this;

		hasWeight = mesh.hasWeights();

		useSkin = skinIndex != -1;
		shader = useSkin ? Model.SKIN_SHADER : Model.BASE_SHADER;
		if (useSkin) {
			skin = loader.getSkin(skinIndex);
			inverseMatrices = skin.getInverseBindMatrices();
			boneMat = BufferUtils.createFloatBuffer(inverseMatrices.getElementType().size * inverseMatrices.getCount());
		}
		return this;
	}

	private static final ThreadLocal<FloatBuffer> TMP_FB16 = ThreadLocal.withInitial(() -> BufferUtils.createFloatBuffer(16));

	public void render() {
		Model.profiler.startSection("hide.render");
		//モーフィング
		if (hasWeight && !isValidWeight) {
			mesh.calcWeight(weights);
		}

		GL11.glPushMatrix();

		FloatBuffer fb = TMP_FB16.get();
		fb.rewind();
		fb.put(getLocalMat());
		fb.rewind();
		GL11.glMultMatrix(fb);

		Model.profiler.endStartSection("hide.render.debug");
		GlStateManager.disableTexture2D();
		GlStateManager.color(1f, 0.5f, 0.5f, 1);
		GL11.glPointSize(5);
		GL11.glBegin(GL11.GL_POINTS);
		GL11.glVertex3f(0, 0, 0);
		GL11.glEnd();
		GlStateManager.enableTexture2D();
		Model.profiler.endStartSection("hide.render.calcBone");

		shader.use();
		if (useSkin) {
			boneMat.rewind();
			computeJointMatrix(this, boneMat);
			boneMat.rewind();
			GL20.glUniformMatrix4(Model.SKIN_BONE_MAT_INDEX, false, boneMat);
		}
		Model.profiler.endStartSection("hide.render.draw");

		mesh.render();

		GL20.glUseProgram(0);
		Model.profiler.endSection();
		for (HideNode node : children) {
			node.render();
		}

		GL11.glPopMatrix();

	}

	@Override
	public void dispose() {
		mesh.dispose();

		for (HideNode node : children) {
			node.dispose();
		}
	}

	public void setRotation(float[] value) {
		if (!Arrays.equals(rotation, value)) {
			rotation = value;
			isValidLocalMat = false;
			markInvalidGlobalMat();
		}
	}

	public void setScale(float[] value) {
		if (!Arrays.equals(scale, value)) {
			scale = value;
			isValidLocalMat = false;
			markInvalidGlobalMat();
		}
	}

	public void setTranslation(float[] value) {
		if (!Arrays.equals(translation, value)) {
			translation = value;
			isValidLocalMat = false;
			markInvalidGlobalMat();
		}
	}

	public void setWeights(float[] value) {
		if (!Arrays.equals(weights, value)) {
			weights = value;
			isValidWeight = false;
		}
	}

	private void markInvalidGlobalMat() {
		isValidGlobalMat = false;
		for (HideNode child : children) {
			child.markInvalidGlobalMat();
		}
	}

	private static final ThreadLocal<float[]> TMP_MAT4x4 = ThreadLocal.withInitial(() -> new float[16]);

	public float[] getLocalMat() {
		if (!isValidLocalMat) {
			setIdentity4x4(matrix);
			float[] s;

			s = this.translation;
			this.matrix[12] = s[0];
			this.matrix[13] = s[1];
			this.matrix[14] = s[2];

			float[] m;

			s = this.rotation;
			m = TMP_MAT4x4.get();
			quaternionToMatrix4x4(s, m);
			mul4x4(matrix, m, matrix);

			s = this.scale;
			setIdentity4x4(m);
			m[0] = s[0];
			m[5] = s[1];
			m[10] = s[2];
			m[15] = 1.0F;
			mul4x4(matrix, m, matrix);
			isValidLocalMat = true;
		}
		return matrix;
	}

	public float[] getGlobalMat() {
		if (!isValidGlobalMat) {
			HideNode currentNode = this;
			setIdentity4x4(globalMatrix);
			while (currentNode != null) {
				mul4x4(currentNode.getLocalMat(), globalMatrix, globalMatrix);
				currentNode = currentNode.getParent();
			}
			isValidGlobalMat = true;
			System.out.println("CalcBoneMat");
		}
		return globalMatrix;
	}
}
