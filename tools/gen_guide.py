#!/usr/bin/env python3
"""Generates the guide book's pages, index and recipes in common/src/main/resources/assets/buildcraft/guide.

Most pages come from BuildCraft 7.99's guide (legacy/BuildCraftGuide), with their item ids and chapter names updated. The
pages in NEW_PAGES were written for this port, either for things the old guide never covered or where the mechanics
changed. Run tools/gen_resources.py first: the guide's recipes are read from the generated recipe files.

Page format: a subset of markdown with BuildCraft's tags. See GuidePageParser for what is supported.
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..')
LEGACY = os.path.join(ROOT, 'legacy', 'BuildCraftGuide', 'guide_resources', 'assets')
LEGACY_LANG = os.path.join(ROOT, 'legacy', 'buildcraft_resources', 'assets', 'buildcraft', 'lang', 'en_US.lang')
RESOURCES = os.path.join(ROOT, 'common', 'src', 'main', 'resources')
OUT = os.path.join(RESOURCES, 'assets', 'buildcraft', 'guide')
RECIPES = os.path.join(RESOURCES, 'data', 'buildcraft', 'recipe')
LANG = os.path.join(RESOURCES, 'assets', 'buildcraft', 'lang', 'en_us.json')
NS = 'buildcraft'

# Old ids (module:path) that changed in the port
ID_CHANGES = {
    'buildcraftcore:engine': 'engine_redstone',
    'buildcraftsilicon:plug_gate': 'gate',
    'buildcraftsilicon:plug_facade': 'facade',
    'buildcraftsilicon:plug_filter': 'filter',
    'buildcraftsilicon:plug_lens': 'lens',
    'buildcrafttransport:wire': 'pipe_wire_red',
}


def new_id(old):
    """Turns an old item id into the port's."""
    if old in ID_CHANGES:
        return f'{NS}:{ID_CHANGES[old]}'
    mod, path = old.split(':', 1)
    if not mod.startswith('buildcraft'):
        return old
    path = path.replace('pipe_cobble_', 'pipe_cobblestone_')
    return f'{NS}:{path}'


# Old page ids (module:group/name) and where they went
PAGE_CHANGES = {
    'buildcraftcore:block/engine_wood': 'energy/engine_redstone',
    'buildcraftcore:item/list': 'core/list',
    'buildcraftsilicon:action/pulsar_constant': 'action/pulsar_constant',
    'buildcraftsilicon:action/pulsar_single': 'action/pulsar_single',
    'buildcraftsilicon:item/plug_light_sensor': 'silicon/plug_light_sensor',
    'buildcraftsilicon:item/plug_pulsar': 'silicon/plug_pulsar',
    'buildcraftsilicon:trigger/light_high': 'trigger/light_high',
    'buildcraftsilicon:trigger/light_low': 'trigger/light_low',
    'buildcrafttransport:item/plug_power_adaptor': 'transport/plug_power_adaptor',
    'buildcrafttransport:item/wire': 'transport/wire',
    'buildcrafttransport:pipe/daizuli_item': 'transport/pipe_daizuli_item',
    'buildcrafttransport:pipe/emzuli_item': 'transport/pipe_emzuli_item',
    'buildcrafttransport:pipe/lapis_item': 'transport/pipe_lapis_item',
}

# (old file, relative to <module>/compat/buildcraft/guide/en_us) -> (new page, item id or statement tag)
LEGACY_PAGES = {}


def legacy(module, rel, page, ref):
    LEGACY_PAGES[(module, rel)] = (page, ref)


for gear in ['wood', 'stone', 'iron', 'gold', 'diamond']:
    legacy('buildcraftcore', f'item/gear_{gear}.md', f'core/gear_{gear}', f'item:{NS}:gear_{gear}')
for tool in ['wrench', 'paintbrush', 'list']:
    legacy('buildcraftcore', f'item/{tool}.md', f'core/{tool}', f'item:{NS}:{tool}')
legacy('buildcraftcore', 'block/engine_wood.md', 'energy/engine_redstone', f'item:{NS}:engine_redstone')
legacy('buildcraftenergy', 'block/engine_stone.md', 'energy/engine_stirling', f'item:{NS}:engine_stirling')
legacy('buildcraftenergy', 'block/engine_iron.md', 'energy/engine_combustion', f'item:{NS}:engine_combustion')
for block in ['chute', 'mining_well', 'heat_exchange', 'distiller', 'tank', 'pump', 'flood_gate']:
    legacy('buildcraftfactory', f'block/{block}.md', f'factory/{block}', f'item:{NS}:{block}')
