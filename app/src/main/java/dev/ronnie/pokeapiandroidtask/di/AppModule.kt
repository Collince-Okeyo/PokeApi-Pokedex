package dev.ronnie.pokeapiandroidtask.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ronnie.pokeapiandroidtask.BuildConfig
import dev.ronnie.pokeapiandroidtask.api.PokemonApi
import dev.ronnie.pokeapiandroidtask.utils.BASE_URL
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 *created by Ronnie Otieno on 20-Dec-20.
 **/

/**
 * Dagger Hilt module, used in dependency injection, provides various dependencies used in the app.
 */
@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePokemonApi(retrofit: Retrofit): PokemonApi = retrofit.create(PokemonApi::class.java)

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    @Provides
    @Singleton
    fun providesOkHttpClient(cache: Cache): OkHttpClient {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(cacheInterceptor)
            .cache(cache)

        if (BuildConfig.DEBUG) okHttpClient.addInterceptor(loggingInterceptor)

        return okHttpClient.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /*Caching data for 30 minutes, I can as well cache the data for a month or so because the pokeapi is static and rarely change but
    for practicality went for half an hour, this is basic cache,  could as use room data base */
    @Provides
    @Singleton
    fun provideCache(@ApplicationContext appContext: Context): Cache {

        return Cache(
            File(appContext.applicationContext.cacheDir, "pokemon_cache"),
            10 * 1024 * 1024
        )
    }

    private val cacheInterceptor = object : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val response: Response = chain.proceed(chain.request())
            val cacheControl = CacheControl.Builder()
                .maxAge(30, TimeUnit.MINUTES)
                .build()
            return response.newBuilder()
                .header("Cache-Control", cacheControl.toString())
                .build()
        }
    }
}