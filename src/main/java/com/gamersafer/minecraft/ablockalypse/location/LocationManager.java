package com.gamersafer.minecraft.ablockalypse.location;

import com.gamersafer.minecraft.ablockalypse.AblockalypsePlugin;
import com.gamersafer.minecraft.ablockalypse.Character;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Location;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LocationManager {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Location.class, new LocationTypeAdapter())
            .setPrettyPrinting().create();

    private final Map<Character, Location> cinematicLocations;
    private final List<Location> spawnPoints;
    private Location hospital;

    private int lastSpawnPointIndex;

    public LocationManager() {
        cinematicLocations = new EnumMap<>(Character.class);
        hospital = null;
        spawnPoints = new ArrayList<>();
        lastSpawnPointIndex = -1;

        // try to load locations form json
        loadLocations();
    }

    private void loadLocations() {
        try (Reader reader = Files.newBufferedReader(Paths.get(AblockalypsePlugin.getInstance().getDataFolder() + "/locations.json"))) {

            LocationStorageJson data = GSON.fromJson(reader, LocationStorageJson.class);

            if (data != null) {
                cinematicLocations.putAll(data.cinematicLocations());
                hospital = data.hospital();
                spawnPoints.addAll(data.spawnPoints());
            }
        } catch (NoSuchFileException ignore) {
            // it will be created on shutdown
        } catch (IOException e) {
            throw new RuntimeException("Unable to load the locations from the json file", e);
        }
    }

    private void saveLocations() {
        try (Writer writer = Files.newBufferedWriter(Paths.get(AblockalypsePlugin.getInstance().getDataFolder() + "/locations.json"),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            LocationStorageJson data = new LocationStorageJson(cinematicLocations, spawnPoints, hospital);

            GSON.toJson(data, writer);

        } catch (IOException e) {
            throw new RuntimeException("Unable to save the locations from the json file", e);
        }
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

    /**
     * There can be n spawn points, and we cycle through them to avoid spawning players close to each other.
     *
     * @return the next spawn point or an empty optional if there isn't any spawn point set
     */
    public Optional<Location> getNextSpawnPoint() {
        if (spawnPoints.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(spawnPoints.get(++lastSpawnPointIndex % spawnPoints.size()));
    }

    public boolean addSpawnPoint(Location location) {
        return spawnPoints.add(location);
    }

    public boolean removeSpawnPoint(Location location) {
        return spawnPoints.remove(location);
    }

    public void shutdown() {
        // save the locations to json
        saveLocations();
    }

    @SuppressWarnings({"FieldMayBeFinal"}) // we can't use a record since gson doesn't support them
    private static final class LocationStorageJson {
        private Map<Character, Location> cinematicLocations;
        private List<Location> spawnPoints;
        private Location hospital;

        private LocationStorageJson(Map<Character, Location> cinematicLocations, List<Location> spawnPoints,
                                    Location hospital) {
            this.cinematicLocations = cinematicLocations;
            this.spawnPoints = spawnPoints;
            this.hospital = hospital;
        }

        public Map<Character, Location> cinematicLocations() {
            return cinematicLocations;
        }

        public List<Location> spawnPoints() {
            return spawnPoints;
        }

        public Location hospital() {
            return hospital;
        }
    }

}
