package infra.images.util;

import java.util.EnumSet;

/**
 * @author Dmitry Kurinskiy
 * @since 21.10.11 14:58
 */
public enum ImageCropPolicy {
    DEFAULT(EnumSet.of(CropSide.DEFAULT)),
    NONE(EnumSet.noneOf(CropSide.class)),
    TOP_LEFT(EnumSet.of(CropSide.TOP, CropSide.LEFT)),
    TOP_CENTER(EnumSet.of(CropSide.TOP)),
    TOP_RIGHT(EnumSet.of(CropSide.TOP, CropSide.RIGHT)),
    CENTER_LEFT(EnumSet.of(CropSide.LEFT)),
    CENTER_RIGHT(EnumSet.of(CropSide.RIGHT)),
    CENTER(EnumSet.of(CropSide.DEFAULT)),
    BOTTOM_LEFT(EnumSet.of(CropSide.BOTTOM, CropSide.LEFT)),
    BOTTOM_RIGHT(EnumSet.of(CropSide.BOTTOM, CropSide.RIGHT)),
    BOTTOM_CENTER(EnumSet.of(CropSide.BOTTOM));

    private enum CropSide {
        TOP, RIGHT, BOTTOM, LEFT, DEFAULT // css order
    }

    private final EnumSet<CropSide> cropSides;

    ImageCropPolicy(EnumSet<CropSide> cropSides) {
        this.cropSides = cropSides;
    }

    public boolean isDefault() {
        return cropSides.contains(CropSide.DEFAULT);
    }

    public boolean isNoCrop() {
        return cropSides.isEmpty();
    }

    public boolean isAny() {
        return !cropSides.isEmpty();
    }

    public boolean isTop() {
        return cropSides.contains(CropSide.TOP);
    }

    public boolean isBottom() {
        return cropSides.contains(CropSide.BOTTOM);
    }

    public boolean isLeft() {
        return cropSides.contains(CropSide.LEFT);
    }

    public boolean isRight() {
        return cropSides.contains(CropSide.RIGHT);
    }
}