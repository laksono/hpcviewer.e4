package edu.rice.cs.hpc.remote.data;

import java.io.DataOutputStream;
import java.util.List;

import edu.rice.cs.hpc.data.db.IdTuple;
import edu.rice.cs.hpc.data.experiment.extdata.IFilteredData;
import edu.rice.cs.hpc.data.experiment.extdata.IFileDB.IdTupleOption;
import edu.rice.cs.hpc.data.trace.FilterSet;
import edu.rice.cs.hpc.data.trace.TraceName;




public class RemoteFilteredBaseData implements IFilteredData {

	private final TraceName[] allNames;
	private int[] indexes;
	FilterSet filter;
	
	public RemoteFilteredBaseData(TraceName[] names, int _headerSz, DataOutputStream server) {
		allNames = names;
		filter = new FilterSet();
		indexes = new int[names.length];
		for (int i = 0; i < indexes.length; i++) {
			indexes[i] = i;
		}
	}


	@Override
	public int getNumberOfRanks() {
		return indexes.length;
	}

	@Override
	public void dispose() {
		// Do nothing. The local version disposes the BaseDataFile. The rough
		// equivalent would be to dispose the RemoteDataRetriever, but that is
		// done elsewhere. Plus, because RemoteDataRetriever is in traceviewer,
		// we can't access it here.
	}

	@Override
	public boolean isGoodFilter() {
		return getNumberOfRanks() > 0;
	}
	@Override
	public int getFirstIncluded() {
		return indexes[0];
	}
	@Override
	public int getLastIncluded() {
		return indexes[indexes.length-1];
	}
	@Override
	public boolean isDenseBetweenFirstAndLast() {
		return indexes[indexes.length-1] == indexes.length-1;
	}
	@Override
	public boolean isHybridRank() {
		return allNames[0].toString().contains(".");
	}
	
	@Override
	public int getNumLevels() {
		// TODO Auto-generated method stub
		return 0;
	}

	
	@Override
	public long getLong(long position) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getInt(long position) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getRecordSize() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getMinLoc(int rank) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getMaxLoc(int rank) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public List<IdTuple> getListOfIdTuples(IdTupleOption option) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List<Short> getIdTupleTypes() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setListOfIdTuples(List<IdTuple> listIdTuples) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setIncludeIndex(List<Integer> listOfIncludedIndex) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public List<IdTuple> getDenseListIdTuple(IdTupleOption option) {
		// TODO Auto-generated method stub
		return null;
	}
}
