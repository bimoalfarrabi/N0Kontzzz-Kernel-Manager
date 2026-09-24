package id.nkz.nokontzzzmanager.data.config

data class KernelSupportConfig(
    val supportedSignatures: List<String> = listOf(
        "Lunar",
        "N0Kontzzz",
        "N0kernel",
        "FusionX",
        "perf+",
        "Oxygen+",
        "dead-butterflies",
        "pitchkernel"
    ),

    val kernelHosts: Map<String, List<String>> = mapOf(
        "lunar" to listOf(
            "Kenskuyy@Github",
            "andrian@ServerHive",
            "build-user@build-host"
        ),
        "fusionx" to listOf(
            "andriann@ServerHive",
            "andrian@ServerHive",
            "build-user@build-host",
            "senx@ubuntu",
            "sensei@ServerHive",
            "Senseix@Ubuntu"
        ),
        "n0kontzzz" to listOf(
            "bimoalfarrabi@github.com",
            "build-user@build-host"
        ),
        "n0kernel" to listOf(
            "Impqxr@github.com",
            "build-user@build-host"
        ),
        "perf+" to listOf(
            "rohmanurip@Github",
            "build-user@build-host"
        ),
        "oxygen+" to listOf(
            "danda@pavilion",
            "candy@arch"
        ),
        "dead-butterflies" to listOf(
            "moon.ell@stargaze"
        ),
        "pitchkernel" to listOf(
            "pitchkernel@github-actions",
            "build-user@build-host"
        )
    )
)