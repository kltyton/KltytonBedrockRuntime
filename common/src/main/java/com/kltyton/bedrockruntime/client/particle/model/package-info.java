/**
 * Three-dimensional model particle appearance for the Kltyton Bedrock Runtime particle runtime.
 *
 * <p>This module integrates the resource format and behavior of ThreeDimensionParticle 1.2.1
 * (upstream: <a href="https://github.com/westernat/ThreeDimensionParticle">westernat/ThreeDimensionParticle</a>,
 * LGPL-3.0-only, reference commits {@code f9eab801e014a40c568cd4d37fde2aa9caa00fef} for Forge 1.20.1 and
 * {@code 593269a462a7d060227fbb0dbd4c1651cb613e6c} for NeoForge 1.21.1) into the Bedrock particle
 * runtime that this project already provides. It is an independent implementation: particles are
 * emitted, simulated, collided and evaluated by the runtime classes in
 * {@code com.kltyton.bedrockruntime.client.particle.runtime}; no ThreeDimensionParticle JAR or class is
 * required at runtime.</p>
 *
 * <p>The public registration surface is limited to {@code client.particle.model.api}. The source
 * formats accepted here are the original {@code particle_models/*.json} descriptor, the
 * {@code minecraft:particle_appearance} model appearance fields ({@code size},
 * {@code face_camera_mode}/{@code facing_camera_mode}/{@code face_camera}/{@code facing_camera}, {@code direction}, {@code model_animation})
 * and existing vanilla, GeckoLib and {@code poly_mesh} geometry. The component identifier is
 * {@code kltyton_bedrock_runtime:model_appearance}; the particle type identifier is
 * {@code kltyton_bedrock_runtime:model_particle}. The old ThreeDimensionParticle namespace
 * {@code thr_dim_particle} is intentionally not registered as an alias.</p>
 *
 * <p>Copyright (C) westernat and contributors (ThreeDimensionParticle), LGPL-3.0-only.
 * Kltyton Bedrock Runtime integration code is distributed under the same LGPL-3.0-only terms.
 * See the repository {@code COPYING} and {@code LICENSE} files.</p>
 */
package com.kltyton.bedrockruntime.client.particle.model;

