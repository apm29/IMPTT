package com.imptt.apm29.service

import com.imptt.apm29.IServicePTT
/**
 *  author : ciih
 *  date : 2020/9/11 10:59 AM
 *  description :
 */
class ServicePTTBinder : IServicePTT.Stub() {
    override fun basicTypes(
        anInt: Int,
        aLong: Long,
        aBoolean: Boolean,
        aFloat: Float,
        aDouble: Double,
        aString: String?
    ) {
        println("anInt = [${anInt}], aLong = [${aLong}], aBoolean = [${aBoolean}], aFloat = [${aFloat}], aDouble = [${aDouble}], aString = [${aString}]")
    }
}