legacy('buildcraftfactory', 'block/auto_workbench.md', 'factory/autoworkbench_item', f'item:{NS}:autoworkbench_item')
for plug, item in [('plug_light_sensor', 'plug_light_sensor'), ('plug_pulsar', 'plug_pulsar'), ('plug_lens', 'lens'),
                   ('plug_gate', 'gate'), ('plug_facade', 'facade'), ('plug_filter', 'filter')]:
    legacy('buildcraftsilicon', f'item/{plug}.md', f'silicon/{plug}', f'item:{NS}:{item}')
for plug, item in [('plug_blocker', 'plug_blocker'), ('plug_power_adaptor', 'plug_power_adaptor'), ('wire', 'pipe_wire_red')]:
    legacy('buildcrafttransport', f'item/{plug}.md', f'transport/{plug}', f'item:{NS}:{item}')
PIPES = ['structure', 'wood_item', 'cobble_item', 'stone_item', 'quartz_item', 'iron_item', 'gold_item', 'diamond_item',
         'obsidian_item', 'sandstone_item', 'clay_item', 'void_item', 'stripes_item', 'diamond_wood_item', 'lapis_item',
         'daizuli_item', 'emzuli_item', 'wood_fluid', 'cobble_fluid', 'stone_fluid', 'quartz_fluid', 'iron_fluid',
         'gold_fluid', 'diamond_fluid', 'sandstone_fluid', 'clay_fluid', 'void_fluid', 'diamond_wood_fluid']
for pipe in PIPES:
    item = 'pipe_' + pipe.replace('cobble_', 'cobblestone_')
    legacy('buildcrafttransport', f'pipe/{pipe}.md', f'transport/{item}', f'item:{NS}:{item}')

# Statements: (module, group, file name, statement tag)
STATEMENTS = [
    ('buildcraftcore', 'trigger', 'always_on', 'buildcraft:true'),
    ('buildcraftcore', 'trigger', 'redstone_active', 'buildcraft:redstone.input.active'),
    ('buildcraftcore', 'trigger', 'redstone_inactive', 'buildcraft:redstone.input.inactive'),
    ('buildcraftcore', 'trigger', 'inventory_empty', 'buildcraft:inventory.empty'),
    ('buildcraftcore', 'trigger', 'inventory_contains', 'buildcraft:inventory.contains'),
    ('buildcraftcore', 'trigger', 'inventory_space', 'buildcraft:inventory.space'),
    ('buildcraftcore', 'trigger', 'inventory_full', 'buildcraft:inventory.full'),
    ('buildcraftcore', 'trigger', 'inventory_below_25', 'buildcraft:inventorylevel.below25'),
    ('buildcraftcore', 'trigger', 'inventory_below_50', 'buildcraft:inventorylevel.below50'),
    ('buildcraftcore', 'trigger', 'inventory_below_75', 'buildcraft:inventorylevel.below75'),
    ('buildcraftcore', 'trigger', 'fluid_empty', 'buildcraft:fluid.empty'),
    ('buildcraftcore', 'trigger', 'fluid_contains', 'buildcraft:fluid.contains'),
    ('buildcraftcore', 'trigger', 'fluid_space', 'buildcraft:fluid.space'),
    ('buildcraftcore', 'trigger', 'fluid_full', 'buildcraft:fluid.full'),
    ('buildcraftcore', 'trigger', 'fluid_below_25', 'buildcraft:fluidlevel.below25'),
    ('buildcraftcore', 'trigger', 'fluid_below_50', 'buildcraft:fluidlevel.below50'),
    ('buildcraftcore', 'trigger', 'fluid_below_75', 'buildcraft:fluidlevel.below75'),
    ('buildcraftcore', 'trigger', 'engine_blue', 'buildcraft:engine.blue'),
    ('buildcraftcore', 'trigger', 'engine_green', 'buildcraft:engine.green'),
    ('buildcraftcore', 'trigger', 'engine_yellow', 'buildcraft:engine.yellow'),
    ('buildcraftcore', 'trigger', 'engine_red', 'buildcraft:engine.red'),
    ('buildcraftcore', 'trigger', 'engine_overheat', 'buildcraft:engine.overheat'),
    ('buildcraftcore', 'trigger', 'machine_active', 'buildcraft:machine.scheduled'),
    ('buildcraftcore', 'trigger', 'machine_inactive', 'buildcraft:machine.done'),
    ('buildcraftsilicon', 'trigger', 'light_low', 'buildcraft:light.dark'),
    ('buildcraftsilicon', 'trigger', 'light_high', 'buildcraft:light.bright'),
    ('buildcrafttransport', 'trigger', 'items_traversing', 'buildcraft:pipe.containsItems'),
    ('buildcrafttransport', 'trigger', 'pipe_empty', 'buildcraft:pipe.empty'),
    ('buildcrafttransport', 'trigger', 'fluids_traversing', 'buildcraft:pipe.containsFluids'),
    ('buildcrafttransport', 'trigger', 'pipe_signal_active', 'buildcraft:pipe.wire.red.active'),
    ('buildcrafttransport', 'trigger', 'pipe_signal_inactive', 'buildcraft:pipe.wire.red.inactive'),
    ('buildcraftcore', 'action', 'redstone', 'buildcraft:redstone.output'),
    ('buildcraftcore', 'action', 'machine_control_on', 'buildcraft:machine.on'),
    ('buildcraftcore', 'action', 'machine_control_off', 'buildcraft:machine.off'),
    ('buildcraftcore', 'action', 'machine_control_loop', 'buildcraft:machine.loop'),
    ('buildcraftsilicon', 'action', 'pulsar_constant', 'buildcraft:pulsar.constant'),
    ('buildcraftsilicon', 'action', 'pulsar_single', 'buildcraft:pulsar.single'),
    ('buildcrafttransport', 'action', 'pipe_signal_active', 'buildcraft:pipe.wire.output.red'),
    ('buildcrafttransport', 'action', 'pipe_colour', 'buildcraft:pipe.colour.red'),
    ('buildcrafttransport', 'action', 'extraction_preset', 'buildcraft:extraction.preset.red'),
] + [('buildcrafttransport', 'action', f'pipe_direction_{d}', f'buildcraft:pipe.dir.{d}')
     for d in ['above', 'below', 'north', 'south', 'east', 'west']]

