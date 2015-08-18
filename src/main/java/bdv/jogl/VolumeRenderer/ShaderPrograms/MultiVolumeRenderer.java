package bdv.jogl.VolumeRenderer.ShaderPrograms;

import static bdv.jogl.VolumeRenderer.utils.MatrixUtils.getNewIdentityMatrix;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import bdv.jogl.VolumeRenderer.Scene.Texture;
import bdv.jogl.VolumeRenderer.ShaderPrograms.ShaderSources.MultiVolumeRendererShaderSource;
import bdv.jogl.VolumeRenderer.gui.TransferFunction1D;
import bdv.jogl.VolumeRenderer.gui.TransferFunctionListener;
import bdv.jogl.VolumeRenderer.utils.GeometryUtils;

import static bdv.jogl.VolumeRenderer.ShaderPrograms.ShaderSources.MultiVolumeRendererShaderSource.*;
import static bdv.jogl.VolumeRenderer.utils.MatrixUtils.*;
import bdv.jogl.VolumeRenderer.utils.VolumeDataBlock;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.Matrix4;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.util.glsl.ShaderCode;


/**
 * Renderer for multiple volume data
 * @author michael
 *
 */
public class MultiVolumeRenderer extends AbstractShaderSceneElement{

	private float[] coordinates = GeometryUtils.getUnitCubeVerticesQuads(); 

	private final Map<Integer,VolumeDataBlock> dataValues = new HashMap<Integer, VolumeDataBlock>();

	private final Map<Integer,Texture> volumeTextureMap = new HashMap<Integer, Texture>();

	private TransferFunction1D tf;
	
	private Texture colorTexture;

	private boolean isColorUpdateable;

	private boolean isEyeUpdateable = true;

	private Matrix4 drawCubeTransformation = getNewIdentityMatrix();

	private MultiVolumeRendererShaderSource sources =new MultiVolumeRendererShaderSource (); 

	public MultiVolumeRenderer(){
		for(ShaderCode code:sources.getShaderCodes()){
			shaderCodes.add(code);
		}
	}

	private float[] calculateEyePositions(){
		final int maxNumVolumes = sources.getMaxNumberOfVolumes();
		float eyePositionsObjectSpace[] = new float[3*maxNumVolumes];

		Matrix4 globalTransformation = getNewIdentityMatrix();
		globalTransformation.multMatrix(getView());
		globalTransformation.multMatrix(getModelTransformation());

		for(int i =0; i< maxNumVolumes;i++){
			int fieldOffset = 3*i;
			if(!getVolumeDataMap().containsKey(i)){
				break;
			}
			VolumeDataBlock data = getVolumeDataMap().get(i);
			Matrix4 modelViewMatrixInverse= copyMatrix(globalTransformation);

			modelViewMatrixInverse.multMatrix(copyMatrix(data.localTransformation));
			modelViewMatrixInverse.scale(data.dimensions[0], data.dimensions[1], data.dimensions[2]);
			modelViewMatrixInverse.invert();

			eyePositionsObjectSpace[fieldOffset] = modelViewMatrixInverse.getMatrix()[12];
			eyePositionsObjectSpace[fieldOffset+1] = modelViewMatrixInverse.getMatrix()[13];
			eyePositionsObjectSpace[fieldOffset+2] = modelViewMatrixInverse.getMatrix()[14];

		}
		return eyePositionsObjectSpace;
	}

	private void updateEyes(GL2 gl2){
		if(!isEyeUpdateable ){
			return;
		}

		float [] eyePositions = calculateEyePositions();

		//eye position
		gl2.glUniform3fv(getLocation(suvEyePosition), 
				sources.getMaxNumberOfVolumes(),eyePositions,0);

		isEyeUpdateable = false;
	}

	@Override
	protected void updateShaderAttributesSubClass(GL2 gl2) {

		updateActiveVolumes(gl2);

		updateLocalTransformationInverse(gl2);

		boolean update = updateTextureData(gl2);
		if(update){
			updateGlobalScale(gl2);

			updateMaxDiagonalLength(gl2);
		}

		updateColor(gl2);

		updateEyes(gl2);
	}


