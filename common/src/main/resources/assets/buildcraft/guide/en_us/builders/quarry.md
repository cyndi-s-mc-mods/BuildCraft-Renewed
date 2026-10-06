<chapter name="Quarry"/>
<lore>
Mining by hand is slow and dangerous. A quarry digs out a whole area, all the way down, while you do something more interesting.
</lore>
<no_lore>
The quarry mines out an area, layer by layer, down to bedrock.
</no_lore>

<chapter name="Setting up"/>
Mark out an area with <link to="core/marker_volume"/>s (at least 4 by 4) and place the quarry next to a corner, facing away from it. Without markers, the quarry mines an 11 by 11 area behind it. It builds a frame around the area, then lowers its drill to mine each layer.

<chapter name="Running"/>
The quarry needs power: the more it gets, the faster it mines. Mined items go into an adjacent chest or pipe. Fluids are left behind. A quarry keeps the chunks it works in loaded while it runs.
<recipes_usages stack="buildcraft:quarry"/>
