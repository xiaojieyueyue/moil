package com.example.bluetoothconsole.data.dashboard

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import com.example.bluetoothconsole.model.DashboardProject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class DashboardStorage(context: Context, scope: CoroutineScope) {

    private val dataStore: DataStore<List<DashboardProject>> = DataStoreFactory.create(
        serializer = DashboardListSerializer,
        scope = scope
    ) {
        context.dataStoreFile("dashboards.json")
    }

    val dashboards: Flow<List<DashboardProject>> = dataStore.data

    suspend fun update(transform: (List<DashboardProject>) -> List<DashboardProject>) {
        dataStore.updateData(transform)
    }
}

private object DashboardListSerializer : Serializer<List<DashboardProject>> {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override val defaultValue: List<DashboardProject> = emptyList()

    override suspend fun readFrom(input: InputStream): List<DashboardProject> {
        return try {
            val text = input.readBytes().decodeToString()
            if (text.isBlank()) {
                emptyList()
            } else {
                json.decodeFromString(ListSerializer(DashboardProject.serializer()), text)
            }
        } catch (exception: SerializationException) {
            emptyList()
        }
    }

    override suspend fun writeTo(t: List<DashboardProject>, output: OutputStream) {
        val text = json.encodeToString(ListSerializer(DashboardProject.serializer()), t)
        output.write(text.encodeToByteArray())
    }
}
