package br.com.dio.app.repositories.data.di

import android.util.Log
import br.com.dio.app.repositories.data.model.Repo
import br.com.dio.app.repositories.data.repo.RepoInfo
import br.com.dio.app.repositories.data.repo.RepoInfoImpl
import br.com.dio.app.repositories.data.repodetails.RepoDetails
import br.com.dio.app.repositories.data.repodetails.RepoDetailsImpl
import br.com.dio.app.repositories.data.repositories.RepoRepository
import br.com.dio.app.repositories.data.repositories.RepoRepositoryImpl
import br.com.dio.app.repositories.data.services.GithubService
import br.com.dio.app.repositories.data.services.GithubServiceRaw
import br.com.dio.app.repositories.data.user.UserInfo
import br.com.dio.app.repositories.data.user.UserInfoImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * Essa object class é reponsável por instanciar e configurar os serviços web.
 * Ela tem duas responsabilidades: configurar os serviços web (OkHttp e Retrofit)
 * e fazer a injeçãoo das dependências por meio do Koin.
 * A filosofia adotada é agrupar os módulos por camadas.
 */

object DataModule {

    /**
     * Estou usando duas constantes para a URL: uma acessa a API geral, a
     * outra acessa o conteúdo bruto. Cada uma delas é vinculada a um
     * GithubService específico.
     */
    private const val OK_HTTP = "Ok Http"
    private const val BASE_URL = "https://api.github.com"
    private const val RAW_CONTENT_URL = "https://raw.githubusercontent.com"


    /**
     * Essa função fica exposta publicamente e é chamada na classe App.
     * Note o uso de concatenação para juntar várias listas de módulos em
     * uma única chamada. Isso evita duplicação de código na classe App.
     */
    fun load() {
        loadKoinModules(networkModule() + repositoriesModule()
                + userModule() + repoModule())
    }

    /**
     * Cria um módulo de rede com interceptação
     */
    private fun networkModule(): Module {
        return module {
            single {
                val interceptor = HttpLoggingInterceptor {
                    Log.e(OK_HTTP, it)
                }
                interceptor.level = HttpLoggingInterceptor.Level.BODY

                OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build()
            }

            /**
             * Usando a bibliteca Moshi para parsear o JSON. Também poderia
             * ser o Gson.
             */
            single {
                Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            }

            /**
             * Finalmente, cria um objeto Retrofit usando o OkHttpClient
             * e a factory de conversão Json. Esse é o service principal
             * que acessa a API.
             */
            single {
                createService<GithubService>(get(), get(), BASE_URL)
            }

            /**
             * E esse instancia o serviço que acessa o conteúdo bruto
             */
            single {
                createService<GithubServiceRaw>(get(), get(), RAW_CONTENT_URL)
            }

        }

    }

    /**
     * Esse método instancia uma RepoRepositoryImpl indicando sua dependência
     */
    private fun repositoriesModule() : Module {
        return module {
            single<RepoRepository> { RepoRepositoryImpl(get()) }
        }
    }

    /**
     * Esse método instancia o UserInforImpl; aqui pode ser usada uma factory
     * normal - não vejo necessidade de tê-lo como um singleton.
     */
    private fun userModule() : Module {
        return module {
            factory<UserInfo> { UserInfoImpl(get()) }
        }
    }


    /**
     * Esse método é responsável por instanciar um RepoInfoImpl; como no
     * caso do UserInfo, preferi usar uma factory porque faz sentido ter um singleton.
     */
    private fun repoModule() : Module {
        return module {
            factory<RepoInfo> { RepoInfoImpl(get()) }
            factory<RepoDetails> { RepoDetailsImpl(get()) }
        }
    }

    /**
     * Essa função instancia um objeto Retrofit a partir dos
     * parâmetros recebidos via construtor: o cliente OkHttp (por causa do interceptor)
     * e o conversor de Json.
     * São usados dois conversores diferentes. O ScalarConverter é invocado
     * quando se trata do acesso ao endpoint de raw content
     * enquanto o Moshi é utilizado na situação padrão.
     * O tipo de serviço (normal ou raw) é definido à partir da string
     * de URL passada como parâmetro do construtor.
     */
    private inline fun <reified T> createService(
        client: OkHttpClient,
        factory: Moshi,
        url: String
    ): T {
        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .apply {
                if (url == RAW_CONTENT_URL) addConverterFactory(ScalarsConverterFactory.create())
                else addConverterFactory(MoshiConverterFactory.create(factory))
            }
            .build()
            .create(T::class.java)

    }



}