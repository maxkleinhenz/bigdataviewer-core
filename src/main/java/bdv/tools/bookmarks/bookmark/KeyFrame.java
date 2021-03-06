package bdv.tools.bookmarks.bookmark;

import java.util.Objects;
import net.imglib2.realtransform.AffineTransform3D;

public class KeyFrame {

	private int timepoint;
	private AffineTransform3D transform;

	public KeyFrame(int timepoint, AffineTransform3D transform) {
		this.timepoint = timepoint;
		this.transform = transform;
	}
	
	protected KeyFrame(KeyFrame k){
		this.timepoint = k.timepoint;
		this.transform = k.transform.copy();
	}

	public int getTimepoint() {
		return timepoint;
	}

	public AffineTransform3D getTransform() {
		return transform;
	}

	public void setTransform(AffineTransform3D transform) {
		this.transform = transform;
	}
	
	public KeyFrame copy(){
		return new KeyFrame(this);
	}
	
    @Override
	public boolean equals(Object other){
		if(other instanceof KeyFrame){
			if (getClass() == other.getClass()) {
				KeyFrame k = (KeyFrame) other;
				return this.timepoint == k.getTimepoint();
			}
		}
		return false;
	}

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), this.timepoint);
    }
    
}
