package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import kotlin.reflect.KClass

class DeviceProfileTest : BuilderTest<DeviceProfile, DeviceProfile.Builder>() {
    override fun getImplementationClass(): KClass<DeviceProfile> = DeviceProfile::class

    override fun getFilledUpBuilder(): DeviceProfile.Builder {
        return DeviceProfile.Builder()
            .customConfig("123")
            .deviceType(mockk(relaxed = true))
    }
}
