package jp.daisen_solution.printsampleapp.print

class PrintData {

    var objectDataList: Map<String?, String?>? = null
    var lfmFileFullPath = ""
    var printCount = 1
        set(printCount) {
            var printCount = printCount
            if (printCount < 1) {
                printCount = 1
            }
            field = printCount
        }

    var currentIssueMode = 1
    var statusMessage = ""
    var result: Long = 0

    init {
        objectDataList = HashMap()
    }

    protected fun finalize() {
        objectDataList = null
    }
}