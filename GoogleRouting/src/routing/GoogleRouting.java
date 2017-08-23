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
	 * The arguments should be in the following order:
	 * APIKey
	 * date [year/month/day]
	 * input file path
	 * output file path
	 * index of trip id [first column index is 0]
	 * start latitude index
	 * start longitude index
	 * end latitude index
	 * end longitude index
	 * waypoints latitude index
	 * waypoints longitude index
	 * index of start time in minutes
	 * 
	 * Coordinates should be in WGS84 format
	 * latitude and longitude GPS coordinates
	 * example of a valid coordinate in Switzerland: 47.3 8.7 
	 * @throws Exception
	 */
	public static void carbikewalkRouting(String[] args, TravelMode mode) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(args[2]));
		BufferedWriter writer = new BufferedWriter(new FileWriter(args[3]));
		writer.write("tripId;travelTime;distance;expectedModeUsed");
		writer.newLine();
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
		int indexWaypointsX = Integer.parseInt(args[9]);
		int indexWaypointsY = Integer.parseInt(args[10]);
		int indexTime = Integer.parseInt(args[11]);

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
			
			String[] waypointsX = arr[indexWaypointsX].split(",");
			String[] waypointsY = arr[indexWaypointsY].split(",");
			String[] waypoints = new String[waypointsX.length];
			for(int i = 0; i < waypointsX.length; i++) {
				
				waypoints[i] = waypointsX + ", " + waypointsY;
			}
			
			DirectionsRoute[] route = (DirectionsApi.getDirections(context, arr[indexStartCoordX] + " " + arr[indexStartCoordY] ,
				arr[indexEndCoordX] + " " + arr[indexEndCoordY]).mode(mode).waypoints(waypoints).
					departureTime(time).alternatives(false).await()).routes;
			
			if (route == null || route.length == 0) {
					writer.write(arr[indexId] + ";-99;-99;-99");
					writer.newLine();
				}
			else if (route.length > 0) {
				boolean transit = false;
				
				for (DirectionsLeg l : route[0].legs) {
					
					travelTime += l.duration.inSeconds;					
					distance += l.distance.inMeters;
					
					//we check if the trip had steps with the required mode
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
	
	public static void ptRouting(String[] args) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(args[2]));
		BufferedWriter writer = new BufferedWriter(new FileWriter(args[3]));
		writer.write("tripId;travelTime;expectedModeUsed;accessTime;waitingTime;transferTime;transfers;inVehicleTime;inVehicleDistance;egressTime");
		writer.newLine();
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
				arr[indexEndCoordX] + " " + arr[indexEndCoordY]).mode(TravelMode.TRANSIT).
					departureTime(time).alternatives(false).await()).routes;
			
			if (route == null || route.length == 0) {
					writer.write(arr[indexId] + ";-99;-99;-99;-99;-99;-99;-99;-99;-99");
					writer.newLine();
				}
			else if (route.length > 0) {
				boolean transit = false;
				int transfers = -1;
				double transferTime = 0;
				double inVehicleTime = 0;
				double inVehicleDistance = 0;
				double accessTime = 0;
				double egressTime = 0;
				double firstWaitingTime = 0.0;
				
				DirectionsLeg l = route[0].legs[0];
				if (l.steps.length > 1) {
				
					if (l.steps[0].travelMode.name().equals("WALKING"))
						accessTime = l.steps[0].duration.inSeconds;
					if (l.steps[l.steps.length - 1].travelMode.name().equals("WALKING"))
						egressTime = l.steps[l.steps.length - 1].duration.inSeconds;
					for (DirectionsStep ds :l.steps) {
						
						if (ds.travelMode.name().equals("TRANSIT")) {
							transit = true;
							transfers++;
							inVehicleTime += ds.duration.inSeconds;
							inVehicleDistance += ds.distance.inMeters;
						}
								
					}	
						
					transferTime = ((int)(l.arrivalTime.getMillis() - l.departureTime.getMillis())/1000)
							- egressTime - inVehicleTime; 
					firstWaitingTime = (l.departureTime.getDayOfMonth() - Integer.parseInt(day))
							* 24 * 3600 + l.departureTime.getSecondOfDay() - m * 60;
					travelTime = ((int)(l.arrivalTime.getMillis() - l.departureTime.getMillis())/1000) + firstWaitingTime;
					writer.write(arr[0] + ";" +  travelTime + ";" + transit
							 + ";" + accessTime + ";" + firstWaitingTime + ";" + transferTime 
							 + ";" + transfers + ";" + inVehicleTime + ";" + inVehicleDistance
							 + ";" + egressTime);
					writer.newLine();
				}
				else {
					writer.write(arr[indexId] + ";-99;false;-99;-99;-99;-99;-99;-99;-99");
					writer.newLine();
					
				}
			}				

			s = reader.readLine();
		}
				
		writer.flush();
		writer.close();
		reader.close();		

	}	
	
	public static void main(String[] args) throws Exception {
	
		//pt routing
		GoogleRouting.ptRouting(args);
		
		//all other kinds of rounting
		//second argument passed to this method
		//defines the mode to be used
		//GoogleRouting.carbikewalkRouting(args, TravelMode.DRIVING);
		
	}	
	
}