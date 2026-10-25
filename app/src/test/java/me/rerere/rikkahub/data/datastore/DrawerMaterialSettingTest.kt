package me.rerere.rikkahub.data.datastore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
class DrawerMaterialSettingTest {
    @Test fun defaultFollowsGlobal() {
        val setting = DisplaySetting()
        assertNull(setting.drawerMaterialMode)
        assertEquals(setting.materialMode, setting.drawerMaterialMode ?: setting.materialMode)
    }
    @Test fun overrideDoesNotChangeGlobal() {
        for (mode in listOf(DisplayMaterialMode.FLAT, DisplayMaterialMode.TRANSLUCENT, DisplayMaterialMode.GLASS)) {
            val setting = DisplaySetting(materialMode = DisplayMaterialMode.TRANSLUCENT, drawerMaterialMode = mode)
            assertEquals(mode, setting.drawerMaterialMode)
            assertEquals(DisplayMaterialMode.TRANSLUCENT, setting.materialMode)
            val cleared = setting.copy(drawerMaterialMode = null)
            assertEquals(cleared.materialMode, cleared.drawerMaterialMode ?: cleared.materialMode)
        }
    }
}
