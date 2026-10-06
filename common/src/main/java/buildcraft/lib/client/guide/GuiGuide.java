/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.guide;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.BuildCraft;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.recipes.AssemblyRecipes;
import buildcraft.api.recipes.IngredientStack;

/** The guide book: the contents (with a search box) and the pages, shown two at a time like an open book. */
public class GuiGuide extends Screen {
    private static final Identifier ICONS = BuildCraft.id("textures/gui/guide/icons.png");
    private static final Identifier LEFT_PAGE = BuildCraft.id("textures/gui/guide/left_page.png");
    private static final Identifier RIGHT_PAGE = BuildCraft.id("textures/gui/guide/right_page.png");
    private static final int PAGE_W = 193, PAGE_H = 248, TEXT_W = 168, TEXT_H = 190, TEXT_Y = 25;
    private static final int LEFT_TEXT_X = 23, RIGHT_TEXT_X = 4;
    private static final int TEXT_COLOUR = 0xFF1A1A1A;

    /** Whether lore is shown, remembered while the game runs. */
    private static boolean lore = true;
    @Nullable
    private static String lastPage;

    /** Something laid out on a page. */
    private interface Elem {
        int height();

        void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY);

        /** @return What clicking at the given position (relative to the element) does, or null. */
        default @Nullable Runnable click(int x, int y) {
            return null;
        }
    }

    private record Placed(Elem elem, int y) {}

    private record View(@Nullable String page, int spread) {}

    private final GuideData data;
    private final Deque<View> history = new ArrayDeque<>();
    @Nullable
    private String page;
    private List<List<Placed>> pages = List.of();
    private int spread = 0;
    @Nullable
    private EditBox search;
    private int left, top;

    public GuiGuide() {
        super(Component.translatable("item.buildcraft.guide"));
        this.data = GuideData.load();
        this.page = lastPage;
    }

    @Override
    protected void init() {
        left = (width - PAGE_W * 2) / 2;
        top = Math.max(0, (height - PAGE_H) / 2);
        String query = search == null ? "" : search.getValue();
        search = new EditBox(font, left + LEFT_TEXT_X, top + 10, 110, 12, Component.translatable("gui.buildcraft.guide.search"));
        search.setValue(query);
        search.setHint(Component.translatable("gui.buildcraft.guide.search").withStyle(ChatFormatting.GRAY));
        search.setResponder(text -> {
            spread = 0;
            layout();
        });
        addRenderableWidget(search);
        layout();
    }

    private void open(@Nullable String newPage) {
        history.push(new View(page, spread));
        page = newPage;
        lastPage = newPage;
        spread = 0;
        layout();
    }

    private void back() {
        View view = history.isEmpty() ? new View(null, 0) : history.pop();
        page = view.page();
        lastPage = page;
        layout();
        spread = Math.min(view.spread(), maxSpread());
    }

    private int maxSpread() {
        return Math.max(0, (pages.size() - 1) / 2);
    }

    // Layout

    private void layout() {
        List<Elem> elems = new ArrayList<>();
        if (page == null) {
            layoutContents(elems);
        } else {
            layoutPage(elems, page);
        }
        if (search != null) search.visible = page == null;
        List<List<Placed>> laidOut = new ArrayList<>();
        List<Placed> current = new ArrayList<>();
        int y = 0;
        for (int i = 0; i < elems.size(); i++) {
            Elem elem = elems.get(i);
            // Headings stay on the same page as what follows them
            boolean heading = elem instanceof ChapterElem || elem instanceof SmallHeadingElem;
            int needed = elem.height() + (heading && i + 1 < elems.size() && elems.get(i + 1) != NEW_PAGE ? elems.get(i + 1).height() : 0);
            if (heading && y > 0 && y + needed > TEXT_H) {
                laidOut.add(current);
                current = new ArrayList<>();
                y = 0;
            }
            if (elem == NEW_PAGE) {
                if (!current.isEmpty()) {
                    laidOut.add(current);
                    current = new ArrayList<>();
                    y = 0;
                }
                continue;
            }
            if (y == 0 && elem instanceof GapElem) continue;
            if (y + elem.height() > TEXT_H && !current.isEmpty()) {
                laidOut.add(current);
                current = new ArrayList<>();
                y = 0;
                if (elem instanceof GapElem) continue;
            }
            current.add(new Placed(elem, y));
            y += elem.height();
        }
        if (!current.isEmpty()) laidOut.add(current);
        pages = laidOut;
        spread = Math.min(spread, maxSpread());
    }

    private static final Elem NEW_PAGE = new GapElem();

    private void layoutContents(List<Elem> elems) {
        String query = search == null ? "" : search.getValue().strip().toLowerCase(Locale.ROOT);
        for (GuideData.Category category : data.categories) {
            List<GuideData.Entry> shown = new ArrayList<>();
            for (GuideData.Entry entry : category.entries()) {
                if (query.isEmpty() || entry.title().getString().toLowerCase(Locale.ROOT).contains(query)) shown.add(entry);
            }
            if (shown.isEmpty()) continue;
            elems.add(new ChapterElem(Component.literal(category.title()), 0));
            for (GuideData.Entry entry : shown) {
                elems.add(new LinkElem(entry, entry.title(), () -> open(entry.page())));
            }
            elems.add(new GapElem());
        }
        if (elems.isEmpty()) {
            elems.add(new TextElem(Component.translatable("gui.buildcraft.guide.no_results").getVisualOrderText(), 0));
        }
    }

    private void layoutPage(List<Elem> elems, String pageId) {
        GuideData.Entry entry = data.findEntry(pageId);
        Component title = entry == null ? Component.literal(pageId) : entry.title();
        if (entry != null) elems.add(new HeaderElem(entry, title));
        boolean first = true;
        for (GuideParser.Part part : GuideParser.parse(GuideData.loadPage(pageId), lore, false, data)) {
            if (first && part instanceof GuideParser.Chapter chapter && chapter.title().getString().equalsIgnoreCase(title.getString())) {
                first = false;
                continue;
            }
            first = false;
            switch (part) {
                case GuideParser.Text text -> {
                    int indent = text.bullet() ? 8 : 0;
                    List<FormattedCharSequence> lines = font.split(text.text(), TEXT_W - indent);
                    for (int i = 0; i < lines.size(); i++) {
                        elems.add(new TextElem(lines.get(i), indent, text.bullet() && i == 0));
                    }
                }
                case GuideParser.Chapter chapter -> elems.add(new ChapterElem(chapter.title(), chapter.level()));
                case GuideParser.Gap gap -> elems.add(new GapElem());
                case GuideParser.NewPage newPage -> elems.add(NEW_PAGE);
                case GuideParser.Link link -> elems.add(linkElem(link.target(), link.toItem()));
                case GuideParser.Image image -> elems.add(new ImageElem(image.texture(), image.width(), image.height()));
                case GuideParser.Recipes recipes -> addRecipes(elems, recipes);
            }
        }
    }

    private Elem linkElem(String target, boolean toItem) {
        Component title = GuideParser.linkTitle(target, toItem, data);
        if (toItem) {
            Item item = GuideData.item(target);
            GuideData.Entry entry = item == null ? null : data.findEntry(item);
            return new LinkElem(item == null ? ItemStack.EMPTY : new ItemStack(item), null, title,
                entry == null ? null : () -> open(entry.page()));
        }
        GuideData.Entry entry = data.findEntry(target);
        if (entry == null) return new TextElem(title.getVisualOrderText(), 0);
        return new LinkElem(entry, title, () -> open(entry.page()));
    }

    private void addRecipes(List<Elem> elems, GuideParser.Recipes part) {
        Item item = part.item();
        ItemStack stack = new ItemStack(item);
        if (part.recipes()) {
            List<Elem> found = new ArrayList<>();
            for (GuideData.Recipe recipe : data.recipes.getOrDefault(item, List.of())) {
                found.add(new RecipeElem(recipe));
            }
            for (AssemblyRecipes.AssemblyRecipe recipe : AssemblyRecipes.getAll()) {
                if (recipe.output().is(item)) found.add(new AssemblyElem(recipe));
            }
            if (!found.isEmpty()) {
                elems.add(new SmallHeadingElem(Component.translatable("gui.buildcraft.guide.recipes")));
                elems.addAll(found);
            }
        }
        if (part.usages()) {
            List<Elem> found = new ArrayList<>();
            for (List<GuideData.Recipe> recipes : data.recipes.values()) {
                for (GuideData.Recipe recipe : recipes) {
                    if (found.size() < 6 && uses(recipe, stack)) found.add(new RecipeElem(recipe));
                }
            }
            for (AssemblyRecipes.AssemblyRecipe recipe : AssemblyRecipes.getAll()) {
                if (found.size() < 9 && recipe.inputs().stream().anyMatch(i -> i.test(stack))) found.add(new AssemblyElem(recipe));
            }
            if (!found.isEmpty()) {
                elems.add(new SmallHeadingElem(Component.translatable("gui.buildcraft.guide.usages")));
                elems.addAll(found);
            }
        }
    }

    private static boolean uses(GuideData.Recipe recipe, ItemStack stack) {
        for (List<String> row : recipe.grid()) {
            for (String cell : row) {
                if (cell != null && !ingredientStacks(cell).isEmpty() && ingredientStacks(cell).stream().anyMatch(s -> s.is(stack.getItem()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** @return The items an ingredient (an item id, or a tag starting with #) can be. */
    private static List<ItemStack> ingredientStacks(String ingredient) {
        if (ingredient.startsWith("#")) {
            Identifier id = Identifier.tryParse(ingredient.substring(1));
            if (id == null) return List.of();
            List<ItemStack> stacks = new ArrayList<>();
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, id))) {
                stacks.add(new ItemStack(holder.value()));
            }
            return stacks;
        }
        Item item = GuideData.item(ingredient);
        return item == null ? List.of() : List.of(new ItemStack(item));
    }

    /** @return One of the given stacks, changing every second. */
    private static ItemStack cycle(List<ItemStack> stacks) {
        if (stacks.isEmpty()) return ItemStack.EMPTY;
        return stacks.get((int) ((System.currentTimeMillis() / 1000) % stacks.size()));
    }

    // Elements

    private static final class GapElem implements Elem {
        @Override
        public int height() {
            return 5;
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {}
    }

    private final class TextElem implements Elem {
        private final FormattedCharSequence line;
        private final int indent;
        private final boolean bullet;

        TextElem(FormattedCharSequence line, int indent) {
            this(line, indent, false);
        }

        TextElem(FormattedCharSequence line, int indent, boolean bullet) {
            this.line = line;
            this.indent = indent;
            this.bullet = bullet;
        }

        @Override
        public int height() {
            return 10;
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
            if (bullet) graphics.text(font, "•", x + 1, y, TEXT_COLOUR, false);
            graphics.text(font, line, x + indent, y, TEXT_COLOUR, false);
        }

        @Override
        public @Nullable Runnable click(int x, int y) {
            Style style = styleAt(line, x - indent);
            String insertion = style == null ? null : style.getInsertion();
            if (insertion == null || !insertion.startsWith(GuideParser.LINK_PREFIX)) return null;
            String target = insertion.substring(GuideParser.LINK_PREFIX.length());
            if (target.startsWith("page:")) {
                String to = target.substring(5);
                return data.findEntry(to) == null ? null : () -> open(to);
            }
            Item item = GuideData.item(target.substring(5));
            GuideData.Entry entry = item == null ? null : data.findEntry(item);
            return entry == null ? null : () -> open(entry.page());
        }
    }

    /** @return The style of the text at the given width along a line, or null if it's past the end. */
    @Nullable
    private Style styleAt(FormattedCharSequence line, int x) {
        if (x < 0) return null;
        int[] width = { 0 };
        Style[] found = { null };
        line.accept((index, style, codepoint) -> {
            int w = font.width(FormattedCharSequence.forward(Character.toString(codepoint), style));
            if (x < width[0] + w) {
                found[0] = style;
                return false;
            }
            width[0] += w;
            return true;
        });
        return found[0];
    }

    private final class ChapterElem implements Elem {
        private final Component title;
        private final int level;

        ChapterElem(Component title, int level) {
            this.title = title;
            this.level = level;
        }

        @Override
        public int height() {
            return level == 0 ? 16 : 13;
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
            int colour = level == 0 ? 0xFF1F3F8F : 0xFF3F3F7F;
            graphics.text(font, title.copy().withStyle(ChatFormatting.BOLD), x + level * 6, y + 3, colour, false);
            if (level == 0) graphics.fill(x, y + 13, x + TEXT_W, y + 14, 0x401F3F8F);
        }
    }

    private final class SmallHeadingElem implements Elem {
        private final Component title;

        SmallHeadingElem(Component title) {
            this.title = title;
        }

        @Override
        public int height() {
            return 12;
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
            graphics.text(font, title.copy().withStyle(ChatFormatting.ITALIC), x, y + 2, 0xFF404040, false);
        }
    }

    /** The title of a page, with its icon. */
    private final class HeaderElem implements Elem {
        private final GuideData.Entry entry;
        private final Component title;

        HeaderElem(GuideData.Entry entry, Component title) {
            this.entry = entry;
            this.title = title;
        }

        @Override
        public int height() {
            return 24;
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
            drawIcon(graphics, entry.item() == null ? ItemStack.EMPTY : new ItemStack(entry.item()), entry.statementIcon(), x, y + 2);
            graphics.text(font, title.copy().withStyle(ChatFormatting.BOLD), x + 20, y + 6, 0xFF1F3F8F, false);
            graphics.fill(x, y + 20, x + TEXT_W, y + 21, 0x401F3F8F);
        }
    }

    private void drawIcon(GuiGraphicsExtractor graphics, ItemStack stack, @Nullable Identifier statementIcon, int x, int y) {
        if (!stack.isEmpty()) {
            graphics.item(stack, x, y);
        } else if (statementIcon != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, statementIcon, x, y, 0, 0, 16, 16, 16, 16);
        }
    }

    /** A line with an icon that opens a page when clicked. */
    private final class LinkElem implements Elem {
        private final ItemStack stack;
        @Nullable
        private final Identifier statementIcon;
        private final Component title;
        @Nullable
        private final Runnable action;

        LinkElem(GuideData.Entry entry, Component title, @Nullable Runnable action) {
            this(entry.item() == null ? ItemStack.EMPTY : new ItemStack(entry.item()), entry.statementIcon(), title, action);
        }

        LinkElem(ItemStack stack, @Nullable Identifier statementIcon, Component title, @Nullable Runnable action) {
            this.stack = stack;
            this.statementIcon = statementIcon;
            this.title = title;
            this.action = action;
        }

        @Override
        public int height() {
            return 18;
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
            boolean hovered = action != null && mouseX >= x && mouseX < x + TEXT_W && mouseY >= y && mouseY < y + 18;
            if (hovered) graphics.fill(x - 1, y, x + TEXT_W, y + 18, 0x20000000);
            drawIcon(graphics, stack, statementIcon, x, y + 1);
            graphics.text(font, title, x + 20, y + 5, hovered ? 0xFF1F3F8F : TEXT_COLOUR, false);
        }

        @Override
        public @Nullable Runnable click(int x, int y) {
            return action;
        }
    }

    private final class ImageElem implements Elem {
        private final Identifier texture;
        private final int width, height;

        ImageElem(Identifier texture, int width, int height) {
            this.texture = texture;
            this.width = Math.min(width, TEXT_W);
            this.height = Math.min(height * this.width / Math.max(1, width), TEXT_H - 10);
        }

        @Override
        public int height() {
            return height + 4;
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + (TEXT_W - width) / 2, y + 2, 0, 0, width, height, width, height);
        }
    }

    /** Draws an item in a slot, with its tooltip when hovered; clicking opens its page. */
    private void slot(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x, y, x + 17, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + 17, 0xFF373737);
        graphics.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        if (stack.isEmpty()) return;
        graphics.item(stack, x + 1, y + 1);
        graphics.itemDecorations(font, stack, x + 1, y + 1);
        if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
            graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
        }
    }

    @Nullable
    private Runnable slotClick(ItemStack stack) {
        GuideData.Entry entry = stack.isEmpty() ? null : data.findEntry(stack.getItem());
        return entry == null || entry.page().equals(page) ? null : () -> open(entry.page());
    }

    private final class RecipeElem implements Elem {
        private final GuideData.Recipe recipe;

        RecipeElem(GuideData.Recipe recipe) {
            this.recipe = recipe;
        }

        @Override
        public int height() {
            return 58;
        }

        private ItemStack at(int row, int col) {
            if (row >= recipe.grid().size() || col >= recipe.grid().get(row).size()) return ItemStack.EMPTY;
            String cell = recipe.grid().get(row).get(col);
            return cell == null ? ItemStack.EMPTY : cycle(ingredientStacks(cell));
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
            int gx = x + 20, gy = y + 2;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    slot(graphics, at(row, col), gx + col * 18, gy + row * 18, mouseX, mouseY);
                }
            }
            graphics.text(font, "→", gx + 62, gy + 22, 0xFF404040, false);
            slot(graphics, new ItemStack(recipe.result(), recipe.count()), gx + 80, gy + 18, mouseX, mouseY);
        }

        @Override
        public @Nullable Runnable click(int x, int y) {
            int gx = 20, gy = 2;
            if (x >= gx + 80 && x < gx + 98 && y >= gy + 18 && y < gy + 36) return slotClick(new ItemStack(recipe.result()));
            int col = (x - gx) / 18, row = (y - gy) / 18;
            if (x < gx || col > 2 || row < 0 || row > 2) return null;
            return slotClick(at(row, col));
        }
    }

    private final class AssemblyElem implements Elem {
        private final AssemblyRecipes.AssemblyRecipe recipe;

        AssemblyElem(AssemblyRecipes.AssemblyRecipe recipe) {
            this.recipe = recipe;
        }

        @Override
        public int height() {
            return 34;
        }

        private ItemStack input(int i) {
            IngredientStack ingredient = recipe.inputs().get(i);
            ItemStack stack = cycle(ingredient.getDisplayStacks());
            return stack.isEmpty() ? stack : stack.copyWithCount(ingredient.count());
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
            graphics.text(font, Component.translatable("gui.buildcraft.guide.assembly", recipe.powerRequired() / MjAPI.MJ), x, y + 1,
                0xFF404040, false);
            int n = Math.min(recipe.inputs().size(), 6);
            for (int i = 0; i < n; i++) {
                slot(graphics, input(i), x + i * 18, y + 12, mouseX, mouseY);
            }
            graphics.text(font, "→", x + n * 18 + 4, y + 17, 0xFF404040, false);
            slot(graphics, recipe.output(), x + n * 18 + 16, y + 12, mouseX, mouseY);
        }

        @Override
        public @Nullable Runnable click(int x, int y) {
            int n = Math.min(recipe.inputs().size(), 6);
            if (y < 12 || y >= 30) return null;
            if (x >= n * 18 + 16 && x < n * 18 + 34) return slotClick(recipe.output());
            return x >= 0 && x < n * 18 ? slotClick(input(x / 18)) : null;
        }
    }

    // Drawing and input

    private int pageX(int side) {
        return left + side * PAGE_W + (side == 0 ? LEFT_TEXT_X : RIGHT_TEXT_X);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, LEFT_PAGE, left, top, 0, 0, PAGE_W, PAGE_H, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, RIGHT_PAGE, left + PAGE_W, top, 0, 0, PAGE_W, PAGE_H, 256, 256);
        for (int side = 0; side < 2; side++) {
            int index = spread * 2 + side;
            if (index >= pages.size()) continue;
            int x = pageX(side), y = top + TEXT_Y;
            for (Placed placed : pages.get(index)) {
                placed.elem().draw(graphics, x, y + placed.y(), mouseX, mouseY);
            }
            String number = Integer.toString(index + 1);
            graphics.text(font, number, x + (TEXT_W - font.width(number)) / 2, top + PAGE_H - 22, 0xFF808080, false);
        }
        if (page == null) {
            graphics.text(font, Component.translatable("gui.buildcraft.guide.lore"), left + PAGE_W + RIGHT_TEXT_X + 18, top + 12, TEXT_COLOUR,
                false);
            icon(graphics, left + PAGE_W + RIGHT_TEXT_X, top + 8, lore ? 48 : 0, 164, 16, 16);
        } else {
            boolean hovered = isOver(mouseX, mouseY, left + LEFT_TEXT_X, top + 10, 17, 9);
            icon(graphics, left + LEFT_TEXT_X, top + 10, 48, hovered ? 152 : 139, 17, 9);
        }
        if (spread > 0) {
            boolean hovered = isOver(mouseX, mouseY, left + LEFT_TEXT_X, top + PAGE_H - 24, 18, 10);
            icon(graphics, left + LEFT_TEXT_X, top + PAGE_H - 24, 23, hovered ? 152 : 139, 18, 10);
        }
        if (spread < maxSpread()) {
            int x = left + PAGE_W * 2 - LEFT_TEXT_X - 18;
            boolean hovered = isOver(mouseX, mouseY, x, top + PAGE_H - 24, 18, 10);
            icon(graphics, x, top + PAGE_H - 24, 0, hovered ? 152 : 139, 18, 10);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    private void icon(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int w, int h) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, u, v, w, h, 256, 256);
    }

    private static boolean isOver(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (buildcraft.lib.gui.GuiBC.containerButton(event) == 0) {
            if (page == null && isOver(mx, my, left + PAGE_W + RIGHT_TEXT_X, top + 8, 16, 16)) {
                lore = !lore;
                return true;
            }
            if (page != null && isOver(mx, my, left + LEFT_TEXT_X, top + 10, 17, 9)) {
                back();
                return true;
            }
            if (spread > 0 && isOver(mx, my, left + LEFT_TEXT_X, top + PAGE_H - 24, 18, 10)) {
                spread--;
                return true;
            }
            if (spread < maxSpread() && isOver(mx, my, left + PAGE_W * 2 - LEFT_TEXT_X - 18, top + PAGE_H - 24, 18, 10)) {
                spread++;
                return true;
            }
            for (int side = 0; side < 2; side++) {
                int index = spread * 2 + side;
                if (index >= pages.size()) continue;
                int x = pageX(side), y = top + TEXT_Y;
                for (Placed placed : pages.get(index)) {
                    int ex = (int) mx - x, ey = (int) my - y - placed.y();
                    if (ex >= 0 && ex < TEXT_W && ey >= 0 && ey < placed.elem().height()) {
                        Runnable action = placed.elem().click(ex, ey);
                        if (action != null) {
                            action.run();
                            return true;
                        }
                    }
                }
            }
        } else if (buildcraft.lib.gui.GuiBC.containerButton(event) == 1 && page != null) {
            back();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY > 0 && spread > 0) {
            spread--;
        } else if (scrollY < 0 && spread < maxSpread()) {
            spread++;
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
