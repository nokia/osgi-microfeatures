package com.alcatel_lucent.as.service.dns;

import java.util.*;

/**
 * This is a utility Class used to help sort out SRV records (rfc 2782).
 */

public class SRVHelper {

	private static final Random RND = new Random ();

	/**
	 * Sorts records according to rfc 2782.
	 * <p>
	 * It implements the load balancing of records of same priority.<br/>
	 * The list of records must be ordered by ascending priority (natural ordering of RecordSRV records is OK). Note that the returned list preserves that ordering.
	 * @param records the list of records to sort - can be null (response is empty)
	 * @return the sorted list (new instance of list)
	 */
	public static List<RecordSRV> loadBalance (List<RecordSRV> records){
		List<RecordSRV> resp = new ArrayList<RecordSRV> ();
		if (records == null) return resp;
		int prio = -1;
		int total = 0;
		List<RecordSRV> tmp = new ArrayList<RecordSRV> ();
		for (RecordSRV record : records){
			int thisPrio = record.getPriority ();
			if (thisPrio > prio){
				if (prio != -1){
					shuffle (tmp, resp, total);
					total = 0;
				}
				prio = thisPrio;
			} // else thisPrio == prio since the list is ordered
			if (record.getWeight () == 0){
				tmp.add (0, record);
			} else {
				tmp.add (record);
				total += record.getWeight ();
			}
		}
		shuffle (tmp, resp, total);
		
		return resp;
	}
	
	private static void shuffle (List<RecordSRV> in, List<RecordSRV> out, int total){
		if (in.size () == 0) return;
		int rnd = RND.nextInt (total+1);
		int acc = 0;
		for (int i=0; i<in.size (); i++){
			RecordSRV record = in.get (i);
			acc += record.getWeight ();
			if (acc >= rnd){
				out.add (record);
				in.remove (i);
				shuffle (in, out, total - record.getWeight ());
				return;
			}
		}
	}
}