package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserNerarbyAttraction;
import com.openclassrooms.tourguide.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	//single atribute of rewardCentral objet
	private final RewardCentral rewardsCentral;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;
	//attribute of eXecutorService get the newCachedThreadPool method
	ExecutorService executor = Executors.newCachedThreadPool();	


	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService, RewardCentral rewardsCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		this.rewardsCentral = rewardsCentral;
		
		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	//modification of the method getUserLocation who use trackUserLocation whith parameter use
	//if the locationsvisited of user is empty and
	//else if return the last visited location of the user
	public VisitedLocation getUserLocation(User user) throws InterruptedException, ExecutionException {
		if(user.getVisitedLocations().isEmpty()){
			trackUserLocation(user);
		}		
		return  user.getLastVisitedLocation();
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}


	//modification of the trackUserLocation method to use completableFuture 
	//and executor method to accelerate the process unlike the old version of method who return visited location 
	public void trackUserLocation(User user)  { 
  CompletableFuture.supplyAsync(() -> {
            return gpsUtil.getUserLocation(user.getUserId());
        }, executor)
			.thenAccept(location ->{
				user.addToVisitedLocations(location);
				try {
					rewardsService.calculateRewards(user);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			
			});		
	}
	//modification of getNearByattractions for create list of the five objetc who have this attributes:
	// Name of Tourist attraction, 
	// Tourist attractions lat/long, 
	// The user's location lat/long, 
	// The distance in miles between the user's location and each of the attractions.
	// The reward points for visiting each Attraction.
	public List<UserNerarbyAttraction> getNearByAttractions(VisitedLocation visitedLocation) {
		List<UserNerarbyAttraction> nearbyAttractions = new ArrayList<>();
		List<UserNerarbyAttraction> nearbyAttractSort = new ArrayList<>();


		
		for (Attraction attraction : gpsUtil.getAttractions()) {
			UserNerarbyAttraction userAtract = new UserNerarbyAttraction();
			userAtract.setAttractionName(attraction.attractionName);
			userAtract.setAttractLong(attraction.longitude);
			userAtract.setAttractLat( attraction.latitude);
			userAtract.setUserLong(visitedLocation.location.longitude);
			userAtract.setUserLat(visitedLocation.location.latitude);
			userAtract.setDistance(rewardsService.getDistance(attraction, visitedLocation.location));
			userAtract.setAttractRewardPoints(rewardsCentral.getAttractionRewardPoints(attraction.attractionId, visitedLocation.userId));
			
			nearbyAttractions.add(userAtract);
		
		}
		Collections.sort(nearbyAttractions, (o1, o2) -> (o1.getDistance() > o2.getDistance()) ? 1 :
                                       (o1.getDistance()< o2.getDistance()) ? -1 : 0);

		int nbAttract = 5;
	if(nearbyAttractions.size() < nbAttract){
		nbAttract = nearbyAttractions.size();
	}
	for (int i = 0; i < nbAttract; i++) {
        nearbyAttractSort.add(nearbyAttractions.get(i));
    }



		return nearbyAttractSort;

	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
