import commonClient.di.dataModule
import commonClient.di.domainModule
import commonClient.di.presentationModule
import di.initKoin
import di.jsKoinModule
import kotlinx.browser.document
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import react.StrictMode
import react.createContext
import react.dom.link
import react.dom.render
import react.router.dom.BrowserRouter
import styled.injectGlobal

object AppKoinComponent : KoinComponent {
    init {
        initKoin()
    }
}

val AppKoinComponentContext = createContext<AppKoinComponent>()

fun main() {
    val root = document.getElementById("root")
    root?.let {
        render(it) {
            link(href = "https://fonts.googleapis.com/icon?family=Material+Icons", "stylesheet") {}
            injectGlobal(globalStyle)
            AppKoinComponentContext.Provider(AppKoinComponent) {
                StrictMode {
                    BrowserRouter {
                        App()
                    }
                }
            }
        }
    }
}