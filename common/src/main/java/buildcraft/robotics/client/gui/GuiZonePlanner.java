/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

import buildcraft.BuildCraft;
import buildcraft.core.item.ItemPaintbrush;
import buildcraft.lib.gui.GuiBC;
import buildcraft.lib.misc.ZonePlan;
import buildcraft.lib.net.BCNetwork;
import buildcraft.robotics.container.ContainerZonePlanner;
import buildcraft.robotics.tile.TileZonePlanner;
import buildcraft.robotics.zone.ZonePackets;

/** The zone planner's GUI: a map of the area around the planner, seen from above. Drag the map to move it and scroll
 * to zoom. Holding a paintbrush (picked up from a slot), drag over the map to add an area to that colour's zone, or
 * drag with the right button to remove it. */
public class GuiZonePlanner extends GuiBC<ContainerZonePlanner> {
    private static final int MAP_X = 8, MAP_Y = 9, MAP_W = 213, MAP_H = 100;
    private static final float[] ZOOMS = { 0.5f, 1, 2, 4, 8 };
    private static final Identifier MAP_TEXTURE = BuildCraft.id("dynamic/zone_planner_map");

    /** The map colours of the chunks the server has sent, for the level they're in. */
    private static final Map<Long, int[]> CHUNKS = new HashMap<>();
    @Nullable
    private static ClientLevel chunksLevel;

    public static void receive(ZonePackets.MapData data) {
        CHUNKS.put(data.chunk(), data.colours());
    }

    private final Set<Long> requested = new HashSet<>();
    @Nullable
    private DynamicTexture texture;
    private double viewX, viewZ;
    private int zoom = 1;
    private boolean panning = false;
    /** The block where painting started, and the button it started with. */
    @Nullable
    private BlockPos paintStart;
    private int paintButton;
    @Nullable
    private BlockPos hovered;

    public GuiZonePlanner(ContainerZonePlanner menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/zone_planner.png"), 256, 228);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelY = -1000;
        inventoryLabelY = -1000;
        BlockPos planner = menu.getPlannerPos();
        viewX = planner.getX() + 0.5;
        viewZ = planner.getZ() + 0.5;
        Minecraft mc = Minecraft.getInstance();
        if (chunksLevel != mc.level) {
            CHUNKS.clear();
            chunksLevel = mc.level;
        }
        if (texture == null) {
            texture = new DynamicTexture(() -> "BuildCraft zone planner map", MAP_W, MAP_H, true);
            mc.getTextureManager().register(MAP_TEXTURE, texture);
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(MAP_TEXTURE);
            texture = null;
        }
    }

