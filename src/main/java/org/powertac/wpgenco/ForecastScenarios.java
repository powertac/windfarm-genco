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
	
	
	public static class Scenario {
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
	private List<Scenario> windSpeedForecastErrorScenarios = new ArrayList<Scenario>();
	private List<Scenario> windSpeedForecastScenarios = new ArrayList<Scenario>();
	private List<Scenario> windFarmPowerOutputScenarios = new ArrayList<Scenario>();
	
	public ForecastScenarios(WindfarmGenco ref) {
		this.windfarmGenco = ref;
		readWindForecastErrorScenarios();
	}
	
	/**
	 * Read wind forecast error scenarios.
	 * populate list windSpeedForecastErrorScenarios
	 */
	private void readWindForecastErrorScenarios() {
		
	}
	
	/**
	 * calculate wind speed forecast scenarios
	 */
	public void calcWindSpeedForecastScenarios() {
		windSpeedForecastScenarios.clear();		
		List<Double> windSpeedForecastValues = windfarmGenco.getWeatherForecast().getWindSpeeds();
		List<Double> windSpeeds = new ArrayList<Double>();
		for (Scenario errorScenario : windSpeedForecastErrorScenarios) {
			double probability = errorScenario.getProbability();
			List<Double> errorValues = errorScenario.getValues();
			for (int i = 0; i < errorValues.size(); i++) {
				double errval = errorValues.get(i);
				double windforecast = windSpeedForecastValues.get(i);
				double windSpeed = windforecast + errval;
				windSpeeds.add(windSpeed);
			}
			windSpeedForecastScenarios.add(new Scenario(probability,windSpeeds));
		} //for each error scenario
	} //calcWindSpeedForecastScenarios()
	
	/**
	 * Calculate Wind Power output scenarios.
	 */
	public void calcPowerOutputScenarios() {
		windFarmPowerOutputScenarios.clear();
		List<Double> powerOutputs = new ArrayList<Double>();
		List<Double> airPressures = windfarmGenco.getWeatherForecast().getAirPressure();
		List<Double> temperatures = windfarmGenco.getWeatherForecast().getTemperature();
		for (Scenario windSpForecastScenario : windSpeedForecastScenarios) {
			double probability = windSpForecastScenario.getProbability();
			powerOutputs.clear();
			List<Double> windSpeeds = windSpForecastScenario.getValues();
			for (int i = 0; i < windSpeeds.size(); i++) {
				double airpressure = airPressures.get(i);
				double windspeed = windSpeeds.get(i);
				double temperature = temperatures.get(i);
				double airdensity = WindfarmGenco.getDryAirDensity(airpressure, temperature);
				double powerout = windfarmGenco.getEstimatedPowerOutput(windspeed, airdensity);
				powerOutputs.add(powerout);
			}
			windFarmPowerOutputScenarios.add(new Scenario(probability, powerOutputs));
		} //for each wind speed scenario
	}
	
	public List<Scenario> getWindPowerOutputScenarios() {
		return Collections.unmodifiableList(windFarmPowerOutputScenarios);
	}

}
