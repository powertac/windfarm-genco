package org.powertac.wpgenco;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class provides functionality to determine optimal offers from the wind farm for 
 * each open timeslot.
 * It also holds data that is required for this computation.
 * @author spande00 (Shashank Pande)
 *
 */
public class WindFarmOfferCalculator {
	private static Logger log = Logger.getLogger(WindFarmOfferCalculator.class);
	
	@Autowired
	TimeslotRepo timeslotRepo;
	
	private Map<Timeslot, Double> mapTimeSlotClearingPrices = new HashMap<Timeslot, Double>();
	private Map<Timeslot, Double> mapTimeSlotMinAskPrices = new HashMap<Timeslot, Double>();
	private Map<Timeslot, Double> mapTimeSlotMaxAskPrices = new HashMap<Timeslot, Double>();
	
	/**
	 * Constructor.
	 */
	public WindFarmOfferCalculator() {
		
	}

	public void addClearingPrices(double[] prices) {
		addPrices(mapTimeSlotClearingPrices, prices);
	}
	
	public void addMinAskPrices(double[] prices) {
		addPrices(mapTimeSlotMinAskPrices, prices);
	}
	
	public void addMaxAskPrices(double[] prices) {
		addPrices(mapTimeSlotMaxAskPrices, prices);
	}
	
	private void addPrices(Map<Timeslot, Double> mapPriceStore, double[] prices) {
		//sanity check
		if (prices == null) {
			return;
		}
		if (prices.length <= 0) {
			return;
		}
		//get open timeslots
		List<Timeslot> tsList = timeslotRepo.enabledTimeslots();
		if (tsList == null) {
			log.error("addClearingPrices: there are no enabled timeslots");
			return;
		}
		if (tsList.size() != prices.length) {
			log.error("addClearingPrices: invalid data");
			return;
		}
		for (int i = 0; i < prices.length; i++) {
			Timeslot ts = tsList.get(i);
			Double cp = prices[i];
			mapPriceStore.put(ts, cp);
		}
		//delete old data
		Timeslot currTimeslot = timeslotRepo.currentTimeslot();
		List<Timeslot> tobeRemoved =  new ArrayList<Timeslot>();
		Set<Timeslot> keySet = mapPriceStore.keySet();
		for (Timeslot ts : keySet) {
			Instant keyInstant = ts.getStartInstant();
			Instant now = currTimeslot.getStartInstant();
			if (now.isAfter(keyInstant.getMillis())) {
				tobeRemoved.add(ts);
			}
		}
		for (Timeslot ts : tobeRemoved) {
			mapPriceStore.remove(ts);
		}
	} //addClearingPrices()
	
} //class WindFarmOfferCalculator
