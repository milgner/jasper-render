package net.illunis

import org.json.simple.JSONObject
import java.math.BigDecimal
import java.math.BigInteger

class JsonUtils {
    companion object {

        fun extractValue(jsonObject: JSONObject, key: String, clazz: Class<*>): Any? {
            val value = jsonObject[key] ?: return null
            if (value.javaClass == clazz) {
                return value
            }
            val converters = CONVERTERS.getOrDefault(value.javaClass, STRING_CONVERTERS)
            return (converters[clazz] ?: error("Unsupported type conversion")).invoke(value)
        }

        private val STRING_CONVERTERS = mapOf<Class<*>, (Any) -> Any>(
            java.math.BigDecimal::class.java to { value -> BigDecimal(value as String) },
            java.lang.Double::class.java to { value -> java.lang.Double.parseDouble(value as String) },
            java.lang.Float::class.java to { value -> java.lang.Float.parseFloat(value as String) },
            java.lang.Boolean::class.java to { value -> (value as String).toLowerCase().compareTo("true") == 0 },
            java.util.Date::class.java to { value -> java.time.format.DateTimeFormatter.ISO_DATE_TIME.parse(value as String) }
        )
        private val LONG_CONVERTERS = mapOf<Class<*>, (Any) -> Any>(
            java.lang.String::class.java to { value -> value.toString() },
            java.math.BigDecimal::class.java to { value -> BigDecimal(value as Long) },
            java.lang.Double::class.java to { value -> (value as Long).toDouble() },
            java.lang.Float::class.java to { value -> (value as Long).toFloat() }
        )

        private val BOOL_CONVERTERS = mapOf<Class<*>, (Any) -> Any>(
            java.lang.String::class.java to { value -> (value as String).trim().isNotEmpty() }
        )

        private val DOUBLE_CONVERTERS = mapOf<Class<*>, (Any) -> Any>(
            java.math.BigDecimal::class.java to { value -> BigDecimal(value as Double) },
            java.lang.String::class.java to { value -> value.toString() },
            java.lang.Float::class.java to { value -> (value as Double).toFloat() },
            java.math.BigInteger::class.java to { value -> java.math.BigInteger.valueOf((value as Double).toLong()) }
        )

        private val CONVERTERS = mapOf<Class<*>, Map<Class<*>, (Any) -> Any>>(
            JSONObject::class.java to emptyMap(),
            java.lang.String::class.java to STRING_CONVERTERS,
            java.lang.Long::class.java to LONG_CONVERTERS,
            java.lang.Boolean::class.java to BOOL_CONVERTERS,
            java.lang.Double::class.java to DOUBLE_CONVERTERS
        )

    }
}