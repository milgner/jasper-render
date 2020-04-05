package net.illunis

import net.sf.jasperreports.engine.JRDataSource
import net.sf.jasperreports.engine.JRException
import net.sf.jasperreports.engine.JRField
import net.sf.jasperreports.engine.JRRewindableDataSource
import org.json.simple.JSONArray
import org.json.simple.JSONObject


/**
 * An advanced version of the simple JsonDataSource from JasperReports.
 * This version correctly converts values into the types appropriate for their field.
 *
 * @see net.sf.jasperreports.engine.data.JsonDataSource
 */
class JsonItemDataSource : JRDataSource, JRRewindableDataSource {
    private var items: JSONArray

    @get:Synchronized
    var currentIndex = -1
        private set

    constructor(_items: JSONArray) {
        items = _items
    }

    constructor(_ref: JsonItemDataSource) {
        items = _ref.items
    }

    @Synchronized
    @Throws(JRException::class)
    override fun moveFirst() {
        currentIndex = 0
    }

    @Synchronized
    @Throws(JRException::class)
    override fun next(): Boolean {
        return ++currentIndex < items.size
    }

    @Throws(JRException::class)
    override fun getFieldValue(jrField: JRField): Any? {
        val jsonObject = items[currentIndex] as JSONObject
        return JsonUtils.extractValue(jsonObject, jrField.name, jrField.valueClass)
    }
}
