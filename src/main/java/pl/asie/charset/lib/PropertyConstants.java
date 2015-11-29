package pl.asie.charset.lib;

import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;

public final class PropertyConstants {
	public static final PropertyDirection FACING = PropertyDirection.create("facing");

	public static final PropertyBool UP = PropertyBool.create("up");
	public static final PropertyBool DOWN = PropertyBool.create("down");
	public static final PropertyBool NORTH = PropertyBool.create("north");
	public static final PropertyBool EAST = PropertyBool.create("east");
	public static final PropertyBool SOUTH = PropertyBool.create("south");
	public static final PropertyBool WEST = PropertyBool.create("west");

	private PropertyConstants() {

	}
}
