package com.gamersafer.minecraft.ablockalypse.location;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.IOException;

public class LocationTypeAdapter extends TypeAdapter<Location> {

    @Override
    public void write(JsonWriter out, Location location) throws IOException {
        if (location == null) {
            if (!out.getSerializeNulls()) {
                out.setSerializeNulls(true);
                out.nullValue();
                out.setSerializeNulls(false);
            } else {
                out.nullValue();
            }
        } else {
            out.beginObject();
            out.name("world");
            out.value(location.getWorld().getName());
            out.name("x");
            out.value(location.getX());
            out.name("y");
            out.value(location.getY());
            out.name("z");
            out.value(location.getZ());
            out.name("yaw");
            out.value(location.getYaw());
            out.name("pitch");
            out.value(location.getPitch());
            out.endObject();
        }
    }

    @Override
    public Location read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        String worldName = "world";
        double x = 0, y = 0, z = 0;
        float yaw = 0, pitch = 0;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "world" -> worldName = in.nextString();
                case "x" -> x = in.nextDouble();
                case "y" -> y = in.nextDouble();
                case "z" -> z = in.nextDouble();
                case "yaw" -> yaw = (float) in.nextDouble();
                case "pitch" -> pitch = (float) in.nextDouble();
            }
        }
        in.endObject();

        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }
}
