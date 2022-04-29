package com.gamersafer.minecraft.ablockalypse.location;

import com.gamersafer.minecraft.ablockalypse.Character;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LocationManager {

    private final Map<Character, Location> cinematicLocations;
    private final List<Location> spawnPoints;
    private Location hospital;

    public LocationManager() {
        cinematicLocations = new EnumMap<>(Character.class);
        hospital = null;
        spawnPoints = new ArrayList<>();
        // todo load persisted locations
    }

    public Optional<Location> getHospital() {
        return Optional.ofNullable(hospital);
    }

    public void setHospital(Location hospital) {
        this.hospital = hospital;
    }

    public Optional<Location> getCinematicLoc(Character character) {
        return Optional.ofNullable(cinematicLocations.get(character));
    }

    public void setCinematicLoc(Character character, Location location) {
        cinematicLocations.put(character, location);
    }

    public List<Location> getSpawnPoints() {
        return spawnPoints;
    }

    public boolean addSpawnPoint(Location location) {
        return spawnPoints.add(location);
    }

    public boolean removeSpawnPoint(Location location) {
        return spawnPoints.remove(location);
    }

    public void shutdown() {
        // todo persist locations
    }

}
