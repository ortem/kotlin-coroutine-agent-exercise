package agent

import jdk.internal.org.objectweb.asm.*
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

class Agent : ClassFileTransformer {
    companion object {
        @JvmStatic
        fun premain(agentArgs: String?, inst: Instrumentation) {
            println("Agent started.")
            inst.addTransformer(Agent())
        }
    }

    override fun transform(loader: ClassLoader?, className: String?, classBeingRedefined: Class<*>?,
                           protectionDomain: ProtectionDomain?, classfileBuffer: ByteArray?): ByteArray {
        val reader = ClassReader(classfileBuffer)
        val writer = ClassWriter(0)
        val visitor = MyVisitor(writer)
        reader.accept(visitor, 0)
        return writer.toByteArray()
    }
}


class MyVisitor(cv: ClassVisitor) : ClassVisitor(Opcodes.ASM5, cv) {
    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<String>?): MethodVisitor {
        val mv = cv.visitMethod(access, name, desc, signature, exceptions)
        return MyMethodVisitor(mv)
    }
}

class MyMethodVisitor(mv: MethodVisitor) : MethodVisitor(Opcodes.ASM5, mv) {
    private val testOwner = "example/CoroutineExampleKt"
    private val testDesc = "(Lkotlin/coroutines/experimental/Continuation;)Ljava/lang/Object;"

    private var isVisited = false

    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, desc: String?, itf: Boolean) {
        if (opcode == Opcodes.INVOKESTATIC && owner == testOwner && name == "test" && desc == testDesc) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
            mv.visitLdcInsn("Test detected")
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)

            isVisited = true
        }

        mv.visitMethodInsn(opcode, owner, name, desc, itf)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        if (isVisited)
            mv.visitMaxs(maxStack + 2, maxLocals)
        else
            mv.visitMaxs(maxStack, maxLocals)
    }
}
