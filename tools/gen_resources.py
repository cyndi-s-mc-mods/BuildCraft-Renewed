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

# ---------------------------------------------------------------- factory

LEGACY_MODELS = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'legacy', 'buildcraft_resources', 'assets',
                             'buildcraftfactory', 'models', 'block')


def legacy_elements(model):
    """The elements of a 1.12 factory model, without comments."""
    with open(os.path.join(LEGACY_MODELS, f'{model}.json')) as f:
        elements = json.load(f)['elements']
    for e in elements:
        e.pop('comment', None)
    return elements


def block_item(block, model=None):
    write(f'models/item/{block}.json', {'parent': model or f'{NS}:block/{block}'})
    item_def(block)


def cube(model, **faces):
    tex = {k: f'{NS}:block/{v}' for k, v in faces.items()}
    for v in faces.values():
        tex_exists(f'{NS}:block/{v}')
    write(f'models/block/{model}.json', {'parent': 'minecraft:block/cube', 'textures': tex})


# Tank
TANK_ELEMENT = [{'from': [2, 0, 2], 'to': [14, 16, 14], 'faces': {
    'down': {'texture': '#end', 'cullface': 'down'}, 'up': {'texture': '#end', 'cullface': 'up'},
    'north': {'texture': '#side'}, 'south': {'texture': '#side'}, 'west': {'texture': '#side'}, 'east': {'texture': '#side'}}}]
for model, side in [('tank', 'side'), ('tank_joined_below', 'side_joined_below')]:
    tex_exists(f'{NS}:block/tank/{side}')
    write(f'models/block/{model}.json', {'textures': {'particle': f'{NS}:block/tank/{side}', 'end': f'{NS}:block/tank/end',
                                                      'side': f'{NS}:block/tank/{side}'}, 'elements': TANK_ELEMENT})
write('blockstates/tank.json', {'variants': {'joined_below=false': {'model': f'{NS}:block/tank'},
                                             'joined_below=true': {'model': f'{NS}:block/tank_joined_below'}}})
block_item('tank')
name('block', 'tank', 'Tank')
drops_self('tank')
shaped('tank', ['ggg', 'g g', 'ggg'], {'g': '#c:glass_blocks/colorless'})

# Mining well and pump
cube('mining_well', particle='mining_well/side', down='mining_well/bottom', up='mining_well/top', north='mining_well/front',
     east='mining_well/side', south='mining_well/back', west='mining_well/side')
write('blockstates/mining_well.json', {'variants': {
    f'facing={d}': ({'model': f'{NS}:block/mining_well', 'y': y} if y else {'model': f'{NS}:block/mining_well'})
    for d, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}})
block_item('mining_well')
name('block', 'mining_well', 'Mining Well')
drops_self('mining_well')
shaped('mining_well', ['iri', 'igi', 'ipi'], {'i': '#c:ingots/iron', 'r': '#c:dusts/redstone', 'g': '#c:gears/iron',
                                              'p': 'minecraft:iron_pickaxe'})

cube('pump', particle='pump/side', down='pump/bottom', up='pump/top', north='pump/side', east='pump/side',
     south='pump/side', west='pump/side')
write('blockstates/pump.json', {'variants': {'': {'model': f'{NS}:block/pump'}}})
block_item('pump')
name('block', 'pump', 'Pump')
drops_self('pump')
shaped('pump', ['iri', 'igi', 'tbt'], {'i': '#c:ingots/iron', 'r': '#c:dusts/redstone', 'g': '#c:gears/iron',
                                       't': f'{NS}:tank', 'b': 'minecraft:bucket'})

tex_exists(f'{NS}:block/mining_well/tube')
write('models/block/tube.json', {'textures': {'particle': f'{NS}:block/mining_well/tube', 't': f'{NS}:block/mining_well/tube'},
                                 'elements': [{'from': [4, 0, 4], 'to': [12, 16, 12], 'faces': {
                                     d: {'uv': [4, 0, 12, 16] if d not in ('up', 'down') else [4, 4, 12, 12], 'texture': '#t',
                                         **({'cullface': d} if d in ('up', 'down') else {})}
                                     for d in ['down', 'up', 'north', 'south', 'west', 'east']}}]})
write('blockstates/tube.json', {'variants': {'': {'model': f'{NS}:block/tube'}}})
name('block', 'tube', 'Tube')
tag('block', 'minecraft:dragon_immune', f'{NS}:tube')
tag('block', 'minecraft:wither_immune', f'{NS}:tube')

# Chute
CHUTE_TEXTURES = {k: f'{NS}:block/chute/{k}' for k in ['top', 'top_bottom', 'top_side', 'bottom', 'side', 'side2']}
CHUTE_TEXTURES['particle'] = f'{NS}:block/chute/top'
for v in CHUTE_TEXTURES.values():
    tex_exists(v)
write('models/block/chute.json', {'textures': CHUTE_TEXTURES, 'elements': legacy_elements('chute')})
write('models/block/chute_connected.json', {'textures': CHUTE_TEXTURES, 'elements': legacy_elements('chute_connected')})
OPPOSITE = {'up': 'down', 'down': 'up', 'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}
parts = [{'when': {'facing': d}, 'apply': {'model': f'{NS}:block/chute', **FACING_ROTATIONS[d]}} for d in FACING_ROTATIONS]
# The connection model points down, so rotate it as a model pointing up would be rotated to face the other way
parts += [{'when': {f'connected_{d}': 'true'}, 'apply': {'model': f'{NS}:block/chute_connected', **FACING_ROTATIONS[OPPOSITE[d]]}}
          for d in FACING_ROTATIONS]
write('blockstates/chute.json', {'multipart': parts})
block_item('chute')
name('block', 'chute', 'Chute')
drops_self('chute')
shaped('chute', ['ici', 'igi', ' i '], {'i': '#c:ingots/iron', 'c': 'minecraft:chest', 'g': '#c:gears/stone'})

# Flood gate: every side but the top shows whether it is open
FLOOD_SIDES = ['down', 'north', 'south', 'west', 'east']
for state in ['open', 'closed']:
    tex_exists(f'{NS}:block/flood_gate/{state}')
tex_exists(f'{NS}:block/flood_gate/top')
write('models/block/flood_gate_top.json', {'textures': {'particle': f'{NS}:block/flood_gate/top', 't': f'{NS}:block/flood_gate/top'},
                                           'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {
                                               'up': {'texture': '#t', 'cullface': 'up'}}}]})
