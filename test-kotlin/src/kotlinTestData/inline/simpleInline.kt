package kotlinTestData.inline
private fun a() = print("hello")
private inline fun c() = a()
fun b() = c()