package habanero

import habanero.app.Application
import io.jooby.runApp

/**
 * アプリケーション起動の始点
 *
 * @author Ryutaro Akiya
 */
fun main(args: Array<String>) = runApp(args, Application::class)