for d in FLOOD_SIDES:
    for state in ['open', 'closed']:
        write(f'models/block/flood_gate_{d}_{state}.json', {
            'textures': {'particle': f'{NS}:block/flood_gate/{state}', 't': f'{NS}:block/flood_gate/{state}'},
            'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {d: {'texture': '#t', 'cullface': d}}}]})
parts = [{'apply': {'model': f'{NS}:block/flood_gate_top'}}]
for d in FLOOD_SIDES:
    for state, value in [('open', 'true'), ('closed', 'false')]:
        parts.append({'when': {f'open_{d}': value}, 'apply': {'model': f'{NS}:block/flood_gate_{d}_{state}'}})
write('blockstates/flood_gate.json', {'multipart': parts})
cube('flood_gate_item', particle='flood_gate/open', down='flood_gate/open', up='flood_gate/top', north='flood_gate/open',
     east='flood_gate/open', south='flood_gate/open', west='flood_gate/open')
block_item('flood_gate', f'{NS}:block/flood_gate_item')
name('block', 'flood_gate', 'Flood Gate')
drops_self('flood_gate')
shaped('flood_gate', ['igi', 'btb', 'ibi'], {'i': '#c:ingots/iron', 'g': '#c:gears/iron', 'b': 'minecraft:iron_bars',
                                             't': f'{NS}:tank'})

for block in ['mining_well', 'pump', 'chute', 'flood_gate']:
    tag('block', 'minecraft:mineable/pickaxe', f'{NS}:{block}')

# Distiller: the model faces west
tex_exists(f'{NS}:block/distiller/tank_sprite_a')
tex_exists(f'{NS}:block/distiller/tank_sprite_b')
write('models/block/distiller.json', {'textures': {'particle': f'{NS}:block/distiller/tank_sprite_a',
                                                   'sprite_a': f'{NS}:block/distiller/tank_sprite_a',
                                                   'sprite_b': f'{NS}:block/distiller/tank_sprite_b'},
                                      'elements': legacy_elements('distiller')})
HORIZONTAL_FROM_WEST = {'west': 0, 'north': 90, 'east': 180, 'south': 270}
write('blockstates/distiller.json', {'variants': {
    f'facing={d}': {'model': f'{NS}:block/distiller', **({'y': y} if y else {})} for d, y in HORIZONTAL_FROM_WEST.items()}})
block_item('distiller')
name('block', 'distiller', 'Distiller')
drops_self('distiller')
shaped('distiller', ['rtr', 'tgt'], {'r': 'minecraft:redstone_torch', 't': f'{NS}:tank', 'g': '#c:gears/diamond'})

# Heat exchanger: converted from 1.12's conditional model, which faces west
with open(os.path.join(LEGACY_MODELS, '..', 'tiles', 'heat_exchange_static.json')) as f:
    HEAT_MODEL = json.load(f)


def heat_visible(expr, part, left, right):
    if not expr:
        return True
    env = {'MIDDLE': 'middle', 'START': 'start', 'END': 'end', 'part': part, 'connected_left': left, 'connected_right': right}
    py = expr.replace('&&', ' and ').replace('!', ' not ').replace(' not =', '!=')
    return eval(py, {}, env)


HEAT_TEXTURES = {f'sprite_{c}': f'{NS}:block/heat_exchange/sprite_{c}' for c in 'abcde'}
for v in HEAT_TEXTURES.values():
    tex_exists(v)
HEAT_TEXTURES['particle'] = HEAT_TEXTURES['sprite_b']
heat_variants = {}
for part in ['start', 'middle', 'end']:
    for left in [False, True]:
        for right in [False, True]:
            elements = []
            for e in HEAT_MODEL['elements']:
                if not heat_visible(e.get('visible'), part, left, right):
                    continue
                faces = {}
                for d, face in e['faces'].items():
                    out = {'uv': face['uv'], 'texture': face['texture']}
                    if face.get('rotation'):
                        out['rotation'] = face['rotation'] * 90
                    faces[d] = out
                elements.append({'from': e['from'], 'to': e['to'], 'faces': faces})
            model = f'heat_exchange_{part}' + ('' if part != 'middle' else f'_{"l" if left else ""}{"r" if right else ""}')
            write(f'models/block/{model}.json', {'textures': HEAT_TEXTURES, 'elements': elements})
            for d, y in HORIZONTAL_FROM_WEST.items():
                key = f'connected_left={str(left).lower()},connected_right={str(right).lower()},facing={d},part={part}'
                heat_variants[key] = {'model': f'{NS}:block/{model}', **({'y': y} if y else {})}
write('blockstates/heat_exchange.json', {'variants': heat_variants})
block_item('heat_exchange', f'{NS}:block/heat_exchange_middle_')
name('block', 'heat_exchange', 'Heat Exchanger')
drops_self('heat_exchange')
shaped('heat_exchange', ['igi', '###', 'igi'], {'i': '#c:ingots/iron', 'g': '#c:gears/iron', '#': '#c:glass_blocks/colorless'})

# Auto workbench
cube('autoworkbench_item', particle='auto_workbench_item/side', down='auto_workbench_item/bottom', up='auto_workbench_item/top',
     north='auto_workbench_item/side', east='auto_workbench_item/side', south='auto_workbench_item/side', west='auto_workbench_item/side')
write('blockstates/autoworkbench_item.json', {'variants': {'': {'model': f'{NS}:block/autoworkbench_item'}}})
block_item('autoworkbench_item')
name('block', 'autoworkbench_item', 'Auto Workbench')
drops_self('autoworkbench_item')
shaped('autoworkbench_item', ['gwg'], {'g': '#c:gears/stone', 'w': 'minecraft:crafting_table'})
tag('block', 'minecraft:mineable/axe', f'{NS}:autoworkbench_item')

# Water gel
gel_variants = {}
for stage in ['spread_0', 'spread_1', 'spread_2', 'spread_3', 'gelling_0', 'gelling_1', 'gel']:
    tex_exists(f'{NS}:block/gel/{stage}')
    write(f'models/block/water_gel_{stage}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/gel/{stage}'}})
    gel_variants[f'stage={stage}'] = {'model': f'{NS}:block/water_gel_{stage}'}
