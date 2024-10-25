@file:Suppress("WildcardImport")
package es.unizar.urlshortener

import com.google.zxing.qrcode.QRCodeWriter
import es.unizar.urlshortener.core.BaseUrlProvider
import es.unizar.urlshortener.core.BaseUrlProviderImpl
import es.unizar.urlshortener.core.GeoLocationService
import es.unizar.urlshortener.core.usecases.*
import es.unizar.urlshortener.infrastructure.delivery.HashServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.ValidatorServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ClickEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ClickRepositoryServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlRepositoryServiceImpl
import es.unizar.urlshortener.thirdparties.ipinfo.GeoLocationServiceImpl
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import ua_parser.Parser

/**
 * Wires use cases with service implementations, and services implementations with repositories.
 *
 * **Note**: Spring Boot is able to discover this [Configuration] without further configuration.
 */
@Suppress("TooManyFunctions")
@Configuration
class ApplicationConfiguration(
    @Autowired val shortUrlEntityRepository: ShortUrlEntityRepository,
    @Autowired val clickEntityRepository: ClickEntityRepository
) {
    /**
     * Provides an implementation of the ClickRepositoryService.
     * @return an instance of ClickRepositoryServiceImpl.
     */
    @Bean
    fun clickRepositoryService() = ClickRepositoryServiceImpl(clickEntityRepository)

    /**
     * Provides an implementation of the ShortUrlRepositoryService.
     * @return an instance of ShortUrlRepositoryServiceImpl.
     */
    @Bean
    fun shortUrlRepositoryService() = ShortUrlRepositoryServiceImpl(shortUrlEntityRepository)


    /**
     * Provides an implementation of the ValidatorService.
     * @return an instance of ValidatorServiceImpl.
     */
    @Bean
    fun validatorService() = ValidatorServiceImpl()

    /**
     * Provides an implementation of the HashService.
     * @return an instance of HashServiceImpl.
     */
    @Bean
    fun hashService() = HashServiceImpl()

    /**
     * Provides an implementation of the RedirectUseCase.
     * @return an instance of RedirectUseCaseImpl.
     */
    @Bean
    fun redirectUseCase() = RedirectUseCaseImpl(shortUrlRepositoryService())

    /**
     * Provides an implementation of the LogClickUseCase.
     * @return an instance of LogClickUseCaseImpl.
     */
    @Bean
    fun logClickUseCase() = LogClickUseCaseImpl(clickRepositoryService())

    /**
     * Provides an implementation of the CreateShortUrlUseCase.
     * @return an instance of CreateShortUrlUseCaseImpl.
     */
    @Bean
    fun createShortUrlUseCase() =
        CreateShortUrlUseCaseImpl(shortUrlRepositoryService(), validatorService(), hashService())

    /**
     * Provides a QRCodeWriter.
     * @return an instance of QRCodeWriter.
     */
    @Bean
    fun qrCodeWriter(): QRCodeWriter = QRCodeWriter()

    /**
     * Provides an implementation of the CreateQRUseCase.
     * @return an instance of CreateQRUseCaseImpl.
     */
    @Bean
    fun createQRUseCase(qrCodeWriter: QRCodeWriter) = CreateQRUseCaseImpl(qrCodeWriter)

    /**
     * Provides an implementation of the ProcessCsvUseCase.
     * @return an instance of ProcessCsvUseCaseImpl.
     */
    @Bean
    fun processCsvUseCase(createShortUrlUseCase: CreateShortUrlUseCase,
                          baseUrlProvider: BaseUrlProvider): ProcessCsvUseCase {
        return ProcessCsvUseCaseImpl(createShortUrlUseCase, baseUrlProvider)
    }

    /**
     * Provides a RedirectionCountRepository.
     * @return an instance of InMemoryRedirectionCountRepository.
     */
    @Bean
    fun redirectionCountRepository(): RedirectionCountRepository {
        return InMemoryRedirectionCountRepository()
    }

    /**
     * Provides an implementation of the RedirectionLimitUseCase.
     * @return an instance of RedirectionLimitUseCaseImpl.
     */
    @Bean
    fun redirectionLimitUseCase(redirectionCountRepository: RedirectionCountRepository): RedirectionLimitUseCase {
        return RedirectionLimitUseCaseImpl(redirectionLimit = 10, redirectionCountRepository)
    }

    /**
     * Provides a WebClient.
     * @return an instance of WebClient.
     */
    @Bean
    fun webClient(): WebClient = WebClient.builder().build()

    /**
     * Provides a DotEnv.
     * @return an instance of DotEnv.
     */
    @Bean
    fun dotEnv(): Dotenv = Dotenv.load()

    /**
     * Provides an implementation of the GeoLocationService.
     * @return an instance of GeoLocationServiceImpl.
     */
    @Bean
    fun geoLocationService(webClient: WebClient, dotEnv: Dotenv): GeoLocationService {
        return GeoLocationServiceImpl(webClient, dotEnv)
    }

    /**
     * Provides a Parser.
     * @return an instance of Parser.
     */
    @Bean
    fun uaParser(): Parser = Parser()

    /**
     * Provides an implementation of the BrowserPlatformIdentificationUseCase.
     * @return an instance of BrowserPlatformIdentificationUseCaseImpl.
     */
    @Bean
    fun browserPlatformIdentificationUseCase(uaParser: Parser): BrowserPlatformIdentificationUseCase =
        BrowserPlatformIdentificationUseCaseImpl(uaParser)

    /**
     * Provides an implementation of the UrlAccessibilityCheckUseCase.
     * @return an instance of UrlAccessibilityCheckUseCaseImpl.
     */
    @Bean
    fun urlAccesibilityCheckUseCase(webClient: WebClient): UrlAccessibilityCheckUseCase =
        UrlAccessibilityCheckUseCaseImpl(webClient)

    @Bean
    fun baseUrlProvider(): BaseUrlProvider = BaseUrlProviderImpl()
}
