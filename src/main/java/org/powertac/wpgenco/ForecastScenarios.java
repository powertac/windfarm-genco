package org.powertac.wpgenco;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;

/**
 * This class represents forecast scenarios
 * 
 * @author Shashank Pande (spande00@gmail.com)
 *
 */
@ConfigurableInstance
public class ForecastScenarios {
	
	private static class Scenario {
		private double probability = 0.0;
		private List<Double> values = new ArrayList<Double>();
		public Scenario(double prob, List<Double> vals) {
			this.probability = prob;
			this.values = vals;
		}
		public double getProbability() {
			return this.probability;
		}
		public List<Double> getValues() {
			return this.values;
		}
	}
	
	@ConfigurableValue(valueType = "Integer", description="lead time to the first value of forecast")
	private int forecastRangeStart = 1;
	@ConfigurableValue(valueType = "Integer", description="lead time to the last value of forecast")
	private int forecastRangeEnd   = 24;
	
	private WindfarmGenco  windfarmGenco;
	private List<Double>   windSpeedForecast = new ArrayList<Double>();
	private List<Scenario> windSpeedForecastErrorScenarios = new ArrayList<Scenario>();
	private List<Scenario> windSpeedForecastScenarios = new ArrayList<Scenario>();
	private List<Scenario> windFarmPowerOutputScenarios = new ArrayList<Scenario>();
	
	public ForecastScenarios(WindfarmGenco ref) {
		this.windfarmGenco = ref;
	}
	


}