write('blockstates/water_gel.json', {'variants': gel_variants})
name('block', 'water_gel', 'Gelled Water')
write_data(f'{NS}/loot_table/blocks/water_gel.json', {
    'type': 'minecraft:block',
    'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:gel'}]}],
    'random_sequence': f'{NS}:blocks/water_gel'})
tag('block', 'minecraft:mineable/shovel', f'{NS}:water_gel')
simple_item('water_gel_spawn', 'water_gel')
name('item', 'water_gel_spawn', 'Water Gel')
simple_item('gel')
name('item', 'gel', 'Gel')
shaped('water_gel_spawn', [' s ', 'srs', ' s '], {'s': 'minecraft:sand', 'r': f'{NS}:oil_residue_bucket'})
shaped('water_gel_to_bucket', ['g', 'b'], {'g': f'{NS}:gel', 'b': 'minecraft:bucket'}, result='minecraft:water_bucket')

for block in ['distiller', 'heat_exchange']:
    tag('block', 'minecraft:mineable/pickaxe', f'{NS}:{block}')


# ---------------------------------------------------------------- markers

TORCH_ELEMENT = [{'from': [7, 0, 7], 'to': [9, 9, 9], 'shade': False, 'faces': {
    'east': {'uv': [2, 5, 4, 14], 'texture': '#all'}, 'north': {'uv': [4, 5, 6, 14], 'texture': '#all'},
    'west': {'uv': [6, 5, 8, 14], 'texture': '#all'}, 'south': {'uv': [0, 5, 2, 14], 'texture': '#all'},
    'down': {'uv': [0, 14, 2, 16], 'texture': '#all'}, 'up': {'uv': [0, 3, 2, 5], 'texture': '#all'}}}]
tex_exists(f'{NS}:block/marker/volume')
write('models/block/marker_volume.json', {'textures': {'particle': f'{NS}:block/marker/volume', 'all': f'{NS}:block/marker/volume'},
                                          'elements': TORCH_ELEMENT})
write('blockstates/marker_volume.json', {'variants': {
    f'facing={d}': {'model': f'{NS}:block/marker_volume', **rot} for d, rot in FACING_ROTATIONS.items()}})
simple_item('marker_volume')
name('block', 'marker_volume', 'Volume Marker')
drops_self('marker_volume')
shaped('marker_volume', ['l', 't'], {'l': '#c:dyes/blue', 't': 'minecraft:redstone_torch'})

# ---------------------------------------------------------------- builders

cube('quarry', particle='quarry/normal/side', down='quarry/normal/bottom', up='quarry/normal/top', north='quarry/normal/front',
     east='quarry/normal/side', south='quarry/normal/back', west='quarry/normal/side')
write('blockstates/quarry.json', {'variants': {
    f'facing={d}': {'model': f'{NS}:block/quarry', **({'y': y} if y else {})}
    for d, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}})
block_item('quarry')
name('block', 'quarry', 'Quarry')
drops_self('quarry')
shaped('quarry', ['iri', 'gig', 'dpd'], {'i': '#c:gears/iron', 'r': '#c:dusts/redstone', 'g': '#c:gears/gold',
                                         'd': '#c:gears/diamond', 'p': 'minecraft:diamond_pickaxe'})
tag('block', 'minecraft:mineable/pickaxe', f'{NS}:quarry')

tex_exists(f'{NS}:block/frame/default')
FRAME_TEX = {'particle': f'{NS}:block/frame/default', 'all': f'{NS}:block/frame/default'}
write('models/block/frame/base.json', {'textures': FRAME_TEX, 'elements': [{'from': [4, 4, 4], 'to': [12, 12, 12], 'faces': {
    d: {'texture': '#all'} for d in ['down', 'up', 'north', 'south', 'west', 'east']}}]})
write('models/block/frame/connection.json', {'textures': FRAME_TEX, 'elements': [{'from': [4, 4, 0], 'to': [12, 12, 4], 'faces': {
    d: {'texture': '#all'} for d in ['down', 'up', 'west', 'east']}}]})
