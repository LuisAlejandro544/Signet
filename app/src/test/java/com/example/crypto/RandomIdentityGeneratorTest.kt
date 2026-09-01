package com.example.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomIdentityGeneratorTest {

    @Test
    fun `random common name is valid and non-empty`() {
        val name = RandomIdentityGenerator.randomCommonName()
        assertNotNull(name)
        assertTrue(name.isNotBlank())
    }

    @Test
    fun `random organization and organizational unit are valid`() {
        val org = RandomIdentityGenerator.randomOrganization()
        val ou = RandomIdentityGenerator.randomOrganizationalUnit()
        assertNotNull(org)
        assertNotNull(ou)
        assertTrue(org.isNotBlank())
        assertTrue(ou.isNotBlank())
    }

    @Test
    fun `random country code is strictly 2 uppercase letters`() {
        repeat(50) {
            val code = RandomIdentityGenerator.randomCountryCode()
            assertEquals(2, code.length)
            assertTrue(code[0].isUpperCase())
            assertTrue(code[1].isUpperCase())
        }
    }

    @Test
    fun `random filename and alias are non empty and url safe`() {
        repeat(20) {
            val filename = RandomIdentityGenerator.randomFileName()
            val alias = RandomIdentityGenerator.randomAlias()
            assertTrue(filename.isNotBlank())
            assertTrue(alias.isNotBlank())
            assertFalse(filename.contains(" "))
            assertFalse(alias.contains(" "))
        }
    }

    @Test
    fun `generate full dn identity produces valid composite object`() {
        val identity = RandomIdentityGenerator.generateFullDnIdentity()
        assertNotNull(identity)
        assertTrue(identity.commonName.isNotBlank())
        assertTrue(identity.organization.isNotBlank())
        assertTrue(identity.organizationalUnit.isNotBlank())
        assertTrue(identity.locality.isNotBlank())
        assertTrue(identity.state.isNotBlank())
        assertEquals(2, identity.countryCode.length)
    }
}
