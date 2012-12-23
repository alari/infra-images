package ru.mirari.infra.image

import ru.mirari.infra.FileStorageService
import ru.mirari.infra.image.annotations.BaseFormat
import ru.mirari.infra.image.annotations.Format
import ru.mirari.infra.image.annotations.Image
import ru.mirari.infra.image.annotations.ImagesHolder
import ru.mirari.infra.image.format.AnnotationFormat
import ru.mirari.infra.image.format.BasesFormat
import ru.mirari.infra.image.util.ImageBundle

/**
 * @author alari
 * @since 12/21/12 9:36 PM
 */
class AnnotatedImagesManagerProvider {
    private final BasesFormat basesFormat
    private Map<String, ImageBundle> images = [:]
    private FileStorageService fileStorageService

    AnnotatedImagesManagerProvider(Class annotatedClass, FileStorageService fileStorageService) {
        ImagesHolder holder = annotatedClass.getAnnotation(ImagesHolder)

        this.fileStorageService = fileStorageService

        BaseFormat base = holder.baseFormat()
        basesFormat = new BasesFormat(base)

        for (Image image : holder.images()) {
            Map<String, AnnotationFormat> formats = [:]
            BasesFormat imageBase = new BasesFormat(base, image.baseFormat())
            for (Format format : image.formats()) {
                formats.put(format.name(), new AnnotationFormat(format, imageBase))
            }
            images.put(image.name(), new ImageBundle(image.name(), formats, imageBase))
        }
    }

    ImagesManager getManager(def domain) {}
}