FROM_NORTH = {'north': {}, 'south': {'y': 180}, 'east': {'y': 90}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
parts = [{'apply': {'model': f'{NS}:block/frame/base'}}]
parts += [{'when': {f'connected_{d}': 'true'}, 'apply': {'model': f'{NS}:block/frame/connection', **rot}} for d, rot in FROM_NORTH.items()]
write('blockstates/frame.json', {'multipart': parts})
name('block', 'frame', 'Frame')
tag('block', 'minecraft:mineable/pickaxe', f'{NS}:frame')

# ---------------------------------------------------------------- silicon

for v in ['top', 'side', 'bottom']:
    tex_exists(f'{NS}:block/laser/{v}')
write('models/block/laser.json', {'textures': {'particle': f'{NS}:block/laser/bottom', 'top': f'{NS}:block/laser/top',
                                               'side': f'{NS}:block/laser/side', 'bottom': f'{NS}:block/laser/bottom'},
                                  'elements': [
                                      {'from': [0, 0, 0], 'to': [16, 4, 16], 'faces': {
                                          'down': {'texture': '#bottom', 'cullface': 'down'}, 'up': {'texture': '#top'},
                                          'north': {'texture': '#side', 'cullface': 'north'}, 'south': {'texture': '#side', 'cullface': 'south'},
                                          'west': {'texture': '#side', 'cullface': 'west'}, 'east': {'texture': '#side', 'cullface': 'east'}}},
                                      {'from': [5, 4, 5], 'to': [11, 13, 11], 'faces': {
                                          'up': {'texture': '#top'}, 'north': {'texture': '#side'}, 'south': {'texture': '#side'},
                                          'west': {'texture': '#side'}, 'east': {'texture': '#side'}}}]})
write('blockstates/laser.json', {'variants': {f'facing={d}': {'model': f'{NS}:block/laser', **rot} for d, rot in FACING_ROTATIONS.items()}})
block_item('laser')
name('block', 'laser', 'Laser')
drops_self('laser')
shaped('laser', ['rro', 'rdd', 'rro'], {'r': '#c:dusts/redstone', 'd': '#c:gems/diamond', 'o': 'minecraft:obsidian'})

LEGACY_SILICON = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'legacy', 'buildcraft_resources', 'assets',
                              'buildcraftsilicon', 'models', 'block', 'table')
for table, legacy in [('assembly_table', 'assembly'), ('advanced_crafting_table', 'advanced_crafting')]:
    with open(os.path.join(LEGACY_SILICON, f'{legacy}.json')) as f:
        model = json.load(f)
    textures = {k: v.replace('buildcraftsilicon:blocks/', f'{NS}:block/') for k, v in model['textures'].items()}
    for v in textures.values():
        tex_exists(v)
    write(f'models/block/{table}.json', {'textures': textures, 'elements': model['elements']})
    write(f'blockstates/{table}.json', {'variants': {'': {'model': f'{NS}:block/{table}'}}})
    block_item(table)
    drops_self(table)
    tag('block', 'minecraft:mineable/pickaxe', f'{NS}:{table}')
name('block', 'assembly_table', 'Assembly Table')
name('block', 'advanced_crafting_table', 'Advanced Crafting Table')
shaped('assembly_table', ['OdO', 'OrO', 'OgO'], {'O': 'minecraft:obsidian', 'd': '#c:gems/diamond', 'r': '#c:dusts/redstone',
                                                 'g': '#c:gears/diamond'})
shaped('advanced_crafting_table', ['OtO', 'OcO', 'OrO'], {'O': 'minecraft:obsidian', 't': 'minecraft:crafting_table',
                                                          'c': '#c:chests/wooden', 'r': f'{NS}:chipset_redstone'})
tag('block', 'minecraft:mineable/pickaxe', f'{NS}:laser')

for chip, english in [('redstone', 'Redstone'), ('iron', 'Iron'), ('gold', 'Golden'), ('quartz', 'Quartz'), ('diamond', 'Diamond')]:
    simple_item(f'chipset_{chip}', f'redstone_chipset/{"red" if chip == "redstone" else chip}')
    name('item', f'chipset_{chip}', f'{english} Chipset')

# ---------------------------------------------------------------- pluggables

def plug_item(item, texture, elements):
    tex_exists(f'{NS}:{texture}')
    write(f'models/item/{item}.json', {'parent': 'minecraft:block/block', 'textures': {'particle': f'{NS}:{texture}', 'all': f'{NS}:{texture}'},
                                       'elements': [{'from': f, 'to': t, 'faces': {d: {'texture': '#all'} for d in
                                                                                   ['down', 'up', 'north', 'south', 'west', 'east']}}
                                                    for f, t in elements]})
    item_def(item)


plug_item('plug_blocker', 'block/pipes/plug', [([6, 4, 4], [10, 12, 12])])
name('item', 'plug_blocker', 'Pipe Plug')
shapeless('plug_blocker', [f'{NS}:pipe_structure'], count=4)
plug_item('plug_power_adaptor', 'block/pipes/power_adapter', [([7, 4, 4], [9, 12, 12]), ([9, 3, 3], [11, 13, 13])])
name('item', 'plug_power_adaptor', 'Power Adapter')
shaped('plug_power_adaptor', ['sis', 'sgs', 'srs'], {'s': f'{NS}:pipe_structure', 'i': '#c:ingots/gold',
                                                     'g': '#c:gears/stone', 'r': '#c:dusts/redstone'}, count=4)
LANG['tooltip.buildcraft.pluggable.remove'] = 'Sneak and right click with an empty hand or a wrench to remove'

# ---------------------------------------------------------------- gates, wires and statements

GATE_MATERIAL_TEX = {'clay_brick': 'minecraft:block/bricks', 'iron': 'minecraft:block/iron_block',
                     'nether_brick': 'minecraft:block/nether_bricks', 'gold': 'minecraft:block/gold_block'}
GATE_MODIFIER_TEX = {'lapis': 'minecraft:block/lapis_block', 'quartz': 'minecraft:block/quartz_block_top',
                     'diamond': 'minecraft:block/diamond_block'}


def gate_box(frm, to, tex):
    side = {'uv': [2, 5, 4, 11], 'texture': tex}
    end = {'uv': [5, 5, 11, 11], 'texture': tex}
    return {'from': frm, 'to': to, 'faces': {'down': side, 'up': side, 'north': side, 'south': side, 'west': end, 'east': end}}


