package jp.daisen_solution.printsampleapp.print

import java.util.concurrent.atomic.AtomicBoolean

class ConnectionData {

    var issueMode = 0
    var portSetting: String? = null
    var isOpen = AtomicBoolean(false)

}