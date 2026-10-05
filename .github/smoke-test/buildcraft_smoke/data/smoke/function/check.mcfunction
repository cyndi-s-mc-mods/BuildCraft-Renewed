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
say SMOKE done
stop
