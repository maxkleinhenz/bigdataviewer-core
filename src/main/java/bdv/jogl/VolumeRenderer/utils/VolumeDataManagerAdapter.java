package bdv.jogl.VolumeRenderer.utils;

import com.jogamp.opengl.math.geom.AABBox;

/**
 * adapter class for the volume manager listeners
 * @author michael
 *
 */
public class VolumeDataManagerAdapter implements IVolumeDataManagerListener {
	@Override
	public void addedData(Integer i) {}
	
	@Override
	public void dataUpdated(Integer i) {}
	
	@Override
	public void dataRemoved(Integer i) {}
	
	@Override
	public void dataEnabled(Integer i, Boolean flag) {	
	}
	
}