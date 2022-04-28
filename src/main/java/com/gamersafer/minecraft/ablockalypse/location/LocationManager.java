package com.gamersafer.minecraft.ablockalypse.location;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.Location;

import java.util.Collection;

public class LocationManager {

    private final Multimap<LocationType, Location> locations;

    public LocationManager() {
        locations = HashMultimap.create();
    }

    // TODO SHOWROOM locations need to be associated with a character type

    public Collection<Location> getLocations(LocationType locationType) {
        return locations.get(locationType);
    }

    public boolean addLocation(LocationType locationType, Location location) {
        return locations.put(locationType, location);
    }

    public boolean removeLocation(LocationType locationType, Location location) {
        return locations.remove(locationType, location);
    }

}
