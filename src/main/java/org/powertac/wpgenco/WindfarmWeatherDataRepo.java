package org.powertac.wpgenco;


import java.util.ArrayList;
import java.util.List;

import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;

/**
 * This class provides data structure to unmarshal the XML file received from weather service and store that data
 * @author Shashank Pande (spande00@gmail.com)
 *
 */
public class WindfarmWeatherDataRepo {
	private List<WeatherForecastPrediction> weatherForecasts = new ArrayList<WeatherForecastPrediction>();
	
	public WindfarmWeatherDataRepo() {
		
	}
	
	public void clearOldData() {
		weatherForecasts.clear();
	}
	
	public void addWeatherForecastPrediction(WeatherForecastPrediction wfp) {
		if (wfp == null) return;
		weatherForecasts.add(wfp);
	}
	
	public List<Double> getWindSpeedForecast() {
		List<Double> windSpeeds = new ArrayList<Double>();
		for (WeatherForecastPrediction wfp : weatherForecasts) {
			windSpeeds.add(wfp.getWindSpeed());
		}
		return windSpeeds;
	}
	
	public List<Double> getTemperatureForecast() {
		List<Double> temperatures = new ArrayList<Double>();
		for (WeatherForecastPrediction wfp : weatherForecasts) {
			temperatures.add(wfp.getTemperature());
		}		
		return temperatures;
	}
	
	public List<Double> getAirPressureForecast() {
		List<Double> airpressures = new ArrayList<Double>();
		for (WeatherForecastPrediction wfp : weatherForecasts) {
			airpressures.add(1.225);
		}
		return airpressures;
	}
}
