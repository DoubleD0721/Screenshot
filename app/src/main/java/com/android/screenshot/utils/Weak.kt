package com.android.screenshot.utils

import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

/**
 * Created by Debin Kong on 2024/3/25
 * @author Debin Kong
 */
class Weak<T : Any>(initializer: () -> T?) {
    private var weakReference = WeakReference<T?>(initializer())

    constructor() : this({
        null
    })

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return weakReference.get()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        weakReference = WeakReference(value)
    }
}