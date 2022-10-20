# Ablockalypse

## Permissions

| Permission                         | Action                                                                                     |
|------------------------------------|--------------------------------------------------------------------------------------------|
| ablockalypse.cmd.reload            | Access to _/ablockalypse reload_. Reload the config                                        |
| ablockalypse.cmd.backstory         | Access to _/ablockalypse backstory_. Select a character                                    |
| ablockalypse.cmd.stories.own       | Access to _/ablockalypse stories_ to see own past stories                                  |
| ablockalypse.cmd.stories.others    | Access to _/ablockalypse stories <player>_ to see the stories made by other players        |
| ablockalypse.cmd.story             | Access to _/ablockalypse story_ subcommands                                                |
| ablockalypse.cmd.hospital.list     | Access to _/ablockalypse hospital list_ to display all hospital locations                  |
| ablockalypse.cmd.hospital.add      | Access to _/ablockalypse hospital add_ to add a hospital location                          |
| ablockalypse.cmd.hospital.tp       | Ability to teleport to hospital locations by click on them in the list                     |
| ablockalypse.cmd.hospital.remove   | Access to _/ablockalypse hospital remove_ to delete a hospital location                    |
| ablockalypse.cmd.spawnpoint.list   | Access to _/ablockalypse spawnpoint list_ to display all spawnpoints                       |
| ablockalypse.cmd.spawnpoint.add    | Access to _/ablockalypse spawnpoint add_ to add a spawnpoint location                      |
| ablockalypse.cmd.spawnpoint.tp     | Ability to teleport to spawnpoints by click on them in the list                            |
| ablockalypse.cmd.spawnpoint.remove | Access to _/ablockalypse spawnpoint remove_ to remove a spawnpoint location                |
| ablockalypse.cmd.cinematic.set     | Access to _/ablockalypse cinematic <character> set_ to set a character cinematic location  |
| ablockalypse.cmd.cinematic.tp      | Access to _/ablockalypse cinematic <character> tp_ to tp to a character cinematic location |
| ablockalypse.cmd.cinematic.tp      | Access to _/ablockalypse cinematic <character> tp_ to tp to a character cinematic location |
| ablockalypse.canselect.<CHARACTER> | Ability to select a specific character from the backstory menu                             |

## PAPI placeholders

| Placeholder                                            | Description                                                                                                        |
|--------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| %ablockalypse_character%                               | The name of the character of the active story                                                                      |
| %ablockalypse_displayname%                             | The name the player choose for their current character                                                             |
| %ablockalypse_survivaltime%                            | How long the player survived with their current character                                                          |
| %ablockalypse_playtime%                                | The sum of the survival times of all the stories started by the player                                             |
| %ablockalypse_level%                                   | The level of the current character                                                                                 |
| %ablockalypse_ptop_survivaltime_playtime%              | Formatted longest survival time achieved by the player                                                             |
| %ablockalypse_ptop_survivaltime_character%             | Display name of the character used by the player in their longest survival time                                    |
| %ablockalypse_ptop_survivaltime_CHARATER_playtime%     | Formatted longest survival time achieved by the player with the given character                                    |
| %ablockalypse_alltop_survivaltime_X_name%              | Name of the player at the X position on the top survival time leaderboard (X is a number)                          |
| %ablockalypse_alltop_survivaltime_X_playtime%          | Formatted survival time of the story at the X position on the top survival time leaderboard                        |
| %ablockalypse_alltop_survivaltime_X_character%         | Display name of the story character at the X position on the top survival time leaderboard                         |
| %ablockalypse_alltop_survivaltime_CHARATER_X_name%     | Name of the player at the X position on the top survival time leaderboard of the given character(X is a number)    |
| %ablockalypse_alltop_survivaltime_CHARATER_X_playtime% | Formatted survival time of the story at the X position on the top survival time leaderboard by the given character |
