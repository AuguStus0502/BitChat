package com.bitchat.core.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [IdGenerator] to validate unique ID generation.
 *
 * Verifies that generated IDs are non-empty, unique, and conform
 * to expected format constraints.
 */
class IdGeneratorTest {

    /**
     * Verify that generated IDs are non-empty strings.
     */
    @Test
    fun generateId_isNotEmpty() {
        val id = IdGenerator.generateId()
        assertTrue("Generated ID must not be empty", id.isNotEmpty())
    }

    /**
     * Verify that two consecutive IDs are distinct.
     * Collision probability should be negligible for UUID-based generators.
     */
    @Test
    fun generateId_isUnique() {
        val id1 = IdGenerator.generateId()
        val id2 = IdGenerator.generateId()
        assertNotEquals(
            "Two consecutive IDs must be different",
            id1, id2
        )
    }

    /**
     * Verify that generated IDs maintain consistency under rapid generation.
     * Generates 1000 IDs and ensures all are unique.
     */
    @Test
    fun generateId_rapidGeneration_allUnique() {
        val ids = (1..1000).map { IdGenerator.generateId() }.toSet()
        assertEquals(
            "1000 rapid-generated IDs must all be unique (set size should equal list size)",
            1000, ids.size
        )
    }

    /**
     * Verify ID format consistency — no whitespace or special control characters.
     */
    @Test
    fun generateId_noWhitespaceOrControlChars() {
        val id = IdGenerator.generateId()
        assertFalse(
            "ID must not contain whitespace",
            id.contains("\\s".toRegex())
        )
        assertFalse(
            "ID must not contain control characters",
            id.any { it.code < 32 }
        )
    }

    /**
     * Verify that the same ID generator produces IDs of consistent length.
     */
    @Test
    fun generateId_consistentLength() {
        val lengths = (1..100).map { IdGenerator.generateId().length }.toSet()
        assertEquals(
            "All IDs from the same generator should have consistent length",
            1, lengths.size
        )
    }
}