    @Nullable
    private TileZonePlanner getTile() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getBlockEntity(menu.getPlannerPos()) instanceof TileZonePlanner tile ? tile : null;
    }

    /** @return The colour of the paintbrush being carried, or -1. */
    private int brushColour() {
        ItemStack carried = menu.getCarried();
        if (!(carried.getItem() instanceof ItemPaintbrush)) return -1;
        DyeColor colour = ItemPaintbrush.getColour(carried);
        return colour == null ? -1 : colour.getId();
    }

    private float scale() {
        return ZOOMS[zoom];
    }

    private int blockX(double screenX) {
        return (int) Math.floor(viewX + (screenX - leftPos - MAP_X - MAP_W / 2.0) / scale());
    }

    private int blockZ(double screenY) {
        return (int) Math.floor(viewZ + (screenY - topPos - MAP_Y - MAP_H / 2.0) / scale());
    }

    private boolean isOverMap(double mouseX, double mouseY) {
        return isHovering(MAP_X, MAP_Y, MAP_W, MAP_H, mouseX, mouseY);
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        drawMap();
        graphics.blit(RenderPipelines.GUI_TEXTURED, MAP_TEXTURE, leftPos + MAP_X, topPos + MAP_Y, 0, 0, MAP_W, MAP_H, MAP_W, MAP_H);
        int copyTime = TileZonePlanner.copyTime();
        int in = menu.progressInput.getInt() * 28 / copyTime;
        if (in > 0) blitPart(graphics, 44, 128, 9, 228, in, 9);
        int out = menu.progressOutput.getInt() * 28 / copyTime;
        if (out > 0) blitPart(graphics, 236, 45, 0, 228, 9, out);
    }

    /** Draws the map (and the zones on it) into the map texture. */
    private void drawMap() {
        if (texture == null || texture.getPixels() == null) return;
        TileZonePlanner tile = getTile();
        BlockPos planner = menu.getPlannerPos();
        ChunkPos centre = ChunkPos.containing(planner);
        int brush = brushColour();
        List<Long> toRequest = new ArrayList<>();
        int selX0 = 0, selZ0 = 0, selX1 = -1, selZ1 = -1;
        if (paintStart != null && hovered != null) {
            selX0 = Math.min(paintStart.getX(), hovered.getX());
            selX1 = Math.max(paintStart.getX(), hovered.getX());
            selZ0 = Math.min(paintStart.getZ(), hovered.getZ());
            selZ1 = Math.max(paintStart.getZ(), hovered.getZ());
        }
        for (int py = 0; py < MAP_H; py++) {
            int z = (int) Math.floor(viewZ + (py - MAP_H / 2.0) / scale());
            for (int px = 0; px < MAP_W; px++) {
                int x = (int) Math.floor(viewX + (px - MAP_W / 2.0) / scale());
                int chunkX = x >> 4, chunkZ = z >> 4;
                int colour;
                if (Math.abs(chunkX - centre.x()) > TileZonePlanner.MAP_RADIUS || Math.abs(chunkZ - centre.z()) > TileZonePlanner.MAP_RADIUS) {
                    colour = 0xFF0A0A0A;
                } else {
                    long key = ChunkPos.pack(chunkX, chunkZ);
                    int[] chunk = CHUNKS.get(key);
                    if (chunk == null) {
                        colour = 0xFF303030;
                        if (requested.add(key)) toRequest.add(key);
                    } else {
                        colour = chunk[(x & 15) + (z & 15) * 16];
                    }
                }
                if (tile != null) {
                    for (int c = 0; c < 16; c++) {
                        if (brush >= 0 && c != brush) continue;
                        if (tile.getLayer(c).get(x, z)) {
                            colour = ARGB.srgbLerp(0.55f, colour, 0xFF000000 | DyeColor.byId(c).getTextureDiffuseColor());
                        }
                    }
                }
                if (x >= selX0 && x <= selX1 && z >= selZ0 && z <= selZ1) {
                    int paint = paintButton == 0 && brush >= 0 ? 0xFF000000 | DyeColor.byId(brush).getTextureDiffuseColor() : 0xFF000000;
                    colour = ARGB.srgbLerp(0.6f, colour, paint);
                }
                if (x == planner.getX() && z == planner.getZ()) colour = 0xFFFF2020;
                texture.getPixels().setPixel(px, py, colour);
            }
        }
        texture.upload();
        for (int i = 0; i < toRequest.size(); i += 64) {
            BCNetwork.sendToServer(new ZonePackets.MapRequest(planner, List.copyOf(toRequest.subList(i, Math.min(toRequest.size(), i + 64)))));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        hovered = isOverMap(mouseX, mouseY) ? new BlockPos(blockX(mouseX), 0, blockZ(mouseY)) : null;
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        if (hovered != null) {
            graphics.text(font, Component.translatable("gui.buildcraft.zone_planner.position", hovered.getX(), hovered.getZ()),
                leftPos + 100, topPos + 128, 0xFF404040, false);
        }
        if (hovered != null && menu.getCarried().isEmpty()) {
            showTooltip(graphics, List.of(Component.translatable(brushColour() >= 0 ? "gui.buildcraft.zone_planner.paint"
                : "gui.buildcraft.zone_planner.help")), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isOverMap(event.x(), event.y())) {
            if (brushColour() >= 0) {
                paintStart = new BlockPos(blockX(event.x()), 0, blockZ(event.y()));
                paintButton = containerButton(event);
            } else if (containerButton(event) == 0) {
                panning = true;
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (panning) {
            viewX -= dx / scale();
            viewZ -= dy / scale();
            BlockPos planner = menu.getPlannerPos();
            double limit = TileZonePlanner.MAP_RADIUS * 16;
            viewX = Math.clamp(viewX, planner.getX() - limit, planner.getX() + limit);
            viewZ = Math.clamp(viewZ, planner.getZ() - limit, planner.getZ() + limit);
            return true;
        }
        if (paintStart != null) return true;
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (panning) {
            panning = false;
            return true;
        }
        if (paintStart != null) {
            int brush = brushColour();
            if (brush >= 0 && isOverMap(event.x(), event.y())) {
                int x = blockX(event.x()), z = blockZ(event.y());
                BCNetwork.sendToServer(new ZonePackets.Paint(menu.getPlannerPos(), brush, paintStart.getX(), paintStart.getZ(), x, z,
                    paintButton == 0));
            }
            paintStart = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (isOverMap(x, y) && scrollY != 0) {
            zoom = Math.clamp(zoom + (scrollY > 0 ? 1 : -1), 0, ZOOMS.length - 1);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }
}
