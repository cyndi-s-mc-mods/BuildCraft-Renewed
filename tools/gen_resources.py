#!/usr/bin/env python3
"""Generates BuildCraft's block states, models, item model definitions, translations, loot tables, tags and recipes.

Run from anywhere: python3 tools/gen_resources.py
Hand-written resources live alongside the generated ones; this script only overwrites the files it generates.
"""
import json
import os

RESOURCES = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'common', 'src', 'main', 'resources')
ROOT = os.path.join(RESOURCES, 'assets', 'buildcraft')
DATA = os.path.join(RESOURCES, 'data')
NS = 'buildcraft'

LANG = {'itemGroup.buildcraft.main': 'BuildCraft'}
TAGS = {}  # (registry folder, tag id) -> list of values


def _dump(full, data):
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def write(path, data):
    _dump(os.path.join(ROOT, path), data)


def write_data(path, data):
    _dump(os.path.join(DATA, path), data)


def name(kind, id, english):
    """kind is 'block' or 'item'."""
    LANG[f'{kind}.{NS}.{id}'] = english


def tag(folder, tag_id, *values):
    TAGS.setdefault((folder, tag_id), []).extend(values)


def drops_self(block):
    write_data(f'{NS}/loot_table/blocks/{block}.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                   'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{block}'}]}],
        'random_sequence': f'{NS}:blocks/{block}'})


def shaped(id, pattern, key, result=None, count=1, category='misc'):
    result = result or f'{NS}:{id}'
    data = {'type': 'minecraft:crafting_shaped', 'category': category, 'key': key, 'pattern': pattern,
            'result': {'id': result}}
    if count != 1:
        data['result']['count'] = count
    write_data(f'{NS}/recipe/{id}.json', data)


def shapeless(id, ingredients, result=None, count=1, category='misc'):
    result = result or f'{NS}:{id}'
    data = {'type': 'minecraft:crafting_shapeless', 'category': category, 'ingredients': ingredients,
            'result': {'id': result}}
    if count != 1:
        data['result']['count'] = count
    write_data(f'{NS}/recipe/{id}.json', data)


def finish():
    for (folder, tag_id), values in TAGS.items():
        ns, path = tag_id.split(':')
        write_data(f'{ns}/tags/{folder}/{path}.json', {'values': sorted(set(values))})
    write('lang/en_us.json', dict(sorted(LANG.items())))


