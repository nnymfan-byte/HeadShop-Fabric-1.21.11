package net.nnymfan.headshop;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HeadShopState {
    private static final String FILE = "headshop_data.nbt";
    private final Map<UUID, PlayerData> players = new HashMap<>();

    public static HeadShopState load(MinecraftServer server) {
        HeadShopState state = new HeadShopState();
        Path path = server.getSavePath(WorldSavePath.ROOT).resolve(FILE);
        try {
            NbtCompound root = NbtIo.read(path);
            if (root != null && root.contains("Players")) {
                NbtCompound all = root.getCompound("Players").orElse(new NbtCompound());
                for (String key : all.getKeys()) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        NbtCompound n = all.getCompound(key).orElse(new NbtCompound());
                        state.players.put(uuid, PlayerData.fromNbt(n));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (IOException ignored) {}
        return state;
    }

    public void save(MinecraftServer server) {
        Path path = server.getSavePath(WorldSavePath.ROOT).resolve(FILE);
        try {
            Files.createDirectories(path.getParent());
            NbtCompound root = new NbtCompound();
            NbtCompound all = new NbtCompound();
            for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
                all.put(entry.getKey().toString(), entry.getValue().toNbt());
            }
            root.put("Players", all);
            NbtIo.write(root, path);
        } catch (IOException ignored) {}
    }

    public PlayerData get(UUID uuid) {
        return players.computeIfAbsent(uuid, k -> new PlayerData());
    }

    public void reset() {
        players.clear();
    }

    public static final class PlayerData {
        int heads = 0;
        int hearts = 10;
        int strength = 0;
        int haste = 0;
        int resistance = 0;
        int speed = 0;
        int jump = 0;
        boolean regeneration = false;
        boolean fireResistance = false;
        boolean nightVision = false;
        boolean waterBreathing = false;

        NbtCompound toNbt() {
            NbtCompound n = new NbtCompound();
            n.putInt("Heads", heads); n.putInt("Hearts", hearts);
            n.putInt("Strength", strength); n.putInt("Haste", haste);
            n.putInt("Resistance", resistance); n.putInt("Speed", speed);
            n.putInt("Jump", jump); n.putBoolean("Regeneration", regeneration);
            n.putBoolean("FireResistance", fireResistance); n.putBoolean("NightVision", nightVision);
            n.putBoolean("WaterBreathing", waterBreathing);
            return n;
        }

        static PlayerData fromNbt(NbtCompound n) {
            PlayerData d = new PlayerData();
            d.heads = Math.max(0, Math.min(15, n.getInt("Heads").orElse(0)));
            d.hearts = Math.max(10, Math.min(20, n.getInt("Hearts").orElse(10)));
            d.strength = Math.max(0, Math.min(3, n.getInt("Strength").orElse(0)));
            d.haste = Math.max(0, Math.min(3, n.getInt("Haste").orElse(0)));
            d.resistance = Math.max(0, Math.min(3, n.getInt("Resistance").orElse(0)));
            d.speed = Math.max(0, Math.min(3, n.getInt("Speed").orElse(0)));
            d.jump = Math.max(0, Math.min(3, n.getInt("Jump").orElse(0)));
            d.regeneration = n.getBoolean("Regeneration").orElse(false);
            d.fireResistance = n.getBoolean("FireResistance").orElse(false);
            d.nightVision = n.getBoolean("NightVision").orElse(false);
            d.waterBreathing = n.getBoolean("WaterBreathing").orElse(false);
            return d;
        }
    }
}
