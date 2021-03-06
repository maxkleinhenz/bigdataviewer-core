package bdv.tools.bookmarks.bookmark;

import bdv.viewer.animate.SimilarityTransformAnimator;
import static java.util.Collections.unmodifiableSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import mpicbg.spim.data.XmlHelpers;
import net.imglib2.realtransform.AffineTransform3D;
import org.jdom2.Element;

public class DynamicBookmark extends Bookmark {

	public static final String XML_ELEM_BOOKMARK_NAME = "DynamicBookmark";
	public static final String XML_ELEM_KEYFRAME_NAME = "keyframe";
	public static final String XML_ELEM_KEYFRAMES_NAME = "keyframes";
	public static final String XML_ELEM_TIMEPOINT_NAME = "timepoint";
	public static final String XML_ELEM_TRANSFORM_NAME = "transform";

	private final TreeSet<KeyFrame> keyframes;

	private final List<DynamicBookmarkChangedListener> listeners = Collections.synchronizedList(new ArrayList<>());

	public DynamicBookmark(final String key) {
		super(key);
		this.keyframes = new TreeSet<>(new KeyFrameComparator());
	}

	public DynamicBookmark(Element element) {
		super(element);
		this.keyframes = new TreeSet<>(new KeyFrameComparator());

		restoreKeyframesFromXml(element);
	}

	protected DynamicBookmark(DynamicBookmark d) {
		super(d);
		this.keyframes = new TreeSet<>(new KeyFrameComparator());
		for (KeyFrame k : d.keyframes)
			this.keyframes.add(k.copy());
	}

