package net.essentialsx.fabric.api;

/**
 * Versioned root of the stable Essentials Fabric API.
 * Implementations are provided by the core mod; never instantiate from other mods.
 */
public interface EssentialsApi {
    /** Semantic API version, independent from config schema (Section 3). */
    int API_VERSION = 1;
}
