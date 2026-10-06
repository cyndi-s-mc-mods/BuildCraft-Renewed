<chapter name="Path Marker"/>
<lore>
Some structures, like walls and roads, are long and thin and would take forever to build one blueprint at a time.
</lore>
<no_lore>
Path markers mark out a path for the builder to build along.
</no_lore>

<chapter name="Making a path"/>
Place path markers along the path you want, then join them in order with the <link to="core/marker_connector"/>: right click the first marker, then the second, the third and so on. A marker can be at most 64 blocks from the next. Joining the last marker back to the first makes a loop.

Right click a path marker with an empty hand to reverse the direction of its path.

<chapter name="Building along a path"/>
Place a <link to="builders/builder"/> next to one end of the path. It takes the path (the markers drop as items) and builds its blueprint or template at every block along the path, one after another.
<recipes_usages stack="buildcraft:marker_path"/>
