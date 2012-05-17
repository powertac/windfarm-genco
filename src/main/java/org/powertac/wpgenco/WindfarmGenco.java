/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.powertac.wpgenco;

import java.util.List;
import org.apache.log4j.Logger;
import org.joda.time.Instant;

import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.IdGenerator;
import org.powertac.common.PluginConfig;
import org.powertac.common.Order;
import org.powertac.common.Timeslot;
import org.powertac.common.MarketPosition;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.common.RandomSeed;

/**
 * Represents a producer of power in the transmission domain. Individual
 * models are players on the wholesale side of the Power TAC day-ahead
 * market.
 * @author jcollins
 */
@Domain
@ConfigurableInstance
public class WindfarmGenco
  extends Broker
{
  static private Logger log = Logger.getLogger(WindfarmGenco.class.getName());

  // id values are standardized
  private long id = IdGenerator.createId();
  
  /** Current capacity of this producer in mW */
  private double currentCapacity;
  
  /** Per-timeslot variability */
  private double variability = 0.01;
  
  /** Mean-reversion tendency - portion of variability to revert
   *  back to nominal capacity */
  private double meanReversion = 0.2;
  
  private boolean inOperation = true;
  
  /** Proportion of time plant is working */
  private double reliability = 0.98;
  
  /** efficiency curve */
  WindFarmEfficiencyCurve efficiencyCurve = null;
  
  /** True if this is a renewable source */
  @SuppressWarnings("unused")
  private boolean renewable = true;
  
  protected BrokerProxy brokerProxyService;
 

  // configured parameters  
  //private String name;
  private int numberOfTurbines = 100;
  private double turbineCapacity = 1.5;
  private double cost = 1.0;
  private int commitmentLeadtime = 1;
  private double carbonEmissionRate = 0.0; 
  private double cutInSpeed = 4.0; // meters/sec
  private double cutOutSpeed = 25.0; //meters/sec
  private double maxPowerspeed = 14.0; //meters/sec
  private double sweepAreaOfTurbine = 2391.2; //m^2

  public WindfarmGenco (String username)
  {
    super(username, true, true);
    efficiencyCurve = new WindFarmEfficiencyCurve();
  }
  
  public void init (BrokerProxy proxy, RandomSeedRepo randomSeedRepo)
  {
    log.info("init " + getUsername());
    this.brokerProxyService = proxy;
    currentCapacity = turbineCapacity * numberOfTurbines;
  }
 
  /** True if plant is currently operating */
  public boolean isInOperation ()
  {
    return inOperation;
  }

  /**
   * Nominal or mean capacity of plant. This is the value toward which the
   * mean-reverting random walk reverts.
   */
  public double getNominalCapacity ()
  {
    return turbineCapacity * numberOfTurbines;
  }
  
  /**
   * Fluent setter for nominal capacity
   */
  @ConfigurableValue(valueType = "Double",
      description = "nominal output capacity of this windfarm-genco in MW")
  @StateChange
  public WindfarmGenco withNominalCapacity (double capacity)
  {
    this.turbineCapacity = capacity;
    this.currentCapacity = capacity * this.numberOfTurbines;
    return this;
  }

  /**
   * Ask price for energy from this plant.
   */
  public double getCost ()
  {
    return cost;
  }
  
  /**
   * Fluent setter for nominal capacity
   */
  @ConfigurableValue(valueType = "Double",
      description = "minimum payment/mwh needed to operate this plant")
  @StateChange
  public WindfarmGenco withCost (double cost)
  {
    this.cost = cost;
    return this;
  }
  
  /**
   * Probability that this plant will submit asks in any given timeslot
   */
  public double getReliability ()
  {
    return reliability;
  }
  
  /**
   * Fluent setter for reliability.
   */
  @ConfigurableValue(valueType = "Double",
      description = "probability that plant will participate in wholesale market")
  @StateChange
  public WindfarmGenco withReliability (double reliability)
  {
    this.reliability = reliability;
    return this;
  }

  /**
   * Leadtime to commit energy from this plant, expressed in number of
   * timeslots. Plant will not send orders to the market within this
   * leadtime unless it has at least partially committed power for the
   * timeslot in question.
   */
  public int getCommitmentLeadtime ()
  {
    return commitmentLeadtime;
  }
  
  /**
   * Fluent setter for commitment leadtime.
   */
  @ConfigurableValue(valueType = "Integer",
      description = "minimum leadtime for first commitment, in hours")
  @StateChange
  public WindfarmGenco withCommitmentLeadtime (int leadtime)
  {
    this.commitmentLeadtime = leadtime;
    return this;
  }
  
  /**
   * Rate at which this plant emits carbon, relative to a coal-fired 
   * thermal plant.
   */
  public double getCarbonEmissionRate ()
  {
    return carbonEmissionRate;
  }
  
  /**
   * Fluent setter for carbonEmissionRate.
   */
  @ConfigurableValue(valueType = "Double",
      description = "carbon emissions/mwh, relative to coal-fired plant")
  @StateChange
  public WindfarmGenco withCarbonEmissionRate (double rate)
  {
    this.carbonEmissionRate = rate;
    return this;
  }
  
  /**
   * Current capacity, varies by a mean-reverting random walk.
   */
  double getCurrentCapacity ()
  {
    return currentCapacity;
  }

  /**
   * Updates this model for the current timeslot, by adjusting
   * capacity, checking for downtime, and creating exogenous
   * commitments.
   */
  public void updateModel (Instant currentTime)
  {
    log.info("Update " + getUsername());

  }

	/**
	 * Generates Orders in the market to sell available capacity. No Orders are
	 * submitted if the plant is not in operation.
	 */
	public void generateOrders(Instant now, List<Timeslot> openSlots) {
		if (!inOperation) {
			log.info("not in operation - no orders");
			return;
		}
		// 1. get forecast error scenarios
		// 2. get wind speed forecast
		// 3. generate wind speed scenarios (wind forecast + forecast error)
		// 4. generate power output scenarios
		// 5. run optimization to determine bid quantity for all timeslots
		log.info("Generate orders for " + getUsername());
		int skip = (commitmentLeadtime - Competition.currentCompetition()
				.getDeactivateTimeslotsAhead());
		if (skip < 0)
			skip = 0;
//		for (Timeslot slot : openSlots) {
//			double availableCapacity = currentCapacity;
//			// do we receive these?
//			MarketPosition posn = findMarketPositionByTimeslot(slot);
//			if (skip-- > 0 && (posn == null || posn.getOverallBalance() == 0.0))
//				continue;
//			if (posn != null) {
//				// posn.overallBalance is negative if we have sold power in this
//				// slot
//				availableCapacity += posn.getOverallBalance();
//			}
//			if (availableCapacity > 0.0) {
//				// make an offer to sell
//				Order offer = new Order(this, slot, -availableCapacity, cost);
//				log.debug(getUsername() + " offers " + availableCapacity
//						+ " in " + slot.getSerialNumber() + " for " + cost);
//				brokerProxyService.routeMessage(offer);
//			}
//		}

	} //generateOrders()
  
  
  private void updateCapacity (double val)
  {

  }
  
  @StateChange
  private void setCurrentCapacity (double val)
  {
    currentCapacity = val;
  }
  
  private void updateInOperation (double val)
  {
    setInOperation(val <= reliability);
  }
  
  @StateChange
  private void setInOperation (boolean op)
  {
    inOperation = op;
  }
  
 /**
  * Estimate power output from given wind speed and air density 
  * @param windSpeed sind speed in m/sec
  * @param airDensity air density in kg/m^3
  * @return estimated power output in MW
  */
  private double getEstimatedPowerOutput(double windSpeed, double airDensity) {
	  if (windSpeed < cutInSpeed) {
		  return 0;
	  } else if ((windSpeed >= maxPowerspeed) && (windSpeed < cutOutSpeed)) {
		  return (this.turbineCapacity * this.numberOfTurbines);
	  } else if (windSpeed > this.cutOutSpeed) {
		  return 0;
	  } else {
		  double powerOutput = 0;
		  double efficiency = efficiencyCurve.getEfficiency(windSpeed);
		  powerOutput = 0.5 * efficiency * sweepAreaOfTurbine * airDensity * Math.pow(windSpeed, 3) * numberOfTurbines;
		  return powerOutput / 1000000; // convert Watts to MW
	  }
  }
  
  /**
   * get air density from air pressure in Pa and temperature in centigrade
   * @param airPressure air pressure in Pa
   * @param tempInCentigrade temperature in centigrade
   * @return air density in kg/m^3
   */
  private static double getDryAirDensity(double airPressure, double tempInCentigrade) {
	  double T = tempInCentigrade + 273.15; //temp in deg Kelvin
	  double R = 287.05; //Specific gas constant for dry air J/kg.K
	  
	  double airDensity = airPressure/(R * T);
	  return airDensity;	  
  }
  
  
}
