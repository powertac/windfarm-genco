package org.powertac.wpgenco;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.wpgenco.ForecastScenarios.Scenario;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class provides functionality to determine optimal offers from the wind
 * farm for
 * each open timeslot.
 * It also holds data that is required for this computation.
 * 
 * @author spande00 (Shashank Pande)
 * 
 */
public class WindFarmOfferCalculator
{
  private static Logger log = Logger.getLogger(WindFarmOfferCalculator.class);
  private static double stepSize = 0.1; // must be > 0 and less than 0.5

  @Autowired
  TimeslotRepo timeslotRepo;

  private Map<Timeslot, Double> mapTimeSlotClearingPrices =
    new HashMap<Timeslot, Double>();
  private Map<Timeslot, Double> mapTimeSlotMinAskPrices =
    new HashMap<Timeslot, Double>();
  private Map<Timeslot, Double> mapTimeSlotMaxAskPrices =
    new HashMap<Timeslot, Double>();
  private Map<Timeslot, Integer> mapTimeslotToHour =
    new HashMap<Timeslot, Integer>();

  private double maxCapacity = 0; // maximum capacity of windfarm
  private List<Scenario> windfarmOutputScenarios = null;

  /**
   * Constructor.
   */
  public WindFarmOfferCalculator (double maxCap, List<Scenario> wpScenarios)
  {
    this.maxCapacity = maxCap;
    this.windfarmOutputScenarios = wpScenarios;
    for (int i = 0; i < timeslotRepo.enabledTimeslots().size(); i++) {
      Timeslot ts = timeslotRepo.enabledTimeslots().get(i);
      mapTimeslotToHour.put(ts, i + 1);
    }
  }

  public void addClearingPrices (double[] prices)
  {
    addPrices(mapTimeSlotClearingPrices, prices);
  }

  public void addMinAskPrices (double[] prices)
  {
    addPrices(mapTimeSlotMinAskPrices, prices);
  }

  public void addMaxAskPrices (double[] prices)
  {
    addPrices(mapTimeSlotMaxAskPrices, prices);
  }

  public List<Double> getOptimalOfferCapacities (List<Timeslot> openSlots)
  {
    List<Double> offerCaps = new ArrayList<Double>();

    for (Timeslot ts: openSlots) {
      double oc = determineOfferCapacity(ts);
      offerCaps.add(oc);
    }

    return offerCaps;
  }

