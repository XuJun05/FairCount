package gay.xujun05.fc.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ModInfo(String id, String sha256, boolean isNested) {
    public static final StreamCodec<ByteBuf, ModInfo> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ModInfo::id,
            ByteBufCodecs.STRING_UTF8, ModInfo::sha256,
            ByteBufCodecs.BOOL, ModInfo::isNested,
            ModInfo::new
    );
}
