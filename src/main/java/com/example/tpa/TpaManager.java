package com.example.tpa;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TpaManager {
    private record Request(UUID sender, UUID target, boolean here, int createdTick) {}

    /** target UUID -> (sender UUID -> request) */
    private static final Map<UUID, Map<UUID, Request>> PENDING = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();

    private TpaManager() {}

    private static ServerPlayerEntity find(MinecraftServer s, UUID id) {
        return s.getPlayerManager().getPlayer(id);
    }

    private static String name(ServerPlayerEntity p) { return p.getGameProfile().getName(); }

    private static Text msg(String s, Formatting f) { return Text.literal(s).formatted(f); }

    private static Text button(String label, Formatting color, String command) {
        return Text.literal(label).styled(st -> st.withColor(color).withBold(true)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    // ---------------------------------------------------------------- /tpa, /tpahere
    public static int start(ServerPlayerEntity sender, ServerPlayerEntity target, boolean here) {
        TpaConfig cfg = TpaConfig.get();
        MinecraftServer server = sender.getServer();
        if (server == null) return 0;

        if (sender.getUuid().equals(target.getUuid())) {
            sender.sendMessage(msg("You can't send a request to yourself.", Formatting.RED));
            return 0;
        }

        int now = server.getTicks();
        Integer last = LAST_SENT.get(sender.getUuid());
        if (last != null && now - last < cfg.cooldownSeconds * 20) {
            int left = (int) Math.ceil((cfg.cooldownSeconds * 20 - (now - last)) / 20.0);
            sender.sendMessage(msg("Please wait " + left + "s before sending another request.", Formatting.RED));
            return 0;
        }

        UUID senderId = sender.getUuid();
        UUID targetId = target.getUuid();
        Runnable send = () -> sendRequest(server, senderId, targetId, here);

        if (cfg.showConfirmBeforeSending) {
            String q = here
                ? "Are you sure you want to teleport " + name(target) + " to you?"
                : "Are you sure you want to teleport to " + name(target) + "?";
            Text title = Text.literal(here ? "Bring " + name(target) + "?" : "Teleport to " + name(target) + "?");
            ConfirmGui.open(sender, title, target.getGameProfile(), q, send, () ->
                sender.sendMessage(msg("Request cancelled.", Formatting.GRAY)));
        } else {
            send.run();
        }
        return 1;
    }

    private static void sendRequest(MinecraftServer server, UUID senderId, UUID targetId, boolean here) {
        ServerPlayerEntity sender = find(server, senderId);
        ServerPlayerEntity target = find(server, targetId);
        if (sender == null) return;
        if (target == null) {
            sender.sendMessage(msg("That player is no longer online.", Formatting.RED));
            return;
        }

        TpaConfig cfg = TpaConfig.get();
        int now = server.getTicks();
        PENDING.computeIfAbsent(targetId, k -> new HashMap<>())
            .put(senderId, new Request(senderId, targetId, here, now));
        LAST_SENT.put(senderId, now);

        sender.sendMessage(msg("Teleport request sent to " + name(target) + ". It expires in "
            + cfg.requestTimeoutSeconds + "s.", Formatting.GREEN));

        String sName = name(sender);
        String line = here ? sName + " wants you to teleport to them. " : sName + " wants to teleport to you. ";
        target.sendMessage(msg(line, Formatting.YELLOW)
            .copy().append(button("[Accept]", Formatting.GREEN, "/tpaccept " + sName))
            .append(Text.literal(" "))
            .append(button("[Deny]", Formatting.RED, "/tpdeny " + sName)));

        if (cfg.openGuiForTarget) {
            String q = here
                ? "Are you sure you want to teleport to " + sName + "?"
                : "Are you sure you want to let " + sName + " teleport to you?";
            Text title = Text.literal(here ? "Go to " + sName + "?" : "Accept " + sName + "?");
            ConfirmGui.open(target, title, sender.getGameProfile(), q,
                () -> accept(target, senderId), () -> deny(target, senderId));
        }
    }

    // ---------------------------------------------------------------- /tpaccept, /tpdeny
    private static Request pick(MinecraftServer server, UUID targetId, UUID senderFilter) {
        Map<UUID, Request> map = PENDING.get(targetId);
        if (map == null) return null;
        int now = server.getTicks();
        int timeout = TpaConfig.get().requestTimeoutSeconds * 20;
        map.values().removeIf(r -> now - r.createdTick() > timeout);
        if (senderFilter != null) return map.get(senderFilter);
        return map.values().stream().max(Comparator.comparingInt(Request::createdTick)).orElse(null);
    }

    public static int accept(ServerPlayerEntity target, UUID senderFilter) {
        MinecraftServer server = target.getServer();
        if (server == null) return 0;
        Request r = pick(server, target.getUuid(), senderFilter);
        if (r == null) {
            target.sendMessage(msg("You have no pending teleport request.", Formatting.RED));
            return 0;
        }
        PENDING.get(target.getUuid()).remove(r.sender());

        ServerPlayerEntity sender = find(server, r.sender());
        if (sender == null) {
            target.sendMessage(msg("That player is no longer online.", Formatting.RED));
            return 0;
        }

        ServerPlayerEntity mover = r.here() ? target : sender;
        ServerPlayerEntity dest = r.here() ? sender : target;

        if (!TpaConfig.get().allowCrossDimension && mover.getServerWorld() != dest.getServerWorld()) {
            target.sendMessage(msg("Cross-dimension teleport is disabled.", Formatting.RED));
            sender.sendMessage(msg("Cross-dimension teleport is disabled.", Formatting.RED));
            return 0;
        }

        mover.teleport(dest.getServerWorld(), dest.getX(), dest.getY(), dest.getZ(), dest.getYaw(), dest.getPitch());
        mover.sendMessage(msg("Teleported to " + name(dest) + ".", Formatting.GREEN));
        dest.sendMessage(msg(name(mover) + " teleported to you.", Formatting.GREEN));
        return 1;
    }

    public static int deny(ServerPlayerEntity target, UUID senderFilter) {
        MinecraftServer server = target.getServer();
        if (server == null) return 0;
        Request r = pick(server, target.getUuid(), senderFilter);
        if (r == null) {
            target.sendMessage(msg("You have no pending teleport request.", Formatting.RED));
            return 0;
        }
        PENDING.get(target.getUuid()).remove(r.sender());
        target.sendMessage(msg("Request denied.", Formatting.GRAY));
        ServerPlayerEntity sender = find(server, r.sender());
        if (sender != null) sender.sendMessage(msg(name(target) + " denied your teleport request.", Formatting.RED));
        return 1;
    }

    public static void clear(UUID player) {
        PENDING.remove(player);
        PENDING.values().forEach(m -> m.remove(player));
        LAST_SENT.remove(player);
    }
}
