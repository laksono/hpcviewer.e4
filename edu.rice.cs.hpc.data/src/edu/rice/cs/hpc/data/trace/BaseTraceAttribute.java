package edu.rice.cs.hpc.data.trace;

import java.util.Map;

import edu.rice.cs.hpc.data.util.CallPath;

public class BaseTraceAttribute 
{
	public static final int DEFAULT_RECORD_SIZE = 24;
	public static final long PER_NANO_SECOND  = 1000000000;

	public long dbUnitTime;
	public long dbTimeMin;
	public long dbTimeMax;
	
	public int maxDepth;

	public Map<Integer, CallPath> mapCpidToCallpath;

	public BaseTraceAttribute() {
		dbTimeMax = Integer.MIN_VALUE;
		dbTimeMin = Integer.MAX_VALUE;
		dbUnitTime   = PER_NANO_SECOND;
	}
}