def item_def(name, model=None):
    write(f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': model or f'{NS}:item/{name}'}})


def simple_item(name, texture=None):
    write(f'models/item/{name}.json', {'parent': 'minecraft:item/generated',
                                       'textures': {'layer0': f'{NS}:item/{texture or name}'}})
    item_def(name)


def handheld_item(name, texture=None):
    write(f'models/item/{name}.json', {'parent': 'minecraft:item/handheld',
                                       'textures': {'layer0': f'{NS}:item/{texture or name}'}})
    item_def(name)


# Rotations that turn a model pointing up into one pointing in each direction.
FACING_ROTATIONS = {
    'up': {}, 'down': {'x': 180},
    'north': {'x': 90}, 'south': {'x': 90, 'y': 180},
    'east': {'x': 90, 'y': 90}, 'west': {'x': 90, 'y': 270},
}

# ---------------------------------------------------------------- engines

STAGES = ['blue', 'green', 'yellow', 'red', 'overheat', 'black']


def box(name, frm, to, faces):
    return {'name': name, 'from': frm, 'to': to, 'faces': faces}


def plate(name, y0, y1):
    side = {'uv': [0, 0, 16, y1 - y0], 'texture': '#side'}
    return box(name, [0, y0, 0], [16, y1, 16], {
        'down': {'uv': [0, 0, 16, 16], 'texture': '#back'},
        'up': {'uv': [0, 0, 16, 16], 'texture': '#back'},
        'north': side, 'south': side, 'west': side, 'east': side})


TRUNK = box('trunk', [4, 4, 4], [12, 16, 12], {
    'down': {'uv': [0, 0, 8, 8], 'texture': '#trunk'},
    'up': {'uv': [0, 0, 8, 8], 'texture': '#trunk'},
    'north': {'uv': [8, 0, 16, 12], 'texture': '#trunk'},
    'south': {'uv': [8, 0, 16, 12], 'texture': '#trunk'},
    'west': {'uv': [8, 0, 16, 12], 'texture': '#trunk'},
    'east': {'uv': [8, 0, 16, 12], 'texture': '#trunk'}})

write('models/block/engine/base.json', {
    'textures': {'particle': '#back'},
    'elements': [plate('base', 0, 4), TRUNK]})
write('models/block/engine/item.json', {
    'parent': 'minecraft:block/block',
    'textures': {'particle': '#back'},
    'elements': [plate('base', 0, 4), plate('base_moving', 4, 8), TRUNK]})

ENGINES = {
    # block id: texture folder
    'engine_redstone': 'wood',
    'engine_stirling': 'stone',
    'engine_combustion': 'iron',
    'engine_creative': 'creative',
}

for engine, tex in ENGINES.items():
    textures = {'back': f'{NS}:block/engine/{tex}/back', 'side': f'{NS}:block/engine/{tex}/side'}
    variants = {}
    for stage in STAGES:
        trunk = 'creative' if engine == 'engine_creative' else stage
        write(f'models/block/{engine}_{stage}.json', {
            'parent': f'{NS}:block/engine/base',
            'textures': dict(textures, trunk=f'{NS}:block/engine/trunk_{trunk}')})
        for facing, rot in FACING_ROTATIONS.items():
            variants[f'facing={facing},stage={stage}'] = dict({'model': f'{NS}:block/{engine}_{stage}'}, **rot)
    write(f'blockstates/{engine}.json', {'variants': variants})
    trunk = 'creative' if engine == 'engine_creative' else 'blue'
    write(f'models/item/{engine}.json', {
        'parent': f'{NS}:block/engine/item',
        'textures': dict(textures, trunk=f'{NS}:block/engine/trunk_{trunk}')})
    item_def(engine)
    drops_self(engine)
    tag('block', 'minecraft:mineable/pickaxe' if engine != 'engine_redstone' else 'minecraft:mineable/axe', f'{NS}:{engine}')

name('block', 'engine_redstone', 'Redstone Engine')
name('block', 'engine_stirling', 'Stirling Engine')
name('block', 'engine_combustion', 'Combustion Engine')
name('block', 'engine_creative', 'Creative Engine')
LANG['chat.buildcraft.engine.creative.mode'] = 'Creative engine output: %s MJ/t'

ENGINE_RECIPE = ['www', ' g ', 'GpG']
shaped('engine_redstone', ENGINE_RECIPE, {'w': '#minecraft:planks', 'g': '#c:glass_blocks/colorless',
                                         'G': '#c:gears/wood', 'p': 'minecraft:piston'}, category='redstone')
shaped('engine_stirling', ENGINE_RECIPE, {'w': '#c:cobblestones', 'g': '#c:glass_blocks/colorless',
                                         'G': '#c:gears/stone', 'p': 'minecraft:piston'}, category='redstone')
shaped('engine_combustion', ENGINE_RECIPE, {'w': '#c:ingots/iron', 'g': '#c:glass_blocks/colorless',
                                           'G': '#c:gears/iron', 'p': 'minecraft:piston'}, category='redstone')
LANG['gui.buildcraft.tank.empty'] = 'Empty (%s mB)'
LANG['gui.buildcraft.tank.amount'] = '%s / %s mB'
LANG['gui.buildcraft.engine.heat'] = 'Heat: %s \u00b0C'
LANG['gui.buildcraft.engine.stored'] = 'Stored: %s / %s MJ'
LANG['gui.buildcraft.engine.output'] = 'Output: %s MJ/t'

# ---------------------------------------------------------------- core items

handheld_item('wrench')
name('item', 'wrench', 'Wrench')
shaped('wrench', ['I I', ' G ', ' I '], {'I': '#c:ingots/iron', 'G': '#c:gears/stone'}, category='equipment')

GEARS = {'wood': 'Wood Gear', 'stone': 'Stone Gear', 'iron': 'Iron Gear', 'gold': 'Gold Gear', 'diamond': 'Diamond Gear'}
for gear, english in GEARS.items():
    simple_item(f'gear_{gear}')
    name('item', f'gear_{gear}', english)
    tag('item', f'c:gears/{gear}', f'{NS}:gear_{gear}')
    tag('item', 'c:gears', f'#c:gears/{gear}')
GEAR_RING = [' o ', 'oio', ' o ']
shaped('gear_wood', [' o ', 'o o', ' o '], {'o': '#c:rods/wooden'})
shaped('gear_stone', GEAR_RING, {'o': '#c:cobblestones', 'i': '#c:gears/wood'})
shaped('gear_iron', GEAR_RING, {'o': '#c:ingots/iron', 'i': '#c:gears/stone'})
shaped('gear_gold', GEAR_RING, {'o': '#c:ingots/gold', 'i': '#c:gears/iron'})
shaped('gear_diamond', GEAR_RING, {'o': '#c:gems/diamond', 'i': '#c:gears/gold'})

# ---------------------------------------------------------------- fluids

# name: (English name, light colour) for each oil and fuel, matching BCEnergyFluids
FLUIDS = {
    'oil': ('Crude Oil', 0x505050),
    'oil_residue': ('Residue', 0x100F10),
    'oil_heavy': ('Heavy Oil', 0xA08F1F),
    'oil_dense': ('Dense Oil', 0x876E77),
    'oil_distilled': ('Distilled Oil', 0xE4AF78),
    'fuel_dense': ('Dense Fuel', 0xFFAF3F),
    'fuel_mixed_heavy': ('Mixed Heavy Fuels', 0xF2A700),
    'fuel_light': ('Light Fuel', 0xFFFF30),
    'fuel_mixed_light': ('Mixed Light Fuels', 0xF6D700),
    'fuel_gaseous': ('Gaseous Fuel', 0xFAF630),
}
HEAT_PREFIX = ['', 'Hot ', 'Searing ']

write('models/item/fluid_bucket.json', {'parent': 'minecraft:item/generated', 'textures': {
    'layer0': 'minecraft:item/bucket', 'layer1': f'{NS}:item/bucket_fluid_overlay'}})
for fluid, (english, colour) in FLUIDS.items():
    for heat in range(3):
        id = fluid if heat == 0 else f'{fluid}_heat_{heat}'
        write(f'blockstates/{id}.json', {'variants': {'': {'model': f'{NS}:block/fluid/{id}'}}})
        write(f'models/block/fluid/{id}.json', {'textures': {'particle': f'{NS}:block/fluids/{fluid}_heat_{heat}_still'}})
        write(f'items/{id}_bucket.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/fluid_bucket', 'tints': [
            {'type': 'minecraft:constant', 'value': -1},
            {'type': 'minecraft:constant', 'value': colour - 0x1000000}]}})
        name('block', id, HEAT_PREFIX[heat] + english)
        name('item', f'{id}_bucket', HEAT_PREFIX[heat] + english + ' Bucket')
        tag('fluid', f'{NS}:{fluid}', f'{NS}:{id}', f'{NS}:flowing_{id}')

