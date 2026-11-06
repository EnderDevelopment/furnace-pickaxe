package com.enderdevelopment.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Main extends JavaPlugin implements Listener {
    private NamespacedKey toolKey;

    @Override
    public void onEnable() {
        toolKey = new NamespacedKey(this, "furnace_pickaxe");
        Bukkit.getPluginManager().registerEvents(this, this);

        ItemStack pickaxe = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta meta = pickaxe.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Furnace Pickaxe").color(NamedTextColor.GOLD));
            meta.lore(List.of(Component.text("Smelts mined drops automatically").color(NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(toolKey, PersistentDataType.BYTE, (byte) 1);
            Enchantment efficiency = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("efficiency"));
            if (efficiency != null) meta.addEnchant(efficiency, 2, true);
            pickaxe.setItemMeta(meta);
        }

        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "furnace_pickaxe_recipe"), pickaxe);
        recipe.shape("FFF", " S ", " S ");
        recipe.setIngredient('F', Material.FURNACE);
        recipe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(recipe);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType() == Material.AIR || !isFurnacePickaxe(tool)) return;

        Block block = event.getBlock();
        Collection<ItemStack> naturalDrops = block.getDrops(tool, player);
        if (naturalDrops.isEmpty()) return;

        List<ItemStack> smeltedDrops = new ArrayList<>();
        for (ItemStack drop : naturalDrops) {
            ItemStack result = smelt(drop);
            smeltedDrops.add(result == null ? drop.clone() : result);
        }

        event.setDropItems(false);
        for (ItemStack drop : smeltedDrops) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(drop);
            for (ItemStack leftover : leftovers.values()) {
                block.getWorld().dropItemNaturally(block.getLocation(), leftover);
            }
        }
    }

    private boolean isFurnacePickaxe(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(toolKey, PersistentDataType.BYTE);
    }

    private ItemStack smelt(ItemStack input) {
        if (input.getType() == Material.AIR) return null;
        var recipes = Bukkit.recipeIterator();
        while (recipes.hasNext()) {
            var recipe = recipes.next();
            if (recipe instanceof FurnaceRecipe furnaceRecipe
                    && furnaceRecipe.getInputChoice().test(input)) {
                ItemStack result = furnaceRecipe.getResult().clone();
                result.setAmount(result.getAmount() * input.getAmount());
                return result;
            }
        }
        return null;
    }
}