GATE_VARIANTS = [('and', 'clay_brick', 'no_modifier')] + [(l, m, mod) for m in ['iron', 'nether_brick', 'gold'] for l in ['and', 'or']
                                                          for mod in ['no_modifier', 'lapis', 'quartz', 'diamond']]
gate_cases = []
for logic, material, modifier in GATE_VARIANTS:
    vname = material if material == 'clay_brick' else f'{material}_{logic}_{modifier}'
    textures = {'particle': GATE_MATERIAL_TEX[material], 'material': GATE_MATERIAL_TEX[material], 'off': f'{NS}:block/gates/gate_off'}
    # The gate model is made for the west side of a pipe: turn it to face the viewer
    elements = [gate_box([7, 5, 5], [9.01, 11, 11], '#material'), gate_box([6.9, 6, 6], [9.1, 10, 10], '#off')]
    if material != 'clay_brick':
        textures['logic'] = f'{NS}:block/gates/gate_{logic}'
        elements.append(gate_box([6.8, 7, 7], [9.2, 9, 9], '#logic'))
    if modifier != 'no_modifier':
        textures['modifier'] = GATE_MODIFIER_TEX[modifier]
        for y, z in [(5.5, 5.5), (9.5, 5.5), (5.5, 9.5), (9.5, 9.5)]:
            elements.append(gate_box([6.8, y, z], [9.2, y + 1, z + 1], '#modifier'))
    write(f'models/item/gate/{vname}.json', {'parent': 'minecraft:block/block', 'textures': textures, 'elements': elements,
                                             'display': {'gui': {'rotation': [0, 90, 0], 'scale': [1.4, 1.4, 1.4]},
                                                         'ground': {'rotation': [0, 90, 0], 'scale': [0.5, 0.5, 0.5]},
                                                         'fixed': {'rotation': [0, 90, 0]},
                                                         'thirdperson_righthand': {'rotation': [75, 135, 0], 'translation': [0, 2.5, 0], 'scale': [0.5, 0.5, 0.5]},
                                                         'firstperson_righthand': {'rotation': [0, 135, 0], 'scale': [0.6, 0.6, 0.6]}}})
    gate_cases.append({'when': {'logic': logic, 'material': material, 'modifier': modifier},
                       'model': {'type': 'minecraft:model', 'model': f'{NS}:item/gate/{vname}'}})
write('items/gate.json', {'model': {'type': 'minecraft:select', 'property': 'minecraft:component', 'component': f'{NS}:gate_variant',
                                    'cases': gate_cases,
                                    'fallback': {'type': 'minecraft:model', 'model': f'{NS}:item/gate/clay_brick'}}})


def gate_result(logic, material, modifier='no_modifier'):
    return {'id': f'{NS}:gate', 'components': {f'{NS}:gate_variant': {'logic': logic, 'material': material, 'modifier': modifier}}}


def gate_recipe(rid, material_key, result):
    write_data(f'{NS}/recipe/{rid}.json', {'type': 'minecraft:crafting_shaped', 'category': 'redstone',
                                            'key': {'m': material_key, 'r': '#c:dusts/redstone', 'b': f'{NS}:plug_blocker'},
                                            'pattern': [' m ', 'mrm', ' b '], 'result': result})


gate_recipe('gate_basic', 'minecraft:brick', gate_result('and', 'clay_brick'))
gate_recipe('gate_iron', '#c:ingots/iron', gate_result('and', 'iron'))
gate_recipe('gate_nether_brick', 'minecraft:nether_brick', gate_result('and', 'nether_brick'))

LANG.update({
    'item.buildcraft.gate': 'Gate',
    'gate.buildcraft.name': '%s %s Gate', 'gate.buildcraft.name.basic': 'Basic Gate', 'gate.buildcraft.name.modified': '%s (%s)',
    'gate.buildcraft.material.iron': 'Iron', 'gate.buildcraft.material.nether_brick': 'Nether Brick', 'gate.buildcraft.material.gold': 'Gold',
    'gate.buildcraft.logic.and': 'AND', 'gate.buildcraft.logic.or': 'OR',
    'gate.buildcraft.modifier.lapis': 'Lapis', 'gate.buildcraft.modifier.quartz': 'Quartz', 'gate.buildcraft.modifier.diamond': 'Diamond',
    'gate.buildcraft.side': '%s (on the %s side)',
    'direction.buildcraft.down': 'bottom', 'direction.buildcraft.up': 'top', 'direction.buildcraft.north': 'north',
    'direction.buildcraft.south': 'south', 'direction.buildcraft.west': 'west', 'direction.buildcraft.east': 'east',
    'gate.buildcraft.parameter.item.empty': 'Any item (click with an item to choose one)',
    'gate.buildcraft.parameter.gate_side_only': 'Gate side only', 'gate.buildcraft.parameter.all_sides': 'All sides',
    'gate.buildcraft.trigger.true': 'Always On',
    'gate.buildcraft.trigger.redstone.input.active': 'Redstone Signal On', 'gate.buildcraft.trigger.redstone.input.inactive': 'Redstone Signal Off',
    'gate.buildcraft.trigger.machine.scheduled': 'Has Work', 'gate.buildcraft.trigger.machine.done': 'Work Done',
    'gate.buildcraft.trigger.engine.blue': 'Engine Blue', 'gate.buildcraft.trigger.engine.green': 'Engine Green',
    'gate.buildcraft.trigger.engine.yellow': 'Engine Yellow', 'gate.buildcraft.trigger.engine.red': 'Engine Red',
    'gate.buildcraft.trigger.engine.overheat': 'Engine Overheat',
    'gate.buildcraft.trigger.inventory.empty': 'Inventory Empty', 'gate.buildcraft.trigger.inventory.contains': 'Items in Inventory',
    'gate.buildcraft.trigger.inventory.space': 'Space in Inventory', 'gate.buildcraft.trigger.inventory.full': 'Inventory Full',
    'gate.buildcraft.trigger.inventorylevel.below': 'Contains < %s%%',
    'gate.buildcraft.trigger.fluid.empty': 'Tank Empty', 'gate.buildcraft.trigger.fluid.contains': 'Fluid in Tank',
    'gate.buildcraft.trigger.fluid.space': 'Space for Fluid', 'gate.buildcraft.trigger.fluid.full': 'Tank Full',
    'gate.buildcraft.trigger.fluidlevel.below': 'Tank < %s%% full',
    'gate.buildcraft.trigger.pipe.empty': 'Pipe Empty', 'gate.buildcraft.trigger.pipe.containsItems': 'Items Traversing',
    'gate.buildcraft.trigger.pipe.containsFluids': 'Fluid Traversing', 'gate.buildcraft.trigger.pipe.containsEnergy': 'Power Traversing',
    'gate.buildcraft.trigger.pipe.wire.active': '%s Pipe Signal On', 'gate.buildcraft.trigger.pipe.wire.inactive': '%s Pipe Signal Off',
    'gate.buildcraft.action.redstone.signal': 'Redstone Signal',
    'gate.buildcraft.action.machine.on': 'On', 'gate.buildcraft.action.machine.off': 'Off', 'gate.buildcraft.action.machine.loop': 'Loop',
    'gate.buildcraft.action.pipe.wire': '%s Pipe Signal', 'gate.buildcraft.action.pipe.direction': 'Face the %s side',
})

