package com.example.servicehub.data

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/*
 * Two small helpers that turn Firestore "live listeners" into Kotlin Flows.
 * Every time the data changes in the cloud (from any phone), the Flow sends the new value,
 * so screens update by themselves without a refresh button.
 */

/** Live list of all documents matching this query, converted to [clazz] objects. */
fun <T : Any> Query.liveList(clazz: Class<T>): Flow<List<T>> = callbackFlow {
    val listener = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)          // ends the flow with the error (the ViewModel catches it)
            return@addSnapshotListener
        }
        trySend(snapshot?.toObjects(clazz).orEmpty())
    }
    awaitClose { listener.remove() }   // stop listening when nobody needs the data
}

/** Live single document, converted to [clazz]. Sends null if the document does not exist. */
fun <T : Any> DocumentReference.liveObject(clazz: Class<T>): Flow<T?> = callbackFlow {
    val listener = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        trySend(snapshot?.toObject(clazz))
    }
    awaitClose { listener.remove() }
}