for module, group, name, tag in STATEMENTS:
    legacy(module, f'{group}/{name}.md', f'{group}/{name}', f'statement:{tag}')

# Pages written for the port: page -> (icon, markup). They replace any old page of the same name.
NEW_PAGES = {}


def unwrap(text):
    """Joins the lines of each paragraph (and list item), which are wrapped here only to keep the source readable."""
    out = []
    for line in text.strip().split('\n'):
        if line and out and out[-1] and not line.startswith(('<', '- ', '#')) and not out[-1].startswith('<') \
                and not out[-1].endswith('/>'):
            out[-1] += ' ' + line
        else:
            out.append(line)
    return '\n'.join(out) + '\n'


def page(path, icon, text):
    NEW_PAGES[path] = (icon, unwrap(text))


page('core/guide', f'item:{NS}:guide', '''
<chapter name="BuildCraft"/>
In your quest to build the most magnificent base imaginable it is often useful to know what all the tools and machines
at your disposal can do.

This book describes every block and item that BuildCraft adds, and the triggers and actions that gates use. Pick a
category from the contents to start, or follow the links in pages.

<chapter name="Using this book"/>
- Click an entry in the contents to read its page.
- The arrows at the bottom turn the pages, and the arrow at the top goes back to the contents.
- Tick "Lore" on the contents page to read pages as the player who discovered them, or untick it for plain facts.
- Pages for blocks and items show how to make them (and what they're used in) where they can.

<recipes stack="buildcraft:guide"/>
''')

page('energy/power', f'item:{NS}:engine_redstone', '''
<chapter name="Minecraft Joules"/>
BuildCraft's machines run on power measured in Minecraft Joules (MJ). Engines make power and push it into the machine (or
power pipe) they face; machines store some of what they receive and use it as they work.

<chapter name="Engines"/>
Engines need a redstone signal to run. They push power from their front, and turn to face a machine when hit with a
wrench. As they run they warm up, changing colour from blue to green, yellow and red. A red engine is close to
overheating: Stirling and combustion engines that overheat stop and must be cooled before they run again.
- <link to="energy/engine_redstone"/>
- <link to="energy/engine_stirling"/>
- <link to="energy/engine_combustion"/>
- <link to="energy/engine_creative"/>

<chapter name="Moving power"/>
Power pipes (kinesis pipes) carry power from engines to machines. Wooden power pipes take power from engines, and
the other kinds pass it along. The <link to="transport/plug_power_adaptor"/> lets item and fluid pipes connect to
power pipes. Lasers send power to laser tables without pipes.
''')

page('energy/engine_creative', f'item:{NS}:engine_creative', '''
<chapter name="Creative Engine"/>
The creative engine makes power from nothing, and never overheats. It can only be had in creative mode.

<chapter name="Output"/>
Right click it with a wrench to change how much power it makes each tick, from 1 MJ up to 1,280 MJ. Like the other
engines, it only runs while it has a redstone signal.
''')

page('energy/oil', f'item:{NS}:oil_bucket', '''
<chapter name="Oil"/>
Oil is found in the world in oil wells (pools of oil, sometimes with spouts reaching the surface) and oil lakes in
deserts and oceans. It is collected with a <link to="factory/pump"/>.

<chapter name="Refining"/>
Crude oil can be burnt in a combustion engine, but it is much better refined. The <link to="factory/distiller"/>
splits a hot oil into two lighter (gas) and heavier (liquid) parts, and the <link to="factory/heat_exchange"/> heats and
cools fluids. Each fluid comes in three temperatures: cold, hot and searing.
- Crude oil splits into gaseous fuel and dense oil; dense oil into heavy oil and residue.
- Distilled oil, heavy oil and the fuels can be split further into light, mixed and dense fuels.

<chapter name="Fuels"/>
The lighter a fuel, the more power it gives in a <link to="energy/engine_combustion"/>. Residue is not a fuel.
''')

