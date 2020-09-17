package com.imptt.apm29.utilities

/**
 *  author : ciih
 *  date : 2020/9/11 3:38 PM
 *  description :
 *
 *  class TestSingleton private constructor() {
 *      companion object : ISingleton<TestSingleton>() {
 *          override fun createInstance(): TestSingleton {
 *              return TestSingleton()
 *          }
 *      }
 *  }
 *  使用：
 *  TestSingleton.getInstance()
 */
abstract class IParamSingleton<T,P> {

    @Volatile
    private var instance: T? = null

    fun getInstance(param:P): T {
        return instance ?: synchronized(this) {
            instance ?: createInstance(param).also { instance = it }
        }
    }

    abstract fun createInstance(param:P): T
}