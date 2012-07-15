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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a scenario.
 * A scenario can be a wind speed error scenario,
 * or a wind power production scenario etc.
 * 
 * @author spande00 (Shashank Pande)
 * 
 */
public class Scenario implements Comparable<Scenario>
{
  private int scenarioNumber = 0;
  private double probability = 0.0;
  private Map<Integer, Double> values = new HashMap<Integer, Double>();

  public Scenario (final int number, final double prob)
  {
    this.scenarioNumber = number;
    this.probability = prob;
  }

  public int getScenarioNumber ()
  {
    return this.scenarioNumber;
  }

  public double getProbability ()
  {
    return this.probability;
  }

  public Map<Integer, Double> getValues ()
  {
    return Collections.unmodifiableMap(this.values);
  }

  public void addValue (int hour, double value)
  {
    if (hour > 0) {
      values.put(hour, value);
    }
  }

  @Override
  public int compareTo (Scenario o)
  {
    if (this.scenarioNumber < o.getScenarioNumber()) {
      return -1;
    }
    else if (this.scenarioNumber > o.getScenarioNumber()) {
      return 1;
    }
    return 0;
  }

  @Override
  public boolean equals (Object o)
  {
    if (o instanceof Scenario) {
      Scenario obj = (Scenario) o;
      return (this.scenarioNumber == obj.getScenarioNumber());
    }
    return false;
  }
}
