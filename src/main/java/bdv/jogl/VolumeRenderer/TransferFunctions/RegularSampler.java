package bdv.jogl.VolumeRenderer.TransferFunctions;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.TreeMap;

import com.jogamp.common.nio.Buffers;

import bdv.jogl.VolumeRenderer.ShaderPrograms.ShaderSources.funtions.IFunction;
import bdv.jogl.VolumeRenderer.ShaderPrograms.ShaderSources.funtions.RegularTransferFunctionInterpreter;

/**
 * Samples transfer function texture regularly 
 * @author michael
 *
 */
public class RegularSampler implements ITransferFunctionSampler {
	
	private final RegularTransferFunctionInterpreter desampler = new RegularTransferFunctionInterpreter();  
	
	/**
	 * Samples tf data and returns 1d texture
	 * @param transferFunction
	 * @param sampleStep
	 * @return
	 */
	public FloatBuffer sample(TransferFunction1D transferFunction, float sampleStep){
		TreeMap<Integer, Color> colorMap = transferFunction.getTexturColor();
		//get Buffer last key is the highest number 
		FloatBuffer buffer = Buffers.newDirectFloatBuffer(((colorMap.lastKey()-colorMap.firstKey())+1)*4);


		//make samples
		Integer latestMapIndex = colorMap.firstKey();
		//iterate candidates
		for(Integer currentMapIndex: colorMap.keySet()){
			if(currentMapIndex == colorMap.firstKey()){
				continue;
			}

			float[] currentColor = {0,0,0,(float)(colorMap.get(latestMapIndex).getAlpha())/255.f};
			float[] finalColor = {0,0,0,(float)(colorMap.get(currentMapIndex).getAlpha())/255.f};
			float[] colorGradient = {0,0,0,0};
			colorMap.get(latestMapIndex).getColorComponents(currentColor);
			colorMap.get(currentMapIndex).getColorComponents(finalColor);

			//forward difference
			for(int dim = 0; dim < colorGradient.length; dim++){
				colorGradient[dim] = (finalColor[dim]-currentColor[dim])/(currentMapIndex-latestMapIndex);
			}

			//sample linear
			for(Integer step = latestMapIndex; step < currentMapIndex; step++ ){

				//add to buffer and increment
				for(int dim = 0; dim < colorGradient.length; dim++){
					buffer.put(Math.min( finalColor[dim],  currentColor[dim]));
					currentColor[dim] += colorGradient[dim];
				}
			}		
			//add latest color
			if(currentMapIndex == colorMap.lastKey()){
				for(int dim = 0; dim < finalColor.length; dim++){
					buffer.put(finalColor[dim]);
				}
			}
			latestMapIndex = currentMapIndex;
		}

		buffer.rewind();
		return buffer;
	};
	
	/**
	 * Retruns the de sampling shader code
	 * @return
	 */
	public IFunction getShaderCode(){
		return desampler;
	}
	
}
