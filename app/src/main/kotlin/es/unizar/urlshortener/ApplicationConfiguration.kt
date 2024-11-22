@file:Suppress("WildcardImport")
package es.unizar.urlshortener

import RedirectionLimitUseCase
import RedirectionLimitUseCaseImpl
import com.google.zxing.qrcode.QRCodeWriter
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
import es.unizar.urlshortener.infrastructure.delivery.HashServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.ValidatorServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ClickEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ClickRepositoryServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlRepositoryServiceImpl
import es.unizar.urlshortener.thirdparties.ipinfo.GeoLocationServiceImpl
import es.unizar.urlshortener.thirdparties.ipinfo.UrlSafetyServiceImpl
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
    fun validatorService(urlAccessibilityCheckUseCase: UrlAccessibilityCheckUseCase,
                         urlSafetyService: UrlSafetyService): ValidatorService =
        ValidatorServiceImpl(urlAccessibilityCheckUseCase, urlSafetyService)
    
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
    fun redirectUseCase(
        shortUrlRepositoryService: ShortUrlRepositoryServiceImpl,
        redirectionLimitUseCase: RedirectionLimitUseCase
    ): RedirectUseCase {
    return RedirectUseCaseImpl(
        shortUrlRepository = shortUrlRepositoryService,
        redirectionLimitUseCase = redirectionLimitUseCase
    )
}

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
    fun createShortUrlUseCase(
        shortUrlRepositoryService: ShortUrlRepositoryServiceImpl,
        validatorService: ValidatorService,
        hashService: HashServiceImpl
    ) = CreateShortUrlUseCaseImpl(shortUrlRepositoryService, validatorService, hashService)
    
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
    fun processCsvUseCase(generateEnhancedShortUrlUseCaseImpl: GenerateEnhancedShortUrlUseCaseImpl): ProcessCsvUseCase {
        return ProcessCsvUseCaseImpl(generateEnhancedShortUrlUseCaseImpl)
    }

    /**
     * Provides an implementation of the RedirectionLimitUseCase.
     * @return an instance of RedirectionLimitUseCaseImpl.
     */
    @Bean
    fun redirectionLimitUseCase(clickRepositoryService: ClickRepositoryService): RedirectionLimitUseCase {
        return RedirectionLimitUseCaseImpl(redirectionLimit = 10, timeFrameInSeconds = 60, clickRepositoryService)
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
    fun dotEnv(): Dotenv {
        return Dotenv.configure()
            .ignoreIfMissing()
            .load()
    }

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
    fun urlSafetyService(webClient: WebClient, dotEnv: Dotenv): UrlSafetyService {
        return UrlSafetyServiceImpl(webClient, dotEnv)
    }

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

    /**
     * Defines the [GetAnalyticsUseCase] bean.
     *
     * Creates an instance of [GetAnalyticsUseCaseImpl] with the required dependencies:
     * [ClickRepositoryService] for click data and [ShortUrlRepositoryService] for URL validation.
     */
    @Bean
    fun getAnalyticsUseCase(
        clickRepository: ClickRepositoryService,
        shortUrlRepository: ShortUrlRepositoryService
    ): GetAnalyticsUseCase =
        GetAnalyticsUseCaseImpl(clickRepository, shortUrlRepository);

    @Bean
    fun baseUrlProvider(): BaseUrlProvider = BaseUrlProviderImpl()

    @Bean
    fun generateEnhancedShortUrlUseCase(
        createShortUrlUseCase: CreateShortUrlUseCase,
        geoLocationService: GeoLocationService,
        baseUrlProvider: BaseUrlProvider
    ): GenerateEnhancedShortUrlUseCase {
        return GenerateEnhancedShortUrlUseCaseImpl(
            createShortUrlUseCase = createShortUrlUseCase,
            geoLocationService = geoLocationService,
            baseUrlProvider = baseUrlProvider
        )
    }
}
