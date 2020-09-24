
package edu.rice.cs.hpc.data.db.version3;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;

import edu.rice.cs.hpc.data.db.IdTuple;
import edu.rice.cs.hpc.data.experiment.metric.MetricValueSparse;

/*********************************************
 * 
 * Class to handle summary.db file generated by hpcprof
 *
 *********************************************/
public class DataSummary extends DataCommon 
{
	// --------------------------------------------------------------------
	// constants
	// --------------------------------------------------------------------
	private final static String HEADER_MAGIC_STR  = "HPCPROF-tmsdb_____";
	private static final int    METRIC_VALUE_SIZE = 8 + 2;
	private static final int    CCT_RECORD_SIZE   = 4 + 8;
	
	// --------------------------------------------------------------------
	// object variable
	// --------------------------------------------------------------------
	
	private RandomAccessFile file;
	
	private List<IdTuple>  listTuple;
	private List<ProfInfo> listProfInfo;
	
	
	// --------------------------------------------------------------------
	// Public methods
	// --------------------------------------------------------------------
	
	/***
	 *  <p>Opening for data summary metric file</p>
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.db.version3.DataCommon#open(java.lang.String)
	 */
	@Override
	public void open(final String filename)
			throws IOException
	{
		super.open(filename);
		file = new RandomAccessFile(filename, "r");
	}
	

