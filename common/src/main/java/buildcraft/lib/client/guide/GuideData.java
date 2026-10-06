/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.guide;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import buildcraft.BuildCraft;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.StatementManager;

/** The guide book's contents, pages and crafting recipes, read from assets/buildcraft/guide (made by
 * tools/gen_guide.py). */
public final class GuideData {
    /** A page in the contents, shown with the icon of an item or a gate statement. */
    public record Entry(String page, @Nullable Item item, @Nullable String statement, @Nullable String fixedTitle) {
        public Component title() {
            if (fixedTitle != null) return Component.literal(fixedTitle);
            if (item != null) return new ItemStack(item).getHoverName();
            IStatement s = statement == null ? null : StatementManager.getStatement(statement);
            if (s != null) return s.getDescription();
            String name = page.substring(page.lastIndexOf('/') + 1).replace('_', ' ');
            return Component.literal(Character.toUpperCase(name.charAt(0)) + name.substring(1));
        }

        @Nullable
        public Identifier statementIcon() {
            IStatement s = statement == null ? null : StatementManager.getStatement(statement);
            return s == null ? null : s.getIcon();
        }
    }

    public record Category(String title, List<Entry> entries) {}

    /** A crafting recipe: each ingredient is an item id or a tag (starting with #), or null for an empty slot. */
    public record Recipe(Item result, int count, List<List<@Nullable String>> grid) {}

    public final List<Category> categories = new ArrayList<>();
    public final Map<Item, List<Recipe>> recipes = new HashMap<>();

    private GuideData() {}

    public static GuideData load() {
        GuideData data = new GuideData();
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        JsonElement index = readJson(resources, BuildCraft.id("guide/index.json"));
        if (index instanceof JsonArray array) {
            for (JsonElement category : array) {
                JsonObject obj = category.getAsJsonObject();
                List<Entry> entries = new ArrayList<>();
                for (JsonElement e : obj.getAsJsonArray("entries")) {
                    JsonObject entry = e.getAsJsonObject();
                    Item item = entry.has("item") ? item(entry.get("item").getAsString()) : null;
                    String statement = entry.has("statement") ? entry.get("statement").getAsString() : null;
                    String title = entry.has("title") ? entry.get("title").getAsString() : null;
                    entries.add(new Entry(entry.get("page").getAsString(), item, statement, title));
                }
                data.categories.add(new Category(obj.get("title").getAsString(), entries));
            }
        }
        JsonElement recipes = readJson(resources, BuildCraft.id("guide/recipes.json"));
        if (recipes instanceof JsonObject obj) {
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                Item result = item(entry.getKey());
                if (result == null) continue;
                for (JsonElement r : entry.getValue().getAsJsonArray()) {
                    JsonObject recipe = r.getAsJsonObject();
                    List<List<String>> grid = new ArrayList<>();
                    for (JsonElement row : recipe.getAsJsonArray("grid")) {
                        List<String> cells = new ArrayList<>();
                        for (JsonElement cell : row.getAsJsonArray()) {
                            cells.add(cell.isJsonNull() ? null : cell.getAsString());
                        }
                        grid.add(cells);
                    }
                    data.recipes.computeIfAbsent(result, k -> new ArrayList<>())
                        .add(new Recipe(result, recipe.get("count").getAsInt(), grid));
                }
            }
        }
        return data;
    }

    @Nullable
    public static Item item(String id) {
        Identifier location = Identifier.tryParse(id);
        if (location == null) return null;
        Item item = BuiltInRegistries.ITEM.getValue(location);
        return item == Items.AIR ? null : item;
    }

    /** @return The text of a page, in the player's language if there is a translation. */
    public static String loadPage(String page) {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        String language = Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase(Locale.ROOT);
        for (String lang : List.of(language, "en_us")) {
            Optional<Resource> resource = resources.getResource(BuildCraft.id("guide/" + lang + "/" + page + ".md"));
            if (resource.isPresent()) {
                try (BufferedReader reader = resource.get().openAsReader()) {
                    return reader.lines().collect(Collectors.joining("\n"));
                } catch (IOException e) {
                    BuildCraft.LOGGER.warn("Couldn't read guide page {}", page, e);
                }
            }
        }
        return "<chapter name=\"" + page + "\"/>\nThis page is missing.";
    }

    @Nullable
    private static JsonElement readJson(ResourceManager resources, Identifier id) {
        Optional<Resource> resource = resources.getResource(id);
        if (resource.isEmpty()) return null;
        try (BufferedReader reader = resource.get().openAsReader()) {
            return JsonParser.parseReader(reader);
        } catch (IOException | RuntimeException e) {
            BuildCraft.LOGGER.warn("Couldn't read {}", id, e);
            return null;
        }
    }

    @Nullable
    public Entry findEntry(String page) {
        for (Category category : categories) {
            for (Entry entry : category.entries()) {
                if (entry.page().equals(page)) return entry;
            }
        }
        return null;
    }

    @Nullable
    public Entry findEntry(Item item) {
        for (Category category : categories) {
            for (Entry entry : category.entries()) {
                if (entry.item() == item) return entry;
            }
        }
        return null;
    }
}
