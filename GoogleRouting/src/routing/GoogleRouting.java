package routing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.TravelMode;



public class GoogleRouting {

	/**
	 * 
	 * @param args
	 * 
	 * The arguments should be in teh following order:
	 * APIKey
	 * date [year/month/day]
	 * input file path
	 * output file path
	 * index of trip id [first column index is 0]
	 * startCoord X index
	 * startCoord Y index
	 * endCoord X index
	 * endCoord Y index
	 * index of start time in minutes
	 * 
	 * Coordinates should be in WGS84 format
	 * @throws Exception
	 */
	public static void ptRouting(String[] args, TravelMode mode) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(args[2]));
		BufferedWriter writer = new BufferedWriter(new FileWriter(args[3]));
		
		GeoApiContext context = new GeoApiContext.Builder()
			    .apiKey(args[0])
			    .build();
		
		String year = args[1].split("/")[0];
		String month = args[1].split("/")[1];
		String day = args[1].split("/")[2];

		int indexId = Integer.parseInt(args[4]);
		int indexStartCoordX = Integer.parseInt(args[5]);
		int indexStartCoordY = Integer.parseInt(args[6]);
		int indexEndCoordX = Integer.parseInt(args[7]);
		int indexEndCoordY = Integer.parseInt(args[8]);
		int indexTime = Integer.parseInt(args[9]);

		reader.readLine();
		String s = reader.readLine();
		
		while (s != null) {
			
			String[] arr = s.split(";");
			//time in minutes
			double m = Integer.parseInt(arr[indexTime]);
			 
			int h = (int) (m / 60);
			int min = (int)m - h * 60;
			DateTime time = new DateTime(Integer.parseInt(year), 
					Integer.parseInt(month), Integer.parseInt(day),
					h, min, DateTimeZone.getDefault());			
		
			double distance = 0.0;		
			double travelTime = 0.0;
			//mb: alternatives are not working at the moment aug '17
			DirectionsRoute[] route = (DirectionsApi.getDirections(context, arr[indexStartCoordX] + " " + arr[indexStartCoordY] ,
				arr[indexEndCoordX] + " " + arr[indexEndCoordY]).mode(mode).
					departureTime(time).alternatives(false).await()).routes;
			
			if (route == null || route.length == 0) {
					writer.write(arr[indexId] + ";-99;-99;-99");
					writer.newLine();
				}
			else if (route.length > 0) {
				boolean transit = false;
				
				for (DirectionsLeg l : route[0].legs) {
					if (mode.name().equals("TRANSIT")) {
						if (l.steps.length > 1) {
							
							int arrSec = l.arrivalTime.getSecondOfDay();
							
							if (l.arrivalTime.getDayOfMonth() != Integer.parseInt(day))
								arrSec += 24 * 3600;
													
							travelTime = arrSec - (h * 3600 + min * 60);
						}
					}
					else {
						travelTime += l.duration.inSeconds;
						
					}
					distance += l.distance.inMeters;
					
					//we check if the trip had transit legs
					for (DirectionsStep ds :l.steps) {
						
						if (ds.travelMode.name().equals(mode.name()))
							transit = true;
					}
				}
				//only the trips that were routed with transit:
				
				writer.write(arr[0] + ";" +  travelTime + ";"+ distance + ";" + transit);
				writer.newLine();
			}				

			s = reader.readLine();
		}
				
		writer.flush();
		writer.close();
		reader.close();		

	}
	
	public static void main(String[] args) throws Exception {
	
		GoogleRouting.ptRouting(args, TravelMode.DRIVING);
	}
	
	
}