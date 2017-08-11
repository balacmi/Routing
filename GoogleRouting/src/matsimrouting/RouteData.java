package matsimrouting;

public class RouteData {
	
	String hId;	
	String tripId;
	String travlelTime;
	String departureTime;
	String accessTime;
	String egressTime;
	String firstWaiting;
	String transferTime;
	String numberOfTransfers;
	String distance;
	
	public RouteData() { }
	
	public RouteData(String hId, String tripId, String travlelTime, String departureTime, String accessTime,
			String egressTime, String firstWaiting, String transferTime, String numberOfTransfers, String distance) {
		this.hId = hId;
		this.tripId = tripId;
		this.travlelTime = travlelTime;
		this.departureTime = departureTime;
		this.accessTime = accessTime;
		this.egressTime = egressTime;
		this.firstWaiting = firstWaiting;
		this.transferTime = transferTime;
		this.numberOfTransfers = numberOfTransfers;
		this.distance = distance;
	}	

	public void sethId(String hId) {
		this.hId = hId;
	}

	public void setTripId(String tripId) {
		this.tripId = tripId;
	}

	public void setTravlelTime(String travlelTime) {
		this.travlelTime = travlelTime;
	}

	public void setDepartureTime(String departureTime) {
		this.departureTime = departureTime;
	}

	public void setAccessTime(String accessTime) {
		this.accessTime = accessTime;
	}

	public void setEgressTime(String egressTime) {
		this.egressTime = egressTime;
	}

	public void setFirstWaiting(String firstWaiting) {
		this.firstWaiting = firstWaiting;
	}

	public void setTransferTime(String transferTime) {
		this.transferTime = transferTime;
	}

	public void setNumberOfTransfers(String numberOfTransfers) {
		this.numberOfTransfers = numberOfTransfers;
	}

	public void setDistance(String distance) {
		this.distance = distance;
	}

	@Override
	public String toString() {
		return hId + ";" + tripId + ";" + travlelTime + ";" + departureTime + ";" + accessTime + 
				";" + egressTime + ";" + firstWaiting + ";" + transferTime + ";" + numberOfTransfers
				+ ";" + distance;
	}
	

}
