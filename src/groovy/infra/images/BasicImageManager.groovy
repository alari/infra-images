package infra.images

import groovy.transform.CompileStatic
import infra.file.storage.FilesManager
import infra.file.storage.LocalFileStorage
import infra.images.format.CustomFormat
import infra.images.format.ImageFormat
import infra.images.formatter.ImageFormatter
import infra.images.util.ImageBox
import infra.images.util.ImageFormatsBundle
import infra.images.util.ImageInfo
import infra.images.util.ImageSize
import org.apache.log4j.Logger
import org.springframework.web.multipart.MultipartFile

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * @author alari
 * @since 1/16/13 11:58 AM
 */
@CompileStatic
class BasicImageManager implements ImageManager {
    private final FilesManager filesManager
    private final ImageFormatsBundle imageBundle
    private final ImageFormatter imageFormatter
    private ImageBox originalImage
    private List<Closure> onStoreFileCallbacks = []
    private List<Closure> onBeforeDelete = []

    private static final Logger log = Logger.getLogger(BasicImageManager)

    BasicImageManager(FilesManager filesManager, ImageFormatsBundle imageBundle, ImageFormatter imageFormatter) {
        this.filesManager = filesManager
        this.imageBundle = imageBundle
        this.imageFormatter = imageFormatter
    }

    @Override
    ImageFormatsBundle getFormatsBundle() {
        imageBundle
    }

    @Override
    ImageInfo getInfo() {
        getInfo(imageBundle.original)
    }

    @Override
    ImageInfo getInfo(String formatName) {
        getInfo(getFormat(formatName))
    }

    @Override
    ImageInfo getInfo(ImageFormat format) {
        if (format instanceof CustomFormat) {
            format.baseFormat = imageBundle.baseFormat
        }
        new ImageInfo(format, getSize(format), getSrc(format))
    }

    @Override
    boolean isStored() {
        filesManager.exists(getFilename(imageBundle.original))
    }

    @Override
    FilesManager getFilesManager() {
        filesManager
    }

    @Override
    Map<String, ImageSize> store(File image) {
        if (isStored()) {
            delete()
        }
        originalImage = new ImageBox(image)
        Map<String, ImageSize> fileSizes = [:]

        // TODO: use GPars
        for (ImageFormat format in imageBundle.formats.values()) {
            ImageBox box = imageFormatter.format(format, originalImage)
            fileSizes.put(storeFile(box, format), box.size)
            box.file.delete()
        }

        fileSizes
    }

    @Override
    Map<String, ImageSize> store(MultipartFile image) {
        if (!image || !image.size) {
            return null
        }
        File imageFile = File.createTempFile("infra-image", "store")
        image.transferTo(imageFile)
        Map<String, ImageSize> storeData = store(imageFile)
        imageFile.delete()
        storeData
    }

    @Override
    void reformat(String formatName) {
        if (!isStored()) return;
        reformat(getFormat(formatName));
    }

    @Override
    void reformat(ImageFormat format) {
        if (!isStored()) return;
        if (format instanceof CustomFormat) {
            format.baseFormat = imageBundle.baseFormat
        }
        loadOriginal()
        if (originalImage.file?.exists()) {
            ImageBox box = imageFormatter.format(format, originalImage)
            storeFile(box, format)
            box.file.delete()
        }
    }

    @Override
    void reformat() {
        loadOriginal()
        if (originalImage.file?.exists()) store(originalImage.file)
    }

    @Override
    void removeFormat(String formatName) {
        if (!isStored()) return;
        removeFormat(getFormat(formatName))
    }

    @Override
    void removeFormat(ImageFormat format) {
        if (!isStored()) return;
        onBeforeDelete*.call(format)
        filesManager.delete(getFilename(format))
    }

    private String storeFile(ImageBox image, ImageFormat format) {
        String s = filesManager.store(image.file, getFilename(format))
        for (Closure c in onStoreFileCallbacks) {
            c.call(image, format)
        }
        s
    }

    private void loadOriginal() {
        if (!originalImage) originalImage = new ImageBox(filesManager.getFile(getFilename(imageBundle.original)))
    }

    private String getFilename(ImageFormat format) {
        imageBundle.getFormatFilename(format)
    }

    @Override
    String getSrc() {
        getExistentFormatSrc(imageBundle.original)
    }

    @Override
    String getSrc(String formatName) {
        getExistentFormatSrc(getFormat(formatName))
    }

    @Override
    String getSrc(ImageFormat format) {
        if (format instanceof CustomFormat) {
            format.setBaseFormat(imageBundle.baseFormat)
            if (!filesManager.exists(getFilename(format))) {
                loadOriginal()
                ImageBox box = imageFormatter.format(format, originalImage)
                storeFile(box, format)
                box.file.delete()
            }
        }
        filesManager.getUrl(getFilename(format))
    }

    private String getExistentFormatSrc(ImageFormat format) {
        filesManager.getUrl(getFilename(format))
    }

    @Override
    void delete() {
        onBeforeDelete*.call()
        filesManager.delete()
    }

    @Override
    ImageSize getSize() {
        getSize(imageBundle.original)
    }

    @Override
    ImageSize getSize(String formatName) {
        getSize(getFormat(formatName))
    }

    @Override
    ImageSize getSize(ImageFormat format) {
        if (filesManager.storage instanceof LocalFileStorage) {
            if (filesManager.exists(getFilename(format))) {
                final BufferedImage bimg = ImageIO.read(filesManager.getFile(getFilename(format)))
                if (bimg) {
                    return ImageSize.buildReal(bimg.width, bimg.height, format.density)
                } else return format.size
            }
            return format.size
        }
        getSizeBySrc(getSrc(format), format.density) ?: format.size
    }

    @Override
    void onStoreFile(Closure callback) {
        onStoreFileCallbacks.add(callback)
    }

    @Override
    void onBeforeDelete(Closure callback) {
        onBeforeDelete.add(callback)
    }

    protected ImageFormat getFormat(String formatName) {
        if (!imageBundle.formats.containsKey(formatName)) {
            throw new IllegalArgumentException("Format ${formatName} is not defined by name")
        }
        imageBundle.formats.get(formatName)
    }

    private static ImageSize getSizeBySrc(String src, float density) {
        try {
            URL url = new URL(src)
            final BufferedImage bimg = ImageIO.read(url);
            if (bimg) {
                return ImageSize.buildReal(bimg.width, bimg.height, density)
            }

        } catch (Exception e) {
            log.info "Image file not found: ${src}"
        }
        ImageSize.buildReal(0, 0)
    }
}
