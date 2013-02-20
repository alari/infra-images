package infra.images

import groovy.transform.CompileStatic
import infra.file.storage.FileStorageService
import infra.file.storage.FilesHolder
import infra.file.storage.FilesManager
import infra.images.annotations.Format
import infra.images.annotations.Image
import infra.images.annotations.ImageHolder
import infra.images.domain.DomainImageManager
import infra.images.domain.ImageDomainRepoProvider
import infra.images.format.AnnotationFormat
import infra.images.format.BasesFormat
import infra.images.format.ImageFormat
import infra.images.formatter.ImageFormatter
import infra.images.util.ImageFormatsBundle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @author alari
 * @since 12/23/12 4:06 PM
 */
@Component
@CompileStatic
class AnnotatedImageManagerProvider {
    private volatile Map<Class, Provider> providers = [:]

    @Autowired
    FileStorageService fileStorageService
    @Autowired
    ImageFormatter imageFormatter
    @Autowired
    ImageDomainRepoProvider imageDomainRepoProvider

    Provider getProvider(Class aClass) {
        if (!providers.containsKey(aClass)) {
            providers.put(aClass, new Provider(aClass))
        }
        providers.get(aClass)
    }

    ImageManager getManager(def domain) {
        getProvider(domain.class).getManager(domain)
    }

    void clear() {
        providers = [:]
    }

    private class Provider {
        private final ImageFormatsBundle imageBundle
        private final Class domainClass
        private final FilesHolder filesHolder

        private boolean storeDomains
        private String versionProperty

        Provider(Class aClass) {
            domainClass = aClass
            ImageHolder holder = domainClass.getAnnotation(ImageHolder)
            storeDomains = holder.enableImageDomains()
            versionProperty = holder.versionProperty() ?: null

            filesHolder = holder.filesHolder()

            Image imageAnnotation = holder.image()

            BasesFormat basesFormat = new BasesFormat(imageAnnotation.baseFormat())
            Map<String, ImageFormat> formats = [:]
            for (Format format : imageAnnotation.formats()) {
                formats.put(format.name(), new AnnotationFormat(format, basesFormat))
            }

            imageBundle = new ImageFormatsBundle(imageAnnotation.name(), formats, basesFormat)
        }

        // TODO: add cache
        ImageManager getManager(def domain) {
            ImageManager m = new BasicImageManager(getFilesManager(domain), imageBundle, imageFormatter)
            if (storeDomains) {
                m = new DomainImageManager(m, imageDomainRepoProvider)
            }
            if(versionProperty) {
                m = new VersionedImageManager(m, domain, versionProperty)
            }
            m
        }

        private FilesManager getFilesManager(def domain) {
            fileStorageService.getManager(domain, filesHolder as FilesHolder)
        }
    }
}
