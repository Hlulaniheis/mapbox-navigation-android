package com.mapbox.navigation.core.telemetry.events

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.SerializedName
import com.mapbox.navigation.base.metrics.NavigationMetrics
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.ArrayList
import java.util.zip.GZIPInputStream
import org.apache.commons.io.IOUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SchemaTest {

    private val eventSchemas: List<EventSchema> = unpackSchemas()

    @Test
    @Throws(Exception::class)
    fun checkNavigationArriveEventSize() {
        val schema = grabEventSchema(NavigationMetrics.ARRIVE)
        val fields = grabSchemaPropertyFields(NavigationArriveEvent::class.java)
        assertEquals(schema.properties.size().toLong(), fields.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun checkNavigationArriveEventFields() {
        val schema = grabEventSchema(NavigationMetrics.ARRIVE)
        val fields = grabSchemaPropertyFields(NavigationArriveEvent::class.java)
        schemaContainsPropertyFields(schema.properties, fields)
    }

    @Test
    @Throws(Exception::class)
    fun checkNavigationCancelEventSize() {
        val schema = grabEventSchema(NavigationMetrics.CANCEL_SESSION)
        val fields = grabSchemaPropertyFields(NavigationCancelEvent::class.java)
        assertEquals(schema.properties.size().toLong(), fields.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun checkNavigationCancelEventFields() {
        val schema = grabEventSchema(NavigationMetrics.CANCEL_SESSION)
        val fields = grabSchemaPropertyFields(NavigationCancelEvent::class.java)
        schemaContainsPropertyFields(schema.properties, fields)
    }

    @Test
    @Throws(Exception::class)
    fun checkNavigationDepartEventSize() {
        val schema = grabEventSchema(NavigationMetrics.DEPART)
        val fields = grabSchemaPropertyFields(NavigationDepartEvent::class.java)
        assertEquals(schema.properties.size().toLong(), fields.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun checkNavigationDepartEventFields() {
        val schema = grabEventSchema(NavigationMetrics.DEPART)
        val fields = grabSchemaPropertyFields(NavigationDepartEvent::class.java)
        schemaContainsPropertyFields(schema.properties, fields)
    }

    @Test
    @Throws(Exception::class)
    fun checkNavigationFeedbackEventSize() {
        val schema = grabEventSchema(NavigationMetrics.FEEDBACK, version = "2.1")
        val fields = grabSchemaPropertyFields(NavigationFeedbackEvent::class.java)
        assertEquals(schema.properties.size().toLong(), fields.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun checkNavigationFeedbackEventFields() {
        val schema = grabEventSchema(NavigationMetrics.FEEDBACK, version = "2.1")
        val fields = grabSchemaPropertyFields(NavigationFeedbackEvent::class.java)
        schemaContainsPropertyFields(schema.properties, fields)
    }

    @Test
    @Throws(Exception::class)
    fun checkNavigationRerouteEventSize() {
        val schema = grabEventSchema(NavigationMetrics.REROUTE)
        val fields = grabSchemaPropertyFields(NavigationRerouteEvent::class.java)
        assertEquals(schema.properties.size().toLong(), fields.size.toLong())
    }

    @Test
    fun checkNavigationRerouteEventFields() {
        val schema = grabEventSchema(NavigationMetrics.REROUTE)
        val fields = grabSchemaPropertyFields(NavigationRerouteEvent::class.java)
        schemaContainsPropertyFields(schema.properties, fields)
    }

    private fun schemaContainsPropertyFields(
        properties: JsonObject,
        fields: List<Field>
    ) {
        var distanceRemainingCount = 0
        var durationRemainingCount = 0
        for (i in fields.indices) {
            val thisField = fields[i].toString()
            val fieldArray = thisField.split(" ".toRegex()).toTypedArray()
            val typeArray = fieldArray[fieldArray.size - 2].split("\\.".toRegex()).toTypedArray()
            val type = typeArray[typeArray.size - 1]
            val nameArray = fieldArray[fieldArray.size - 1].split("\\.".toRegex()).toTypedArray()
            var field = nameArray[nameArray.size - 1]
            val serializedName = fields[i].getAnnotation(SerializedName::class.java)
            if (serializedName != null) {
                field = serializedName.value
            }
            if (field.equals("durationRemaining", ignoreCase = true)) {
                durationRemainingCount++
                if (durationRemainingCount > 1) {
                    field = "step$field"
                }
            }
            if (field.equals("distanceRemaining", ignoreCase = true)) {
                distanceRemainingCount++
                if (distanceRemainingCount > 1) {
                    field = "step$field"
                }
            }
            val thisSchema = findSchema(properties, field)
            assertNotNull(field, thisSchema)
            if (thisSchema.has("type")) {
                typesMatch(thisSchema, type)
            }
        }
    }

    private fun findSchema(schema: JsonObject, field: String): JsonObject {
        return schema.getAsJsonObject(field)
    }

    private fun typesMatch(schema: JsonObject, type: String) {
        val eventType = if (type.equals("int", ignoreCase = true) ||
            type.equals("integer", ignoreCase = true) ||
            type.equals("double", ignoreCase = true) ||
            type.equals("float", ignoreCase = true)
        ) {
            "number"
        } else if (type.contains("[]")) {
            "array"
        } else if (type.equals("string", ignoreCase = true)) {
            "string"
        } else if (type.equals("AppMetadata", ignoreCase = true)) {
            "object"
        } else {
            type
        }
        val typeClass: Class<out JsonElement> = schema["type"].javaClass
        val jsonElement = JsonParser().parse(eventType.toLowerCase())
        if (typeClass == JsonPrimitive::class.java) {
            val typePrimitive = schema["type"]
            assertEquals(typePrimitive, jsonElement)
        } else {
            val arrayOfTypes = schema.getAsJsonArray("type")
            assertTrue(arrayOfTypes.contains(jsonElement))
        }
    }

    private fun grabSchemaPropertyFields(aClass: Class<*>): List<Field> {
        val fields: MutableList<Field> = ArrayList()
        val allFields = aClass.declaredFields
        for (field in allFields) {
            if (field.type == NavigationStepData::class.java) {
                val dataFields =
                    field.type.declaredFields
                for (dataField in dataFields) {
                    if (Modifier.isPrivate(dataField.modifiers) && !Modifier.isStatic(dataField.modifiers)) {
                        fields.add(dataField)
                    }
                }
            } else if (Modifier.isPrivate(field.modifiers) && !Modifier.isStatic(field.modifiers)) {
                fields.add(field)
            }
        }
        val superFields = aClass.superclass!!.declaredFields
        for (field in superFields) {
            if (Modifier.isPrivate(field.modifiers) && !Modifier.isStatic(field.modifiers)) {
                fields.add(field)
            }
        }

        // filter out non-property fields
        fields.removeIf { it.name == "version" }
        return fields
    }

    private fun grabEventSchema(eventName: String, version: String = "2.0"): EventSchema =
        eventSchemas.filter { it.name == eventName && it.version == version }.let {
            when {
                it.isEmpty() -> {
                    throw IllegalArgumentException("missing $eventName schema for version $version")
                }
                it.size > 1 -> {
                    throw IllegalArgumentException("multiple $eventName schemas for version $version")
                }
                else -> {
                    it.first()
                }
            }
        }

    @Throws(IOException::class)
    private fun unpackSchemas(): List<EventSchema> {
        val inputStream =
            SchemaTest::class.java.classLoader!!.getResourceAsStream("mobile-event-schemas.jsonl.gz")
        val byteOut = IOUtils.toByteArray(inputStream)
        val schemaFileStream = ByteArrayInputStream(byteOut)
        val gzis = GZIPInputStream(schemaFileStream)
        val reader = InputStreamReader(gzis)
        val `in` = BufferedReader(reader)
        val schemaList = mutableListOf<EventSchema>()
        val gson = Gson()
        var read: String?
        while (`in`.readLine().also { read = it } != null) {
            val schema = gson.fromJson(read, JsonObject::class.java)
            val name = schema["name"].asString
            val version = schema["version"].asString
            val properties = getProperties(schema)
            schemaList.add(EventSchema(name, version, properties))
        }
        return schemaList
    }

    @Throws(IOException::class)
    private fun getProperties(schema: JsonObject): JsonObject {
        val gson = Gson()
        var schemaJson = gson.toJson(schema["properties"])
        var properties = gson.fromJson(schema["properties"], JsonObject::class.java)
        if (properties.has("step")) {
            val stepJson = properties["step"].asJsonObject
            val stepProperties = stepJson["properties"].asJsonObject
            val stepPropertiesJson = gson.toJson(stepProperties)
            schemaJson = generateStepSchemaJson(stepPropertiesJson, schemaJson)
            properties = gson.fromJson(schemaJson, JsonObject::class.java)
            properties.remove("step")
        }
        properties.remove("userAgent")
        properties.remove("received")
        properties.remove("token")
        properties.remove("authorization")
        properties.remove("owner")
        properties.remove("locationAuthorization")
        properties.remove("locationEnabled")
        // temporary need to work out a solution to include this data
        properties.remove("platform")
        return properties
    }

    private fun generateStepSchemaJson(
        stepJson: String,
        schemaString: String
    ): String {
        var stepJson = stepJson
        var schemaString = schemaString
        stepJson = stepJson.replace("\"distanceRemaining\"", "\"stepdistanceRemaining\"")
        stepJson = stepJson.replace("durationRemaining", "stepdurationRemaining")
        stepJson = stepJson.replaceFirst("\\{".toRegex(), ",")
        schemaString = schemaString.replace("}$".toRegex(), "")
        schemaString += stepJson
        return schemaString
    }
}

private data class EventSchema(val name: String, val version: String, val properties: JsonObject)
