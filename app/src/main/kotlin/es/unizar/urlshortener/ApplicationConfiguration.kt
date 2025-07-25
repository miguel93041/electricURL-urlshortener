@file:Suppress("WildcardImport", "MagicNumber", "LongParameterList")

package es.unizar.urlshortener

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.zxing.qrcode.QRCodeWriter
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.queues.*
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
import kotlinx.coroutines.Job
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.web.reactive.function.client.WebClient
import ua_parser.Parser
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.channels.Channel

/**
 * Wires use cases with service implementations, and services implementations with repositories.
 */
@Suppress("TooManyFunctions")
@Configuration
class ApplicationConfiguration(
    @Autowired val shortUrlEntityRepository: ShortUrlEntityRepository,
    @Autowired val clickEntityRepository: ClickEntityRepository,
    @Autowired val r2dbcEntityTemplate: R2dbcEntityTemplate
) {

    /**
     * Provides an implementation of the [ClickRepositoryService].
     * @return an instance of [ClickRepositoryServiceImpl].
     */
    @Bean
    fun clickRepositoryService() = ClickRepositoryServiceImpl(clickEntityRepository, r2dbcEntityTemplate, clickCache())

    /**
     * Provides an implementation of the [ShortUrlRepositoryService].
     * @return an instance of [ShortUrlRepositoryServiceImpl].
     */
    @Bean
    fun shortUrlRepositoryService() =
        ShortUrlRepositoryServiceImpl(shortUrlEntityRepository, r2dbcEntityTemplate, shortUrlCache())

    /**
     * Provides an implementation of the [UrlSafetyService].
     * @return an instance of [UrlValidatorServiceImpl].
     */
    @Bean
    fun validatorService(
        urlAccessibilityCheckUseCase: UrlAccessibilityCheckUseCase,
        urlSafetyService: UrlSafetyService
    ): UrlValidatorService =
        UrlValidatorServiceImpl(urlAccessibilityCheckUseCase, urlSafetyService)

    /**
     * Provides an implementation of the [HashService].
     * @return an instance of [HashServiceImpl].
     */
    @Bean
    fun hashService() = HashServiceImpl()

    /**
     * Provides an implementation of the [RedirectUseCase].
     * @return an instance of [RedirectUseCaseImpl].
     */
    @Bean
    fun redirectUseCase(
        shortUrlRepositoryService: ShortUrlRepositoryServiceImpl
    ): RedirectUseCase {
        return RedirectUseCaseImpl(shortUrlService = shortUrlRepositoryService)
    }

    /**
     * Provides an implementation of the [LogClickUseCase].
     * @return an instance of [LogClickUseCaseImpl].
     */
    @Bean
    fun logClickUseCase() = LogClickUseCaseImpl(clickRepositoryService())

    /**
     * Provides an implementation of the [CreateShortUrlUseCase].
     * @return an instance of [CreateShortUrlUseCaseImpl].
     */
    @Bean
    fun createShortUrlUseCase(
        shortUrlRepositoryService: ShortUrlRepositoryServiceImpl,
        hashService: HashServiceImpl
    ) = CreateShortUrlUseCaseImpl(shortUrlRepositoryService, hashService)

    /**
     * Provides a [QRCodeWriter].
     * @return an instance of [QRCodeWriter].
     */
    @Bean
    fun qrCodeWriter(): QRCodeWriter = QRCodeWriter()

    /**
     * Provides an implementation of the [CreateQRUseCase].
     * @return an instance of [CreateQRUseCaseImpl].
     */
    @Bean
    fun createQRUseCase(qrCodeWriter: QRCodeWriter) = CreateQRUseCaseImpl(qrCodeWriter)

    /**
     * Provides an implementation of the [ProcessCsvUseCase].
     * @return an instance of [ProcessCsvUseCaseImpl].
     */
    @Bean
    fun processCsvUseCase(generateShortUrlService: GenerateShortUrlService): ProcessCsvUseCase {
        return ProcessCsvUseCaseImpl(generateShortUrlService)
    }

    /**
     * Provides an implementation of the [RedirectionLimitUseCase].
     * @return an instance of [RedirectionLimitUseCaseImpl].
     */
    @Bean
    fun redirectionLimitUseCase(clickRepositoryService: ClickRepositoryService): RedirectionLimitUseCase {
        return RedirectionLimitUseCaseImpl(redirectionLimit = 10, timeFrameInSeconds = 60, clickRepositoryService)
    }

    /**
     * Provides a [WebClient].
     * @return an instance of [WebClient].
     */
    @Bean
    fun webClient(): WebClient = WebClient.builder().build()

    /**
     * Provides a [Dotenv].
     * @return an instance of [Dotenv].
     */
    @Bean
    fun dotEnv(): Dotenv {
        return Dotenv.configure()
            .ignoreIfMissing()
            .load()
    }

    /**
     * Provides an implementation of the [GeoLocationService].
     * @return an instance of [GeoLocationServiceImpl].
     */
    @Bean
    fun geoLocationService(
        webClient: WebClient,
        dotEnv: Dotenv,
        cache: AsyncCache<String, GeoLocation>
    ): GeoLocationService {
        return GeoLocationServiceImpl(webClient, dotEnv, cache)
    }

    /**
     * Provides an implementation of the [urlSafetyService].
     * @return an instance of [UrlSafetyServiceImpl].
     */
    @Bean
    fun urlSafetyService(
        webClient: WebClient,
        dotEnv: Dotenv,
        @Qualifier("urlSafeCache") cache: AsyncCache<String, Boolean>
    ): UrlSafetyService {
        return UrlSafetyServiceImpl(webClient, dotEnv, cache)
    }

    /**
     * Provides a [Parser].
     * @return an instance of [Parser].
     */
    @Bean
    fun uaParser(): Parser = Parser()

    /**
     * Provides an implementation of the [BrowserPlatformIdentificationUseCase].
     * @return an instance of [BrowserPlatformIdentificationUseCaseImpl].
     */
    @Bean
    fun browserPlatformIdentificationUseCase(uaParser: Parser): BrowserPlatformIdentificationUseCase =
        BrowserPlatformIdentificationUseCaseImpl(uaParser)

    /**
     * Provides an implementation of the [UrlAccessibilityCheckUseCase].
     * @return an instance of [UrlAccessibilityCheckUseCaseImpl].
     */
    @Bean
    fun urlAccesibilityCheckUseCase(
        webClient: WebClient,
        @Qualifier("urlReachableCache") cache: AsyncCache<String, Boolean>
    ): UrlAccessibilityCheckUseCase = UrlAccessibilityCheckUseCaseImpl(webClient, cache)

    /**
     * Provides an implementation of the [GetAnalyticsUseCase].
     * @return an instance of [GetAnalyticsUseCaseImpl].
     */
    @Bean
    fun getAnalyticsUseCase(
        clickRepository: ClickRepositoryService
    ): GetAnalyticsUseCase = GetAnalyticsUseCaseImpl(clickRepository)

    /**
     * Provides an implementation of the [BaseUrlProvider].
     * @return an instance of [BaseUrlProviderImpl].
     */
    @Bean
    fun baseUrlProvider(): BaseUrlProvider = BaseUrlProviderImpl()

    /**
     * Provides an implementation of the [GenerateShortUrlService].
     * @return an instance of [GenerateShortUrlServiceImpl].
     */
    @Bean
    fun generateShortUrlService(
        createShortUrlUseCase: CreateShortUrlUseCase,
        baseUrlProvider: BaseUrlProvider,
        geolocationChannelService: GeolocationChannelService,
        urlValidationChannelService: UrlValidationChannelService
    ): GenerateShortUrlService {
        return GenerateShortUrlServiceImpl(
            createShortUrlUseCase,
            baseUrlProvider,
            geolocationChannelService,
            urlValidationChannelService,
        )
    }

    /**
     * Provides an implementation of the [CsvService].
     * @return an instance of [CsvServiceImpl].
     */
    @Bean
    fun csvService(
        processCsvUseCase: ProcessCsvUseCase
    ): CsvService {
        return CsvServiceImpl(processCsvUseCase)
    }

    /**
     * Provides an implementation of the [HashValidatorService].
     * @return an instance of [HashValidatorServiceImpl].
     */
    @Bean
    fun hashValidatorService(
        shortUrlRepositoryService: ShortUrlRepositoryService
    ): HashValidatorService {
        return HashValidatorServiceImpl(shortUrlRepositoryService)
    }

    /**
     * Provides an implementation of the [AnalyticsService].
     * @return an instance of [AnalyticsServiceImpl].
     */
    @Bean
    fun analyticsService(
        hashValidatorService: HashValidatorService,
        analyticsUseCase: GetAnalyticsUseCase
    ): AnalyticsService {
        return AnalyticsServiceImpl(hashValidatorService, analyticsUseCase)
    }

    /**
     * Provides an implementation of the [QrService].
     * @return an instance of [QrServiceImpl].
     */
    @Bean
    fun qrService(
        hashValidatorService: HashValidatorService,
        qrUseCase: CreateQRUseCase,
        baseUrlProvider: BaseUrlProvider
    ): QrService {
        return QrServiceImpl(hashValidatorService, qrUseCase, baseUrlProvider)
    }

    /**
     * Provides an implementation of the [RedirectService].
     * @return an instance of [RedirectServiceImpl].
     */
    @Bean
    fun redirectService(
        hashValidatorService: HashValidatorService,
        redirectUseCase: RedirectUseCase,
        logClickUseCase: LogClickUseCase,
        browserPlatformIdentificationUseCase: BrowserPlatformIdentificationUseCase,
        redirectionLimitUseCase: RedirectionLimitUseCase,
        clickRepositoryService: ClickRepositoryService,
        geolocationChannelService: GeolocationChannelService
    ): RedirectService {
        return RedirectServiceImpl(
            hashValidatorService,
            redirectUseCase,
            logClickUseCase,
            browserPlatformIdentificationUseCase,
            redirectionLimitUseCase,
            clickRepositoryService,
            geolocationChannelService,
        )
    }

    /**
     * Provides an asynchronous cache for safe URLs.
     * @return an instance of [AsyncCache<String, Boolean>].
     */
    @Bean("urlSafeCache")
    fun urlSafeCache(): AsyncCache<String, Boolean> {
        return Caffeine.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .maximumSize(500)
            .buildAsync()
    }

    /**
     * Provides an asynchronous cache for geographic locations.
     * @return an instance of [AsyncCache<String, GeoLocation>].
     */
    @Bean
    fun geoLocationCache(): AsyncCache<String, GeoLocation> {
        return Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1000)
            .buildAsync()
    }

    /**
     * Provides an asynchronous cache for reachable URLs.
     * @return an instance of [AsyncCache<String, Boolean>].
     */
    @Bean("urlReachableCache")
    fun urlReachableCache(): AsyncCache<String, Boolean> {
        return Caffeine.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .maximumSize(500)
            .buildAsync()
    }

    /**
     * Provides an asynchronous cache for click data.
     * @return an instance of [AsyncCache<String, List<Click>>].
     */
    @Bean
    fun clickCache(): AsyncCache<String, List<Click>> {
        return Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(1000)
            .buildAsync()
    }

    /**
     * Provides an asynchronous cache for short URLs.
     * @return an instance of [AsyncCache<String, ShortUrl>].
     */
    @Bean
    fun shortUrlCache(): AsyncCache<String, ShortUrl> {
        return Caffeine.newBuilder()
            .expireAfterAccess(12, TimeUnit.HOURS)
            .maximumSize(1000)
            .buildAsync()
    }

    /**
     * Creates an asynchronous channel for URL validation.
     * @return A [Channel<UrlValidationEvent>] where URL validation events can be received.
     */
    @Bean
    fun urlValidationChannel() = Channel<UrlValidationEvent>()

    /**
     * Creates an asynchronous channel for geolocation.
     * @return A [Channel<GeoLocationEvent>] where geolocation events can be received.
     */
    @Bean
    fun geolocationChannel() = Channel<GeoLocationEvent>()

    /**
     * Initializes the channel service for geolocation events.
     * @param geolocationChannel The channel from which geolocation events will be received.
     * @return A [GeolocationChannelService] instance that handles the channelization of these events.
     */
    @Bean
    fun geolocationChannelService(geolocationChannel: Channel<GeoLocationEvent>) =
        GeolocationChannelService(geolocationChannel)

    /**
     * Initializes and starts processing geo-location events.
     * @param geolocationChannel The channel from which geo-location events will be received.
     * @return An instance of [GeolocationConsumerService] that handles the processing.
     */
    @Bean
    fun geolocationConsumerService(
        geoLocationService: GeoLocationService,
        clickRepositoryService: ClickRepositoryService,
        shortUrlRepositoryService: ShortUrlRepositoryService,
        geolocationChannel: Channel<GeoLocationEvent>
    ) =
        GeolocationConsumerService(geoLocationService, clickRepositoryService, shortUrlRepositoryService)
            .startProcessing(geolocationChannel)

    /**
     * Initializes the URL validation channel service.
     * @param urlValidationChannel The channel from which URL validation events will be received.
     * @return An instance of [UrlValidationChannelService] that handles the processing.
     */
    @Bean
    fun urlValidationChannelService(urlValidationChannel: Channel<UrlValidationEvent>) =
        UrlValidationChannelService(urlValidationChannel)

    /**
     * Initializes and starts processing URL validation events.
     * @return A [Job] that manages the processing of URL validation events.
     */
    @Bean
    fun urlValidationConsumerService(
        urlValidatorService: UrlValidatorService,
        shortUrlRepositoryService: ShortUrlRepositoryService,
        urlValidationChannel: Channel<UrlValidationEvent>
    ): Job =
        UrlValidationConsumerService(urlValidatorService, shortUrlRepositoryService)
            .startProcessing(urlValidationChannel)
}
