say SMOKE check
scoreboard objectives add smoke dummy
execute store result score #dst smoke run data get block 5 -60 0 Items[0].count
execute if score #dst smoke matches 1.. run say SMOKE PASS items went through the pipes
execute unless score #dst smoke matches 1.. run say SMOKE FAIL no items went through the pipes
execute if block 1 -60 4 minecraft:redstone_lamp[lit=true] run say SMOKE PASS gate gives a redstone signal
execute unless block 1 -60 4 minecraft:redstone_lamp[lit=true] run say SMOKE FAIL gate gives no redstone signal
execute store result score #water smoke run data get block 21 -60 0 tank.amount
execute if score #water smoke matches 1.. run say SMOKE PASS pump filled the tank
execute unless score #water smoke matches 1.. run say SMOKE FAIL the tank is empty
execute store result score #fill smoke run fill 32 -60 0 34 -59 2 minecraft:andesite replace minecraft:stone
execute if score #fill smoke matches 18 run say SMOKE PASS filler filled its box
execute unless score #fill smoke matches 18 run say SMOKE FAIL filler did not fill its box
execute if blocks 40 -60 10 42 -59 12 50 -60 10 all run say SMOKE PASS builder copied the blueprint
execute unless blocks 40 -60 10 42 -59 12 50 -60 10 all run say SMOKE FAIL builder's copy is different
execute if block 60 -60 0 buildcraft:pipe_lapis_item[colour=blue] run say SMOKE PASS gate painted the lapis pipe
execute unless block 60 -60 0 buildcraft:pipe_lapis_item[colour=blue] run say SMOKE FAIL gate did not paint the lapis pipe
execute if entity @e[type=buildcraft:volume_box,nbt={addons:{addon0:{type:"buildcraft:filler_planner"}}}] run say SMOKE PASS volume box with a filler planner
execute unless entity @e[type=buildcraft:volume_box,nbt={addons:{addon0:{type:"buildcraft:filler_planner"}}}] run say SMOKE FAIL no volume box
execute if block 60 -60 10 buildcraft:zone_planner if data block 68 -60 0 prev run say SMOKE PASS zone planner and path markers placed
execute unless block 60 -60 10 buildcraft:zone_planner run say SMOKE FAIL zone planner missing
say SMOKE done
stop