page('core/spring', f'item:{NS}:spring', '''
<chapter name="Springs"/>
Water springs are found in the bedrock at the bottom of the world. A pump placed above one (with pipes reaching down to
it) can pump water from it forever, as the spring keeps refilling the water above it.

Oil springs are found under the spouts of large oil wells. They can't be broken, even in creative mode.
''')

page('core/power_tester', f'item:{NS}:power_tester', '''
<chapter name="Power Tester"/>
The power tester takes all the power it is given. Right click it to see how much power it received in the last tick:
useful for checking how much power an engine or a line of power pipes really delivers.
''')

page('core/marker_volume', f'item:{NS}:marker_volume', '''
<chapter name="Volume Marker"/>
<lore>
Controlling where you want your machines to run is very important during construction, otherwise they may end up out
of control.
</lore>
<no_lore>
Volume markers mark out a box for machines such as the quarry, filler and architect table.
</no_lore>

<chapter name="Marking an area"/>
Place markers at the corners of the box you want: up to 64 blocks apart, and in line with each other along each axis.
Right click a marker to connect it to the markers in line with it, or connect two markers with the
<link to="core/marker_connector"/>. A box needs connections along two or three axes: a flat area or a full box.

Powering a marker with redstone shows lasers in every direction that it can connect along.

<chapter name="Using the area"/>
Place a machine next to a corner of the box, outside it. The machine takes the area and the markers drop as items.
<recipes_usages stack="buildcraft:marker_volume"/>
''')

page('core/marker_path', f'item:{NS}:marker_path', '''
<chapter name="Path Marker"/>
<lore>
Some structures, like walls and roads, are long and thin and would take forever to build one blueprint at a time.
</lore>
<no_lore>
Path markers mark out a path for the builder to build along.
</no_lore>

<chapter name="Making a path"/>
Place path markers along the path you want, then join them in order with the <link to="core/marker_connector"/>:
right click the first marker, then the second, the third and so on. A marker can be at most 64 blocks from the next.
Joining the last marker back to the first makes a loop.

Right click a path marker with an empty hand to reverse the direction of its path.

<chapter name="Building along a path"/>
Place a <link to="builders/builder"/> next to one end of the path. It takes the path (the markers drop as items) and
builds its blueprint or template at every block along the path, one after another.
<recipes_usages stack="buildcraft:marker_path"/>
''')

page('core/marker_connector', f'item:{NS}:marker_connector', '''
<chapter name="Marker Connector"/>
<lore>
Finer control over the areas that your machines work is vital in any large scale engineering project, and you need a
tool to control that.
</lore>
<no_lore>
The marker connector joins markers together, and moves and removes volume boxes.
</no_lore>

<chapter name="Markers"/>
Right click a marker to select it, then right click another marker to join them. Path markers stay selected after
joining, so a path can be made by clicking each marker in turn. Sneak right click the air to forget the selected marker.

<chapter name="Volume boxes"/>
- Right click a corner of a <link to="core/volume_box"/> to pick it up: the corner follows where you look. Right click
again to put it down, or sneak right click to put it back where it was.
- Right click the addon on a corner (such as a <link to="builders/filler_planner"/>) to use it, or sneak right click it
to take it off.
- Sneak right click a volume box to remove it.

Boxes that a machine is using can't be changed.
<recipes_usages stack="buildcraft:marker_connector"/>
''')

page('core/volume_box', f'item:{NS}:volume_box', '''
<chapter name="Volume Box"/>
<lore>
Markers can be awkward at times: they take a lot of breaking and placing to change the size of an area. The volume box
is a simpler solution: each corner of the box can be moved with ease.
</lore>
<no_lore>
A volume box is a box of lasers that marks out an area, without any blocks.
</no_lore>

<chapter name="Using it"/>
Right click a block with a volume box to place a one block box against it. Then use the
<link to="core/marker_connector"/> to drag its corners where you want them. A box can be up to 64 blocks along each side.

Fillers and architect tables placed next to a volume box use its area, and lock it while they do. Each corner can hold
an addon, such as the <link to="builders/filler_planner"/>.
<recipes_usages stack="buildcraft:volume_box"/>
''')

page('core/map_location', f'item:{NS}:map_location', '''
<chapter name="Map Location"/>
A map location remembers a place in the world. Right click with a blank one to store:
- a path, when clicking a joined path marker;
- an area, when clicking a volume marker or a volume box;
- a spot (a block and its side), when clicking anything else.

Zones are stored on map locations by the <link to="robotics/zone_planner"/>. While a map location is held, the place it
remembers is shown with particles. Sneak right click to make it blank again.
<recipes_usages stack="buildcraft:map_location"/>
''')