	protected DynamicBookmark(DynamicBookmark d, String newKey) {
		super(d, newKey);
		this.keyframes = new TreeSet<>(new KeyFrameComparator());
		for (KeyFrame k : d.keyframes)
			this.keyframes.add(k.copy());
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof DynamicBookmark) {
			if (getClass() == other.getClass()) {
				DynamicBookmark b = (DynamicBookmark) other;
				return this.getKey() == b.getKey();
			}
		}
		return false;
	}

	/**
	 * Returns all KeyFrames as an unmodifiable set.
	 * 
	 * @return Returns never {@code null}.
	 */
	public Set<KeyFrame> getFrameSet() {
		return unmodifiableSet(this.keyframes);
	}

	public boolean add(final KeyFrame keyframe) {
		remove(keyframe);
		final boolean b = keyframes.add(keyframe);
		if (b) {
			fireDynamicBookmarkChanged();
		}
		return b;
	}
	
	/**
	 * Updates the given KeyFrame with the given Timepoint.
	 * When alreadey a KeyFrame with the given Timepoint exists, the KeyFrame will not be updated.
	 * This method returns the updated KeyFrame or null when it could not be updated.
	 * 
	 * @param keyframe the KeyFrame which will be updated
	 * @param timepoint the new Timepoint
	 * @return the updated KeyFrame or null
	 */
	public KeyFrame updateWithoutOverride(final KeyFrame keyframe, final int timepoint){
		
		if(keyframes.contains(new KeyFrame(timepoint, null))){
			// a keyframe with the new timepoit alrady exists
			// dont update
			return null;
		}
		else{
			keyframes.remove(keyframe);
			final KeyFrame newKeyFrame = new KeyFrame(timepoint, keyframe.getTransform());
			if(this.keyframes.add(newKeyFrame)){
				fireDynamicBookmarkChanged();
				return newKeyFrame;
			}
		}
		
		return null;
	}

	public boolean remove(final KeyFrame keyframe) {
		final boolean b = keyframes.remove(keyframe);
		if (b) {
			fireDynamicBookmarkChanged();
		}
		return b;
	}

	@Override
	public Element toXmlNode() {
		final Element elemBookmark = new Element(XML_ELEM_BOOKMARK_NAME);
		elemBookmark.addContent(XmlHelpers.textElement(XML_ELEM_KEY_NAME, this.getKey()));
		elemBookmark.addContent(XmlHelpers.textElement(XML_ELEM_KEY_TITLE, this.getTitle()));
		elemBookmark.addContent(XmlHelpers.textElement(XML_ELEM_KEY_DESCRIPTION, this.getDescription()));

		final Element elemKeyframes = new Element(XML_ELEM_KEYFRAMES_NAME);

		for (final KeyFrame keyframe : keyframes) {
			final Element elemKeyframe = new Element(XML_ELEM_KEYFRAME_NAME);
			elemKeyframe.addContent(XmlHelpers.intElement(XML_ELEM_TIMEPOINT_NAME, keyframe.getTimepoint()));
			elemKeyframe
					.addContent(XmlHelpers.affineTransform3DElement(XML_ELEM_TRANSFORM_NAME, keyframe.getTransform()));
			elemKeyframes.addContent(elemKeyframe);
		}

		elemBookmark.addContent(elemKeyframes);

		return elemBookmark;
	}

	private void restoreKeyframesFromXml(final Element parent) {
		final Element elemKeyframes = parent.getChild(XML_ELEM_KEYFRAMES_NAME);

		for (final Element elemKeyframe : elemKeyframes.getChildren(XML_ELEM_KEYFRAME_NAME)) {
			final int timepoint = XmlHelpers.getInt(elemKeyframe, XML_ELEM_TIMEPOINT_NAME);
			final AffineTransform3D transform = XmlHelpers.getAffineTransform3D(elemKeyframe, XML_ELEM_TRANSFORM_NAME);
			add(new KeyFrame(timepoint, transform));
		}
	}

	/**
	 * Returns the greatest keyframe by timepoint less than to the given
	 * timepoint, or null if there is no such timepoint.
	 * 
	 * @param timepoint
	 *            the reference value
	 * @return previous keyframe by timepoint which is less the given timepoint
	 *         or null
	 */
	public KeyFrame getPreviousKeyFrame(final int timepoint) {
		KeyFrame k = new KeyFrame(timepoint, null);
		return keyframes.lower(k);
	}

	/**
	 * Returns the greatest keyframe by timepoint less than or equal to the
	 * given timepoint, or null if there is no such timepoint.
	 * 
	 * @param timepoint
	 *            the reference value
	 * @return previous keyframe by timepoint which is less or equals the given
	 *         timepoint or null
	 */
	public KeyFrame getPreviousOrEqualKeyFrame(final int timepoint) {
		KeyFrame k = new KeyFrame(timepoint, null);
		return keyframes.floor(k);
	}

	/**
	 * Returns the least keyframe by timepoint strictly greater than the given
	 * timepoint, or null if there is no such timepoint.
	 * 
	 * @param timepoint
	 *            the reference value
	 * @return next keyframe by timepoint which is greater than the given
	 *         timepoint or null
	 */
	public KeyFrame getNextKeyFrame(int timepoint) {
		KeyFrame k = new KeyFrame(timepoint, null);
		return keyframes.higher(k);
	}

	/**
	 * Returns interpolated transform between {@link #getPreviousTransform(int)
	 * previous} and {@link #getNextTransform(int) next} transform for given
	 * timepoint.
	 * 
	 * @param timepoint
	 * @param cX
	 * @param cY
	 * @return
	 */
	public AffineTransform3D getInterpolatedTransform(final int timepoint, final double cX, final double cY) {

		if (keyframes.size() < 1) {
			return null;
		}
		
		KeyFrame previousKeyframe = getPreviousOrEqualKeyFrame(timepoint);
		
		final KeyFrame nextKeyframe = getNextKeyFrame(timepoint);

		if (previousKeyframe == null) {
			previousKeyframe = keyframes.first();
		}

		if (nextKeyframe == null) {
			AffineTransform3D transform = previousKeyframe.getTransform().copy();
			return transform;
		}
		int animatorTimepoint = Math.min(timepoint, previousKeyframe.getTimepoint());

		final SimilarityTransformAnimator transAnimator = new SimilarityTransformAnimator(
				previousKeyframe.getTransform(), nextKeyframe.getTransform(), cX, cY,
				nextKeyframe.getTimepoint() - animatorTimepoint);
		transAnimator.setTime(0);

		final AffineTransform3D targetTransform = transAnimator.getCurrent(timepoint - animatorTimepoint);

		targetTransform.set(targetTransform.get(0, 3) - cX, 0, 3);
		targetTransform.set(targetTransform.get(1, 3) - cY, 1, 3);

		return targetTransform;
	}

	@Override
	public DynamicBookmark copy() {
		return new DynamicBookmark(this);
	}

	@Override
	public DynamicBookmark copy(String newKey) {
		return new DynamicBookmark(this, newKey);
	}

	public void addDynamicBookmarkChangedListener(DynamicBookmarkChangedListener l) {
		listeners.add(l);
	}

	public void removeDynamicBookmarkChangedListener(DynamicBookmarkChangedListener l) {
		listeners.remove(l);
	}

	private void fireDynamicBookmarkChanged() {
		for (DynamicBookmarkChangedListener l : listeners) {
			l.changed();
		}
	}
}
