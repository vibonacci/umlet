package com.umlet.element.experimental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.baselet.control.NewGridElementConstants;
import com.baselet.control.enumerations.Direction;
import com.baselet.control.enumerations.LineType;
import com.baselet.diagram.draw.BaseDrawHandler;
import com.baselet.diagram.draw.geom.Dimension;
import com.baselet.diagram.draw.geom.DimensionFloat;
import com.baselet.diagram.draw.geom.Line;
import com.baselet.diagram.draw.geom.Rectangle;
import com.baselet.diagram.draw.helper.ColorOwn;
import com.baselet.element.GridElement;
import com.baselet.element.GroupGridElement;
import com.baselet.element.StickingPolygon;
import com.baselet.gui.AutocompletionText;
import com.umlet.element.experimental.settings.Settings;
import com.umlet.element.experimental.settings.facets.DefaultGlobalFacet.ElementStyleEnum;
import com.umlet.element.experimental.settings.facets.DefaultGlobalFacet.GlobalSetting;
import com.umlet.element.experimental.settings.facets.Facet;

public abstract class NewGridElement implements GridElement {

	//	private static final Logger log = Logger.getLogger(NewGridElement.class);

	private boolean stickingBorderActive;

	private BaseDrawHandler drawer; // this is the drawer for element specific stuff
	private BaseDrawHandler metaDrawer; // this is a separate drawer to draw stickingborder, selection-background etc.

	private GroupGridElement group = null;

	private Properties properties;

	private ComponentInterface component;

	private DrawHandlerInterface handler;
	
	private String additionalAttributes;

	public void init(Rectangle bounds, String panelAttributes, String additionalAttributes, ComponentInterface component, DrawHandlerInterface handler) {
		this.component = component;
		this.drawer = component.getDrawHandler();
		this.metaDrawer = component.getMetaDrawHandler();
		setRectangle(bounds);
		properties = new Properties(panelAttributes, drawer);
		setAdditionalAttributes(additionalAttributes);
		this.handler = handler;
	}

	public BaseDrawHandler getDrawer() {
		return drawer;
	}

	public BaseDrawHandler getMetaDrawer() {
		return metaDrawer;
	}

	@Override
	public String getPanelAttributes() {
		return properties.getPanelAttributes();
	}

	@Override
	public boolean isSelected() {
		return handler.getSelector().isSelected(this);
	}

	@Override
	public void setPanelAttributes(String panelAttributes) {
		properties.setPanelAttributes(panelAttributes);
		this.updateModelFromText();
	}

	@Override
	public void setGroup(GroupGridElement group) {
		this.group = group;
	}

	@Override
	public GridElement CloneFromMe() {
		return handler.clone();
	}
	
	/**
	 * "selected" variable is only used in OldGridElement.
	 * NewGridElement directly accesses Selector (to avoid saving redundant information) and uses this method call only to update metadrawer and redraw (for selection color)
	 */
	@Override
	public void setSelected(Boolean selected) {
		updateMetaDrawer(metaDrawer);
		repaint();
	}
	
	/**
	 * ugly workaround to avoid that the Resize().execute() call which calls setSize() on this model updates the model during the
	 * calculated model update from autoresize. Otherwise the drawer cache would get messed up (it gets cleaned up 2 times in a row and afterwards everything gets drawn 2 times).
	 * Best testcase is an autoresize element with a background. Write some text and everytime autresize triggers, the background is drawn twice.
	 */
	private boolean autoresizePossiblyInProgress = false;

	@Override
	public void updateModelFromText() {
		this.autoresizePossiblyInProgress = true;
		drawer.clearCache();
		drawer.resetStyle(); // must be set before actions which depend on the fontsize (otherwise a changed fontsize would be recognized too late)
		Integer oldLayer = getLayer();
		properties.initSettingsFromText(this);
		updateMetaDrawer(metaDrawer);
		updateConcreteModel(drawer, properties);
		if (oldLayer != null && !oldLayer.equals(getLayer())) {
			handler.updateLayer();
		}
		this.autoresizePossiblyInProgress = false;
	}

	protected abstract void updateConcreteModel(BaseDrawHandler drawer, Properties properties);

	protected void updateMetaDrawer(BaseDrawHandler drawer) {
		drawer.clearCache();
		if (isSelected()) { // draw blue rectangle around selected gridelements
			drawer.setForegroundColor(ColorOwn.TRANSPARENT);
			drawer.setBackgroundColor(ColorOwn.SELECTION_BG);
			drawer.drawRectangle(0, 0, getRealSize().width, getRealSize().height);
			drawer.resetColorSettings();
			if (NewGridElementConstants.show_stickingpolygon && !this.isPartOfGroup()) {
				drawStickingPolygon(drawer);
			}
		}
	}

	@Override
	public void updateProperty(GlobalSetting key, String newValue) {
		properties.updateSetting(key, newValue);
		handler.updatePropertyPanel();
		//		this.getHandler().getDrawPanel().getSelector().updateSelectorInformation(); // update the property panel to display changed attributes
		this.updateModelFromText();
		this.repaint();
	}

	@Override
	public String getSetting(GlobalSetting key) {
		return properties.getSetting(key);
	}

	@Override
	public GroupGridElement getGroup() {
		return this.group;
	}

	@Override
	public String getAdditionalAttributes() {
		return additionalAttributes;
	}

