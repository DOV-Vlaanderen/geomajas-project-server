/*
 * This is part of Geomajas, a GIS framework, http://www.geomajas.org/.
 *
 * Copyright 2008-2011 Geosparc nv, http://www.geosparc.com/, Belgium.
 *
 * The program is available in open source according to the GNU Affero
 * General Public License. All contributions in this program are covered
 * by the Geomajas Contributors License Agreement. For full licensing
 * details, see LICENSE.txt in the project root.
 */

package org.geomajas.gwt.example.base;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.types.SortArrow;
import com.smartgwt.client.types.TreeModelType;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;
import org.geomajas.gwt.example.base.i18n.ExampleBaseMessages;

/**
 * <p>
 * The tree used in the left of the sample overview.
 * </p>
 * 
 * @author Pieter De Graef
 */
public class SampleTree extends TreeGrid {

	private static final ExampleBaseMessages MESSAGES = GWT.create(ExampleBaseMessages.class);

	public SampleTree() {
		super();
		setWidth100();
		setHeight100();
		setCustomIconProperty("icon");
		setAnimateFolderTime(100);
		setAnimateFolders(true);
		setAnimateFolderSpeed(500);
		setNodeIcon("[ISOMORPHIC]/geomajas/example/image/silk/application_view_list.png");
		setShowSortArrow(SortArrow.CORNER);
		setShowAllRecords(true);
		setLoadDataOnDemand(false);
		setCanSort(false);

		TreeGridField field = new TreeGridField();
		field.setCanFilter(true);
		field.setName("name");
		field.setTitle("<b>" + MESSAGES.sampleTitle("1.9.0") + "</b>");
		setFields(field);

		Tree tree = new Tree();
		tree.setModelType(TreeModelType.PARENT);
		tree.setIdField("nodeId");
		tree.setRootValue("topLevel");
		tree.setData(SampleTreeNodeRegistry.getSampleTreeNodeArray());

		setData(tree);
		// tree.openAll(tree.getRoot());
		tree.openFolder(tree.getChildren(tree.getRoot())[2]);
	}
}
