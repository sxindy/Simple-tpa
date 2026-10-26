package com.example.tpa;

import com.mojang.authlib.GameProfile;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Set;

/**
 * หน้า UI ยืนยัน (ฝั่งเซิร์ฟเวอร์ล้วน ๆ ใช้ GUI หีบ 3 แถว)
 * ผู้เล่นไม่ต้องลงมอดฝั่งไคลเอนต์ และ Bedrock ผ่าน Geyser ก็เห็นได้
 *
 *  ช่อง 8  = ไอคอนเตือน (มุมขวาบน)
 *  ช่อง 13 = หัวผู้เล่น
 *  ช่อง 18-21 = Cancel (แดง)   ช่อง 23-26 = Confirm (เขียว)
 */
public final class ConfirmGui {
    private static final Set<Integer> CANCEL = Set.of(18, 19, 20, 21);
    private static final Set<Integer> CONFIRM = Set.of(23, 24, 25, 26);

    private ConfirmGui() {}

    private static Text plain(String s, Formatting f) {
        return Text.literal(s).styled(st -> st.withItalic(false).withColor(f));
    }

    private static ItemStack icon(Item item, Text name, List<Text> lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, name);
        if (lore != null && !lore.isEmpty()) s.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return s;
    }

    public static void open(ServerPlayerEntity player, Text title, GameProfile headOf,
                            String question, Runnable onConfirm, Runnable onCancel) {
        SimpleInventory inv = new SimpleInventory(27);

        ItemStack filler = icon(Items.GRAY_STAINED_GLASS_PANE, Text.literal(" "), null);
        for (int i = 0; i < 27; i++) inv.setStack(i, filler.copy());

        inv.setStack(8, icon(Items.YELLOW_CONCRETE, plain("\u26A0 Warning", Formatting.YELLOW),
            List.of(plain(question, Formatting.GRAY))));

        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponentTypes.PROFILE, new ProfileComponent(headOf));
        head.set(DataComponentTypes.CUSTOM_NAME, plain(headOf.getName(), Formatting.WHITE));
        head.set(DataComponentTypes.LORE, new LoreComponent(List.of(plain(question, Formatting.GRAY))));
        inv.setStack(13, head);

        ItemStack cancel = icon(Items.RED_CONCRETE, plain("Cancel", Formatting.RED), null);
        ItemStack confirm = icon(Items.LIME_CONCRETE, plain("Confirm", Formatting.GREEN), null);
        for (int s : CANCEL) inv.setStack(s, cancel.copy());
        for (int s : CONFIRM) inv.setStack(s, confirm.copy());

        player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new Handler(syncId, playerInv, inv, onConfirm, onCancel), title));
    }

    private static final class Handler extends GenericContainerScreenHandler {
        private final Runnable onConfirm;
        private final Runnable onCancel;
        private boolean used = false;

        Handler(int syncId, PlayerInventory playerInv, Inventory inv, Runnable onConfirm, Runnable onCancel) {
            super(ScreenHandlerType.GENERIC_9X3, syncId, playerInv, inv, 3);
            this.onConfirm = onConfirm;
            this.onCancel = onCancel;
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
            // ไม่ให้หยิบ/ย้ายไอเทมใด ๆ ทั้งสิ้น ตอบสนองเฉพาะปุ่มของเรา
            if (used || actionType != SlotActionType.PICKUP) return;
            if (CANCEL.contains(slotIndex)) {
                used = true;
                player.closeHandledScreen();
                onCancel.run();
            } else if (CONFIRM.contains(slotIndex)) {
                used = true;
                player.closeHandledScreen();
                onConfirm.run();
            }
        }

        @Override
        public net.minecraft.item.ItemStack quickMove(PlayerEntity player, int slot) {
            return net.minecraft.item.ItemStack.EMPTY;
        }
    }
}