# ---------------------------------------------------------------- pipes

DIRS = ['down', 'up', 'north', 'south', 'west', 'east']
OPPOSITE = {'down': 'up', 'up': 'down', 'north': 'south', 'south': 'north', 'west': 'east', 'east': 'west'}
TEXTURE_ROOT = os.path.join(ROOT, 'textures')


def tex_exists(tex):
    path = os.path.join(TEXTURE_ROOT, tex.split(':')[1] + '.png')
    if not os.path.exists(path):
        print('WARNING: missing texture', tex)


def face_box(direction):
    """The core box face pointing in a direction, plus an inward-facing wall behind it."""
    lo, hi = 4, 12
    outer = {'from': [lo, lo, lo], 'to': [hi, hi, hi], 'faces': {direction: {'texture': '#t'}}}
    # A flat element on the same wall, facing into the pipe so the far side shows through the glass
    pos = {'down': (1, lo), 'up': (1, hi), 'north': (2, lo), 'south': (2, hi), 'west': (0, lo), 'east': (0, hi)}[direction]
    frm, to = [lo, lo, lo], [hi, hi, hi]
    frm[pos[0]] = to[pos[0]] = pos[1]
    inner = {'from': frm, 'to': to, 'faces': {OPPOSITE[direction]: {'texture': '#t'}}}
    return [outer, inner]


