/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.guide;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** Reads the guide's page format: lines of text (a little markdown: "#" headings and "- " lists) with BuildCraft's tags.
 * <ul>
 * <li>Whole-line tags: &lt;chapter name="..."/&gt;, &lt;new_page/&gt;, &lt;recipes stack="..."/&gt;, &lt;usages .../&gt;,
 * &lt;recipes_usages .../&gt;, &lt;recipe .../&gt;, &lt;image src="..." width="..." height="..."/&gt;.</li>
 * <li>Sections shown or hidden by the player's settings: &lt;lore&gt;, &lt;no_lore&gt;, &lt;hint&gt;, &lt;no_hint&gt;.</li>
 * <li>Inline tags: &lt;link to="page"/&gt; (or to="item" type="item_stack"), &lt;bold&gt;, &lt;italic&gt;, &lt;underline&gt;,
 * &lt;strikethrough&gt; and the colour names.</li>
 * </ul> */
public final class GuideParser {
    /** A piece of a page, before it is laid out. */
    public sealed interface Part {}

    public record Text(Component text, boolean bullet) implements Part {}

    public record Chapter(Component title, int level) implements Part {}

    public record Gap() implements Part {}

    public record NewPage() implements Part {}

    public record Recipes(Item item, boolean recipes, boolean usages) implements Part {}

    /** A link on a line of its own (shown with the target's icon). */
    public record Link(String target, boolean toItem) implements Part {}

    public record Image(Identifier texture, int width, int height) implements Part {}

    /** Links in text keep their target in the style's insertion, starting with this. */
    public static final String LINK_PREFIX = "buildcraft:guide_link:";

    private static final Pattern SECTION = Pattern.compile("</?(lore|no_lore|hint|no_hint|no_detail|note|json_insn|guide_md)(\\s[^>]*)?>");
    private static final Pattern ATTRIBUTE = Pattern.compile("(\\w+)=\"([^\"]*)\"");
    private static final Pattern SINGLE_TAG = Pattern.compile("^<(\\w+)((?:\\s+\\w+=\"[^\"]*\")*)\\s*/>$");
    private static final Pattern INLINE = Pattern.compile("<(/?)(\\w+)((?:\\s+\\w+=\"[^\"]*\")*)\\s*(/?)>");

    /** The colour tags, as in BuildCraft 7.99's guide (Minecraft's chat colours). */
    private static final Map<String, Integer> COLOURS = Map.ofEntries(Map.entry("black", 0x000000), Map.entry("dark_blue", 0x0000AA),
        Map.entry("dark_green", 0x00AA00), Map.entry("dark_aqua", 0x00AAAA), Map.entry("dark_red", 0xAA0000),
        Map.entry("dark_purple", 0xAA00AA), Map.entry("gold", 0xFFAA00), Map.entry("gray", 0xAAAAAA), Map.entry("dark_gray", 0x555555),
        Map.entry("blue", 0x5555FF), Map.entry("green", 0x55FF55), Map.entry("aqua", 0x55FFFF), Map.entry("red", 0xFF5555),
        Map.entry("light_purple", 0xFF55FF), Map.entry("yellow", 0xFFFF55), Map.entry("white", 0xFFFFFF));

    private GuideParser() {}

