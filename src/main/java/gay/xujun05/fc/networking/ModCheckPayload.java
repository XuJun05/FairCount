package gay.xujun05.fc.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import gay.xujun05.fc.FairCount;
import java.util.List;

public record ModCheckPayload(int pureJarCount, int nestedCount, List<ModInfo> mods, List<ResourcePackInfo> resourcePacks) implements CustomPacketPayload {

    public static final Identifier MOD_CHECK_PAYLOAD_ID = Identifier.fromNamespaceAndPath(FairCount.MOD_ID, "check_packet");
    public static final CustomPacketPayload.Type<ModCheckPayload> TYPE = new CustomPacketPayload.Type<>(MOD_CHECK_PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ModCheckPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ModCheckPayload::pureJarCount,
            ByteBufCodecs.INT, ModCheckPayload::nestedCount,
            ModInfo.CODEC.apply(ByteBufCodecs.list()), ModCheckPayload::mods,
            ResourcePackInfo.CODEC.apply(ByteBufCodecs.list()), ModCheckPayload::resourcePacks,
            ModCheckPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}