page('core/goggles', f'item:{NS}:goggles', '''
<chapter name="Goggles"/>
Laser beams can be hidden with the "silicon.renderLaserBeams" setting in config/buildcraft.properties. While wearing
goggles, you can still see them. Goggles give no protection, and never wear out.
<recipes_usages stack="buildcraft:goggles"/>
''')

page('core/fragile_fluid_shard', f'item:{NS}:fragile_fluid_shard', '''
<chapter name="Fragile Fluid Shard"/>
When a tank, a machine with tanks or a fluid pipe is broken, the fluid inside it isn't lost: it drops as fragile fluid
shards, each holding up to half a bucket. Right click a tank (or any block that holds fluid) with a shard to empty it
back in.
''')

page('core/gate_copier', f'item:{NS}:gate_copier', '''
<chapter name="Gate Copier"/>
The gate copier copies the settings of a gate (its triggers, actions and the connections between them) onto other
gates. Right click a gate with an empty copier to copy it, then right click other gates to paste. Gates with fewer
slots get as much of the settings as fit. Sneak right click to empty the copier.

It is made in an <link to="silicon/assembly_table"/>, from a wrench and an iron chipset.
<recipes_usages stack="buildcraft:gate_copier"/>
''')

page('factory/water_gel', f'item:{NS}:water_gel_spawn', '''
<chapter name="Water Gel"/>
Throw water gel at water to turn it into gelled water: a soft block that can be mined with a shovel, giving gel. Gel can
be put in a bucket to make water again. It's a handy way of moving or removing water.
<recipes_usages stack="buildcraft:water_gel_spawn"/>
''')

page('builders/quarry', f'item:{NS}:quarry', '''
<chapter name="Quarry"/>
<lore>
Mining by hand is slow and dangerous. A quarry digs out a whole area, all the way down, while you do something more
interesting.
</lore>
<no_lore>
The quarry mines out an area, layer by layer, down to bedrock.
</no_lore>

<chapter name="Setting up"/>
Mark out an area with <link to="core/marker_volume"/>s (at least 4 by 4) and place the quarry next to a corner, facing
away from it. Without markers, the quarry mines an 11 by 11 area behind it. It builds a frame around the area, then
lowers its drill to mine each layer.

<chapter name="Running"/>
The quarry needs power: the more it gets, the faster it mines. Mined items go into an adjacent chest or pipe. Fluids
are left behind. A quarry keeps the chunks it works in loaded while it runs.
<recipes_usages stack="buildcraft:quarry"/>
''')

page('builders/filler', f'item:{NS}:filler', '''
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
Click the pattern in the GUI to pick one of the 19 patterns: fill, clear, box, frame, pyramids, stairs, spheres and
flat shapes. Some patterns have parameters (such as which way the stairs go), set by clicking the slots next to the
pattern. The invert button swaps which blocks are filled and which are cleared, and the excavate button stops the
filler from breaking blocks in the way.

Gates on a pipe next to the filler can set its pattern with the pattern actions.

<chapter name="Running"/>
The filler needs power and blocks to place. It breaks from the top down and places from the bottom up, and keeps the
chunks it works in loaded.
<recipes_usages stack="buildcraft:filler"/>
''')

page('builders/filler_planner', f'item:{NS}:filler_planner', '''
<chapter name="Filler Planner"/>
The filler planner shows a filler pattern inside a <link to="core/volume_box"/>, before anything is built. Right click
a corner of a volume box with it to add it to that corner, then right click it with the
<link to="core/marker_connector"/> to choose the pattern. Blocks that would be placed are shown as ghostly cubes.

A <link to="builders/filler"/> placed next to the box builds the planned pattern.
<recipes_usages stack="buildcraft:filler_planner"/>
''')

page('builders/architect', f'item:{NS}:architect', '''
<chapter name="Architect Table"/>
The architect table copies a structure onto a template or blueprint. Place it next to the corner of an area marked with
<link to="core/marker_volume"/>s or a <link to="core/volume_box"/>, then put a blank template or blueprint in its left
slot. Once it has scanned the area, the written snapshot comes out on the right.

- A <link to="builders/template"/> remembers only where blocks are, not what they are.
- A <link to="builders/blueprint"/> remembers every block.
<recipes_usages stack="buildcraft:architect"/>
''')

page('builders/template', f'item:{NS}:template', '''
<chapter name="Template"/>
A template remembers the shape of a structure: which blocks are filled, but not what they are. A
<link to="builders/builder"/> builds it with whatever blocks are in its inventory. Write one with an
<link to="builders/architect"/>.
<recipes_usages stack="buildcraft:template"/>
''')