	@Override
	public void setAdditionalAttributes(String additionalAttributes) {
		/*TODO: perhaps refactor the additionattributes stuff completely (why should it be stored as a string? only for easier saving in uxf?)*/
		this.additionalAttributes = additionalAttributes;
	}

	@Override
	public boolean isPartOfGroup() {
		if (this.group != null) return true;
		return false;
	}

	@Override
	public boolean isInRange(Rectangle rect1) {
		return (rect1.contains(getRectangle()));
	}

	@Override
	public Set<Direction> getResizeArea(int x, int y) {
		Set<Direction> returnSet = new HashSet<Direction>();
		if (properties.getElementStyle() == ElementStyleEnum.NORESIZE || properties.getElementStyle() == ElementStyleEnum.AUTORESIZE) {
			return returnSet;
		}
		
		if ((x <= 5) && (x >= 0)) returnSet.add(Direction.LEFT);
		else if ((x <= this.getZoomedSize().width) && (x >= this.getZoomedSize().width - 5)) returnSet.add(Direction.RIGHT);

		if ((y <= 5) && (y >= 0)) returnSet.add(Direction.UP);
		else if ((y <= this.getZoomedSize().height) && (y >= this.getZoomedSize().height - 5)) returnSet.add(Direction.DOWN);
		return returnSet;
	}

	@Override
	public void setStickingBorderActive(boolean stickingBordersActive) {
		this.stickingBorderActive = stickingBordersActive;
	}

	@Override
	public boolean isStickingBorderActive() {
		return stickingBorderActive;
	}

	@Override
	public StickingPolygon generateStickingBorder(int x, int y, int width, int height) {
		StickingPolygon p = new StickingPolygon();
		p.addPoint(x, y);
		p.addPoint(x + width, y);
		p.addPoint(x + width, y + height);
		p.addPoint(x, y + height, true);
		return p;
	}
	
	private final void drawStickingPolygon(BaseDrawHandler drawer) {
		StickingPolygon poly;
		// The Java Implementations in the displaceDrawingByOnePixel list start at (1,1) to draw while any others start at (0,0)
		if (handler.displaceDrawingByOnePixel()) poly = this.generateStickingBorder(1, 1, this.getRealSize().width - 1, this.getRealSize().height - 1);
		else poly = this.generateStickingBorder(0, 0, this.getRealSize().width - 1, this.getRealSize().height - 1);
		if (poly != null) {
			drawer.setLineType(LineType.DASHED);
			drawer.setForegroundColor(ColorOwn.SELECTION_FG);
			for (Line line : poly.getStickLines()) {
				drawer.drawLine(line.getStart().getX(), line.getStart().getY(), line.getEnd().getX(), line.getEnd().getY());
			}
			drawer.setLineType(LineType.SOLID);
			drawer.resetColorSettings();
		}
	}

	@Override
	public void changeSize(int diffx, int diffy) {
		this.setSize(this.getZoomedSize().width + diffx, this.getZoomedSize().height + diffy);
	}

	@Override
	public void setRectangle(Rectangle bounds) {
		component.setBoundsRect(bounds);
	}

	@Override
	public void setLocationDifference(int diffx, int diffy) {
		this.setLocation(this.getRectangle().x + diffx, this.getRectangle().y + diffy);
	}

	@Override
	public void setLocation(int x, int y) {
		Rectangle rect = getRectangle();
		rect.setLocation(x, y);
		component.setBoundsRect(rect);
	}

	@Override
	public void setSize(int width, int height) {
		if (width != getZoomedSize().width || height != getZoomedSize().height) { // only change size if it is really different
			Rectangle rect = getRectangle();
			rect.setSize(width, height);
			component.setBoundsRect(rect);
			if (!this.autoresizePossiblyInProgress) {
				updateModelFromText();
			}
		}
	}

	@Override
	public Rectangle getRectangle() {
		return component.getBoundsRect();
	}

	@Override
	public void repaint() {
		component.repaintComponent();
	}

	@Override
	public Dimension getZoomedSize() {
		Rectangle rect = getRectangle();
		return new Dimension(rect.getWidth(), rect.getHeight());
	}

	/**
	 * @see com.baselet.element.GridElement#getRealSize()
	 */
	@Override
	public Dimension getRealSize() {
		return new Dimension((int) (getZoomedSize().width / handler.getZoomFactor()), (int) (getZoomedSize().height / handler.getZoomFactor()));
	}

	@Override
	public ComponentInterface getComponent() {
		return component;
	}

	public abstract Settings getSettings();

	@Override
	public List<AutocompletionText> getAutocompletionList() {
		List<AutocompletionText> returnList = new ArrayList<AutocompletionText>();
		for (Facet f : getSettings().getGlobalFacets()) {
			returnList.addAll(Arrays.asList(f.getAutocompletionStrings()));
		}
		for (Facet f : getSettings().getFacets()) {
			returnList.addAll(Arrays.asList(f.getAutocompletionStrings()));
		}
		return returnList;
	}

	@Override
	public Integer getLayer() {
		return properties.getLayer();
	}

	public abstract ElementId getId();

	@Override
	public void handleAutoresize(DimensionFloat necessaryElementDimension) {
		float hSpaceLeftAndRight = drawer.getDistanceBetweenTexts() * 2;
		float width = necessaryElementDimension.getWidth() + hSpaceLeftAndRight;
		float height = necessaryElementDimension.getHeight() + drawer.textHeight()/2;
		float diffw = width-getRealSize().width;
		float diffh = height-getRealSize().height;
		handler.Resize(diffw, diffh);
	}

}
