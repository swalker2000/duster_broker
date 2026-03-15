package com.duster.testsupport

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace

class TestEnvironmentExtension : Extension, ExecutionCondition {

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        TestEnvironment.startIfNeeded()
        registerShutdownHookOnce(context)
        return ConditionEvaluationResult.enabled("test environment is up")
    }

    private fun registerShutdownHookOnce(context: ExtensionContext) {
        val store = context.root.getStore(Namespace.create(TestEnvironmentExtension::class.java))
        store.getOrComputeIfAbsent("env") { ShutdownResource() }
    }

    private class ShutdownResource : ExtensionContext.Store.CloseableResource {
        override fun close() {
            TestEnvironment.stop()
        }
    }
}