page('builders/blueprint', f'item:{NS}:blueprint', '''
<chapter name="Blueprint"/>
A blueprint remembers every block of a structure, and which way it faces. A <link to="builders/builder"/> builds an exact
copy (turned to face the way the builder does), as long as it has the blocks. Write one with an
<link to="builders/architect"/>, and keep copies of it in an <link to="builders/library"/>.
<recipes_usages stack="buildcraft:blueprint"/>
''')

page('builders/builder', f'item:{NS}:builder', '''
<chapter name="Builder"/>
<lore>
Building the same house twice is no fun at all. With a blueprint and a builder, you only ever have to build it once.
</lore>
<no_lore>
The builder builds the template or blueprint in its slot, using the blocks in its inventory.
</no_lore>

<chapter name="Building"/>
Put a written template or blueprint in the slot at the top, and the blocks it needs in the inventory. With power, the
builder clears the area and builds the snapshot in front of it: where the scanned area was, relative to the architect
table that wrote it.

<chapter name="Paths"/>
Place the builder next to one end of a path of <link to="core/marker_path"/>s and it builds the snapshot at every block
along the path, one after another.
<recipes_usages stack="buildcraft:builder"/>
''')

page('builders/library', f'item:{NS}:library', '''
<chapter name="Electronic Library"/>
The electronic library keeps a list of templates and blueprints. Put a written snapshot in the top left slot to add it
to the list (it comes back out on the right). Select an entry in the list and put a blank (or written) snapshot of the
same kind in the bottom left slot to copy the entry onto it.
<recipes_usages stack="buildcraft:library"/>
''')

page('builders/replacer', f'item:{NS}:replacer', '''
<chapter name="Replacer"/>
The replacer changes one block for another throughout a blueprint. Put the blueprint in the top slot, a
<link to="builders/schematic_single"/> of the block to replace bottom left, and one of the block to replace it with bottom
right. The schematics are used up.
<recipes_usages stack="buildcraft:replacer"/>
''')

page('builders/schematic_single', f'item:{NS}:schematic_single', '''
<chapter name="Single Schematic"/>
A single schematic remembers one block. Right click a block with a blank schematic to store it, and sneak right click
to clear it. Single schematics are used by the <link to="builders/replacer"/>.
<recipes_usages stack="buildcraft:schematic_single"/>
''')

page('silicon/laser', f'item:{NS}:laser', '''
<chapter name="Laser"/>
The laser sends power to laser tables in front of it, with a beam of light. Power it like any other machine: the more
power it gets, the brighter the beam. Several lasers can power the same table.
- <link to="silicon/assembly_table"/>
- <link to="silicon/advanced_crafting_table"/>
- <link to="silicon/integration_table"/>
<recipes_usages stack="buildcraft:laser"/>
''')

page('silicon/assembly_table', f'item:{NS}:assembly_table', '''
<chapter name="Assembly Table"/>
The assembly table makes chipsets, gates, pluggables, wires and facades from the items put in it, using power from
lasers. Click a recipe on the right to queue it (click again to make it repeat), and the table makes it whenever it has
the items.
<recipes_usages stack="buildcraft:assembly_table"/>
''')

page('silicon/advanced_crafting_table', f'item:{NS}:advanced_crafting_table', '''
<chapter name="Advanced Crafting Table"/>
The advanced crafting table crafts automatically, using power from lasers. Set the recipe in the grid (it only
remembers the items, it doesn't use them), and put the materials in the inventory below. Crafted items go into the
results on the right, and can be taken out with pipes.
<recipes_usages stack="buildcraft:advanced_crafting_table"/>
''')

page('silicon/integration_table', f'item:{NS}:integration_table', '''
<chapter name="Integration Table"/>
The integration table combines the item in its centre slot with the items around it, using power from lasers. BuildCraft
itself adds no integration recipes: other mods and modpacks add them.
<recipes_usages stack="buildcraft:integration_table"/>
''')

page('silicon/chipsets', f'item:{NS}:chipset_redstone', '''
<chapter name="Chipsets"/>
Chipsets are made in an <link to="silicon/assembly_table"/> from redstone and a material: iron, gold, quartz or diamond.
They are used to make gates, gate modifiers and many machines.
<recipes_usages stack="buildcraft:chipset_redstone"/>
''')

page('silicon/plug_timer', f'item:{NS}:plug_timer', '''
<chapter name="Timer"/>
The timer is a pluggable that gives gates on the same pipe its timer triggers, which turn on for a moment every few
seconds: short, medium and long. Place it on a side of a pipe.
<recipes_usages stack="buildcraft:plug_timer"/>
''')

