package serverconnect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.IconStyle;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.LineStyle;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Style;

public class KMLgeneratorPerUser {
	private String historyURL, eventURL, path, event, user;
	private boolean readSucceed;
	private ArrayList<String> users;

	public KMLgeneratorPerUser(String _historyURL, String _path,
			String _eventURL) {
		historyURL = _historyURL;
		readSucceed = true;
		path = _path;
		eventURL = _eventURL;
		users = new ArrayList<String>(); 
	}

	public boolean createKMLPath() {

		Map<EventDate, LatLng> sortedLatLngs = readData();
		Map<String, LatLng> buoysLatLng = readBuoys();
		if (!readSucceed)
			return false;
		return createKMLPath(sortedLatLngs,buoysLatLng);

	}

	public boolean createKMLTimeStamp() {
		Map<EventDate, LatLng> sortedLatLngs = readData();
		Map<String, LatLng> buoysLatLng = readBuoys();
		if (!readSucceed)
			return false;
		return createKMLTimeStamp(sortedLatLngs,buoysLatLng);
	}

	protected Map<EventDate, LatLng> readData() {
		Map<EventDate, LatLng> sortedLatLngs = new TreeMap<EventDate, LatLng>();
		try {
			JSONObject jsonHistory = JsonReader.readJsonFromUrl(historyURL);
			JSONArray jsonArray = jsonHistory.getJSONArray("event");
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObj = (JSONObject) jsonArray.get(i);
				String lat = jsonObj.getString("lat");
				String lng = jsonObj.getString("lng");
				String user = jsonObj.getString("user");
				if(!users.contains(user)){
					users.add(user);
				}
				if (Double.parseDouble(lat) == 0
						|| Double.parseDouble(lng) == 0) {
					continue;
				}
				String time = jsonObj.getString("time");
				String date = jsonObj.getString("date");

				LatLng latLng = new LatLng(Double.parseDouble(lat),
						Double.parseDouble(lng));
				EventDate eventDate = new EventDate(time, date,user);
				sortedLatLngs.put(eventDate, latLng);
			}

		} catch (Exception e) {
			readSucceed = false;
			e.printStackTrace();
		}
		return sortedLatLngs;
	}

	protected Map<String, LatLng> readBuoys() {
		Map<String, LatLng> buoysLatLng = new HashMap<String, LatLng>();
		try {
			JSONObject json = JsonReader.readJsonFromUrl(eventURL);
			JSONArray jsonArray = json.getJSONArray("positions");
			JSONObject jsonObj = (JSONObject) jsonArray.get(0);
			for (int i = 0; i < 10; i++) {
				String blat = "b" + (i + 1) + "lat";
				String blng = "b" + (i + 1) + "lng";
				String buoyName = "buoy#" + (i + 1);
				String lat = jsonObj.getString(blat);
				String lng = jsonObj.getString(blng);

				double tlat = Double.parseDouble(lat);
				double tlng = Double.parseDouble(lng);
				if (tlat != 0 && tlng != 0) {
					buoysLatLng.put(
							buoyName,
							new LatLng(Double.parseDouble(lat), Double
									.parseDouble(lng)));
				} else {
					continue;
				}
			}
			return buoysLatLng;
		} catch (JSONException e) {
			readSucceed = false;
			return null;
		} catch (IOException e) {
			readSucceed = false;
			return null;
		}
	}

	protected int getUserIndex(String user){
		return users.indexOf(user);
	}
	protected int getUserSize(){
		return users.size();
	}
	
	
	private boolean createKMLPath(Map<EventDate, LatLng> sortedLatLngs,Map<String, LatLng> buoysLatLng) {
		Iterator<Map.Entry<EventDate, LatLng>> i = sortedLatLngs.entrySet()
				.iterator();
		Map.Entry<EventDate, LatLng> entry = (Map.Entry<EventDate, LatLng>) i
				.next();
		
		Iterator<Map.Entry<String, LatLng>> buoyIterator = buoysLatLng.entrySet().iterator();
		Map.Entry<String, LatLng> buoyEntry = (Map.Entry<String, LatLng>)buoyIterator.next();

		Kml kml = new Kml();
		Document doc = kml.createAndSetDocument();
		doc.setName("Path");
		doc.setDescription("Event# : " + event + " , Sailor : " + user);

		//Buoys Style		
		Style buoysStyle = createBuoyStyle(doc);
		
		// style 3
		Style style3 = doc.createAndAddStyle();
		style3.setId("style3");
		IconStyle ics3 = style3.createAndSetIconStyle();
		ics3.setScale(1);
		ics3.createAndSetIcon().setHref(
				"http://bmr.comuv.com/ic_user_sailor.png");

		// style 2 line style
		Style style2 = doc.createAndAddStyle();
		style2.setId("style2");
		LineStyle lineStyle = new LineStyle();
		lineStyle.setColor("73FF0000");
		lineStyle.setWidth(5);
		style2.setLineStyle(lineStyle);

		// style 1
		Style style1 = doc.createAndAddStyle();
		style1.setId("style1");
		IconStyle ics2 = style1.createAndSetIconStyle();
		ics2.createAndSetIcon().setHref(
				"http://bmr.comuv.com/ic_user_sailor.png");
		ics2.setScale(1);

		// Start placemark
		doc.createAndAddPlacemark()
				.withName("FROM")
				.withStyleUrl("#" + style3.getId())
				.createAndSetPoint()
				.addToCoordinates(entry.getValue().getLng(),
						entry.getValue().getLat());

		Placemark pathMarks = doc.createAndAddPlacemark();
		pathMarks.setName("PATH");
		pathMarks.setStyleUrl("#" + style2.getId());
		pathMarks.createAndSetExtendedData().createAndAddData("true")
				.setName("_SnapToRoads");
		LineString lineString = pathMarks.createAndSetLineString();
		lineString.setTessellate(true);

		while (i.hasNext()) {
			entry = (Map.Entry<EventDate, LatLng>) i.next();
			/*
			 * System.out.println(); System.out.print(entry.getKey()+",");
			 * System.out.print(entry.getValue().getLat()+",");
			 * System.out.print(entry.getValue().getLng());
			 */
			if (!i.hasNext())
				break;
			lineString.addToCoordinates(entry.getValue().getLng(), entry
					.getValue().getLat());

		}
		while(buoyIterator.hasNext()){
			pathMarks = doc.createAndAddPlacemark();
			pathMarks.setStyleUrl("#" + buoysStyle.getId());
			pathMarks.createAndSetPoint().addToCoordinates(buoyEntry.getValue().getLng(), buoyEntry.getValue().getLat());
			buoyEntry = (Map.Entry<String, LatLng>)buoyIterator.next();
		}
		
		doc.createAndAddPlacemark()
				.withName("TO")
				.withStyleUrl("#" + style1.getId())
				.createAndSetPoint()
				.addToCoordinates(entry.getValue().getLng(),
						entry.getValue().getLat());
		try {
			String timeStamp = new SimpleDateFormat("ddMMyy_HHmmss")
					.format(new Date());
			File f = new File(path + "/" + event + "_" + user + "_" + timeStamp
					+ "_OnlyPath.kml");
			kml.marshal(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	protected boolean createKMLTimeStamp(Map<EventDate, LatLng> sortedLatLngs,Map<String, LatLng> buoysLatLng) {
		Iterator<Map.Entry<EventDate, LatLng>> useIterator = sortedLatLngs.entrySet().iterator();
		Map.Entry<EventDate, LatLng> userEntry = (Map.Entry<EventDate, LatLng>) useIterator.next();
		
		Iterator<Map.Entry<String, LatLng>> buoyIterator = buoysLatLng.entrySet().iterator();
		Map.Entry<String, LatLng> buoyEntry = (Map.Entry<String, LatLng>)buoyIterator.next();
		String when = "";

		Kml kml = new Kml();
		Document doc = kml.createAndSetDocument();
		doc.setName("Path");
		doc.setDescription("Event# : " + event + " , Sailor : " + user);

		// buoysStyle
		Style buoysStyle = createBuoyStyle(doc);
		
		// style 1
		Style style1 = doc.createAndAddStyle();
		style1.setId("style1");
		IconStyle ics1 = style1.createAndSetIconStyle();
		ics1.setScale(1);
		ics1.createAndSetIcon().setHref("http://bmr.comuv.com/ic_user_sailor.png");
		
		// style 2
		Style style2 = doc.createAndAddStyle();
		style2.setId("style2");
		IconStyle ics2 = style2.createAndSetIconStyle();
		ics2.setScale(1);
		ics2.createAndSetIcon().setHref("http://bmr.comuv.com/ic_user_sailor.png");

		// style 3
		Style style3 = doc.createAndAddStyle();
		style3.setId("style3");
		IconStyle ics3 = style3.createAndSetIconStyle();
		ics3.setScale(1);
		ics3.createAndSetIcon().setHref("http://bmr.comuv.com/ic_user_sailor.png");

		Placemark timeMarks = doc.createAndAddPlacemark();
		timeMarks.setName("FROM");
		when = userEntry.getKey().getDate() + "T" + userEntry.getKey().getTime() + "Z";
		timeMarks.createAndSetTimeStamp().setWhen(when);
		timeMarks.setStyleUrl("#" + style1.getId());
		timeMarks.createAndSetPoint().addToCoordinates(
				userEntry.getValue().getLng(), userEntry.getValue().getLat());

		while (useIterator.hasNext()) {
			userEntry = (Map.Entry<EventDate, LatLng>) useIterator.next();
			if (!useIterator.hasNext())
				break;
			timeMarks = doc.createAndAddPlacemark();
			when = userEntry.getKey().getDate() + "T" + userEntry.getKey().getTime()
					+ "Z";
			timeMarks.createAndSetTimeStamp().setWhen(when);
			timeMarks.setStyleUrl("#" + style2.getId());
			timeMarks.createAndSetPoint().addToCoordinates(
					userEntry.getValue().getLng(), userEntry.getValue().getLat());
		}
		
		while(buoyIterator.hasNext()){
			timeMarks = doc.createAndAddPlacemark();
			timeMarks.setStyleUrl("#" + buoysStyle.getId());
			timeMarks.createAndSetPoint().addToCoordinates(
					buoyEntry.getValue().getLng(), buoyEntry.getValue().getLat());
			buoyEntry = (Map.Entry<String, LatLng>)buoyIterator.next();
		}
		

		timeMarks = doc.createAndAddPlacemark();
		timeMarks.setName("TO");
		when = userEntry.getKey().getDate() + "T" + userEntry.getKey().getTime() + "Z";
		timeMarks.createAndSetTimeStamp().setWhen(when);
		timeMarks.setStyleUrl("#" + style3.getId());
		timeMarks.createAndSetPoint().addToCoordinates(
				userEntry.getValue().getLng(), userEntry.getValue().getLat());

		try {
			String timeStamp = new SimpleDateFormat("ddMMyy_HHmmss").format(new Date());
			File f = new File(path + "/" + event + "_" + user + "_" + timeStamp+ "_WithTimeStamp.kml");
			kml.marshal(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		return true;

	}

	public void setEvent(String event) {
		this.event = event;
	}

	public void setUser(String user) {
		this.user = user;
	}
	
	
	
	public String getHistoryURL() {
		return historyURL;
	}

	public String getEventURL() {
		return eventURL;
	}

	public String getPath() {
		return path;
	}

	public String getEvent() {
		return event;
	}

	public String getUser() {
		return user;
	}

	public boolean isReadSucceed() {
		return readSucceed;
	}

	public ArrayList<String> getUsers() {
		return users;
	}

	protected Style createBuoyStyle(Document doc){
		Style buoysStyle = doc.createAndAddStyle();
		buoysStyle.setId("buoysStyle");
		IconStyle icbuoysStyle = buoysStyle.createAndSetIconStyle();
		icbuoysStyle.setScale(1);
		icbuoysStyle.createAndSetIcon().setHref("http://bmr.comuv.com/ic_buoy.png");
		return buoysStyle;
	}

	class LatLng {
		double lat, lng;

		private LatLng(double _lat, double _lng) {
			lat = _lat;
			lng = _lng;
		}

		public double getLat() {
			return lat;
		}

		public double getLng() {
			return lng;
		}

		@Override
		public String toString() {
			return "lat:" + lat + ",Lng:" + lng;
		}
	}

	class EventDate implements Comparable<Object> {
		String time, date,user;

		EventDate(String _time, String _date) {
			time = _time;
			date = _date;
		}
		EventDate(String _time, String _date,String _user) {
			time = _time;
			date = _date;
			user=_user;
		}

		public String getTime() {
			return time;
		}

		public String getDate() {
			return date;
		}
		public String getUser() {
			return user;
		}
		@Override
		public String toString() {

			return "Time : " + time + " , Date : " + date;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			return this.compareTo(obj) == 0;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((time == null) ? 0 : time.hashCode());
			result = prime * result
					+ Integer.parseInt(time.replaceAll(":", ""));
			return result;
		}

		@Override
		public int compareTo(Object o) {
			return Integer.parseInt(time.replaceAll(":", ""))
					- Integer
							.parseInt(((EventDate) o).time.replaceAll(":", ""));
		}

	}

}