  private void addPrices (Map<Timeslot, Double> mapPriceStore, double[] prices)
  {
    // sanity check
    if (prices == null) {
      return;
    }
    if (prices.length <= 0) {
      return;
    }
    // get open timeslots
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
      Double cp = Math.abs(prices[i]);
      mapPriceStore.put(ts, cp);
    }
    // delete old data
    Timeslot currTimeslot = timeslotRepo.currentTimeslot();
    List<Timeslot> tobeRemoved = new ArrayList<Timeslot>();
    Set<Timeslot> keySet = mapPriceStore.keySet();
    for (Timeslot ts: keySet) {
      Instant keyInstant = ts.getStartInstant();
      Instant now = currTimeslot.getStartInstant();
      if (now.isAfter(keyInstant.getMillis())) {
        tobeRemoved.add(ts);
      }
    }
    for (Timeslot ts: tobeRemoved) {
      mapPriceStore.remove(ts);
    }
  } // addClearingPrices()

  /**
   * Determines optimal capacity to submit ask offer
   * 
   * @param ts
   *          timeslot for which the calculation is done.
   * @return optimal capacity
   */
  private double determineOfferCapacity (Timeslot ts)
  {
    if (!mapTimeSlotClearingPrices.containsKey(ts)
        || !mapTimeSlotMinAskPrices.containsKey(ts)
        || !mapTimeSlotMaxAskPrices.containsKey(ts)) {
      return 0;
    }
    Double cmcp = (double) 0; // market clearing price
    Double crup = (double) 0; // regulation up price
    Double crdn = (double) 0; // regulation down price

    // get the prices
    if (!getPrices(ts, cmcp, crdn, crup)) {
      log.error("failed to get prices for timeslot: " + ts);
      return 0;
    }
    // at this point we know we have the prices
    double revenue = 0; // we need to maximize this
    double offerCap = 0;
    for (double currCap = 0; currCap <= maxCapacity; currCap +=
      currCap * stepSize) {
      double currRev = getRevenue(currCap, cmcp, crdn, crup, ts);
      if (currRev > revenue) {
        revenue = currRev;
        offerCap = currCap;
      }
    }

    return offerCap;
  } // calcOfferCapacity()

  private double getRevenue (double pbid, double mcp, double crd, double cru,
                             Timeslot ts)
  {
    double mcpRevenue = pbid * mcp;
    // get imbalance revenue - positive revenue indicate profit, -ve revenue
    // loss
    int tiIndex = mapTimeslotToHour.get(ts) - 1;
    if ((tiIndex < 0) || (tiIndex > 23)) {
      return mcpRevenue; // no data to calculate imbalance revenue
    }
    double imbalanceRevenue = 0;
    double negativeImbalance = 0;
    double positiveImbalance = 0;
    for (Scenario powerScenario: windfarmOutputScenarios) {
      double pi = powerScenario.getValues().get(tiIndex);
      double prob = powerScenario.getProbability();
      if (pi > pbid) { // negative imbalance
        negativeImbalance += (pi - pbid) * prob;
      }
      else if (pbid > pi) { // positive imbalance
        positiveImbalance += (pbid - pi) * prob;
      }
    }
    imbalanceRevenue = (crd * positiveImbalance) + (-cru * negativeImbalance);
    double totalRevenue = mcpRevenue + imbalanceRevenue;
    return totalRevenue;
  } // getRevenue()

  private boolean getPrices (Timeslot ts, Double mcp, Double minp, Double maxp)
  {
    // get the prices - this is little tricky
    if (mapTimeSlotClearingPrices.get(ts) == null) {
      long tsi = ts.getStartInstant().getMillis();
      // go back 24 hours
      tsi -= 24 * 60 * 60 * 1000; // convert 24 hours to miliseconds
      Timeslot oldTs = timeslotRepo.findByInstant(new Instant(tsi));
      if (oldTs == null) {
        oldTs = timeslotRepo.makeTimeslot(new Instant(tsi)); // is this
                                                             // permitted?
      }
      // make sure everything is alright before proceeding further
      if (!mapTimeSlotClearingPrices.containsKey(oldTs)
          || !mapTimeSlotMinAskPrices.containsKey(oldTs)
          || !mapTimeSlotMaxAskPrices.containsKey(oldTs)
          || mapTimeSlotClearingPrices.get(oldTs) == null
          || mapTimeSlotMinAskPrices.get(oldTs) == null
          || mapTimeSlotMaxAskPrices.get(oldTs) == null) {
        return false;
      }
      mcp = mapTimeSlotClearingPrices.get(oldTs);
      maxp = mapTimeSlotMaxAskPrices.get(oldTs);
      minp = mapTimeSlotMinAskPrices.get(oldTs);
    }
    else {
      // make sure everything is alright before proceeding further
      if (!mapTimeSlotClearingPrices.containsKey(ts)
          || !mapTimeSlotMinAskPrices.containsKey(ts)
          || !mapTimeSlotMaxAskPrices.containsKey(ts)
          || mapTimeSlotClearingPrices.get(ts) == null
          || mapTimeSlotMinAskPrices.get(ts) == null
          || mapTimeSlotMaxAskPrices.get(ts) == null) {
        return false;
      }
      mcp = mapTimeSlotClearingPrices.get(ts);
      maxp = mapTimeSlotMaxAskPrices.get(ts);
      minp = mapTimeSlotMinAskPrices.get(ts);
    }

    return true;
  }
} // class WindFarmOfferCalculator
