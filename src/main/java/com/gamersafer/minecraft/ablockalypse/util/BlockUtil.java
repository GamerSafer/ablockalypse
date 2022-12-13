package com.gamersafer.minecraft.ablockalypse.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;

public final class BlockUtil {

    /**
     * Updates the material of the door at the specified location.
     *
     * @param topHalfLocation the location of the top half of the door
     * @param material        the material to set the door to
     */
    public static void updateDoorMaterial(Location topHalfLocation, Material material) {
        if (topHalfLocation == null) {
            throw new IllegalArgumentException("Unable to update the door material. Its location cannot be null");
        }
        Block top = topHalfLocation.getBlock();
        BlockFace face = ((Door) top.getBlockData()).getFacing();
        Block bottom = top.getRelative(BlockFace.DOWN);
        bottom.setType(material, false);
        top.setType(material, false);

        Door bottomDoor = (Door) bottom.getBlockData();
        Door topDoor = (Door) top.getBlockData();
        bottomDoor.setHalf(Bisected.Half.BOTTOM);
        topDoor.setHalf(Bisected.Half.TOP);
        bottomDoor.setFacing(face);
        topDoor.setFacing(face);

        bottom.setBlockData(bottomDoor);
        top.setBlockData(topDoor);
    }

}
