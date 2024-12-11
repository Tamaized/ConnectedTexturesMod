package team.chisel.ctm.client.newctm.json;

import com.mojang.serialization.MapCodec;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import team.chisel.ctm.api.texture.ISubmap;

public class SubmapCodecs {
    
    public static final Codec<MultiSubmap.Grid> GRID = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("width").forGetter(MultiSubmap.Grid::getWidth), 
            Codec.INT.fieldOf("height").forGetter(MultiSubmap.Grid::getHeight))
            .apply(i, MultiSubmap.Grid::new));
    
    public static final Codec<MultiSubmap.Single> UNIT = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("width").forGetter(ISubmap::getWidth),
            Codec.FLOAT.fieldOf("height").forGetter(ISubmap::getHeight),
            Codec.FLOAT.fieldOf("x_offset").forGetter(ISubmap::getXOffset),
            Codec.FLOAT.fieldOf("y_offset").forGetter(ISubmap::getYOffset))
            .apply(i, MultiSubmap.Single::new));
    
    public static final Codec<MultiSubmap.Single> PIXEL = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("width").forGetter(ISubmap::getWidth),
            Codec.FLOAT.fieldOf("height").forGetter(ISubmap::getHeight),
            Codec.FLOAT.fieldOf("x_offset").forGetter(ISubmap::getXOffset),
            Codec.FLOAT.fieldOf("y_offset").forGetter(ISubmap::getYOffset))
            .apply(i, MultiSubmap.Single::new));
    
    public static final Map<String, Codec<? extends MultiSubmap>> TYPES = Map.of(
            "grid", GRID,
            "unit", UNIT,
            "pixel", PIXEL
        );
    
    public static final Codec<MultiSubmap> CODEC = Codec.STRING.dispatch($ -> "unit", TYPES::get);
}