def arm_box(direction):
    """The four walls of the arm going towards a direction, outside and inside."""
    axis = {'down': 1, 'up': 1, 'north': 2, 'south': 2, 'west': 0, 'east': 0}[direction]
    start, end = (0, 4) if direction in ('down', 'north', 'west') else (12, 16)
    frm, to = [4, 4, 4], [12, 12, 12]
    frm[axis], to[axis] = start, end
    sides = [d for d in DIRS if d != direction and d != OPPOSITE[direction]]
    elements = [{'from': frm, 'to': to, 'faces': {d: {'texture': '#t'} for d in sides}}]
    for d in sides:
        a = {'down': 1, 'up': 1, 'north': 2, 'south': 2, 'west': 0, 'east': 0}[d]
        f, t = list(frm), list(to)
        val = 4 if d in ('down', 'north', 'west') else 12
        f[a] = t[a] = val
        elements.append({'from': f, 'to': t, 'faces': {OPPOSITE[d]: {'texture': '#t'}}})
    return elements


for d in DIRS:
    write(f'models/block/pipe/core_{d}.json', {'textures': {'particle': '#t'}, 'elements': face_box(d)})
    write(f'models/block/pipe/arm_{d}.json', {'textures': {'particle': '#t'}, 'elements': arm_box(d)})
write('models/block/pipe/item.json', {
    'parent': 'minecraft:block/block', 'textures': {'particle': '#t'},
    'elements': [e for d in DIRS if d not in ('up', 'down') for e in face_box(d)] + arm_box('up') + arm_box('down')})


def pipe_model(kind, d, texture):
    """A core face or arm model for one texture. Returns the model id."""
    name = texture.split('/')[-1]
    model = f'block/pipe/{kind}/{name}_{d}'
    tex_exists(f'{NS}:{texture}')
    write(f'models/{model}.json', {'parent': f'{NS}:block/pipe/{kind}_{d}', 'textures': {'t': f'{NS}:{texture}'}})
    return f'{NS}:{model}'


COLOURS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan',
           'purple', 'blue', 'brown', 'green', 'red', 'black']
LEGACY_COLOUR = {'light_gray': 'silver'}
LIMIT_SUFFIX = ['_m0', '_m4', '_m8', '_m16', '_m32', '_m64', '_m128']
DIR_VALUES = DIRS + ['center']

PIPES = {
    # id: (English name, style, textures)
    'structure': ('Structure Pipe', 'plain', 'structure'),
    'wood_item': ('Wooden Transport Pipe', 'directional', ('wood_item_clear', 'wood_item_filled')),
    'wood_fluid': ('Wooden Fluid Pipe', 'directional', ('wood_fluid_clear', 'wood_fluid_filled')),
    'wood_power': ('Wooden Kinesis Pipe', 'plain', 'wood_power_clear'),
    'stone_item': ('Stone Transport Pipe', 'plain', 'stone_item'),
    'stone_fluid': ('Stone Fluid Pipe', 'plain', 'stone_fluid'),
    'stone_power': ('Stone Kinesis Pipe', 'plain', 'stone_power'),
    'cobblestone_item': ('Cobblestone Transport Pipe', 'plain', 'cobblestone_item'),
    'cobblestone_fluid': ('Cobblestone Fluid Pipe', 'plain', 'cobblestone_fluid'),
    'cobblestone_power': ('Cobblestone Kinesis Pipe', 'plain', 'cobblestone_power'),
    'quartz_item': ('Quartz Transport Pipe', 'plain', 'quartz_item'),
    'quartz_fluid': ('Quartz Fluid Pipe', 'plain', 'quartz_fluid'),
    'quartz_power': ('Quartz Kinesis Pipe', 'plain', 'quartz_power'),
    'gold_item': ('Golden Transport Pipe', 'plain', 'gold_item'),
    'gold_fluid': ('Golden Fluid Pipe', 'plain', 'gold_fluid'),
    'gold_power': ('Golden Kinesis Pipe', 'plain', 'gold_power'),
    'sandstone_item': ('Sandstone Transport Pipe', 'plain', 'sandstone_item'),
    'sandstone_fluid': ('Sandstone Fluid Pipe', 'plain', 'sandstone_fluid'),
    'sandstone_power': ('Sandstone Kinesis Pipe', 'plain', 'sandstone_power'),
    'iron_item': ('Iron Transport Pipe', 'iron', ('iron_item_clear', 'iron_item_filled')),
    'iron_fluid': ('Iron Fluid Pipe', 'iron', ('iron_fluid_clear', 'iron_fluid_filled')),
    'iron_power': ('Iron Kinesis Pipe', 'limiter', 'iron_power'),
    'diamond_item': ('Diamond Transport Pipe', 'diamond', 'diamond_item'),
    'diamond_fluid': ('Diamond Fluid Pipe', 'diamond', 'diamond_fluid'),
    'diamond_power': ('Diamond Kinesis Pipe', 'limiter', 'diamond_power'),
    'diamond_wood_item': ('Emerald Transport Pipe', 'directional', ('diamond_wood_item_clear', 'diamond_wood_item_filled')),
    'diamond_wood_fluid': ('Emerald Fluid Pipe', 'directional', ('diamond_wood_fluid_clear', 'diamond_wood_fluid_filled')),
    'diamond_wood_power': ('Emerald Kinesis Pipe', 'plain', 'diamond_wood_power_clear'),
    'clay_item': ('Clay Transport Pipe', 'plain', 'clay_item'),
    'clay_fluid': ('Clay Fluid Pipe', 'plain', 'clay_fluid'),
    'void_item': ('Void Transport Pipe', 'plain', 'void_item'),
    'void_fluid': ('Void Fluid Pipe', 'plain', 'void_fluid'),
    'obsidian_item': ('Obsidian Transport Pipe', 'plain', 'obsidian_item'),
    'lapis_item': ('Lapis Transport Pipe', 'lapis', 'lapis_item'),
    'daizuli_item': ('Daizuli Transport Pipe', 'daizuli', 'daizuli_item'),
    'emzuli_item': ('Emzuli Transport Pipe', 'directional', ('emzuli_item_clear', 'emzuli_item_filled')),
    'stripes_item': ('Stripes Transport Pipe', 'plain', 'stripes_item'),
}


