package com.gamersafer.minecraft.ablockalypse.command;

enum Permission {

    CMD_RELOAD,
    CMD_BACKSTORY,
    CMD_BACKSTORY_OTHERS,
    CMD_STORIES_OWN,
    CMD_STORIES_OTHERS,
    CMD_STORY,
    CMD_HOSPITAL_LIST,
    CMD_HOSPITAL_ADD,
    CMD_HOSPITAL_TP,
    CMD_HOSPITAL_REMOVE,
    CMD_SPAWNPOINT_LIST,
    CMD_SPAWNPOINT_ADD,
    CMD_SPAWNPOINT_TP,
    CMD_SPAWNPOINT_REMOVE,
    CMD_CINEMATIC_SET,
    CMD_CINEMATIC_TP,
    CMD_RESET,
    CMD_SAFEHOUSE,

    // boosters permissions are in Booster.java
    ;

    private final String permission;

    Permission() {
        this.permission = "ablockalypse." + name().toLowerCase().replace('_', '.');
    }

    @Override
    public String toString() {
        return permission;
    }

}