	private void updateMaxDiagonalLength(GL2 gl2) {
		float length = Float.MIN_VALUE;
		float[][] globalUnitCubeHighLow = new float[][]{{0,0,0,1},{1,1,1,1}};

		//get cube to global space
		for(int i =0; i < globalUnitCubeHighLow.length;i++){
			drawCubeTransformation.multVec(globalUnitCubeHighLow[i], globalUnitCubeHighLow[i]);
		}

		//cube transform in texture space to get the maximum extend
		for(VolumeDataBlock data: getVolumeDataMap().values()){
			float[][] coordsInTextureSpace = new float[2][4];

			Matrix4 textureTransformation = copyMatrix(data.localTransformation);
			textureTransformation.scale(data.dimensions[0], data.dimensions[1], data.dimensions[2]);
			textureTransformation.invert();		
			for(int i =0; i < globalUnitCubeHighLow.length;i++){
				textureTransformation.multVec(globalUnitCubeHighLow[i], coordsInTextureSpace[i]);
				for(int j = 0; j < 4; j++){
					coordsInTextureSpace[i][j] /= coordsInTextureSpace[i][3];
				}
			}

			float currentLength = VectorUtil.distVec3(coordsInTextureSpace[0], coordsInTextureSpace[1]);

			length = Math.max(currentLength, length);
		}
		gl2.glUniform1f(getLocation(suvMaxDiagonalLength), length);
	}

	private void updateActiveVolumes(GL2 gl2) {
		IntBuffer activeBuffers = Buffers.newDirectIntBuffer(sources.getMaxNumberOfVolumes());
		activeBuffers.rewind();
		for(int i = 0; i<sources.getMaxNumberOfVolumes();i++){
			int active; 
			if(getVolumeDataMap().containsKey(i)){
				active=1;
			}else{
				active=0;
			}
			activeBuffers.put(i, active);

		}
		activeBuffers.rewind();
		gl2.glUniform1iv(getLocation(suvActiveVolumes),
				activeBuffers.capacity(),activeBuffers);
	}

	private void updateLocalTransformationInverse(GL2 gl2) {
		for(Integer index: getVolumeDataMap().keySet()){
			VolumeDataBlock data = getVolumeDataMap().get(index);
			if(!data.needsUpdate()){
				continue;
			}

			Matrix4 localInverse = copyMatrix(data.localTransformation);
			localInverse.scale(data.dimensions[0], data.dimensions[1], data.dimensions[2]);
			localInverse.invert();
			gl2.glUniformMatrix4fv(getLocation(suvTextureTransformationInverse)+index,
					1,false,localInverse.getMatrix(),0);
		}
	}

	private void updateGlobalScale(GL2 gl2) {


		drawCubeTransformation = getNewIdentityMatrix();

		//iterate data for get bounding volume
		float highPoint[] = {Float.MIN_VALUE,Float.MIN_VALUE,Float.MIN_VALUE};
		float lowPoint[] = {Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE};


		for(int index: getVolumeDataMap().keySet()){
			VolumeDataBlock data = getVolumeDataMap().get(index);
			float repesentantsToCheck[][] = {
					{0,0,0,1},
					{0,0,1,1},
					{0,1,0,1},
					{0,1,1,1},
					{1,0,0,1},
					{1,0,1,1},
					{1,1,0,1},
					{1,1,1,1},
			};


			Matrix4 transformation = copyMatrix(data.localTransformation);
			transformation.scale(data.dimensions[0],data.dimensions[1],data.dimensions[2]);
			for(float [] representant: repesentantsToCheck){
				//transform
				float[] globalVolumeCoordinate= new float[4];
				transformation.multVec(representant, globalVolumeCoordinate);


				//build box
				for(int i = 0; i < 3 ; i++){
					globalVolumeCoordinate[i] = globalVolumeCoordinate[i]/ globalVolumeCoordinate[3];
					highPoint[i] = Math.max(highPoint[i], globalVolumeCoordinate[i]);
					lowPoint[i] = Math.min(lowPoint[i], globalVolumeCoordinate[i]);
				} 
			}
		}
		//correct origo
		drawCubeTransformation.translate(lowPoint[0], lowPoint[1], lowPoint[2]);
		drawCubeTransformation.scale(highPoint[0]-lowPoint[0],highPoint[1]-lowPoint[1],highPoint[2]-lowPoint[2]);
		gl2.glUniformMatrix4fv(getLocation(suvDrawCubeTransformation),1,false,drawCubeTransformation.getMatrix(),0);
	}

