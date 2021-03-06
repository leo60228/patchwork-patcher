package com.patchworkmc.event.initialization;

import java.util.Set;
import java.util.function.Consumer;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.patchworkmc.event.EventConstants;
import com.patchworkmc.event.SubscribingClass;
import com.patchworkmc.util.LambdaVisitors;

public class RegisterEventRegistrars implements Consumer<MethodVisitor> {
	private static final String EVENT_REGISTRAR_REGISTRY = "net/minecraftforge/eventbus/api/EventRegistrarRegistry";
	private static final String EVENT_REGISTRAR_REGISTRY_INSTANCE_SIGNATURE = "L" + EVENT_REGISTRAR_REGISTRY + ";";

	private final Set<SubscribingClass> subscribingClasses;

	public RegisterEventRegistrars(Set<SubscribingClass> subscribingClasses) {
		this.subscribingClasses = subscribingClasses;
	}

	@Override
	public void accept(MethodVisitor method) {
		// We store the instance as a local variable for more readable decompiled code.
		method.visitFieldInsn(Opcodes.GETSTATIC, EVENT_REGISTRAR_REGISTRY, "INSTANCE", EVENT_REGISTRAR_REGISTRY_INSTANCE_SIGNATURE);
		method.visitVarInsn(Opcodes.ASTORE, 1);

		for (SubscribingClass subscriber : subscribingClasses) {
			String className = subscriber.getClassName();

			if (subscriber.hasInstanceSubscribers()) {
				method.visitVarInsn(Opcodes.ALOAD, 1); // Push the instance to the stack (1)
				method.visitLdcInsn(Type.getObjectType(className)); // Push the class to the stack (2)
				LambdaVisitors.visitBiConsumerStaticLambda(method, className, EventConstants.REGISTER_INSTANCE, EventConstants.getRegisterInstanceDesc(className), subscriber.isInterface());
				// Pop the instance for calling, and then pop the class lambda as parameters
				method.visitMethodInsn(Opcodes.INVOKEINTERFACE, EVENT_REGISTRAR_REGISTRY, "registerInstance", "(Ljava/lang/Class;Ljava/util/function/BiConsumer;)V", true);
			}

			if (subscriber.hasStaticSubscribers()) {
				method.visitVarInsn(Opcodes.ALOAD, 1); // Push the instance to the stack (1)
				method.visitLdcInsn(Type.getObjectType(className)); // Push the class to the stack (2)
				// Push the lambda to the stack (3)
				LambdaVisitors.visitConsumerStaticLambda(method, className, EventConstants.REGISTER_STATIC, EventConstants.REGISTER_STATIC_DESC, subscriber.isInterface());
				// Pop the instance for calling, and then pop the class lambda as parameters
				method.visitMethodInsn(Opcodes.INVOKEINTERFACE, EVENT_REGISTRAR_REGISTRY, "registerStatic", "(Ljava/lang/Class;Ljava/util/function/Consumer;)V", true);
			}
		}

		method.visitInsn(Opcodes.RETURN);
		method.visitLocalVariable("registryInstance", EVENT_REGISTRAR_REGISTRY_INSTANCE_SIGNATURE, null, new Label(), new Label(), 1);
		method.visitMaxs(3, 2);
		method.visitEnd();
	}
}
