/*******************************************************************************
 * Copyright (c) 2014 OPCoach.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     OPCoach - initial API and implementation
 *******************************************************************************/
package com.opcoach.e34.tools.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.opcoach.e34.tools.Migration34Activator;

@SuppressWarnings("restriction")
public class MigrationStatsView extends ViewPart implements ISelectionListener
{

	private static final String COUNT_COLUMN = "Count";

	private MigrationDataComparator comparator;

	private Map<IPluginModelBase, TreeViewerColumn> columnsCache = new HashMap<IPluginModelBase, TreeViewerColumn>();
	private TreeViewerColumn countCol = null;

	public MigrationStatsView()
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(IViewSite site) throws PartInitException
	{
		// TODO Auto-generated method stub
		super.init(site);
		site.getPage().addSelectionListener(this);
	}

	@Override
	public void dispose()
	{
		getSite().getPage().removeSelectionListener(this);
		super.dispose();
	}

	private Collection<IPluginModelBase> displayedPlugins = Collections.EMPTY_LIST;
	private TreeViewer tv;

	private FilterStats filter;

	@Override
	public void createPartControl(Composite parent)
	{
		parent.setLayout(new GridLayout(2, false));

		createDashBoard(parent);
		createDeprecatedDashBoard(parent);
		updateDashboard();

		createToolBar(parent);

		tv = new TreeViewer(parent);
		provider = new PluginDataProvider();
		tv.setContentProvider(provider);
		tv.setLabelProvider(provider);
		tv.setInput(Platform.getExtensionRegistry());

		final Tree cTree = tv.getTree();
		cTree.setHeaderVisible(true);
		cTree.setLinesVisible(true);
		cTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1)); // hspan=2
		tv.setInput("Foo"); // getElements starts alone

		// Create the first column, containing extension points
		TreeViewerColumn epCol = new TreeViewerColumn(tv, SWT.NONE);
		epCol.getColumn().setWidth(300);
		epCol.getColumn().setText("Extension Points");
		PluginDataProvider labelProvider = new PluginDataProvider();
		epCol.setLabelProvider(labelProvider);
		epCol.getColumn().setToolTipText("Extension points defined in org.eclipse.ui to be migrated");
		epCol.getColumn().addSelectionListener(getHeaderSelectionAdapter(tv, epCol.getColumn(), 0, labelProvider));
		comparator = new MigrationDataComparator(0, labelProvider);
		tv.setComparator(comparator);

		// Set the filters.
		filter = new FilterStats();
		tv.setFilters(new ViewerFilter[] { filter });

		// Open all the tree
		tv.expandAll();

		ColumnViewerToolTipSupport.enableFor(tv);

		parent.layout();

	}

	private void createToolBar(Composite parent)
	{
		ToolBar tb = new ToolBar(parent, SWT.FLAT | SWT.LEFT);
		/*
		 * tb.setLayout(new RowLayout()); RowData rd = new RowData();
		 * tb.setLayoutData(rd);
		 */

		ToolItem expandAll = new ToolItem(tb, SWT.PUSH);
		expandAll.setImage(Migration34Activator.getDefault().getImageRegistry().get(Migration34Activator.IMG_EXPAND));
		expandAll.setToolTipText("Expand all nodes");
		expandAll.addSelectionListener(new SelectionListener()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					tv.expandAll();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e)
				{
				}
			});
		ToolItem collapseAll = new ToolItem(tb, SWT.PUSH);
		collapseAll.setImage(Migration34Activator.getDefault().getImageRegistry().get(Migration34Activator.IMG_COLLAPSE));
		collapseAll.setToolTipText("Collapse nodes");
		collapseAll.addSelectionListener(new SelectionListener()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					tv.collapseAll();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e)
				{
				}
			});

		// Add filters...
		new ToolItem(tb, SWT.SEPARATOR);

		ToolItem ti = new ToolItem(tb, SWT.CHECK | SWT.BORDER);
		ti.setImage(Migration34Activator.getDefault().getImageRegistry().get(Migration34Activator.IMG_FILTER));
		ti.setToolTipText("Filter empty lines");
		ti.addSelectionListener(new SelectionListener()
			{
				public void widgetSelected(SelectionEvent e)
				{
					// filter empty lines...
					filter.setFilterEmptyLines(!filter.getFilterEmptyLines());
					tv.refresh();
				}

				public void widgetDefaultSelected(SelectionEvent e)
				{
				}
			});

		// Create filter deprecated
		ToolItem item = new ToolItem(tb, SWT.DROP_DOWN);
		item.setImage(Migration34Activator.getDefault().getImageRegistry().get(Migration34Activator.IMG_DEPRECATED));
		item.setToolTipText("Filter here deprecated extensions points");

		DropdownSelectionListener dslistener = new DropdownSelectionListener(item);
		dslistener.add(FilterStats.SHOW_ALL);
		dslistener.add(FilterStats.REMOVE_DEPRECATED);
		dslistener.add(FilterStats.ONLY_DEPRECATED);
		item.addSelectionListener(dslistener);

	}

	private void createDashBoard(Composite parent)
	{
		// Create here a part with some different statistic information.
		Group dp = new Group(parent, SWT.BORDER);
		dp.setText("Extension points counters");
		dp.setLayout(new GridLayout(4, true));

		createCounter(dp, "Views : ", "views/view");
		createCounter(dp, "Editors : ", "editors/editor");
		createCounter(dp, "Preference pages : ", "preferencePages/page");
		createCounter(dp, "Property pages : ", "propertyPages/page");
		createCounter(dp, "Actions Sets : ", "actionsSets/actionSet");
		createCounter(dp, "Commands : ", "commands/command");
		createCounter(dp, "Handlers : ", "handlers/handler");
		createCounter(dp, "Menus : ", "menus/menuContribution");

	}

	private void createDeprecatedDashBoard(Composite parent)
	{
		dp = new Group(parent, SWT.BORDER);
		dp.setText("Deprecated Extension points counters");
		dp.setLayout(new GridLayout(4, true));

		createCounter(dp, "Accelerator Config : ", "acceleratorConfigurations/acceleratorConfiguration");
		createCounter(dp, "Accelerator Scopes : ", "acceleratorScopes/acceleratorScope");
		createCounter(dp, "Accelerator Sets : ", "acceleratorScopes/acceleratorSet");
		createCounter(dp, "Actions Definition : ", "actionsDefinitions/actionDefinition");
		createCounter(dp, "Actions Set Part Association : ", "actionsSetPartAssociations/actionsSetPartAssociation");
		createCounter(dp, "Actions Sets : ", "actionSets/actionSet");
		createCounter(dp, "Editor Actions : ", "editorActions/editorContribution");
		createCounter(dp, "View Actions : ", "viewActions/viewContribution");
		createCounter(dp, "Popup Object contrib : ", "popupMenus/objectContribution");
		createCounter(dp, "Popup Viewer contrib : ", "popupMenus/viewerContribution");
		createCounter(dp, "Presentation Factories : ", "presentationFactories/factory");

	}

	private Map<String, Label> countLabels = new HashMap<String, Label>();

	private PluginDataProvider provider;

	private CountDataProvider countProvider;

	private Group dp;

	/**
	 * Create the counter label and remember of it to compute it according to
	 * selection
	 * 
	 * @param parent
	 * @param title
	 *            : the title for the counter
	 * @param xpath
	 *            : the xpath to search for in the plugin xml : ex : views/view,
	 *            editors/editor must not give the full extension point name,
	 *            only simple name
	 */
	public void createCounter(Composite parent, String title, String xpath)
	{
		Label titleLabel = new Label(parent, SWT.BORDER);
		titleLabel.setText(title);
		titleLabel.setToolTipText("org.eclipse.ui." + xpath);
		Label valueLabel = new Label(parent, SWT.BORDER);
		valueLabel.setText("???");
		countLabels.put(xpath, valueLabel);

	}

	/**
	 * Just update the contents of dashboard according to selected plugins
	 */
	private void updateDashboard()
	{
		E4MigrationRegistry reg = E4MigrationRegistry.getDefault();

		for (String xpath : countLabels.keySet())
		{
			int count = reg.countNumberOfExtensions(xpath, displayedPlugins);
			Label label = countLabels.get(xpath);
			label.setText("" + count);
			if (label.getParent() == dp)
			{
				// stand in the deprecated group.. set red if > 0
				if (count > 0)
					label.setForeground(provider.red);
				else
					label.setForeground(null);
			}
		}
	}

	private void createPluginColumns(IPluginModelBase pm)
	{
		// Add columns in the tree one column per selected plugin.
		// Create the first column for the key
		TreeViewerColumn col = new TreeViewerColumn(tv, SWT.NONE);
		TreeColumn swtCol = col.getColumn();
		swtCol.setText(pm.getBundleDescription().getName());
		swtCol.setAlignment(SWT.CENTER);
		PluginDataProvider labelProvider = new PluginDataProvider();

		labelProvider.setPlugin(pm);
		col.setLabelProvider(labelProvider);
		swtCol.setToolTipText(pm.getBundleDescription().getName());
		swtCol.pack();

		columnsCache.put(pm, col);

	}

	private void createCountDataColumns(Collection<IPluginModelBase> pmbs)
	{
		// Always Remove column to recreate it at the end
		if (countCol != null)
		{
			countCol.getColumn().dispose();
			countCol = null;
		}

		// Add columns in the tree one column per selected plugin.
		// Create the first column for the key
		// Only if there are plugins selected
		if (pmbs.size() > 0)
		{
			countCol = new TreeViewerColumn(tv, SWT.NONE);
			TreeColumn swtCol = countCol.getColumn();
			swtCol.setText(COUNT_COLUMN);
			swtCol.setAlignment(SWT.CENTER);
			countProvider = new CountDataProvider();

			countProvider.setPlugins(pmbs);
			countCol.setLabelProvider(countProvider);
			swtCol.setToolTipText("Sum the line");
			swtCol.pack();
		}
	}

	@Override
	public void setFocus()
	{
		tv.getControl().setFocus();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)
	{

		// Try to find selected plugins in selection
		if (selection instanceof IStructuredSelection)
		{
			IStructuredSelection ss = (IStructuredSelection) selection;
			// selectedPlugins.clear();
			Collection<IPluginModelBase> currentSelectedPlugins = new ArrayList<IPluginModelBase>();
			for (@SuppressWarnings("unchecked")
			Iterator<IPluginModelBase> it = ss.iterator(); it.hasNext();)
			{
				Object selected = it.next();
				IProject proj = (IProject) Platform.getAdapterManager().getAdapter(selected, IProject.class);
				if (proj != null)
				{
					IPluginModelBase m = PDECore.getDefault().getModelManager().findModel(proj);
					if (m != null)
					{
						currentSelectedPlugins.add(m);
					}
				}
			}

			mergeTableViewerColumns(currentSelectedPlugins);

			tv.refresh();

			updateDashboard();

		}

	}

	private void mergeTableViewerColumns(Collection<IPluginModelBase> currentSelectedPlugins)
	{
		// Search for plugins to be added or removed
		Collection<IPluginModelBase> toBeAdded = new ArrayList<IPluginModelBase>();
		Collection<IPluginModelBase> toBeRemoved = new ArrayList<IPluginModelBase>();

		for (IPluginModelBase p : currentSelectedPlugins)
		{
			if (!displayedPlugins.contains(p))
				toBeAdded.add(p);
		}

		for (IPluginModelBase p : displayedPlugins)
		{
			if (!currentSelectedPlugins.contains(p))
				toBeRemoved.add(p);
		}

		// Now remove and add columns in viewer..
		for (IPluginModelBase p : toBeRemoved)
		{
			TreeViewerColumn tc = columnsCache.get(p);
			if (tc != null)
				tc.getColumn().dispose();
		}

		for (IPluginModelBase p : toBeAdded)
		{
			createPluginColumns(p);
		}

		createCountDataColumns(currentSelectedPlugins);
		
		displayedPlugins = currentSelectedPlugins;

	}

	private SelectionAdapter getHeaderSelectionAdapter(final TreeViewer viewer, final TreeColumn column, final int columnIndex,
			final ILabelProvider textProvider)
	{
		SelectionAdapter selectionAdapter = new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					viewer.setComparator(comparator);
					comparator.setColumn(columnIndex);
					comparator.setLabelProvider(textProvider);
					viewer.getTree().setSortDirection(comparator.getDirection());
					viewer.getTree().setSortColumn(column);
					viewer.refresh();
				}
			};
		return selectionAdapter;
	}

	/**
	 *  * This class provides the "drop down" functionality for our dropdown
	 * tool items.  
	 */
	private class DropdownSelectionListener extends SelectionAdapter
	{
		private Menu menu;
		private MenuItem currentSelected;

		public DropdownSelectionListener(ToolItem dropdown)
		{
			menu = new Menu(dropdown.getParent().getShell());
		}

		/**
		 * Adds an item to the dropdown list     * @param item the item to add
		 *    
		 */
		public void add(String item)
		{
			MenuItem menuItem = new MenuItem(menu, SWT.CHECK);
			menuItem.setText(item);
			menuItem.addSelectionListener(new SelectionAdapter()
				{
					public void widgetSelected(SelectionEvent event)
					{
						MenuItem selected = (MenuItem) event.widget;
						if ((currentSelected != null) && (currentSelected != selected))
							currentSelected.setSelection(false);
						selected.setSelection(true);
						currentSelected = selected;
						// Update the filter and refresh
						filter.setFilterDeprecated(selected.getText());
						tv.refresh();
					}
				});
		}

		/**
		 * Called when either the button itself or the dropdown arrow is clicked
		 *  
		 */
		public void widgetSelected(SelectionEvent event)
		{
			// If they clicked the arrow, we show the list
			if (event.detail == SWT.ARROW)
			{
				// Determine where to put the dropdown list
				ToolItem item = (ToolItem) event.widget;
				Rectangle rect = item.getBounds();
				Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
				menu.setLocation(pt.x, pt.y + rect.height);
				menu.setVisible(true);
			} else
			{
				// Nothing to do...
				// System.out.println("button pressed");
			}
		}
	}

	class FilterStats extends ViewerFilter
	{
		static final String EMPTY_LINES = "Filter empty lines";
		static final String SHOW_ALL = "Show all";
		static final String REMOVE_DEPRECATED = "Remove deprecated";
		static final String ONLY_DEPRECATED = "Show only deprecated";

		private boolean filterEmptyLines = false;
		private String filterDeprecated = SHOW_ALL;

		void setFilterEmptyLines(boolean fel)
		{
			filterEmptyLines = fel;
		}

		public boolean getFilterEmptyLines()
		{
			return filterEmptyLines;
		}

		void setFilterDeprecated(String mode)
		{
			filterDeprecated = mode;
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element)
		{
			if (filterDeprecated != SHOW_ALL)
			{

				// Must filter the deprecated and may be empty lines
				boolean elementIsDeprecated = provider.isDeprecated(element);
				if ((filterDeprecated == ONLY_DEPRECATED) && !elementIsDeprecated)
					return false;
				if ((filterDeprecated == REMOVE_DEPRECATED) && elementIsDeprecated)
					return false;

				// Can now check if line is empty

				return countProvider == null ? true : !(filterEmptyLines && "0".equals(countProvider.getText(element)));

			} else
				return countProvider == null ? true : !(filterEmptyLines && "0".equals(countProvider.getText(element)));

		}

	}

}