page('transport/pipe_power', f'item:{NS}:pipe_wood_power', '''
<chapter name="Power Pipes"/>
Power (kinesis) pipes carry power from engines to machines. Wooden power pipes take power from engines next to them;
the other kinds carry it along, each with a limit on how much power they can carry each tick.
- Cobblestone and stone pipes carry little power.
- Quartz, iron, gold and diamond pipes carry more, in that order. Iron power pipes can be set (with a wrench) to limit
the power that goes through them.
- Sandstone pipes don't connect to machines, only to other pipes.
<recipes_usages stack="buildcraft:pipe_wood_power"/>
''')

page('transport/pipe_sealant', f'item:{NS}:pipe_sealant', '''
<chapter name="Pipe Sealant"/>
Pipe sealant makes transport pipes waterproof, turning them into fluid pipes: craft a transport pipe with pipe sealant
to get the fluid pipe of the same kind. It is made from green dye, or (eight at a time) from a bucket of residue.
<recipes_usages stack="buildcraft:pipe_sealant"/>
''')

page('robotics/zone_planner', f'item:{NS}:zone_planner', '''
<chapter name="Zone Planner"/>
The zone planner shows a map of the area around it, seen from above, on which zones can be painted in each of the 16
colours.

<chapter name="Using the map"/>
Drag the map to move it, and scroll to zoom in and out. To paint, pick up a coloured
<link to="core/paintbrush"/> (from the planner's slots, or your inventory) and drag over the map: with the left button
to add to the zone of that colour, or with the right button to remove from it. While holding a paintbrush, only its
colour's zone is shown.

<chapter name="Saving zones"/>
Put a paintbrush in the top right slot and a <link to="core/map_location"/> below it to save that colour's zone onto the
map location. Put a paintbrush and a map location holding a zone in the slots at the bottom left to load the zone into
that colour.
<recipes_usages stack="buildcraft:zone_planner"/>
''')

page('trigger/energy_high', 'statement:buildcraft:energy.high', '''
Energy stored high is a gate trigger for machines that store power (such as quarries, fillers, pumps and lasers) next to
the gate.

<chapter name="Requirements"/>
It is active while the machine's power store is more than 95% full.
''')

page('trigger/energy_low', 'statement:buildcraft:energy.low', '''
Energy stored low is a gate trigger for machines that store power (such as quarries, fillers, pumps and lasers) next to
the gate.

<chapter name="Requirements"/>
It is active while the machine's power store is less than 5% full: a sign that it needs more power than it is getting.
''')

page('trigger/power_requested', 'statement:buildcraft:pipe.requestsEnergy', '''
Power requested is a gate trigger for gates on power pipes.

<chapter name="Requirements"/>
It is active while the machines at the end of the pipe want power. Use it to turn engines on only when they are
needed.
''')

page('action/power_limit', 'statement:buildcraft:pipe.power_limit.iron_power.s1', '''
The power limit actions are found on gates on iron and diamond power pipes.

<chapter name="Effect"/>
While active, the pipe lets through no more than the power shown: its full rate, a half, a quarter and so on, down to
none at all. This is the same limit that hitting the pipe with a wrench sets.
''')

# Categories in the contents: (title, [pages])
CATEGORIES = [
    ('Introduction', ['core/guide', 'energy/power']),
    ('Tools', ['core/wrench', 'core/paintbrush', 'core/list', 'core/marker_connector', 'core/map_location', 'core/goggles',
               'core/gate_copier', 'core/fragile_fluid_shard', 'core/power_tester'] +
     [f'core/gear_{g}' for g in ['wood', 'stone', 'iron', 'gold', 'diamond']]),
    ('Areas', ['core/marker_volume', 'core/marker_path', 'core/volume_box', 'builders/filler_planner']),
    ('Energy', ['energy/engine_redstone', 'energy/engine_stirling', 'energy/engine_combustion', 'energy/engine_creative',
                'energy/oil', 'core/spring']),
    ('Factory', ['factory/mining_well', 'factory/pump', 'factory/flood_gate', 'factory/tank', 'factory/chute',
                 'factory/distiller', 'factory/heat_exchange', 'factory/autoworkbench_item', 'factory/water_gel']),
    ('Builders', ['builders/quarry', 'builders/filler', 'builders/architect', 'builders/template', 'builders/blueprint',
                  'builders/builder', 'builders/library', 'builders/replacer', 'builders/schematic_single']),
    ('Silicon', ['silicon/laser', 'silicon/assembly_table', 'silicon/advanced_crafting_table', 'silicon/integration_table',
                 'silicon/chipsets', 'silicon/plug_gate', 'silicon/plug_pulsar', 'silicon/plug_light_sensor',
                 'silicon/plug_timer', 'silicon/plug_lens', 'silicon/plug_filter', 'silicon/plug_facade']),
    ('Transport', [f'transport/pipe_{p.replace("cobble_", "cobblestone_")}' for p in PIPES if p.endswith('_item') or p == 'structure']
     + [f'transport/pipe_{p.replace("cobble_", "cobblestone_")}' for p in PIPES if p.endswith('_fluid')]
     + ['transport/pipe_power', 'transport/pipe_sealant', 'transport/wire', 'transport/plug_blocker',
        'transport/plug_power_adaptor']),
    ('Robotics', ['robotics/zone_planner']),
    ('Triggers', [f'trigger/{name}' for _, group, name, _ in STATEMENTS if group == 'trigger']
     + ['trigger/energy_high', 'trigger/energy_low', 'trigger/power_requested']),
    ('Actions', [f'action/{name}' for _, group, name, _ in STATEMENTS if group == 'action'] + ['action/power_limit']),
]


