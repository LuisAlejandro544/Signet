package com.example.crypto

import java.security.SecureRandom

/**
 * Cryptographically random and realistic test data generator for Keystore Distinguished Name (DN),
 * file names, aliases, and certificate identity parameters.
 * Designed for quick prototyping, test setups, and privacy-preserving mock identities.
 */
object RandomIdentityGenerator {

    private val random = SecureRandom()

    private val COMMON_NAMES = listOf(
        "Android Release Signer",
        "Mobile App Key",
        "OpenSource App Signer",
        "Dev Test Signer",
        "Nova Mobile Studio",
        "Quantum Core Signer",
        "Orion Android Signer",
        "Phoenix Release Key",
        "Vanguard Mobile",
        "Apex Systems Signer",
        "Krypton Labs App",
        "Eclipse Release Team",
        "Titan Mobile Signer",
        "Cosmos Production Key",
        "Aura Developer Studio",
        "Pixel Horizon Signer",
        "Hyperion Mobile App",
        "Zenith App Signer",
        "Synapse Software Signer",
        "Starlight Android Key"
    )

    private val ORGANIZATIONS = listOf(
        "OpenSource Devs",
        "Nebula Technologies LLC",
        "Apex Interactive Labs",
        "Pixel Horizon Studio",
        "Titan Digital Systems",
        "Hyperion Mobile Software",
        "Vortex Dev Team",
        "Synapse Open Foundation",
        "Zenith Core Labs",
        "Nova Global Studio",
        "Quantum Leap Technologies",
        "Starlight Mobile Systems",
        "Aurora Digital Labs",
        "Orion Software Engineering"
    )

    private val ORGANIZATIONAL_UNITS = listOf(
        "Mobile Development",
        "Release Engineering",
        "Core Android Team",
        "Security & Signing",
        "DevOps Division",
        "App Production",
        "Independent Software Lab",
        "QA & Release Signing",
        "Engineering Core",
        "Client Engineering"
    )

    data class LocationPreset(
        val city: String,
        val state: String,
        val countryCode: String
    )

    private val LOCATIONS = listOf(
        LocationPreset("Madrid", "Madrid", "ES"),
        LocationPreset("Barcelona", "Catalunya", "ES"),
        LocationPreset("Sevilla", "Andalucía", "ES"),
        LocationPreset("Valencia", "Comunidad Valenciana", "ES"),
        LocationPreset("Ciudad de México", "CDMX", "MX"),
        LocationPreset("Guadalajara", "Jalisco", "MX"),
        LocationPreset("Monterrey", "Nuevo León", "MX"),
        LocationPreset("San Francisco", "California", "US"),
        LocationPreset("Seattle", "Washington", "US"),
        LocationPreset("Austin", "Texas", "US"),
        LocationPreset("New York", "New York", "US"),
        LocationPreset("Buenos Aires", "Buenos Aires", "AR"),
        LocationPreset("Córdoba", "Córdoba", "AR"),
        LocationPreset("Bogotá", "Cundinamarca", "CO"),
        LocationPreset("Medellín", "Antioquia", "CO"),
        LocationPreset("Santiago", "Santiago", "CL"),
        LocationPreset("Lima", "Lima", "PE"),
        LocationPreset("Berlin", "Berlin", "DE"),
        LocationPreset("Paris", "Île-de-France", "FR"),
        LocationPreset("Tokyo", "Tokyo", "JP"),
        LocationPreset("São Paulo", "São Paulo", "BR"),
        LocationPreset("Toronto", "Ontario", "CA"),
        LocationPreset("London", "London", "GB")
    )

    private val FILE_NAME_PREFIXES = listOf(
        "release-key",
        "prod-signer",
        "app-upload-key",
        "nightly-build-key",
        "signet-test-key",
        "studio-signer",
        "master-release-key",
        "deploy-key",
        "distribution-key",
        "demo-release"
    )

    private val ALIASES = listOf(
        "key0",
        "upload",
        "release",
        "signer",
        "production",
        "appkey",
        "devkey",
        "master",
        "codesign",
        "signet-alias"
    )

    private val COUNTRY_CODES = listOf(
        "ES", "MX", "US", "AR", "CO", "CL", "PE", "DE", "FR", "JP", "BR", "CA", "GB", "IT", "NL", "UY"
    )

    fun randomCommonName(): String = COMMON_NAMES[random.nextInt(COMMON_NAMES.size)]

    fun randomOrganization(): String = ORGANIZATIONS[random.nextInt(ORGANIZATIONS.size)]

    fun randomOrganizationalUnit(): String = ORGANIZATIONAL_UNITS[random.nextInt(ORGANIZATIONAL_UNITS.size)]

    fun randomLocation(): LocationPreset = LOCATIONS[random.nextInt(LOCATIONS.size)]

    fun randomLocality(): String = LOCATIONS[random.nextInt(LOCATIONS.size)].city

    fun randomState(): String = LOCATIONS[random.nextInt(LOCATIONS.size)].state

    fun randomCountryCode(): String = COUNTRY_CODES[random.nextInt(COUNTRY_CODES.size)]

    fun randomFileName(): String {
        val base = FILE_NAME_PREFIXES[random.nextInt(FILE_NAME_PREFIXES.size)]
        val suffix = 100 + random.nextInt(900)
        return "$base-$suffix"
    }

    fun randomAlias(): String = ALIASES[random.nextInt(ALIASES.size)]

    data class RandomDnIdentity(
        val commonName: String,
        val organization: String,
        val organizationalUnit: String,
        val locality: String,
        val state: String,
        val countryCode: String
    )

    fun generateFullDnIdentity(): RandomDnIdentity {
        val loc = randomLocation()
        return RandomDnIdentity(
            commonName = randomCommonName(),
            organization = randomOrganization(),
            organizationalUnit = randomOrganizationalUnit(),
            locality = loc.city,
            state = loc.state,
            countryCode = loc.countryCode
        )
    }
}
