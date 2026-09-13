package gay.xujun05.fc.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ResourcePackInfo(String id, String name, String sha256) {
    public static final StreamCodec<ByteBuf, ResourcePackInfo> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ResourcePackInfo::id,
            ByteBufCodecs.STRING_UTF8, ResourcePackInfo::name,
            ByteBufCodecs.STRING_UTF8, ResourcePackInfo::sha256,
            ResourcePackInfo::new
    );
}