	@Override
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.db.DataCommon#printInfo(java.io.PrintStream)
	 */
	public void printInfo( PrintStream out)
	{
		super.printInfo(out);

		ListCCTAndIndex list = null;
		
		try {
			list = getCCTIndex();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// print random metrics
		for (int i=0; i<list.listOfId.length; i++)
		{
			int cct = list.listOfId[i];
			out.format("[%5d] ", cct);
			printMetrics(out, cct);
		}
	}

	public ListCCTAndIndex getCCTIndex() 
			throws IOException {
		ProfInfo info = listProfInfo.get(0);
		
		// -------------------------------------------
		// read the cct context
		// -------------------------------------------
		
		long positionCCT = info.offset   + 
				   		   info.num_vals * METRIC_VALUE_SIZE;
		int numBytesCCT  = info.num_nz_contexts * CCT_RECORD_SIZE;
		
		ListCCTAndIndex list = new ListCCTAndIndex();
		list.listOfdIndex = new long[info.num_nz_contexts];
		list.listOfId = new int[info.num_nz_contexts];
		
		MappedByteBuffer buffer = file.getChannel().map(MapMode.READ_ONLY, positionCCT, numBytesCCT);
		
		for(int i=0; i<info.num_nz_contexts; i++) {
			list.listOfId[i] = buffer.getInt();
			list.listOfdIndex[i] = buffer.getLong();
		}
		return list;
	}
	
	
	/***
	 * Retrieve the list of tuple IDs.
	 * @return List of Tuple
	 */
	public List<IdTuple> getTuple() {
		return listTuple;
	}
	
	
	/****
	 * Get the value of a specific profile with specific cct and metric id
	 * 
	 * @param profileNum
	 * @param cctId
	 * @param metricId
	 * @return
	 * @throws IOException
	 */
	public double getMetric(int profileNum, int cctId, int metricId) 
			throws IOException
	{
		List<MetricValueSparse> listValues = getMetrics(profileNum, cctId);
		
		if (listValues != null) {
			
			// TODO ugly temporary code
			// We need to grab a value directly from the memory instead of searching O(n)
			
			for (MetricValueSparse mvs: listValues) {
				if (mvs.getIndex() == metricId) {
					return mvs.getValue();
				}
			}
		}
		return 0.0d;
	}
	
	/**********
	 * Reading a set of metrics from the file for a given CCT 
	 * This method does not support concurrency. The caller is
	 * responsible to handle mutual exclusion.
	 * 
	 * @param cct_id
	 * @return List of MetricValueSparse
	 * @throws IOException
	 */
	public List<MetricValueSparse> getMetrics(int cct_id) 
			throws IOException
	{		
		return getMetrics(0, cct_id);
	}
	
	
	/****
	 * Retrieve the list of metrics for a certain profile number and a given cct id
	 * @param profileNum The profile number. For summary profile, the number must be zero
	 * @param cct_id the cct id
	 * @return List of MetricValueSparse
	 * @throws IOException
	 */
	public List<MetricValueSparse> getMetrics(int profileNum, int cct_id) 
			throws IOException 
	{	
		ProfInfo info = listProfInfo.get(profileNum);
		
		// -------------------------------------------
		// read the cct context
		// -------------------------------------------
		
		long positionCCT = info.offset   + 
				   		   info.num_vals * METRIC_VALUE_SIZE;
		int numBytesCCT  = (info.num_nz_contexts+1) * CCT_RECORD_SIZE;
		
		MappedByteBuffer buffer = file.getChannel().map(MapMode.READ_ONLY, positionCCT, numBytesCCT);
		long []indexes = binarySearch(cct_id, 0, 1+info.num_nz_contexts, buffer);
		
		if (indexes == null)
			return null;
		// -------------------------------------------
		// initialize the metrics
		// -------------------------------------------

		int numMetrics   = (int) (indexes[1]-indexes[0]);
		int numBytes     = (int) numMetrics * METRIC_VALUE_SIZE;
		
		ArrayList<MetricValueSparse> values = new ArrayList<MetricValueSparse>(numMetrics);
		
		// -------------------------------------------
		// read the metrics
		// -------------------------------------------
		long positionMetrics = info.offset + indexes[0] * METRIC_VALUE_SIZE;
		file.seek(positionMetrics);
		
		byte []metricBuffer = new byte[numBytes];
		file.readFully(metricBuffer);
		ByteBuffer byteBuffer = ByteBuffer.wrap(metricBuffer);

		for(int i=0; i<numMetrics; i++) {
			double value = byteBuffer.getDouble();
			short metricId = byteBuffer.getShort();
			
			MetricValueSparse mvs = new MetricValueSparse(metricId, (float) value);
			values.add(mvs);
		}
		
		return values;
	}
	
		
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.db.DataCommon#dispose()
	 */
	public void dispose() throws IOException
	{
		file.close();
		super.dispose();
	}
	

	// --------------------------------------------------------------------
	// Protected methods
	// --------------------------------------------------------------------
	
	@Override
	protected boolean isTypeFormatCorrect(long type) {
		return type==1;
	}

	@Override
	protected boolean isFileHeaderCorrect(String header) {
		return header.equals(HEADER_MAGIC_STR);
	}

	@Override
	protected boolean readNextHeader(FileChannel input) 
			throws IOException
	{
		readProfInfo(input);
		readIdTuple(input);
		
		return true;
	}
	
	// --------------------------------------------------------------------
	// Private methods
	// --------------------------------------------------------------------

	/***
	 * read the list of id tuple
	 * @param input FileChannel
	 * @throws IOException
	 */
	private void readIdTuple(FileChannel input) 
			throws IOException
	{
		byte []buff = new byte[8];
		ByteBuffer buffTupleSize = ByteBuffer.wrap(buff);
		input.read(buffTupleSize);
		buffTupleSize.flip();
		
		long idTupleSize = buffTupleSize.getLong();
		
		listTuple = new ArrayList<IdTuple>((int) numItems);
		
		ByteBuffer buffer = ByteBuffer.allocate((int) idTupleSize);
		
		int numBytes      = input.read(buffer);
		assert (numBytes > 0);

		buffer.flip();
		
		for (int i=0; i<numItems; i++) {

			// -----------------------------------------
			// read the tuple section
			// -----------------------------------------

			IdTuple item = new IdTuple();

			item.length = buffer.getShort();			
			assert(item.length>0);
			
			item.kind  = new short[item.length];
			item.index = new long[item.length];
			
			for (int j=0; j<item.length; j++) {
				item.kind[j]  = buffer.getShort();
				item.index[j] = buffer.getLong();
			}
			listTuple.add(item);
		}
	}

	/*****
	 * read the list of Prof Info
	 * @param input FileChannel
	 * @throws IOException
	 */
	private void readProfInfo(FileChannel input) throws IOException {
		listProfInfo = new ArrayList<DataSummary.ProfInfo>((int) numItems);

		long position_profInfo = input.position();
		long profInfoSize = numItems * ProfInfo.SIZE;
		
		MappedByteBuffer mappedBuffer = input.map(MapMode.READ_ONLY, position_profInfo, profInfoSize);

		for(int i=0; i<numItems; i++) {
			ProfInfo info = new ProfInfo();
			info.id_tuple_ptr = mappedBuffer.getLong();
			info.metadata_ptr = mappedBuffer.getLong();
			mappedBuffer.getLong(); // spare 1
			mappedBuffer.getLong(); // spare 2
			info.num_vals = mappedBuffer.getLong();
			info.num_nz_contexts = mappedBuffer.getInt();
			info.offset = mappedBuffer.getLong();
			
			listProfInfo.add(info);
		}
		// make sure the next reader have pointer to the last read position
		
		position_profInfo += profInfoSize;
		input.position(position_profInfo);
	}
	
	
	/***
	 * Binary earch the cct index 
	 * 
	 * @param index the cct index
	 * @param first the beginning of the relative index
	 * @param last  the last of the relative index
	 * @param buffer ByteBuffer of the file
	 * @return 2-length array of indexes: the index of the found cct, and its next index
	 */
	private long[] binarySearch(int index, int first, int last, ByteBuffer buffer) {
		int begin = first;
		int end   = last;
		int mid   = (begin+end)/2;
		
		while (begin <= end) {
			buffer.position(mid * CCT_RECORD_SIZE);
			int cctidx  = buffer.getInt();
			long offset = buffer.getLong();
			
			if (cctidx < index) {
				begin = mid+1;
			} else if(cctidx == index) {
				long nextIndex = offset;
				
				if (mid+1<last) {
					buffer.position(CCT_RECORD_SIZE * (mid+1));
					buffer.getInt();
					nextIndex = buffer.getLong();
				}
				return new long[] {offset, nextIndex};
			} else {
				end = mid-1;
			}
			mid = (begin+end)/2;
		}
		// not found
		return null;
	}
	

	/*******
	 * print a list of metrics for a given CCT index
	 * 
	 * @param out : the outpur stream
	 * @param cct : CCT index
	 */
	private void printMetrics(PrintStream out, int cct)
	{
		try {
			List<MetricValueSparse> values = getMetrics(cct);
			if (values == null)
				return;
			
			for(MetricValueSparse value: values) {
				System.out.print(value.getIndex() + ": " + value.getValue() + " , ");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			out.println();
		}
	}

	
	// --------------------------------------------------------------------
	// Internal Classes
	// --------------------------------------------------------------------

	public static class ListCCTAndIndex
	{
		public int  []listOfId;
		public long []listOfdIndex;
		
		public String toString() {
			String buffer = "";
			if (listOfId != null) {
				buffer += "id: [" + listOfId[0] + ".." + listOfId[listOfId.length-1] + "] ";
			}
			if (listOfdIndex != null) {
				buffer += "idx; [" + listOfdIndex[0] + ".." + listOfdIndex[listOfdIndex.length-1] + "]";
			}
			return buffer;
		}
	}
	
	/*****
	 * 
	 * Prof info data structure
	 *
	 */
	protected static class ProfInfo
	{
		/** the size of the record in bytes  */
		public static final int SIZE = 8 + 8 + 8 + 8 + 8 + 4 + 8;
		
		public long id_tuple_ptr;
		public long metadata_ptr;
		public long num_vals;
		public int  num_nz_contexts;
		public long offset;
		
		public String toString() {
			return "tuple_ptr: " + id_tuple_ptr    + 
				   ", vals: " 	 + num_vals 	   + 
				   ", ccts: "	 + num_nz_contexts + 
				   ", offs: " 	 + offset;
		}
	}

	/***************************
	 * unit test 
	 * 
	 * @param argv
	 ***************************/
	public static void main(String []argv)
	{
		final String DEFAULT_FILE = "/home/la5/git/hpctoolkit/BUILD-prof2/hpctoolkit-hpcstruct-bin-database/thread.db";
		final String filename;
		if (argv != null && argv.length>0)
			filename = argv[0];
		else
			filename = DEFAULT_FILE;
		
		final DataSummary summary_data = new DataSummary();
		try {
			summary_data.open(filename);			
			summary_data.printInfo(System.out);
			summary_data.dispose();	
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
