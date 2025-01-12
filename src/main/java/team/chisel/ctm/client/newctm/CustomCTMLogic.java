package team.chisel.ctm.client.newctm;

import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;

import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.client.newctm.CTMLogicBakery.OutputFace;

@RequiredArgsConstructor
public class CustomCTMLogic implements ICTMLogic {
    
    @VisibleForTesting
    public final int[][] lookups;
    private final OutputFace[] tiles;
    private final LocalDirection[] directions;
    private final ConnectionCheck connectionCheck = new ConnectionCheck();
    
    private class Cache implements ILogicCache {

        @Nullable
        private final ConnectionCheck connectionCheckOverride;
        private int[] cachedSubmapIds;
        private OutputFace[] cachedSubmaps;

        public Cache(@Nullable ConnectionCheck connectionCheck) {
            this.connectionCheckOverride = connectionCheck;
        }

        @Override
        public OutputFace[] getCachedSubmaps() {
            return this.cachedSubmaps;
        }

        @Override
        public long serialized() {
            int stride = directions.length;
            int len = cachedSubmapIds.length;
            if (len * stride > 64) {
                throw new IllegalStateException("Too many submaps to serialize");
            }
            long ret = 0L;
            for (int i = 0; i < cachedSubmapIds.length; i++) {
                ret |= ((long) cachedSubmapIds[i]) << (i * stride);
            }
            return ret;
        }

        @Override
        public void buildConnectionMap(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction side) {
            this.cachedSubmapIds = CustomCTMLogic.this.getSubmapIds(world, pos, state, side, connectionCheckOverride);
            //Manually call with the computed submap ids to avoid having to calculate them a second type
            // like getSubmaps(BlockAndTintGetter, BlockPos, Direction) needs to do, and allows us to use
            // data that is based on our connection check override
            this.cachedSubmaps = CustomCTMLogic.this.getSubmaps(this.cachedSubmapIds);
        }
    }

    @Override
    public int[] getSubmapIds(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction side) {
        return getSubmapIds(world, pos, state, side, connectionCheck);
    }

    private int[] getSubmapIds(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction side, ConnectionCheck connectionCheck) {
        int key = 0;
        for (int i = 0; i < directions.length; i++) {
            boolean isConnected = directions[i].isConnected(connectionCheck, world, pos, state, side);
            key |= (isConnected ? 1 : 0) << i;
        }
        if (key >= lookups.length || lookups[key] == null) {
            throw new IllegalStateException("Input state found that is not in lookup table: " + Integer.toBinaryString(key));
        }
        return lookups[key];
    }

    @Override
    public OutputFace[] getSubmaps(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction side) {
        var tileIds = getSubmapIds(world, pos, state, side);
        return getSubmaps(tileIds);
    }

    private OutputFace[] getSubmaps(int[] tileIds) {
        OutputFace[] ret = new OutputFace[tileIds.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = tiles[tileIds[i]];
        }
        return ret;
    }
    
    @Override
    public ILogicCache cached(@Nullable ConnectionCheck connectionCheck) {
        return this.new Cache(connectionCheck);
    }
    
    private List<ISubmap> outputSubmapCache;
    @Override
    public List<ISubmap> outputSubmaps() {
        if (outputSubmapCache == null) {
            Set<ISubmap> seen = new HashSet<>();
            for (var tile : tiles) {
                seen.add(tile.face());
            }
            outputSubmapCache = List.copyOf(seen);
        }
        return outputSubmapCache;
    }

    @Override
    public ISubmap getFallbackUvs() {
        return tiles.length == 0 ? ICTMLogic.super.getFallbackUvs() : tiles[0].uvs();
    }

    private int textureCountCache = -1;
    @Override
    public int requiredTextures() {
        if (textureCountCache < 0) {
            BitSet seen = new BitSet();
            for (var tile : tiles) {
                seen.set(tile.tex());
            }
            textureCountCache = seen.cardinality();
        }
        return textureCountCache;
    }
}