    public static List<Part> parse(String text, boolean lore, boolean hints, GuideData data) {
        List<Part> parts = new ArrayList<>();
        for (String line : visibleText(text, lore, hints).split("\n")) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                if (!parts.isEmpty() && !(parts.getLast() instanceof Gap)) parts.add(new Gap());
                continue;
            }
            boolean bullet = trimmed.startsWith("- ");
            String content = bullet ? trimmed.substring(2).strip() : trimmed;
            Matcher single = SINGLE_TAG.matcher(content);
            if (single.matches() && addTag(parts, single.group(1), attributes(single.group(2)), data)) continue;
            if (!bullet && trimmed.startsWith("#")) {
                int level = 0;
                while (level < trimmed.length() && trimmed.charAt(level) == '#') level++;
                parts.add(new Chapter(Component.literal(trimmed.substring(level).strip()), level - 1));
                continue;
            }
            parts.add(new Text(inline(content, data), bullet));
        }
        return parts;
    }

    /** Removes the sections the player's settings hide. */
    private static String visibleText(String text, boolean lore, boolean hints) {
        StringBuilder out = new StringBuilder();
        Deque<Boolean> visible = new ArrayDeque<>();
        Matcher m = SECTION.matcher(text);
        int last = 0;
        while (m.find()) {
            if (!visible.contains(false)) out.append(text, last, m.start());
            last = m.end();
            boolean closing = m.group().startsWith("</");
            if (closing) {
                if (!visible.isEmpty()) visible.pop();
            } else {
                visible.push(switch (m.group(1)) {
                    case "lore" -> lore;
                    case "no_lore" -> !lore;
                    case "hint" -> hints;
                    case "no_hint" -> !hints;
                    case "json_insn", "guide_md" -> false;
                    default -> true;
                });
            }
        }
        if (!visible.contains(false)) out.append(text.substring(last));
        return out.toString();
    }

    private static Map<String, String> attributes(String text) {
        Map<String, String> map = new HashMap<>();
        Matcher m = ATTRIBUTE.matcher(text);
        while (m.find()) {
            map.put(m.group(1), m.group(2));
        }
        return map;
    }

    /** @return False if the tag isn't a whole-line tag (so the line is treated as text). */
    private static boolean addTag(List<Part> parts, String tag, Map<String, String> attr, GuideData data) {
        switch (tag) {
            case "chapter" -> {
                int level = 0;
                try {
                    level = Integer.parseInt(attr.getOrDefault("level", "0"));
                } catch (NumberFormatException ignored) {}
                parts.add(new Chapter(Component.literal(attr.getOrDefault("name", "")), level));
            }
            case "new_page" -> parts.add(new NewPage());
            case "recipes", "usages", "recipes_usages", "recipe" -> {
                Item item = GuideData.item(attr.getOrDefault("stack", ""));
                if (item != null) {
                    parts.add(new Recipes(item, !tag.equals("usages"), tag.equals("usages") || tag.equals("recipes_usages")));
                }
            }
            case "link" -> parts.add(new Link(attr.getOrDefault("to", ""), "item_stack".equals(attr.get("type"))));
            case "image" -> {
                Identifier texture = imageTexture(attr.getOrDefault("src", ""));
                if (texture != null) {
                    parts.add(new Image(texture, parseInt(attr.get("width"), 64), parseInt(attr.get("height"), 64)));
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private static int parseInt(@Nullable String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Nullable
    private static Identifier imageTexture(String src) {
        Identifier id = Identifier.tryParse(src);
        if (id == null) return null;
        String path = id.getPath();
        if (!path.startsWith("textures/")) path = "textures/" + path;
        if (!path.endsWith(".png")) path += ".png";
        return Identifier.fromNamespaceAndPath(id.getNamespace(), path);
    }

    /** Turns a line with inline tags into a component. */
    public static Component inline(String line, GuideData data) {
        MutableComponent out = Component.empty();
        Deque<Style> styles = new ArrayDeque<>();
        styles.push(Style.EMPTY);
        Matcher m = INLINE.matcher(line);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) out.append(Component.literal(line.substring(last, m.start())).withStyle(styles.peek()));
            last = m.end();
            boolean closing = !m.group(1).isEmpty(), selfClosing = !m.group(4).isEmpty();
            String tag = m.group(2);
            if (tag.equals("link") && selfClosing) {
                Map<String, String> attr = attributes(m.group(3));
                String target = attr.getOrDefault("to", "");
                boolean toItem = "item_stack".equals(attr.get("type"));
                out.append(linkTitle(target, toItem, data).copy().withStyle(styles.peek().withColor(0x0000AA)
                    .withUnderlined(true).withInsertion(LINK_PREFIX + (toItem ? "item:" : "page:") + target)));
            } else if (closing) {
                if (styles.size() > 1) styles.pop();
            } else {
                Style style = styles.peek();
                Integer colour = COLOURS.get(tag);
                style = switch (tag) {
                    case "bold" -> style.withBold(true);
                    case "italic" -> style.withItalic(true);
                    case "underline" -> style.withUnderlined(true);
                    case "strikethrough" -> style.withStrikethrough(true);
                    default -> colour != null ? style.withColor(colour) : style;
                };
                styles.push(style);
            }
        }
        if (last < line.length()) out.append(Component.literal(line.substring(last)).withStyle(styles.peek()));
        return out;
    }

    public static Component linkTitle(String target, boolean toItem, GuideData data) {
        if (toItem) {
            Item item = GuideData.item(target);
            return item == null ? Component.literal(target) : item.getName(item.getDefaultInstance());
        }
        GuideData.Entry entry = data.findEntry(target);
        return entry == null ? Component.literal(target) : entry.title();
    }
}
