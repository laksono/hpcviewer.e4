 
package edu.rice.cs.hpctraceviewer.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;

import edu.rice.cs.hpc.data.db.IdTuple;
import edu.rice.cs.hpc.data.experiment.extdata.IBaseData;
import edu.rice.cs.hpc.data.experiment.extdata.IFileDB.IdTupleOption;
import edu.rice.cs.hpc.data.experiment.extdata.IFilteredData;
import edu.rice.cs.hpc.filter.dialog.FilterDataItem;
import edu.rice.cs.hpc.filter.dialog.ThreadFilterDialog;
import edu.rice.cs.hpctraceviewer.data.SpaceTimeDataController;
import edu.rice.cs.hpctraceviewer.ui.base.ITracePart;
import edu.rice.cs.hpctraceviewer.ui.internal.TraceEventData;
import edu.rice.cs.hpctraceviewer.ui.util.IConstants;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;

/*****
 * 
 * Filter ranks or threads.
 * This include excluding and including some ranks or filters
 *
 */
public class FilterRanks 
{
	@Execute
	public void execute( MPart part, 
						 @Named(IServiceConstants.ACTIVE_SHELL) Shell shell, 
						 IEventBroker eventBroker) {
		
		Object obj = part.getObject();		
		ITracePart tracePart = (ITracePart) obj;
		SpaceTimeDataController data = (SpaceTimeDataController) tracePart.getInput();
		
		/*
		 * This isn't the prettiest, but when we are local, we don't want to set
		 * it to filtered unless we have to (i.e. unless the user actually
		 * applies a filter). If the data is already filtered, we don't care and
		 * we just return the filtered data we have been using (which makes the
		 * call to set it redundant). If it's not, we wait to replace the
		 * current filter with the new filter until we know we have to.
		 */
        IBaseData filteredBaseData = data.getBaseData();
        if (filteredBaseData == null || !(filteredBaseData instanceof IFilteredData)) {
        	filteredBaseData = data.createFilteredBaseData();
        }
        List<IdTuple> listDenseIds = ((IFilteredData)filteredBaseData).getDenseListIdTuple(IdTupleOption.BRIEF);
        List<IdTuple> listIds = filteredBaseData.getListOfIdTuples(IdTupleOption.BRIEF);
        
        String []items    = new String[listDenseIds.size()];
        boolean []checked = new boolean[listDenseIds.size()];
        
        // initialize the ranks to be displayed in the dialog box
        // to know which ranks are already shown, we need to compare with the filtered ranks.
        // if the id tuple in the original list is the same as the one in filtered list, 
        // them the rank is already displayed.
        // This list needs to be optimized.
        
        for(int i=0, j=0; i<listDenseIds.size(); i++) {
        	items[i] = listDenseIds.get(i).toString();
        	if (j<listIds.size() && listDenseIds.get(i) == listIds.get(j)) {
        		checked[i] = true;
        		j++;
        	} else {
        		checked[i] = false;
        	}
        }
        
        List<FilterDataItem> list = ThreadFilterDialog.filter(shell, items, checked);
		
		if (list != null){
			List<Integer> listChecked = new ArrayList<Integer>();
			for(int i=0; i<list.size(); i++) {
				if (list.get(i).checked) {
					listChecked.add(i);
				}
			}
			if (listChecked.size() == 0) {
				return;
			}
			
			// update the data and broadcast to everyone that we probably have new filtered ranks
			// TODO: we need to check if the new one is the same with the old one or not.
			
			((IFilteredData)filteredBaseData).setIncludeIndex(listChecked);
			data.setBaseData((IFilteredData) filteredBaseData);
			TraceEventData eventData = new TraceEventData(data, tracePart, filteredBaseData);
			eventBroker.post(IConstants.TOPIC_FILTER_RANKS, eventData);
		}
	}
	
	
	@CanExecute
	@Inject
	public boolean canExecute(@Optional @Named(IServiceConstants.ACTIVE_PART)MPart part) {
		
		if (part == null)
			return false;
		
		Object obj = part.getObject();
		if (!(obj instanceof ITracePart))
			return false;

		return ((ITracePart)obj).getInput() != null;
	}
		
}