#!/usr/bin/env python3
"""Recolours the grey fluid texture templates into one texture per BuildCraft oil/fuel fluid and heat level.

BuildCraft 1.12 did this at runtime; modern Minecraft has no hook for it on every loader, so the textures are baked.
Needs Pillow. Run from anywhere: python3 tools/gen_fluid_textures.py
"""
import os
import shutil

from PIL import Image

TEX = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'common', 'src', 'main', 'resources', 'assets',
                   'buildcraft', 'textures', 'block', 'fluids')

# name: (light colour, dark colour), matching BCEnergyFluids in BuildCraft 1.12
FLUIDS = {
    'oil': (0x505050, 0x050505),
    'oil_residue': (0x100F10, 0x421042),
    'oil_heavy': (0xA08F1F, 0x423520),
    'oil_dense': (0x876E77, 0x422424),
    'oil_distilled': (0xE4AF78, 0xB47F00),
    'fuel_dense': (0xFFAF3F, 0xE07F00),
    'fuel_mixed_heavy': (0xF2A700, 0xC48700),
    'fuel_light': (0xFFFF30, 0xE4CF00),
    'fuel_mixed_light': (0xF6D700, 0xC4B700),
    'fuel_gaseous': (0xFAF630, 0xE0D900),
}


def channel(colour, shift):
    return (colour >> shift) & 0xFF


def recolour(src, dst, light, dark):
    img = Image.open(src).convert('RGBA')
    pixels = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = pixels[x, y]
            out = []
            for value, shift in ((r, 16), (g, 8), (b, 0)):
                out.append((channel(dark, shift) * (256 - value) + channel(light, shift) * value) // 256)
            pixels[x, y] = (out[0], out[1], out[2], 255)
    img.save(dst)


for name, (light, dark) in FLUIDS.items():
    for heat in range(3):
        for kind in ('still', 'flow'):
            template = os.path.join(TEX, f'heat_{heat}_{kind}.png')
            target = os.path.join(TEX, f'{name}_heat_{heat}_{kind}.png')
            recolour(template, target, light, dark)
            shutil.copyfile(template + '.mcmeta', target + '.mcmeta')
print('Fluid textures generated')
