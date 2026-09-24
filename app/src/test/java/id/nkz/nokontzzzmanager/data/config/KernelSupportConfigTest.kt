package id.nkz.nokontzzzmanager.data.config

import org.junit.Assert.*
import org.junit.Test

class KernelSupportConfigTest {

    private val config = KernelSupportConfig()

    // --- supportedSignatures ---

    @Test
    fun `default config contains 8 signatures`() {
        assertEquals(8, config.supportedSignatures.size)
    }

    @Test
    fun `N0Kontzzz is a supported signature`() {
        assertTrue(config.supportedSignatures.contains("N0Kontzzz"))
    }

    @Test
    fun `Lunar is a supported signature`() {
        assertTrue(config.supportedSignatures.contains("Lunar"))
    }

    @Test
    fun `FusionX is a supported signature`() {
        assertTrue(config.supportedSignatures.contains("FusionX"))
    }

    @Test
    fun `all expected signatures are present`() {
        val expected = listOf("Lunar", "N0Kontzzz", "N0kernel", "FusionX", "perf+", "Oxygen+", "dead-butterflies", "pitchkernel")
        assertTrue(config.supportedSignatures.containsAll(expected))
    }

    // --- kernelHosts ---

    @Test
    fun `default config contains 8 kernel host entries`() {
        assertEquals(8, config.kernelHosts.size)
    }

    @Test
    fun `n0kontzzz host contains bimoalfarrabi`() {
        val hosts = config.kernelHosts["n0kontzzz"]
        assertNotNull(hosts)
        assertTrue(hosts!!.any { it.contains("bimoalfarrabi") })
    }

    @Test
    fun `lunar host is not empty`() {
        val hosts = config.kernelHosts["lunar"]
        assertNotNull(hosts)
        assertTrue(hosts!!.isNotEmpty())
    }

    @Test
    fun `fusionx has most hosts among all entries`() {
        val fusionxCount = config.kernelHosts["fusionx"]?.size ?: 0
        val maxOther = config.kernelHosts.filter { it.key != "fusionx" }.values.maxOf { it.size }
        assertTrue(fusionxCount >= maxOther)
    }

    @Test
    fun `pitchkernel is a supported signature`() {
        assertTrue(config.supportedSignatures.contains("pitchkernel"))
    }

    @Test
    fun `pitchkernel host contains github-actions builder`() {
        val hosts = config.kernelHosts["pitchkernel"]
        assertNotNull(hosts)
        assertTrue(hosts!!.any { it.contains("pitchkernel@github-actions") })
    }

    @Test
    fun `all host keys match expected kernel identifiers`() {
        val expectedKeys = setOf("lunar", "fusionx", "n0kontzzz", "n0kernel", "perf+", "oxygen+", "dead-butterflies", "pitchkernel")
        assertEquals(expectedKeys, config.kernelHosts.keys)
    }
}
