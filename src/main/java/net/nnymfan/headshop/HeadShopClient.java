package net.nnymfan.headshop;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class HeadShopClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(HeadShopScreenHandler.TYPE, HeadShopScreen::new);
    }

    public static class HeadShopScreen extends HandledScreen<HeadShopScreenHandler> {
        public HeadShopScreen(HeadShopScreenHandler handler, PlayerInventory inventory, Text title) {
            super(handler, inventory, title);
            this.backgroundWidth = 176;
            this.backgroundHeight = 166;
        }

        @Override
        protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
            int x = this.x, y = this.y;
            context.fill(x, y, x + backgroundWidth, y + 97, 0xFF303030);
            context.fill(x + 4, y + 4, x + backgroundWidth - 4, y + 93, 0xFF202020);
            context.fill(x, y + 97, x + backgroundWidth, y + backgroundHeight, 0xFF303030);
        }
    }
}
