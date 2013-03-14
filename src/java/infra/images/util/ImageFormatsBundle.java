package infra.images.util;

import infra.images.format.ImageFormat;
import infra.images.format.OriginalFormat;

import java.util.Map;

/**
 * @author alari
 * @since 12/21/12 9:37 PM
 */
public class ImageFormatsBundle {
    private final Map<String, ImageFormat> formats;
    private final ImageFormat baseFormat;
    private final String name;
    private final ImageFormat original;
    private Integer version;

    public ImageFormatsBundle(String name, Map<String, ImageFormat> formats, ImageFormat baseFormat) {
        this.name = name;
        this.baseFormat = baseFormat;
        this.original = new OriginalFormat(this.name, this.baseFormat);
        formats.put(name, this.original);
        this.formats = formats;
    }

    public Map<String, ImageFormat> getFormats() {
        return formats;
    }

    public ImageFormat getBaseFormat() {
        return baseFormat;
    }

    public String getName() {
        return name;
    }

    public ImageFormat getOriginal() {
        return original;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getFormatFilename(ImageFormat format) {
        return (version != null && version > 0 ? format.getVersionedFilename(version) : format.getFilename());
    }
}
