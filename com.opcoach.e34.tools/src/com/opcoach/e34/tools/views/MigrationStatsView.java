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
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

@SuppressWarnings("restriction")
public class MigrationStatsView extends ViewPart implements ISelectionListener
{

	private MigrationDataComparator comparator;

	private Map<IPluginModelBase, TreeViewerColumn> columnsCache = new HashMap<IPluginModelBase, TreeViewerColumn>();

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

	@Override
	public void createPartControl(Composite parent)
	{

		parent.setLayout(new GridLayout(1, false));

		createDashBoard(parent);

		tv = new TreeViewer(parent);
		PluginDataProvider provider = new PluginDataProvider();
		tv.setContentProvider(provider);
		tv.setLabelProvider(provider);
		tv.setInput(Platform.getExtensionRegistry());

		final Tree cTree = tv.getTree();
		cTree.setHeaderVisible(true);
		cTree.setLinesVisible(true);
		cTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// tv.setInput(a);
		tv.setInput("Foo"); // getElements starts alone

		// Create the first column, containing extension points
		TreeViewerColumn epCol = new TreeViewerColumn(tv, SWT.NONE);
		epCol.getColumn().setWidth(300);
		epCol.getColumn().setText("Extension Points");
		PluginDataProvider labelProvider = new PluginDataProvider();
		epCol.setLabelProvider(labelProvider);
		epCol.getColumn().setToolTipText("Extension point in org.eclipse.ui to be migrated");
		epCol.getColumn().addSelectionListener(getHeaderSelectionAdapter(tv, epCol.getColumn(), 0, labelProvider));
		comparator = new MigrationDataComparator(0, labelProvider);
		tv.setComparator(comparator);

		// Open all the tree
		tv.expandAll();

		ColumnViewerToolTipSupport.enableFor(tv);

	}

	private void createDashBoard(Composite parent)
	{
		// Create here a part with some different statistic information.
		Composite dp = new Composite(parent, SWT.BORDER);

		dp.setLayout(new GridLayout(4, true));

		Label nbExtToMigrateTitle = new Label(dp, SWT.BORDER);
		nbExtToMigrateTitle.setText("Nb of Extensions to migrate : ");
		Label nbExtToMigrateValue = new Label(dp, SWT.BORDER);
		nbExtToMigrateValue.setText("???");

		Label nbDeprecatedExtToCleanTitle = new Label(dp, SWT.BORDER);
		nbDeprecatedExtToCleanTitle.setText("Nb of Deprecated Extensions to fix : ");
		Label nbDeprecatedExtToCleanValue = new Label(dp, SWT.BORDER);
		nbDeprecatedExtToCleanValue.setText("???");

		Label nbViewTitle = new Label(dp, SWT.BORDER);
		nbViewTitle.setText("Nb of views to migrate : ");
		Label nbViewValue = new Label(dp, SWT.BORDER);
		nbViewValue.setText("???");

		Label nbEditorTitle = new Label(dp, SWT.BORDER);
		nbEditorTitle.setText("Nb of editors to migrate : ");
		Label nbEditorValue = new Label(dp, SWT.BORDER);
		nbEditorValue.setText("???");

	}

	private void createPluginColumns(IPluginModelBase pm)
	{
		// Add columns in the tree one column per selected plugin.
		// Create the first column for the key
		TreeViewerColumn col = new TreeViewerColumn(tv, SWT.NONE);
		col.getColumn().setWidth(300);
		col.getColumn().setText(pm.getBundleDescription().getName());
		PluginDataProvider labelProvider = new PluginDataProvider();

		labelProvider.setPlugin(pm);
		col.setLabelProvider(labelProvider);
		col.getColumn().setToolTipText("tooltip a definir");

		columnsCache.put(pm, col);

	}

	@Override
	public void setFocus()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)
	{

		if (selection.isEmpty())
			return;

		// Try to find selected plugins in selection
		if (selection instanceof IStructuredSelection)
		{
			IStructuredSelection ss = (IStructuredSelection) selection;
			// selectedPlugins.clear();
			Collection<IPluginModelBase> currentSelectedPlugins = new ArrayList<IPluginModelBase>();
			for (Iterator<IPluginModelBase> it = ss.iterator(); it.hasNext();)
			{
				Object selected = it.next();
				IProject proj = (IProject) Platform.getAdapterManager().getAdapter(selected, IProject.class);
				if (proj != null)
				{
					IPluginModelBase m = PDECore.getDefault().getModelManager().findModel(proj);
					if (m != null)
					{
						System.out.println("Selected plugin is : " + m.getBundleDescription().getName());
						currentSelectedPlugins.add(m);
					}
				}
			}

			mergeTableViewerColumns(currentSelectedPlugins);

			tv.refresh();

		}

	}

	/**
	 * An entry comparator for the table, dealing with column index, keys and
	 * values
	 */
	public class MigrationDataComparator extends ViewerComparator
	{
		private int columnIndex;
		private int direction;
		private ILabelProvider labelProvider;

		public MigrationDataComparator(int columnIndex, ILabelProvider defaultLabelProvider)
		{
			this.columnIndex = columnIndex;
			direction = SWT.UP;
			labelProvider = defaultLabelProvider;
		}

		public int getDirection()
		{
			return direction;
		}

		/** Called when click on table header, reverse order */
		public void setColumn(int column)
		{
			if (column == columnIndex)
			{
				// Same column as last sort; toggle the direction
				direction = (direction == SWT.UP) ? SWT.DOWN : SWT.UP;
			} else
			{
				// New column; do a descending sort
				columnIndex = column;
				direction = SWT.DOWN;
			}
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2)
		{
			// Compare the text from label provider.
			String lp1 = labelProvider.getText(e1);
			String lp2 = labelProvider.getText(e2);
			String s1 = lp1 == null ? "" : lp1.toLowerCase();
			String s2 = lp2 == null ? "" : lp2.toLowerCase();
			int rc = s1.compareTo(s2);
			// If descending order, flip the direction
			return (direction == SWT.DOWN) ? -rc : rc;
		}

		public void setLabelProvider(ILabelProvider textProvider)
		{
			labelProvider = textProvider;
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
}