# Titles for pages that aren't about the item in their icon
TITLES = {
    'energy/power': 'Power and Engines',
    'energy/oil': 'Oil and Fuels',
    'silicon/chipsets': 'Chipsets',
    'transport/pipe_power': 'Power Pipes',
    'transport/wire': 'Pipe Wire',
    'core/fragile_fluid_shard': 'Fragile Fluid Shard',
}


def load_legacy_lang():
    names = {}
    with open(LEGACY_LANG, encoding='utf-8') as f:
        for line in f:
            if '=' in line:
                k, v = line.rstrip('\n').split('=', 1)
                names[k] = v
    return names


def convert(text, names):
    """Updates an old page: new ids, English chapter names and fixed tags."""
    text = re.sub(r'stack="([a-z_]+:[a-z_]+)"', lambda m: f'stack="{new_id(m.group(1))}"', text)

    def link(m):
        target, rest = m.group(1), m.group(2)
        if 'type="item_stack"' in rest:
            return f'<link to="{new_id(target)}"{rest}'
        return f'<link to="{PAGE_CHANGES.get(target, target)}"{rest}'
    text = re.sub(r'<link to="([^"]+)"([^>]*>)', link, text)
    text = re.sub(r'name="([a-zA-Z_.]+\.name)"', lambda m: f'name="{names.get(m.group(1), m.group(1))}"', text)
    # Some old pages closed tags they meant to open
    text = re.sub(r'</bold>([^<]+)</bold>', r'<bold>\1</bold>', text)
    text = text.replace(' join="false"', '')
    return text.strip() + '\n'


def write(path, text):
    full = os.path.join(OUT, 'en_us', path + '.md')
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w', encoding='utf-8') as f:
        f.write(text)


def ingredient(value):
    """A recipe ingredient as the guide stores it: an item id, or a tag starting with #."""
    if isinstance(value, list):
        value = value[0]
    if isinstance(value, dict):
        value = value.get('item') or ('#' + value['tag'])
    return value


def recipes():
    """Every crafting recipe, by result item."""
    out = {}
    for name in sorted(os.listdir(RECIPES)):
        with open(os.path.join(RECIPES, name)) as f:
            data = json.load(f)
        result = data.get('result', {})
        item, count = result.get('id'), result.get('count', 1)
        if data['type'] == 'minecraft:crafting_shaped':
            grid = [[ingredient(data['key'][c]) if c != ' ' else None for c in row] for row in data['pattern']]
        elif data['type'] == 'minecraft:crafting_shapeless':
            items = [ingredient(i) for i in data['ingredients']]
            grid = [items[i:i + 3] for i in range(0, len(items), 3)]
        else:
            continue
        out.setdefault(item, []).append({'grid': grid, 'count': count})
    return out


def main():
    names = load_legacy_lang()
    icons = {}
    written = set()
    for (module, rel), (path, ref) in LEGACY_PAGES.items():
        if path in NEW_PAGES:
            continue
        full = os.path.join(LEGACY, module, 'compat', 'buildcraft', 'guide', 'en_us', rel)
        with open(full, encoding='utf-8') as f:
            write(path, convert(f.read(), names))
        icons[path] = ref
        written.add(path)
    for path, (icon, text) in NEW_PAGES.items():
        write(path, text)
        icons[path] = icon
        written.add(path)
    index = []
    for title, pages in CATEGORIES:
        entries = []
        for path in pages:
            if path not in written:
                raise SystemExit(f'No page for {path}')
            kind, ref = icons[path].split(':', 1)
            entry = {'page': path, kind: ref}
            if path in TITLES:
                entry['title'] = TITLES[path]
            entries.append(entry)
        index.append({'title': title, 'entries': entries})
    listed = {p for _, pages in CATEGORIES for p in pages}
    missing = written - listed
    if missing:
        raise SystemExit(f'Pages not in the contents: {sorted(missing)}')
    with open(os.path.join(OUT, 'index.json'), 'w') as f:
        json.dump(index, f, indent=1)
        f.write('\n')
    with open(os.path.join(OUT, 'recipes.json'), 'w') as f:
        json.dump(recipes(), f, indent=1, sort_keys=True)
        f.write('\n')
    print(f'Guide generated: {len(written)} pages')


main()
