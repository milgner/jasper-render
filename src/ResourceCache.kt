package net.illunis

import java.io.IOException
import java.io.InputStream

typealias LoaderFun<T> = (InputStream) -> T
class ResourceCache<T>(private val directory: String,
                       private val extension: String,
                       private val loader: LoaderFun<T>) {
    class ResourceNotFoundException(val resourceName: String) : IOException("Resource $resourceName not found")

    private val cache = HashMap<String, T>()

    fun load(resourceName: String): T {
        if (!cache.containsKey(resourceName)) {
            val filename = "/$directory/$resourceName.$extension"
            javaClass.getResourceAsStream(filename).use { stream ->
                if (stream == null) {
                    throw ResourceNotFoundException(resourceName)
                }
                cache[resourceName] = loader.invoke(stream)
            }
        }
        return cache[resourceName]!!
    }
}