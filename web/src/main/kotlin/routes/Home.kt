package routes

import commonClient.logger.ClientLogger
import commonClient.presentation.HomeScreenViewModel
import components.*
import extensions.extract
import kotlinx.css.*
import react.Props
import react.useLayoutEffect
import styled.css
import styled.styledDiv
import utils.vfc

val home = vfc<Props, HomeScreenViewModel> { _, vm ->
    val (state, effect, event) = vm.extract()

    useLayoutEffect {
        ClientLogger.d("Home.kt, useLayoutEffect")
    }

    LayoutContainer {
        css {
            overflowY = Overflow.scroll
        }
        InnerContainer {
            styledDiv {
                css {
                    display = Display.flex
                    flexDirection = FlexDirection.column
                    alignItems = Align.center
                }
                Space(200.px)
                MainTitle("지금 진행중인\n밸런스 게임")
                Space(20.px)
                SubTitle("다양한 사람들과 소통해보세요.")
                Space(2000.px)
            }
        }
    }
}