for colour in ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue',
               'brown', 'green', 'red', 'black']:
    tex = 'silver' if colour == 'light_gray' else colour
    simple_item(f'pipe_wire_{colour}', f'wire/{tex}')
    tex_exists(f'{NS}:block/wires/{tex}')
    name('item', f'pipe_wire_{colour}', colour.replace('_', ' ').title() + ' Pipe Wire')

# Silicon pluggables
for item, tex, english in [('plug_pulsar', 'block/plugs/pulsar_static', 'Pulsar'), ('plug_light_sensor', 'block/plugs/daylight_sensor', 'Light Sensor'),
                           ('plug_timer', 'block/plugs/timer', 'Timer')]:
    plug_item(item, tex, [([7, 5, 5], [9, 11, 11])])
    name('item', item, english)

DYE_RGB = {'white': 0xF9FFFE, 'orange': 0xF9801D, 'magenta': 0xC74EBD, 'light_blue': 0x3AB3DA, 'yellow': 0xFED83D, 'lime': 0x80C71F,
           'pink': 0xF38BAA, 'gray': 0x474F52, 'light_gray': 0x9D9D97, 'cyan': 0x169C9C, 'purple': 0x8932B8, 'blue': 0x3C44AA,
           'brown': 0x835432, 'green': 0x5E7C16, 'red': 0xB02E26, 'black': 0x1D1D21}
for item, frame, english in [('lens', 'lens_frame', 'Lens'), ('filter', 'filter_frame', 'Filter')]:
    tex_exists(f'{NS}:item/lens/{frame}')
    write(f'models/item/{item}.json', {'parent': 'minecraft:item/generated',
                                        'textures': {'layer0': f'{NS}:item/lens/clear', 'layer1': f'{NS}:item/lens/{frame}'}})
    write(f'models/item/{item}_coloured.json', {'parent': 'minecraft:item/generated',
                                                 'textures': {'layer0': f'{NS}:item/lens/transparent', 'layer1': f'{NS}:item/lens/{frame}'}})
    cases = [{'when': colour, 'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{item}_coloured',
                                        'tints': [{'type': 'minecraft:constant', 'value': rgb}, {'type': 'minecraft:constant', 'value': -1}]}}
             for colour, rgb in DYE_RGB.items()]
    write(f'items/{item}.json', {'model': {'type': 'minecraft:select', 'property': 'minecraft:component', 'component': f'{NS}:colour',
                                           'cases': cases, 'fallback': {'type': 'minecraft:model', 'model': f'{NS}:item/{item}'}}})
    name('item', item, english)
LANG['item.buildcraft.lens.coloured'] = '%s %s'
LANG.update({
    'gate.buildcraft.trigger.light.bright': 'Bright', 'gate.buildcraft.trigger.light.dark': 'Dark',
    'gate.buildcraft.trigger.timer': '%s Second Timer',
    'gate.buildcraft.action.pulsar.constant': 'Power Pulsar', 'gate.buildcraft.action.pulsar.single': 'Single Power Pulse',
})

# Paintbrush
for colour in DYE_RGB:
    tex = 'silver' if colour == 'light_gray' else colour
    tex_exists(f'{NS}:item/paintbrush/{tex}')
    write(f'models/item/paintbrush_{colour}.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': f'{NS}:item/paintbrush/{tex}'}})
    write_data(f'{NS}/recipe/paintbrush_{colour}.json', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [f'{NS}:paintbrush', f'#c:dyes/{colour}'],
        'result': {'id': f'{NS}:paintbrush', 'components': {f'{NS}:colour': colour}}})
write('models/item/paintbrush.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': f'{NS}:item/paintbrush/clean'}})
write('items/paintbrush.json', {'model': {'type': 'minecraft:select', 'property': 'minecraft:component', 'component': f'{NS}:colour',
                                          'cases': [{'when': c, 'model': {'type': 'minecraft:model', 'model': f'{NS}:item/paintbrush_{c}'}} for c in DYE_RGB],
                                          'fallback': {'type': 'minecraft:model', 'model': f'{NS}:item/paintbrush'}}})
