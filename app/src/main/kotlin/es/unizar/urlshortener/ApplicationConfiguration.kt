@file:Suppress("WildcardImport", "MagicNumber", "LongParameterList")
package es.unizar.urlshortener

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.zxing.qrcode.QRCodeWriter
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.services.*
import es.unizar.urlshortener.core.usecases.*
import es.unizar.urlshortener.infrastructure.delivery.HashServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.HashValidatorServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.UrlValidatorServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ClickEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ClickRepositoryServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlRepositoryServiceImpl
import es.unizar.urlshortener.thirdparties.ipinfo.GeoLocationServiceImpl
import es.unizar.urlshortener.thirdparties.ipinfo.UrlSafetyServiceImpl
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.web.reactive.function.client.WebClient
import ua_parser.Parser
import java.util.concurrent.TimeUnit

/**
 * Wires use cases with service implementations, and services implementations with repositories.
 *
 * **Note**: Spring Boot is able to discover this [Configuration] without further configuration.
 */
@Suppress("TooManyFunctions")
@Configuration
class ApplicationConfiguration(
    @Autowired val shortUrlEntityRepository: ShortUrlEntityRepository,
    @Autowired val clickEntityRepository: ClickEntityRepository,
    @Autowired val r2dbcEntityTemplate: R2dbcEntityTemplate
) {
    /**
     * Provides an implementation of the ClickRepositoryService.
     * @return an instance of ClickRepositoryServiceImpl.
     */
    @Bean
    fun clickRepositoryService() = ClickRepositoryServiceImpl(clickEntityRepository, r2dbcEntityTemplate, clickCache())

    /**
     * Provides an implementation of the ShortUrlRepositoryService.
     * @return an instance of ShortUrlRepositoryServiceImpl.
     */
    @Bean
    fun shortUrlRepositoryService() =
        ShortUrlRepositoryServiceImpl(shortUrlEntityRepository, r2dbcEntityTemplate, shortUrlCache())


    /**
     * Provides an implementation of the ValidatorService.
     * @return an instance of ValidatorServiceImpl.
     */
    @Bean
    fun validatorService(urlAccessibilityCheckUseCase: UrlAccessibilityCheckUseCase,
                         urlSafetyService: UrlSafetyService): UrlValidatorService =
        UrlValidatorServiceImpl(urlAccessibilityCheckUseCase, urlSafetyService)
    
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
        shortUrlRepositoryService: ShortUrlRepositoryServiceImpl
    ): RedirectUseCase {
        return RedirectUseCaseImpl(
            shortUrlService = shortUrlRepositoryService
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
        hashService: HashServiceImpl
    ) = CreateShortUrlUseCaseImpl(shortUrlRepositoryService, hashService)
    
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
    fun processCsvUseCase(generateShortUrlService: GenerateShortUrlService): ProcessCsvUseCase {
        return ProcessCsvUseCaseImpl(generateShortUrlService)
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
    fun geoLocationService(
        webClient: WebClient,
        dotEnv: Dotenv, cache: AsyncCache<String, GeoLocation>
    ): GeoLocationService {
        return GeoLocationServiceImpl(webClient, dotEnv, cache)
    }

    /**
     * Provides a Parser.
     * @return an instance of Parser.
     */
    @Bean
    fun urlSafetyService(
        webClient: WebClient,
        dotEnv: Dotenv, @Qualifier("urlSafeCache") cache: AsyncCache<String, Boolean>
    ): UrlSafetyService {
        return UrlSafetyServiceImpl(webClient, dotEnv, cache)
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
    fun urlAccesibilityCheckUseCase(
        webClient: WebClient,
        @Qualifier("urlReachableCache") cache: AsyncCache<String, Boolean>
    ): UrlAccessibilityCheckUseCase = UrlAccessibilityCheckUseCaseImpl(webClient, cache)

    /**
     * Defines the [GetAnalyticsUseCase] bean.
     *
     * Creates an instance of [GetAnalyticsUseCaseImpl] with the required dependencies:
     * [ClickRepositoryService] for click data and [ShortUrlRepositoryService] for URL validation.
     */
    @Bean
    fun getAnalyticsUseCase(
        clickRepository: ClickRepositoryService
    ): GetAnalyticsUseCase =
        GetAnalyticsUseCaseImpl(clickRepository);

    @Bean
    fun baseUrlProvider(): BaseUrlProvider = BaseUrlProviderImpl()

    @Bean
    fun generateShortUrlService(
        urlValidatorService: UrlValidatorService,
        createShortUrlUseCase: CreateShortUrlUseCase,
        geoLocationService: GeoLocationService,
        shortUrlRepositoryService: ShortUrlRepositoryService,
        baseUrlProvider: BaseUrlProvider
    ): GenerateShortUrlService {
        return GenerateShortUrlServiceImpl(
            urlValidatorService,
            createShortUrlUseCase,
            geoLocationService,
            shortUrlRepositoryService,
            baseUrlProvider
        )
    }

    @Bean
    fun csvService(
        processCsvUseCase: ProcessCsvUseCase
    ): CsvService {
        return CsvServiceImpl(processCsvUseCase)
    }

    @Bean
    fun hashValidatorService(
        shortUrlRepositoryService: ShortUrlRepositoryService
    ): HashValidatorService {
        return HashValidatorServiceImpl(shortUrlRepositoryService)
    }

    @Bean
    fun analyticsService(
        hashValidatorService: HashValidatorService,
        analyticsUseCase: GetAnalyticsUseCase
    ): AnalyticsService {
        return AnalyticsServiceImpl(hashValidatorService, analyticsUseCase)
    }

    @Bean
    fun qrService(
        hashValidatorService: HashValidatorService,
        qrUseCase: CreateQRUseCase,
        baseUrlProvider: BaseUrlProvider
    ): QrService {
        return QrServiceImpl(hashValidatorService, qrUseCase, baseUrlProvider)
    }

    @Bean
    fun redirectService(
        hashValidatorService: HashValidatorService,
        redirectUseCase: RedirectUseCase,
        logClickUseCase: LogClickUseCase,
        geoLocationService: GeoLocationService,
        browserPlatformIdentificationUseCase: BrowserPlatformIdentificationUseCase,
        redirectionLimitUseCase: RedirectionLimitUseCase,
        clickRepositoryService: ClickRepositoryService,
    ): RedirectService {
        return RedirectServiceImpl(
            hashValidatorService,
            redirectUseCase,
            logClickUseCase,
            geoLocationService,
            browserPlatformIdentificationUseCase,
            redirectionLimitUseCase,
            clickRepositoryService
        )
    }

    @Bean("urlSafeCache")
    fun urlSafeCache(): AsyncCache<String, Boolean> {
        return Caffeine.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .maximumSize(500)
            .buildAsync()
    }

    @Bean
    fun geoLocationCache(): AsyncCache<String, GeoLocation> {
        return Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1000)
            .buildAsync()
    }

    @Bean("urlReachableCache")
    fun urlReachableCache(): AsyncCache<String, Boolean> {
        return Caffeine.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .maximumSize(500)
            .buildAsync()
    }

    fun clickCache(): AsyncCache<String, List<Click>> {
        return Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(1000)
            .buildAsync()
    }

    fun shortUrlCache(): AsyncCache<String, ShortUrl> {
        return Caffeine.newBuilder()
            .expireAfterAccess(12, TimeUnit.HOURS)
            .maximumSize(1000)
            .buildAsync()
    }
}
