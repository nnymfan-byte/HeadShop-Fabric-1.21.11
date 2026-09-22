package net.nnymfan.headshop;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.function.Function;

public class HeadShopMod implements ModInitializer {
    public static final String MOD_ID = "headshop";
    public static final Identifier HEART_MODIFIER = Identifier.of(MOD_ID, "extra_hearts");
    public static HeadShopState STATE;
    private static net.minecraft.server.MinecraftServer SERVER;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> { SERVER = server; STATE = HeadShopState.load(server); });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> { if (STATE != null) STATE.save(server); });
        ServerLifecycleEvents.AFTER_SAVE.register((server, suppressLogs, flush) -> { if (STATE != null) STATE.save(server); });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> applyUpgrades(handler.player));
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, victim, source) -> {
            if (killer instanceof ServerPlayerEntity player && victim instanceof ServerPlayerEntity) {
                HeadShopState.PlayerData data = STATE.get(player.getUuid());
                if (data.heads < 15) {
                    data.heads++;
                    player.getInventory().insertStack(createHead());
                    player.sendMessage(Text.literal("+1 Head  [" + data.heads + "/15]"), true);
                    STATE.save(SERVER);
                }
            }
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient()) return ActionResult.PASS;
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isOf(Items.PLAYER_HEAD) && isCurrencyHead(stack)) {
                openShop(player);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<net.minecraft.server.command.ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("headreset")
                .requires(source -> source.getPermissionLevel() >= 2)
                .executes(ctx -> {
                    STATE.reset();
                    for (ServerPlayerEntity p : ctx.getSource().getServer().getPlayerManager().getPlayerList()) {
                        applyUpgrades(p);
                    }
                    STATE.save(ctx.getSource().getServer());
                    ctx.getSource().sendFeedback(() -> Text.literal("Head Shop data has been reset."), true);
                    return 1;
                }));
    }

    public static ItemStack createHead() {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("Head Shop Currency"));
        return head;
    }

    public static boolean isCurrencyHead(ItemStack stack) {
        return stack.isOf(Items.PLAYER_HEAD) && stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME) != null
                && stack.getName().getString().equals("Head Shop Currency");
    }

    public static void openShop(PlayerEntity player) {
        if (player.getEntityWorld().isClient()) return;
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> {
                    HeadShopScreenHandler menu = new HeadShopScreenHandler(syncId, inv);
                    buildMainMenu(menu, p);
                    return menu;
                }, Text.literal("Head Shop")));
    }

    private static void buildMainMenu(HeadShopScreenHandler menu, PlayerEntity player) {
        menu.clearMenu();
        menu.fill(named(Items.DIAMOND_SWORD, "⚔ Combat"), 10);
        menu.fill(named(Items.SHIELD, "🛡 Defense / Hearts"), 13);
        menu.fill(named(Items.ENDER_PEARL, "✦ Mobility / Utility"), 16);
        menu.fill(named(Items.PLAYER_HEAD, "Heads: " + STATE.get(player.getUuid()).heads + "/15"), 22);
    }

    private static ItemStack named(net.minecraft.item.Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return s;
    }

    public static void handleMenuClick(PlayerEntity player, ItemStack clicked) {
        String name = clicked.getName().getString();
        if (name.contains("Combat")) showCategory((ServerPlayerEntity) player, "Combat", new String[]{"Strength", "Haste"}, new int[]{0, 1});
        else if (name.contains("Defense")) showCategory((ServerPlayerEntity) player, "Defense / Hearts", new String[]{"Hearts", "Resistance", "Regeneration", "Fire Resistance"}, new int[]{2, 3, 4, 5});
        else if (name.contains("Mobility")) showCategory((ServerPlayerEntity) player, "Mobility / Utility", new String[]{"Speed", "Jump Boost", "Night Vision", "Water Breathing"}, new int[]{6, 7, 8, 9});
        else if (name.startsWith("Buy:")) { int end = name.indexOf(" |", 4); if (end > 4) purchase((ServerPlayerEntity) player, Integer.parseInt(name.substring(4, end).trim())); }
        else if (name.equals("← Back")) openShop(player);
    }

    private static void showCategory(ServerPlayerEntity player, String title, String[] names, int[] ids) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) -> {
            HeadShopScreenHandler menu = new HeadShopScreenHandler(syncId, inv);
            menu.clearMenu();
            HeadShopState.PlayerData d = STATE.get(player.getUuid());
            for (int i = 0; i < names.length; i++) {
                int level = getLevel(d, ids[i]);
                int nextCost = ids[i] == 2 ? 1 : (level + 1);
                String state = level >= (ids[i] == 2 ? 20 : 3) ? "MAX" : "Buy: " + ids[i] + " | Lv " + (level + 1) + " | " + nextCost + " head(s)";
                menu.fill(named(iconFor(ids[i]), names[i] + " | " + state), 10 + i * 2);
            }
            menu.fill(named(Items.ARROW, "← Back"), 22);
            menu.fill(named(Items.PLAYER_HEAD, "Heads: " + d.heads + "/15"), 25);
            return menu;
        }, Text.literal(title)));
    }

    private static net.minecraft.item.Item iconFor(int id) {
        return switch (id) {
            case 0 -> Items.DIAMOND_SWORD; case 1 -> Items.GOLDEN_PICKAXE;
            case 2 -> Items.GOLDEN_APPLE; case 3 -> Items.SHIELD; case 4 -> Items.GLISTERING_MELON_SLICE;
            case 5 -> Items.MAGMA_CREAM; case 6 -> Items.SUGAR; case 7 -> Items.RABBIT_FOOT;
            case 8 -> Items.ENDER_EYE; default -> Items.GLASS_BOTTLE;
        };
    }

    private static int getLevel(HeadShopState.PlayerData d, int id) {
        return switch (id) { case 0 -> d.strength; case 1 -> d.haste; case 2 -> d.hearts; case 3 -> d.resistance; case 4 -> d.regeneration ? 1 : 0; case 5 -> d.fireResistance ? 1 : 0; case 6 -> d.speed; case 7 -> d.jump; case 8 -> d.nightVision ? 1 : 0; default -> d.waterBreathing ? 1 : 0; };
    }

    private static void purchase(ServerPlayerEntity player, int id) {
        HeadShopState.PlayerData d = STATE.get(player.getUuid());
        int level = getLevel(d, id);
        int max = id == 2 ? 20 : (id == 4 || id == 5 || id == 8 || id == 9 ? 1 : 3);
        if (level >= max) { player.sendMessage(Text.literal("Already maxed."), true); return; }
        int cost = id == 2 ? 1 : level + 1;
        if (d.heads < cost || countCurrencyHeads(player) < cost) { player.sendMessage(Text.literal("You need " + cost + " Head(s)."), true); return; }
        removeCurrencyHeads(player, cost); d.heads -= cost;
        switch (id) {
            case 0 -> d.strength++; case 1 -> d.haste++; case 2 -> d.hearts++;
            case 3 -> d.resistance++; case 4 -> d.regeneration = true; case 5 -> d.fireResistance = true;
            case 6 -> d.speed++; case 7 -> d.jump++; case 8 -> d.nightVision = true; case 9 -> d.waterBreathing = true;
        }
        applyUpgrades(player); STATE.save(SERVER); player.sendMessage(Text.literal("Upgrade purchased!"), true); player.closeHandledScreen();
    }

    private static int countCurrencyHeads(PlayerEntity p) { int n=0; for (int i=0;i<p.getInventory().size();i++) { ItemStack s=p.getInventory().getStack(i); if (isCurrencyHead(s)) n += s.getCount(); } return n; }
    private static void removeCurrencyHeads(PlayerEntity p, int amount) { for (int i=0;i<p.getInventory().size() && amount>0;i++) { ItemStack s=p.getInventory().getStack(i); if (isCurrencyHead(s)) { int take=Math.min(amount,s.getCount()); s.decrement(take); amount-=take; } } }

    public static void applyUpgrades(ServerPlayerEntity p) {
        if (STATE == null) return;
        HeadShopState.PlayerData d = STATE.get(p.getUuid());
        var attr = p.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (attr != null) attr.overwritePersistentModifier(new EntityAttributeModifier(HEART_MODIFIER, (d.hearts - 10) * 2.0, EntityAttributeModifier.Operation.ADD_VALUE));
        addEffect(p, StatusEffects.STRENGTH, d.strength);
        addEffect(p, StatusEffects.HASTE, d.haste);
        addEffect(p, StatusEffects.RESISTANCE, d.resistance);
        addEffect(p, StatusEffects.SPEED, d.speed);
        addEffect(p, StatusEffects.JUMP_BOOST, d.jump);
        if (d.regeneration) addEffect(p, StatusEffects.REGENERATION, 1);
        if (d.fireResistance) addEffect(p, StatusEffects.FIRE_RESISTANCE, 1);
        if (d.nightVision) addEffect(p, StatusEffects.NIGHT_VISION, 1);
        if (d.waterBreathing) addEffect(p, StatusEffects.WATER_BREATHING, 1);
    }

    private static void addEffect(ServerPlayerEntity p, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int level) {
        if (level > 0) p.addStatusEffect(new StatusEffectInstance(effect, 20 * 60 * 60, level - 1, false, false, true));
    }
}
