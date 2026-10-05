# Builds a few small machines when the world loads; check runs 30 seconds later and stops the server.
say SMOKE setup
forceload add -16 -16 80 32
# Items: chest -> wooden pipe (powered by a redstone engine) -> stone pipes -> chest
setblock 0 -60 0 minecraft:chest{Items:[{Slot:0b,id:"minecraft:cobblestone",count:64}]}
setblock 1 -60 0 buildcraft:pipe_wood_item
setblock 2 -60 0 buildcraft:pipe_stone_item
setblock 3 -60 0 buildcraft:pipe_stone_item
setblock 4 -60 0 buildcraft:pipe_stone_item
setblock 5 -60 0 minecraft:chest
setblock 1 -59 0 buildcraft:engine_redstone[facing=down]
setblock 1 -58 0 minecraft:redstone_block
# Gates: an always-on gate giving a redstone signal to a lamp
setblock 0 -60 4 buildcraft:pipe_structure{plugs:{up:{id:"buildcraft:gate",variant:{logic:"and",material:"iron",modifier:"no_modifier"},trigger0:{kind:"buildcraft:true"},action0:{kind:"buildcraft:redstone.output"}}}}
setblock 1 -60 4 minecraft:redstone_lamp
# Fluids: a pump over a pool of water, filling a tank
fill 19 -63 -1 21 -61 1 minecraft:water
setblock 20 -60 0 buildcraft:pump
setblock 20 -59 0 buildcraft:engine_creative[facing=down]{currentOutputIndex:8}
setblock 20 -58 0 minecraft:redstone_block
setblock 21 -60 0 buildcraft:tank
# Filler: fills a 3x2x3 box with stone
setblock 30 -60 0 buildcraft:filler{battery:16000000000L,pattern:"buildcraft:filler_fill",box:[I;32,-60,0,34,-59,2],inv:{Items:[{Slot:0b,id:"minecraft:stone",count:64}]}}
setblock 30 -59 0 buildcraft:engine_creative[facing=down]{currentOutputIndex:8}
setblock 30 -58 0 minecraft:redstone_block
# Architect table and builder: scans a small structure into a blueprint, then builds a copy
fill 40 -60 10 42 -60 10 minecraft:stone
setblock 41 -60 11 minecraft:furnace[facing=east]
setblock 41 -60 8 buildcraft:architect[facing=north,valid=true]{box:[I;40,-60,10,42,-59,12],in:{Items:[{Slot:0b,id:"buildcraft:blueprint",count:1}]}}
setblock 51 -60 8 buildcraft:builder[facing=north]{battery:16000000000L,inv:{Items:[{Slot:0b,id:"minecraft:stone",count:8},{Slot:1b,id:"minecraft:furnace",count:1}]}}
setblock 51 -59 8 buildcraft:engine_creative[facing=down]{currentOutputIndex:8}
setblock 51 -58 8 minecraft:redstone_block
schedule function smoke:copy_blueprint 40t
schedule function smoke:check 600t