shaped('paintbrush', [' iw', ' gi', 's  '], {'i': '#c:strings', 'w': '#minecraft:wool', 'g': '#c:gears/wood', 's': '#c:rods/wooden'}, category='equipment')
LANG['item.buildcraft.paintbrush.clean'] = 'Paintbrush'
LANG['item.buildcraft.paintbrush.coloured'] = '%s Paintbrush'
name('item', 'paintbrush', 'Paintbrush')

# Facades: drawn by a special item renderer, using the block the facade copies
write('models/item/facade.json', {'parent': 'minecraft:block/block', 'textures': {'particle': 'minecraft:block/stone'}})
write('items/facade.json', {'model': {'type': 'minecraft:special', 'base': f'{NS}:item/facade', 'model': {'type': f'{NS}:facade'}}})
name('item', 'facade', 'Facade')
LANG['item.buildcraft.facade.solid'] = '%s Facade'
LANG['item.buildcraft.facade.hollow'] = 'Hollow %s Facade'

# Springs (they look like bedrock), the power tester and decorated blocks
write('blockstates/spring.json', {'variants': {'type=water': {'model': 'minecraft:block/bedrock'},
                                               'type=oil': {'model': 'minecraft:block/bedrock'}}})
block_item('spring', 'minecraft:block/bedrock')
name('block', 'spring', 'Water Spring')


def cube_all_block(block, texture, english):
    tex_exists(f'{NS}:block/{texture}')
    write(f'models/block/{block}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{texture}'}})
    write(f'blockstates/{block}.json', {'variants': {'': {'model': f'{NS}:block/{block}'}}})
    block_item(block)
    drops_self(block)
    tag('block', 'minecraft:mineable/pickaxe', f'{NS}:{block}')
    name('block', block, english)


cube_all_block('power_tester', 'power_tester', 'Power Tester')
LANG['chat.buildcraft.power_tester'] = 'Last received: %s MJ, last tick: %s MJ, total: %s MJ'
for kind, texture, english in [('destroy', 'misc/texture_red_dark', 'Destroy'), ('blueprint', 'blueprint/blue', 'Blueprint'),
                               ('template', 'blueprint/black', 'Template'), ('laser_back', 'laser/bottom', 'Laser Back')]:
    cube_all_block(f'decorated_{kind}', texture, f'Decorated Block ({english})')

# Filler
cube('filler', particle='filler/side', down='filler/bottom', up='filler/top', north='filler/front', east='filler/side',
     south='filler/side', west='filler/side')
write('blockstates/filler.json', {'variants': {
    f'facing={d}': {'model': f'{NS}:block/filler', **({'y': y} if y else {})}
    for d, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}})
block_item('filler')
name('block', 'filler', 'Filler')
drops_self('filler')
tag('block', 'minecraft:mineable/pickaxe', f'{NS}:filler')
shaped('filler', ['bmb', 'yty', 'gcg'], {'b': '#c:dyes/black', 'm': f'{NS}:marker_volume', 'y': '#c:dyes/yellow',
                                         't': 'minecraft:crafting_table', 'g': '#c:gears/gold', 'c': '#c:chests/wooden'})
for pattern in ['none', 'clear', 'fill', 'box', 'frame', 'pyramid', 'stairs', 'sphere', 'sphere_half', 'sphere_quarter',
                'sphere_eighth', '2d_square', '2d_circle', '2d_semi_circle', '2d_arc', '2d_triangle', '2d_pentagon', '2d_hexagon',
                '2d_octagon']:
    if not os.path.exists(os.path.join(TEXTURE_ROOT, 'gui', 'filler', 'patterns', pattern + '.png')):
        print('WARNING: missing pattern icon', pattern)
LANG.update({
    'fillerpattern.none': 'None', 'fillerpattern.clear': 'Clear', 'fillerpattern.fill': 'Fill', 'fillerpattern.box': 'Box',
    'fillerpattern.frame': 'Frame', 'fillerpattern.pyramid': 'Pyramid', 'fillerpattern.stairs': 'Stairs',
    'fillerpattern.sphere': 'Sphere', 'fillerpattern.sphere_half': 'Hemisphere', 'fillerpattern.sphere_quarter': 'Quarter-Sphere',
    'fillerpattern.sphere_eighth': 'Eighth-Sphere', 'fillerpattern.2d_square': 'Square', 'fillerpattern.2d_circle': 'Circle',
    'fillerpattern.2d_semi_circle': 'Semi-Circle', 'fillerpattern.2d_arc': 'Arc', 'fillerpattern.2d_triangle': 'Triangle',
    'fillerpattern.2d_pentagon': 'Pentagon', 'fillerpattern.2d_hexagon': 'Hexagon', 'fillerpattern.2d_octagon': 'Octagon',
    'fillerpattern.parameter.hollow': 'Hollow', 'fillerpattern.parameter.filled': 'Filled',
    'fillerpattern.parameter.filled_outer': 'Surrounded',
    'buildcraft.param.rotation.0': 'No rotation', 'buildcraft.param.rotation.1': '90 degree rotation',
    'buildcraft.param.rotation.2': '180 degree rotation', 'buildcraft.param.rotation.3': '270 degree rotation',
    'buildcraft.param.axis.x': 'X Axis', 'buildcraft.param.axis.y': 'Y Axis', 'buildcraft.param.axis.z': 'Z Axis',
    'buildcraft.param.facing.down': 'Flat side: bottom', 'buildcraft.param.facing.up': 'Flat side: top',
    'buildcraft.param.facing.north': 'Flat side: north', 'buildcraft.param.facing.south': 'Flat side: south',
    'buildcraft.param.facing.west': 'Flat side: west', 'buildcraft.param.facing.east': 'Flat side: east',
    'direction.buildcraft.center.0': 'North-West', 'direction.buildcraft.center.1': 'North',
    'direction.buildcraft.center.2': 'North-East', 'direction.buildcraft.center.3': 'West', 'direction.buildcraft.center.4': 'Centre',
    'direction.buildcraft.center.5': 'East', 'direction.buildcraft.center.6': 'South-West', 'direction.buildcraft.center.7': 'South',
    'direction.buildcraft.center.8': 'South-East',
    'tip.filler.excavate.on': 'Excavate', 'tip.filler.excavate.off': 'Do Not Excavate',
    'tip.filler.invert.on': 'Invert', 'tip.filler.invert.off': 'Do Not Invert',
    'gui.buildcraft.filler.resources': 'Filling Resources', 'gui.buildcraft.filler.progress': 'Break: %s  Place: %s',
    'gui.buildcraft.filler.finished': 'Finished', 'gui.buildcraft.filler.off': 'Switched off',
    'gui.buildcraft.filler.locked': 'Set by a gate', 'gui.buildcraft.filler.no_area': 'No area: use volume markers',
})

