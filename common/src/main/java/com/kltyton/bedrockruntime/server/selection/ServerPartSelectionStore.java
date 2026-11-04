package com.kltyton.bedrockruntime.server.selection;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.kltyton.bedrockruntime.api.bone.key.ObbBoneKey;
import com.kltyton.bedrockruntime.api.entity.BoneHitboxEntity;
import com.kltyton.bedrockruntime.api.state.runtime.ObbBoneState;
import com.kltyton.bedrockruntime.network.payload.selection.ObbPartSelectionPayload;
import com.kltyton.bedrockruntime.server.sync.snapshot.ServerObbStore;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * CN: 服务端保存客户端准星选择结果；原版攻击/交互发生时直接关联该结果，不重新射线计算。
 * EN: Stores client crosshair selections and correlates them directly when vanilla attack/interaction occurs, without server raycasts.
 */
public final class ServerPartSelectionStore {
    private static final long MAX_SELECTION_AGE_TICKS = 20L;
    private static final Map<UUID, Selection> SELECTIONS = new ConcurrentHashMap<>();

    private ServerPartSelectionStore() {
    }

    public static void update(ServerPlayer player, ObbPartSelectionPayload payload) {
        if (payload.isClear()) {
            SELECTIONS.remove(player.getUUID());
        } else if (com.kltyton.bedrockruntime.server.network.ObbReportValidation.canReport(player,
                ((net.minecraft.server.level.ServerLevel) player.level()).getEntity(payload.entityId()))) {
            SELECTIONS.put(player.getUUID(), new Selection(payload, player.level().getServer().getTickCount(), player.level()));
        }
    }

    public static Optional<ObbBoneState> selectedBone(ServerPlayer player, Entity target) {
        Selection selection = SELECTIONS.get(player.getUUID());
        if (selection == null
                || selection.payload.entityId() != target.getId()
                || selection.level != player.level() || target.level() != player.level()
                || player.level().getServer().getTickCount() - selection.gameTime > MAX_SELECTION_AGE_TICKS
                || !(target instanceof BoneHitboxEntity hitboxEntity)) {
            return Optional.empty();
        }
        ObbBoneKey key = new ObbBoneKey(
                selection.payload.source(),
                selection.payload.partName(),
                Math.max(0, selection.payload.cubeIndex()));
        return ServerObbStore.part(target, key)
                .map(ServerObbStore.ServerObbPart::bone)
                .or(() -> hitboxEntity.kltyton_bedrock_runtime$obbState().resolve(key));
    }

    public static void clear(ServerPlayer player) {
        SELECTIONS.remove(player.getUUID());
    }

    public static void clear() { SELECTIONS.clear(); }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        SELECTIONS.entrySet().removeIf(entry -> server.getPlayerList().getPlayer(entry.getKey()) == null
                || server.getTickCount() - entry.getValue().gameTime > MAX_SELECTION_AGE_TICKS);
    }

    private record Selection(ObbPartSelectionPayload payload, long gameTime, net.minecraft.world.level.Level level) {
    }
}
