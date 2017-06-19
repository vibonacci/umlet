package com.baselet.diagram.io;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.baselet.control.basics.geom.Rectangle;
import com.baselet.control.enums.ElementId;
import com.baselet.diagram.DiagramHandler;
import com.baselet.diagram.DrawPanel;
import com.baselet.element.ElementFactorySwing;
import com.baselet.element.NewGridElement;
import com.baselet.element.facet.common.GroupFacet;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.old.custom.CustomElementCompiler;
import com.baselet.element.old.element.ErrorOccurred;
import com.baselet.gui.BaseGUI;
import com.baselet.gui.CurrentGui;
import com.baselet.gui.command.HelpPanelChanged;

/**
 * Describes what should happen with parsed elements from the input file
 * eg: set DiagramHandler variables, create GridElements etc.
 */
public class InputHandler extends DefaultHandler {

	private static final String[] oldGridElementPackages = new String[] { "com.baselet.element.old.element", "com.baselet.element.old.allinone", "com.baselet.element.old.custom" };

	private static final Logger log = LoggerFactory.getLogger(InputHandler.class);

	private DrawPanel _p = null;
	private GridElement e = null;
	private String elementtext;

	private int x;
	private int y;
	private int w;
	private int h;
	private String entityname;
	private String code;
	private String panelAttributes;
	private String additionalAttributes;

	private Integer currentGroup;
	private final DiagramHandler handler;

	// to be backward compatible - add list of old elements that were removed so that they are ignored when loading old files
	private final List<String> ignoreElements;

	private String id; // Experimental elements have an id instead of an entityname

	public InputHandler(DiagramHandler handler) {
		this.handler = handler;
		_p = handler.getDrawPanel();
		ignoreElements = new ArrayList<String>();
		ignoreElements.add("main.control.Group");
		currentGroup = null;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		elementtext = "";
		if ("element".equals(qName)) {
			panelAttributes = "";
			additionalAttributes = "";
			code = null;
		}
		if ("group".equals(qName)) { // TODO remove group-handling in InputHandler. Until UMLet v13, groups used own element-tags in XML. This has changed to the property group=x, so this handling is only for backwards compatibility
			currentGroup = handler.getDrawPanel().getSelector().getUnusedGroup();
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		String elementname = qName; // [UB]: we are not name-space aware, so use the qualified name

		if ("help_text".equals(elementname)) {
			handler.setHelpText(elementtext);
			handler.getFontHandler().setDiagramDefaultFontSize(HelpPanelChanged.getFontsize(elementtext));
			handler.getFontHandler().setDiagramDefaultFontFamily(HelpPanelChanged.getFontfamily(elementtext));
			BaseGUI gui = CurrentGui.getInstance().getGui();
			if (gui != null && gui.getPropertyPane() != null) { // issue 244: in batchmode, a file can have a help_text but gui will be null
				gui.getPropertyPane().switchToNonElement(elementtext);
			}
		}
		else if ("zoom_level".equals(elementname)) {
			if (handler != null) {
				handler.setGridSize(Integer.parseInt(elementtext));
			}
		}
                else if ("group".equals(elementname)) {
			currentGroup = null;
		}
		else if ("element".equals(elementname)) {
			if (id != null) {
				try {
					NewGridElement e = ElementFactorySwing.create(ElementId.valueOf(id), new Rectangle(x, y, w, h), panelAttributes, additionalAttributes, handler);
                                        e = currentGroupHandler(e);
					_p.addElement(e);
				} catch (Exception e) {
					log.error("Cannot instantiate element with id: " + id, e);
				}
				id = null;
			}
			else if (!ignoreElements.contains(entityname)) { // OldGridElement handling which can be removed as soon as all OldGridElements have been replaced
				try {
					if (code == null) {
						e = InputHandler.getOldGridElementFromPath(entityname);
					}
					else {
						e = CustomElementCompiler.getInstance().genEntity(code);
					}
				} catch (InstantiationException e1) {
					e = new ErrorOccurred();
				} catch (IllegalAccessException e1) {
					e = new ErrorOccurred();
				} catch (ClassNotFoundException e1) {
					e = new ErrorOccurred();
				}
				e.setRectangle(new Rectangle(x, y, w, h));
				e.setPanelAttributes(panelAttributes);
				e.setAdditionalAttributes(additionalAttributes);
				handler.setHandlerAndInitListeners(e);

				if (currentGroup != null) {
					e.setProperty(GroupFacet.KEY, currentGroup);
				}
				_p.addElement(e);
			}
		}
		else if ("type".equals(elementname)) {
			entityname = elementtext;
		}
		else if ("id".equals(elementname)) { // new elements have an id
			id = elementtext;
		}
		else if ("x".equals(elementname)) {
                        x = Integer.parseInt(elementtext);
		}
		else if ("y".equals(elementname)) {
                        y = Integer.parseInt(elementtext);
		}
		else if ("w".equals(elementname)) {
                        w = Integer.parseInt(elementtext);
		}
		else if ("h".equals(elementname)) {
                        h = Integer.parseInt(elementtext);
		}
		else if ("panel_attributes".equals(elementname)) {
			panelAttributes = elementtext;
		}
		else if ("additional_attributes".equals(elementname)) {
			additionalAttributes = elementtext;
		}
		else if ("custom_code".equals(elementname)) {
			code = elementtext;
		}
	}
        
        public NewGridElement currentGroupHandler(NewGridElement e) {
            if (currentGroup != null) {
                e.setProperty(GroupFacet.KEY, currentGroup);
            }        
            return e;
        }

	@Override
	public void characters(char[] ch, int start, int length) {
		elementtext += new String(ch).substring(start, start + length);
	}

	private static GridElement getOldGridElementFromPath(String path) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Class<?> foundClass = null;
		String className = path.substring(path.lastIndexOf('.'));
		for (String possPackage : oldGridElementPackages) {
			try {
				foundClass = Thread.currentThread().getContextClassLoader().loadClass(possPackage + className);
				break;
			} catch (ClassNotFoundException e1) {/* do nothing; try next package */}
		}
		if (foundClass == null) {
			ClassNotFoundException ex = new ClassNotFoundException("class " + path + " not found");
			log.error(null, ex);
			throw ex;
		}
		else {
			return (GridElement) foundClass.newInstance();
		}
	}

}
