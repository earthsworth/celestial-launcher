/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.utils

import org.apache.logging.log4j.core.config.plugins.util.ResolverUtil
import java.lang.reflect.Modifier
import java.net.URI

fun getKotlinName(name: String): String {
    val case = name[0].uppercase()
    val exceptCase = name.substring(1)
    return case + exceptCase
}

fun <T> Any.getKotlinField(name: String): T =
    this::class.java.getDeclaredMethod("get${getKotlinName(name)}").let {
        it.isAccessible = true
        it.invoke(this) as T
    }

inline fun <reified T> Any.setKotlinField(name: String, value: T?) {
    // Fuck Kotlin
    val clazz = when (value) {
        is Boolean -> Boolean::class.java
        is Int -> Int::class.java
        is Short -> Short::class.java
        is Double -> Double::class.java
        is Long -> Long::class.java
        is Char -> Char::class.java
        is Float -> Float::class.java
        else -> T::class.java // not built-in types
    }
    this::class.java.getDeclaredMethod("set${getKotlinName(name)}", clazz).apply {
        isAccessible = true
        invoke(this@setKotlinField, value)
    }
}

// Thanks FDPClient
// https://github.com/SkidderMC/FDPClient/blob/main/src/main/java/net/ccbluex/liquidbounce/utils/ClassUtils.kt

/**
 * scan classes with specified superclass like what Reflections do but with log4j [ResolverUtil]
 * @author liulihaocai
 */
fun <T : Any> String.resolvePackage(klass: Class<T>): List<Class<out T>> {
    // use resolver in log4j to scan classes in target package
    val resolver = ResolverUtil()

    // set class loader
    resolver.classLoader = klass.classLoader

    // set package to scan
    resolver.findInPackage(object : ResolverUtil.Test {
        override fun matches(type: Class<*>?): Boolean {
            return true
        }

        override fun matches(resource: URI?): Boolean {
            return true
        }

        override fun doesMatchClass(): Boolean {
            return true
        }

        override fun doesMatchResource(): Boolean {
            return true
        }
    }, this)

    // use a list to cache classes
    val list = mutableListOf<Class<out T>>()

    for (resolved in resolver.classes) {
        resolved.declaredMethods.find {
            Modifier.isNative(it.modifiers)
        }?.let {
            val klass1 = it.declaringClass.typeName + "." + it.name
            throw UnsatisfiedLinkError(klass1 + "\n\tat ${klass1}(Native Method)") // we don't want native methods
        }
        // check if class is assignable from target class
        if (klass.isAssignableFrom(resolved) && !resolved.isInterface && !Modifier.isAbstract(resolved.modifiers)) {
            // add to list
            list.add(resolved as Class<out T>)
        }
    }
    return list
}