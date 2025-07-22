# AxolotlClient Waypoints

A minimap/worldmap and waypoints mod designed for modern versions.
This means that new features are likely to only get added to the latest version of minecraft.

Features:

- Minimap
	- Integrates into AxolotlClient's HUD system if it is present.
- World and level(/dimension)-specific waypoints
- Worldmap

This map mod also features reasonable performance by using map color values of blocks.

Certain aspects of the mod can be disabled by servers:
- Whether the minimap is enabled
- Whether caves may be seen on the world map or the minimap
	- Whether caves may be seen on the world map in dimensions with roofs (Nether)

This can either be done by
- Installing this mod on the server-side as well as on the client-side.

  It will then generate a config file for these options on the server and send them to the client. You can edit the config file and then use the `/axolotlclient_waypoints reload` command to load the edited config or modify it using `/axolotlclient_waypoints modify_config <property> <value>`. After running either command the values will be sent to all connected players again.
- Making use of system message properties for [xaero's minimap/world map](https://modrinth.com/mod/xaeros-minimap):

	- `§f§a§i§r§x§a§e§r§o` to disable caves on both the minimap & worldmap
	- `§x§a§e§r§o§w§m§n§e§t§h§e§r§i§s§f§a§i§r` to allow caves on the world map in dimensions with roofs
	- `§n§o§m§i§n§i§m§a§p` to disable the minimap
	- `§r§e§s§e§t§x§a§e§r§o` to reset all values

Note however that if the mod is installed on the server the use of xaero's properties will be disabled.

**Disclaimer**: Despite the system described above this mod does not have capabilities to disable itself on servers that do not allow usage of minimaps. It is your responsibility to adhere to server rules.
