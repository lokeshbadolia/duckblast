package com.duckblast.game.data

import android.content.Context
import com.duckblast.game.data.model.MyObjectBox
import io.objectbox.BoxStore

object ObjectBoxStore {
    @Volatile private var _store: BoxStore? = null

    val store: BoxStore
        get() = checkNotNull(_store) {
            "ObjectBoxStore.init(context) must be called before accessing the store."
        }

    fun init(context: Context) {
        if (_store != null) return
        _store = MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .name("duckblast-db")
            .build()
    }

    fun close() {
        _store?.close()
        _store = null
    }
}
