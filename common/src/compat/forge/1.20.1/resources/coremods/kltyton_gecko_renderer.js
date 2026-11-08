var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
var FrameNode = Java.type('org.objectweb.asm.tree.FrameNode');

var rendererName = 'software/bernie/geckolib/renderer/GeoRenderer';
var hookName = 'com/kltyton/bedrockruntime/client/compat/geckolib/render/ForgeGeckoRenderHook';
var geometryName = 'com/kltyton/bedrockruntime/client/compat/geckolib/render/GeckoGeometryHooks';
var cubeDescriptor = '(Lcom/mojang/blaze3d/vertex/PoseStack;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V';
var recursiveDescriptor = '(Lcom/mojang/blaze3d/vertex/PoseStack;Lsoftware/bernie/geckolib/core/animatable/GeoAnimatable;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIIFFFF)V';

function geometryArguments() {
    var instructions = new InsnList();
    instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
    instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
    instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
    instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
    instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
    for (var index = 6; index <= 9; index++) {
        instructions.add(new VarInsnNode(Opcodes.FLOAD, index));
    }
    return instructions;
}

function transformRenderer(classNode) {
    var recursive = null;
    var cubes = null;
    for (var index = 0; index < classNode.methods.size(); index++) {
        var method = classNode.methods.get(index);
        if (method.name == 'renderRecursively' && method.desc == recursiveDescriptor) {
            if (recursive != null) { throw new Error('Duplicate GeckoLib renderRecursively method'); }
            recursive = method;
        }
        if (method.name == 'renderCubesOfBone' && method.desc == cubeDescriptor) {
            if (cubes != null) { throw new Error('Duplicate GeckoLib renderCubesOfBone method'); }
            cubes = method;
        }
    }
    if (recursive == null || cubes == null) {
        throw new Error('KBR requires the GeckoLib 4.8.4 renderer signatures');
    }

    var call = null;
    var recursiveNodes = recursive.instructions.toArray();
    for (var position = 0; position < recursiveNodes.length; position++) {
        var node = recursiveNodes[position];
        if (node.getOpcode() == Opcodes.INVOKEINTERFACE && node.owner == rendererName
                && node.name == 'renderCubesOfBone' && node.desc == cubeDescriptor) {
            if (call != null) { throw new Error('Multiple GeckoLib cube render calls'); }
            call = node;
        }
    }
    var lastReturn = null;
    var cubeNodes = cubes.instructions.toArray();
    for (var cursor = 0; cursor < cubeNodes.length; cursor++) {
        if (cubeNodes[cursor].getOpcode() == Opcodes.RETURN) { lastReturn = cubeNodes[cursor]; }
    }
    if (call == null || lastReturn == null) {
        throw new Error('KBR could not locate the GeckoLib 4.8.4 cube render boundary');
    }

    var originalBody = new LabelNode();
    var enter = geometryArguments();
    enter.add(new MethodInsnNode(Opcodes.INVOKESTATIC, geometryName, 'intercept',
            cubeDescriptor.substring(0, cubeDescriptor.length - 1) + 'Z', false));
    enter.add(new JumpInsnNode(Opcodes.IFEQ, originalBody));
    enter.add(new InsnNode(Opcodes.RETURN));
    enter.add(originalBody);
    enter.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
    cubes.instructions.insert(enter);

    var tail = geometryArguments();
    tail.add(new MethodInsnNode(Opcodes.INVOKESTATIC, geometryName, 'emit', cubeDescriptor, false));
    cubes.instructions.insertBefore(lastReturn, tail);

    recursive.instructions.insertBefore(call, new VarInsnNode(Opcodes.ILOAD, 7));
    var wrappedDescriptor = '(L' + rendererName + ';'
            + cubeDescriptor.substring(1, cubeDescriptor.length - 2) + 'Z)V';
    recursive.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, hookName,
            'renderCubes', wrappedDescriptor, true));
    recursive.maxStack = recursive.maxStack + 1;
    cubes.maxStack = Math.max(cubes.maxStack, 9);
    if (!classNode.interfaces.contains(hookName)) { classNode.interfaces.add(hookName); }
    return classNode;
}

function initializeCoreMod() {
    return {
        'kltyton_gecko_renderer': {
            'target': { 'type': 'CLASS', 'name': 'software.bernie.geckolib.renderer.GeoRenderer' },
            'transformer': transformRenderer
        }
    };
}