# Architect table, builder and electronic library
FACINGS = [('north', 0), ('east', 90), ('south', 180), ('west', 270)]
for valid in ['off', 'on']:
    cube(f'architect_{valid}', particle='architect/back', down='architect/bottom', up='architect/top', north=f'architect/front_{valid}',
         east='architect/left', south='architect/back', west='architect/right')
write('blockstates/architect.json', {'variants': {
    f'facing={d},valid={v}': {'model': f'{NS}:block/architect_{"on" if v == "true" else "off"}', **({'y': y} if y else {})}
    for d, y in FACINGS for v in ['true', 'false']}})
block_item('architect', f'{NS}:block/architect_off')
cube('builder', particle='builder/side', down='builder/bottom', up='builder/top', north='builder/front', east='builder/side',
     south='builder/back', west='builder/side')
cube('library', particle='library/back', down='library/bottom', up='library/top', north='library/front', east='library/left',
     south='library/back', west='library/right')
for block in ['builder', 'library']:
    write(f'blockstates/{block}.json', {'variants': {
        f'facing={d}': {'model': f'{NS}:block/{block}', **({'y': y} if y else {})} for d, y in FACINGS}})
    block_item(block)
for block, english in [('architect', 'Architect Table'), ('builder', 'Builder'), ('library', 'Electronic Library')]:
    name('block', block, english)
    drops_self(block)
    tag('block', 'minecraft:mineable/pickaxe', f'{NS}:{block}')
shaped('architect', ['bmb', 'ycy', 'dsd'], {'b': '#c:dyes/black', 'm': f'{NS}:marker_volume', 'y': '#c:dyes/yellow',
                                            'c': 'minecraft:crafting_table', 'd': '#c:gears/diamond', 's': f'{NS}:blueprint'})
shaped('builder', ['bmb', 'ycy', 'gkg'], {'b': '#c:dyes/black', 'm': f'{NS}:marker_volume', 'y': '#c:dyes/yellow',
                                          'c': 'minecraft:crafting_table', 'g': '#c:gears/diamond', 'k': '#c:chests/wooden'})
shaped('library', ['igi', 'bcb', 'igi'], {'i': '#c:ingots/iron', 'g': '#c:gears/gold', 'b': f'{NS}:blueprint',
                                          'c': '#c:chests/wooden'})
for item, english, dye in [('template', 'Template', '#c:dyes/black'), ('blueprint', 'Blueprint', '#c:gems/lapis')]:
    for state in ['clean', 'used']:
        tex_exists(f'{NS}:item/{item}/{state}')
        write(f'models/item/{item}_{state}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{item}/{state}'}})
    write(f'items/{item}.json', {'model': {'type': 'minecraft:condition', 'property': 'minecraft:has_component',
                                           'component': f'{NS}:snapshot',
                                           'on_true': {'type': 'minecraft:model', 'model': f'{NS}:item/{item}_used'},
                                           'on_false': {'type': 'minecraft:model', 'model': f'{NS}:item/{item}_clean'}}})
    name('item', item, english)
    shaped(item, ['ppp', 'pdp', 'ppp'], {'p': 'minecraft:paper', 'd': dye})
LANG.update({
    'item.buildcraft.snapshot.clean': 'Blank %s', 'item.buildcraft.snapshot.used': '%s (%s)',
    'item.buildcraft.snapshot.named': '%s: %s',
    'gui.buildcraft.architect.no_area': 'No area: place this with volume markers behind it',
    'gui.buildcraft.architect.scanning': 'Scanning...', 'gui.buildcraft.architect.ready': 'Put in a blank template or blueprint',
    'gui.buildcraft.builder.no_plan': 'Put in a template or blueprint',
})

# List
for state in ['clean', 'used']:
    tex_exists(f'{NS}:item/list/{state}')
    write(f'models/item/list_{state}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/list/{state}'}})
write('items/list.json', {'model': {'type': 'minecraft:condition', 'property': 'minecraft:has_component', 'component': f'{NS}:list',
                                    'on_true': {'type': 'minecraft:model', 'model': f'{NS}:item/list_used'},
                                    'on_false': {'type': 'minecraft:model', 'model': f'{NS}:item/list_clean'}}})
name('item', 'list', 'List')
shaped('list', ['ppp', 'pgp', 'ppp'], {'p': 'minecraft:paper', 'g': '#c:dyes/green'})
LANG.update({
    'gui.buildcraft.list.precise.on': 'Matching exactly (including item data)', 'gui.buildcraft.list.precise.off': 'Ignoring item data',
    'gui.buildcraft.list.type.on': 'Matching items of the same type as the first item',
    'gui.buildcraft.list.type.off': 'Not matching by type',
    'gui.buildcraft.list.material.on': 'Matching items of the same material as the first item',
    'gui.buildcraft.list.material.off': 'Not matching by material',
})

finish()
print('Resources generated')
