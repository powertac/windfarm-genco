/*
 * Copyright 2012 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.state.Domain;

/**
 * This class represents the windfarm efficiency curve.
 * It stores the efficiency curve data and provides API to retrieve efficiency at
 * given wind speed.
 * 
 * @author Shashank Pande
 * 
 */
@Domain
@ConfigurableInstance
public class WindFarmEfficiencyCurve
{

  private static class WindSpeedband
  {
    private double fromWindSpeed = 0;
    private double toWindSpeed = 0;

    public WindSpeedband (double fromSpeed, double toSpeed)
    {
      this.fromWindSpeed = fromSpeed;
      this.toWindSpeed = toSpeed;
    }

    public boolean isWithinSpeedband (double windSpeed)
    {
      if ((windSpeed >= fromWindSpeed) && (windSpeed < toWindSpeed)) {
        return true;
      }
      else {
        return false;
      }
    }
  } // static class WindSpeedband

  /** Configured values to be read as List of Strings */
  @ConfigurableValue(valueType = "List", description = "wind speed bands")
  private List<String> cfgWindSpeedbands = null;
  @ConfigurableValue(valueType = "List", description = "value of slope in a linear equation")
  private List<String> cfgSlope = null;
  @ConfigurableValue(valueType = "List", description = "value of y intercept in a linear equation")
  private List<String> cfgYIntercept = null;

  /** This map should be populated from configured values */
  private List<WindSpeedband> windSpeedbands = new ArrayList<WindSpeedband>();
  private List<Double> slope = new ArrayList<Double>();
  private List<Double> yIntercept = new ArrayList<Double>();

  /**
   * Constructor
   */
  public WindFarmEfficiencyCurve ()
  {
    initialize();
  } // WindFarmEfficiencyCurve()

  private void initialize ()
  {

    // write code here to populate the mapWindSpeedToEfficiency
    for (int i = 0; i < cfgWindSpeedbands.size(); i++) {
      String from_to = cfgWindSpeedbands.get(i);
      String[] fromtoarray = from_to.split("-");
      WindSpeedband wspb =
        new WindSpeedband(Double.valueOf(fromtoarray[0]),
                          Double.valueOf(fromtoarray[1]));
      this.windSpeedbands.add(wspb);

      this.slope.add(Double.valueOf(cfgSlope.get(i)));
      this.yIntercept.add(Double.valueOf(cfgYIntercept.get(i)));

    }
  }

  /**
   * get efficiency for given wind speed in m/sec
   * 
   * @param windSpeed
   *          wind speed in m/sec
   * @return efficiency
   */
  public double getEfficiency (double windSpeed)
  {
    int indexInList = -1;
    for (int i = 0; i < windSpeedbands.size(); i++) {
      if (windSpeedbands.get(i).isWithinSpeedband(windSpeed)) {
        indexInList = i;
        break;
      }
    }
    if (indexInList > -1) {
      double m = slope.get(indexInList);
      double b = yIntercept.get(indexInList);
      return (m * windSpeed + b);
    }
    else {
      return 0;
    }
  } // get efficiency

} // class WindFarmEfficiencyCurve