def other_dirs(d):
    return '|'.join(v for v in DIR_VALUES if v != d)


for pipe_id, (english, style, textures) in PIPES.items():
    block = f'pipe_{pipe_id}'
    parts = []
    item_texture = None
    if style == 'plain':
        tex = f'block/pipes/{textures}'
        item_texture = tex
        for d in DIRS:
            parts.append({'when': {d: 'false'}, 'apply': {'model': pipe_model('core', d, tex)}})
            parts.append({'when': {d: 'true'}, 'apply': {'model': pipe_model('arm', d, tex)}})
    elif style in ('directional', 'iron'):
        clear, filled = f'block/pipes/{textures[0]}', f'block/pipes/{textures[1]}'
        # Wooden pipes: the extraction side is filled. Iron pipes: every side but the output is filled.
        special, normal = (filled, clear) if style == 'directional' else (clear, filled)
        core = clear if style == 'directional' else filled
        item_texture = clear
        for d in DIRS:
            parts.append({'when': {d: 'false'}, 'apply': {'model': pipe_model('core', d, core)}})
            parts.append({'when': {d: 'true', 'dir': d}, 'apply': {'model': pipe_model('arm', d, special)}})
            parts.append({'when': {d: 'true', 'dir': other_dirs(d)}, 'apply': {'model': pipe_model('arm', d, normal)}})
    elif style == 'diamond':
        base = f'block/pipes/{textures}'
        item_texture = base
        for d in DIRS:
            parts.append({'when': {d: 'false'}, 'apply': {'model': pipe_model('core', d, base)}})
            parts.append({'when': {d: 'true'}, 'apply': {'model': pipe_model('arm', d, f'{base}_{d}')}})
    elif style == 'limiter':
        for limit in range(7):
            tex = f'block/pipes/{textures}{LIMIT_SUFFIX[6 - limit]}'
            if limit == 0:
                item_texture = tex
            for d in DIRS:
                parts.append({'when': {d: 'false', 'limit': str(limit)}, 'apply': {'model': pipe_model('core', d, tex)}})
                parts.append({'when': {d: 'true', 'limit': str(limit)}, 'apply': {'model': pipe_model('arm', d, tex)}})
    elif style == 'lapis':
        for colour in COLOURS:
            tex = f'block/pipes/{textures}_{LEGACY_COLOUR.get(colour, colour)}'
            if colour == 'white':
                item_texture = tex
            for d in DIRS:
                parts.append({'when': {d: 'false', 'colour': colour}, 'apply': {'model': pipe_model('core', d, tex)}})
                parts.append({'when': {d: 'true', 'colour': colour}, 'apply': {'model': pipe_model('arm', d, tex)}})
    elif style == 'daizuli':
        filled = f'block/pipes/{textures}_filled'
        for colour in COLOURS:
            tex = f'block/pipes/{textures}_{LEGACY_COLOUR.get(colour, colour)}'
            if colour == 'white':
                item_texture = tex
            for d in DIRS:
                parts.append({'when': {d: 'false', 'colour': colour}, 'apply': {'model': pipe_model('core', d, tex)}})
                parts.append({'when': {d: 'true', 'dir': d, 'colour': colour}, 'apply': {'model': pipe_model('arm', d, tex)}})
        for d in DIRS:
            parts.append({'when': {d: 'true', 'dir': other_dirs(d)}, 'apply': {'model': pipe_model('arm', d, filled)}})
    write(f'blockstates/{block}.json', {'multipart': parts})
    tex_exists(f'{NS}:{item_texture}')
    write(f'models/item/{block}.json', {'parent': f'{NS}:block/pipe/item', 'textures': {'t': f'{NS}:{item_texture}'}})
    item_def(block)
    name('block', block, english)
    drops_self(block)

