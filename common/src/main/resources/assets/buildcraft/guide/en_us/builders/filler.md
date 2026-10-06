<chapter name="Filler"/>
<lore>
Filling in a huge hole or flattening a hill is a long, dull job. The filler does it for you.
</lore>
<no_lore>
The filler builds (or clears) a pattern in an area, using the blocks in its inventory.
</no_lore>

<chapter name="Setting up"/>
Place the filler next to the corner of an area marked with <link to="core/marker_volume"/>s or a
<link to="core/volume_box"/>. If the volume box has a <link to="builders/filler_planner"/>, the filler builds the
planned pattern.

<chapter name="Patterns"/>
Click the pattern in the GUI to pick one of the 19 patterns: fill, clear, box, frame, pyramids, stairs, spheres and flat shapes. Some patterns have parameters (such as which way the stairs go), set by clicking the slots next to the pattern. The invert button swaps which blocks are filled and which are cleared, and the excavate button stops the filler from breaking blocks in the way.

Gates on a pipe next to the filler can set its pattern with the pattern actions.

<chapter name="Running"/>
The filler needs power and blocks to place. It breaks from the top down and places from the bottom up, and keeps the chunks it works in loaded.
<recipes_usages stack="buildcraft:filler"/>
