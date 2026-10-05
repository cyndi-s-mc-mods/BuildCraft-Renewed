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
# TODO: combustion engine recipe once the engine is ported
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

finish()
print('Resources generated')