# Pipe recipes: material, glass, material makes 8 pipes
PIPE_MATERIALS = {
    'wood_item': ('#minecraft:planks', None),
    'cobblestone_item': ('#c:cobblestones', None),
    'stone_item': ('minecraft:stone', None),
    'quartz_item': ('minecraft:quartz_block', None),
    'iron_item': ('#c:ingots/iron', None),
    'gold_item': ('#c:ingots/gold', None),
    'clay_item': ('minecraft:clay', None),
    'sandstone_item': ('#c:sandstone/blocks', None),
    'void_item': ('minecraft:black_dye', '#c:dusts/redstone'),
    'obsidian_item': ('minecraft:obsidian', None),
    'diamond_item': ('#c:gems/diamond', None),
    'lapis_item': ('minecraft:lapis_block', None),
    'daizuli_item': ('minecraft:lapis_block', '#c:gems/diamond'),
    'diamond_wood_item': ('#minecraft:planks', '#c:gems/diamond'),
    'stripes_item': ('#c:gears/gold', None),
}
for pipe_id, (left, right) in PIPE_MATERIALS.items():
    shaped(f'pipe_{pipe_id}', ['lgr'], {'l': left, 'g': '#c:glass_blocks/colorless', 'r': right or left}, count=8)
shapeless('pipe_structure', ['buildcraft:pipe_cobblestone_item', 'minecraft:gravel'], count=1)
shapeless('pipe_emzuli_item', ['buildcraft:pipe_diamond_wood_item', 'minecraft:lapis_block'])


def upgrade(from_id, to_id, extra):
    shapeless(f'pipe_{to_id}', [f'{NS}:pipe_{from_id}', extra])
    shapeless(f'pipe_{to_id}_undo', [f'{NS}:pipe_{to_id}'], result=f'{NS}:pipe_{from_id}')


for material in ['wood', 'cobblestone', 'stone', 'quartz', 'iron', 'gold', 'clay', 'sandstone', 'void', 'diamond', 'diamond_wood']:
    upgrade(f'{material}_item', f'{material}_fluid', f'{NS}:pipe_sealant')
for material in ['wood', 'cobblestone', 'stone', 'quartz', 'iron', 'gold', 'sandstone', 'diamond', 'diamond_wood']:
    upgrade(f'{material}_item', f'{material}_power', '#c:dusts/redstone')

simple_item('pipe_sealant', 'pipewaterproof')
name('item', 'pipe_sealant', 'Pipe Sealant')
shapeless('pipe_sealant', ['minecraft:green_dye'])
shapeless('pipe_sealant_from_residue', [f'{NS}:oil_residue_bucket'], result=f'{NS}:pipe_sealant', count=8)

LANG['chat.buildcraft.pipe.power.limit'] = 'Power limit: %s MJ/t'
LANG['gui.buildcraft.pipe.emerald.whitelist'] = 'Whitelist: only extract the filtered items'
LANG['gui.buildcraft.pipe.emerald.blacklist'] = 'Blacklist: extract everything except the filtered items'
LANG['gui.buildcraft.pipe.emerald.roundrobin'] = 'Round robin: extract each filtered item in turn'
LANG['gui.buildcraft.pipe.emzuli.nopaint'] = 'No paint'

finish()
print('Resources generated')