	private boolean updateTextureData(GL2 gl2){

		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;
		boolean somethingUpdated = false;

		for(Integer i : getVolumeDataMap().keySet()){
			VolumeDataBlock data = getVolumeDataMap().get(i);

			min = Math.min(min, data.minValue);
			max = Math.max(max, data.maxValue);

			if(!data.needsUpdate()){
				continue;
			}

			somethingUpdated= true;

			//get Buffer
			FloatBuffer buffer = Buffers.newDirectFloatBuffer(data.data);
			buffer.rewind();

			//uploade data
			volumeTextureMap.get(i).update(gl2, 0, buffer, 
					new int[]{(int)data.dimensions[0], 
							(int)data.dimensions[1], 
							(int)data.dimensions[2]});

			data.setNeedsUpdate(false);
		}

		//update values
		if(somethingUpdated){
			//min max
			gl2.glUniform1f(getLocation(suvMinVolumeValue), min);
			gl2.glUniform1f(getLocation(suvMaxVolumeValue), max);
		}
		return somethingUpdated;
	}

	@Override
	protected void generateIdMappingSubClass(GL2 gl2) {


		mapUniforms(gl2, new String[]{
				suvDrawCubeTransformation,
				suvTextureTransformationInverse,
				suvActiveVolumes,
				suvEyePosition,
				suvMinVolumeValue,
				suvMaxVolumeValue,
				suvVolumeTexture,
				suvColorTexture,
				suvMaxDiagonalLength});

		int location = getLocation(suvVolumeTexture);
		for(int i =0; i< sources.getMaxNumberOfVolumes(); i++){
			Texture volumeTexture = new Texture(GL2.GL_TEXTURE_3D,location+i,GL2.GL_R32F,GL2.GL_RED,GL2.GL_FLOAT);
			volumeTexture.genTexture(gl2);
			volumeTexture.setTexParameteri(gl2,GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
			volumeTexture.setTexParameteri(gl2, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
			volumeTexture.setTexParameteri(gl2, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_BORDER);
			volumeTexture.setTexParameteri(gl2, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_BORDER);
			volumeTexture.setTexParameteri(gl2, GL2.GL_TEXTURE_WRAP_R, GL2.GL_CLAMP_TO_BORDER);
			volumeTextureMap.put(i, volumeTexture);
		}
		location = getLocation(suvColorTexture);
		colorTexture = new Texture(GL2.GL_TEXTURE_1D,location,GL2.GL_RGBA,GL2.GL_RGBA,GL2.GL_FLOAT);
		colorTexture.genTexture(gl2);
		colorTexture.setTexParameteri(gl2,GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
		colorTexture.setTexParameteri(gl2, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
		colorTexture.setTexParameteri(gl2, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_BORDER);
	}

	/**
	 * Return the map of volume data with a user specific index.
	 * @return
	 */
	public final Map<Integer, VolumeDataBlock> getVolumeDataMap() {
		return dataValues;
	}

	@Override
	protected void updateVertexBufferSubClass(GL2 gl2, VertexAttribute position) {
		FloatBuffer bufferData = Buffers.newDirectFloatBuffer(coordinates);
		bufferData.rewind();

		position.setAttributeValues(gl2, bufferData);	
	}

	@Override
	public void setModelTransformation(Matrix4 modelTransformations) {
		super.setModelTransformation(modelTransformations);

		isEyeUpdateable = true;
	}

	@Override
	public void setView(Matrix4 view) {
		super.setView(view);

		isEyeUpdateable = true;
	}

	private void updateColor(GL2 gl2){
		if(!isColorUpdateable){
			return;
		}

		//get Buffer last key is the highest number 
		FloatBuffer buffer = tf.getTexture(); 
				
		//upload data
		colorTexture.update(gl2, 0, buffer, new int[]{buffer.capacity()/4});
		//gl2.glBindTexture(GL2.GL_TEXTURE_1D, 0);
		isColorUpdateable = false;
	}


	@Override
	protected int getVertexBufferSize() {

		return coordinates.length * Buffers.SIZEOF_FLOAT;
	}

	@Override
	protected void renderSubClass(GL2 gl2) {
		gl2.glDrawArrays(GL2.GL_QUADS, 0,coordinates.length/3);
	}

	/**
	 * Updates color data
	 * @param newData
	 */
	public void setTransferFunction(final TransferFunction1D tf){
		this.tf = tf;
		this.tf.addTransferFunctionListener(new TransferFunctionListener() {
			
			@Override
			public void colorChanged(TransferFunction1D transferFunction) {
				isColorUpdateable =true;
				
			}
		});
	}

	/**
	 * @return the drawCubeTransformation
	 */
	public Matrix4 getDrawCubeTransformation() {
		return copyMatrix( drawCubeTransformation);
	}

}