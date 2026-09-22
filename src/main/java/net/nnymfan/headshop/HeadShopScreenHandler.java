package net.nnymfan.headshop;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class HeadShopScreenHandler extends ScreenHandler {
    public static final ScreenHandlerType<HeadShopScreenHandler> TYPE = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(HeadShopMod.MOD_ID, "head_shop"),
            new ScreenHandlerType<>(HeadShopScreenHandler::new, null)
    );

    private final Inventory inventory;
    private final PlayerEntity player;

    public HeadShopScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(27));
    }

    public HeadShopScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(TYPE, syncId);
        this.inventory = inventory;
        this.player = playerInventory.player;
        inventory.onOpen(playerInventory.player);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override public boolean canInsert(ItemStack stack) { return false; }
                    @Override public boolean canTakeItems(PlayerEntity playerEntity) { return false; }
                });
            }
        }
        addPlayerInventory(playerInventory);
    }

    private void addPlayerInventory(PlayerInventory inv) {
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    public Inventory getInventory() { return inventory; }
    public PlayerEntity getPlayer() { return player; }

    @Override public boolean canUse(PlayerEntity player) { return true; }
    @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < 27 && actionType == SlotActionType.PICKUP) {
            HeadShopMod.handleMenuClick(player, inventory.getStack(slotIndex));
            return;
        }
        // The shop locks the whole inventory while open so currency cannot be moved around accidentally.
    }

    public void fill(ItemStack stack, int slot) { inventory.setStack(slot, stack); }
    public void clearMenu() { inventory.clear(); }
}
