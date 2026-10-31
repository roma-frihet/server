package com.romafrihet.server.architecture

import com.tngtech.archunit.base.DescribedPredicate.describe
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import jakarta.persistence.Entity
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import kotlin.coroutines.Continuation
import org.springframework.data.repository.Repository as SpringDataRepository
import org.springframework.stereotype.Repository as RepositoryStereotype

@AnalyzeClasses(
    packages = ["com.romafrihet.server"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class ArchitectureTest {
    @ArchTest
    fun `controllers do not touch repositories`(classes: JavaClasses) {
        noClasses()
            .that()
            .areAnnotatedWith(RestController::class.java)
            .or()
            .areAnnotatedWith(Controller::class.java)
            .should()
            .dependOnClassesThat(
                describe("are repositories") { target: JavaClass ->
                    target.isAssignableTo(SpringDataRepository::class.java) ||
                        target.isAnnotatedWith(RepositoryStereotype::class.java)
                },
            ).check(classes)
    }

    @ArchTest
    fun `controllers do not return entities`(classes: JavaClasses) {
        methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController::class.java)
            .should(notReturnEntity)
            .check(classes)
    }

    @ArchTest
    fun `only web code depends on request and response types`(classes: JavaClasses) {
        noClasses()
            .that()
            .areAnnotatedWith(Entity::class.java)
            .or()
            .haveSimpleNameEndingWith("Command")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..rqrs..")
            .check(classes)
    }

    @ArchTest
    fun `entities are open`(classes: JavaClasses) {
        classes()
            .that()
            .areAnnotatedWith(Entity::class.java)
            .should()
            .notHaveModifier(JavaModifier.FINAL)
            .check(classes)
    }

    @ArchTest
    fun `entities are not data classes`(classes: JavaClasses) {
        classes()
            .that()
            .areAnnotatedWith(Entity::class.java)
            .should(notBeDataClass)
            .check(classes)
    }

    @ArchTest
    fun `transactional methods are not suspend functions`(classes: JavaClasses) {
        methods()
            .that()
            .areAnnotatedWith(Transactional::class.java)
            .or()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Transactional::class.java)
            .should(notBeSuspend)
            .check(classes)
    }

    @ArchTest
    fun `field injection is not used`(classes: JavaClasses) {
        NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(classes)
    }

    @ArchTest
    fun `feature packages are free of cycles`(classes: JavaClasses) {
        slices()
            .matching("com.romafrihet.server.(*)..")
            .should()
            .beFreeOfCycles()
            .check(classes)
    }

    private companion object {
        val notReturnEntity =
            condition<JavaMethod>("not return an entity") { method ->
                method.rawReturnType.isAnnotatedWith(Entity::class.java) to
                    "${method.fullName} returns an entity; return a Response"
            }

        // A data class generates component1 and its equals/hashCode would touch lazy associations.
        val notBeDataClass =
            condition<JavaClass>("not be a Kotlin data class") { clazz ->
                clazz.methods.any { it.name == "component1" } to
                    "${clazz.name} is a data class; use a regular class for entities"
            }

        // JPA transactions are bound to a thread, so they do not follow a suspend function.
        val notBeSuspend =
            condition<JavaMethod>("not be a suspend function") { method ->
                method.rawParameterTypes.any { it.isEquivalentTo(Continuation::class.java) } to
                    "${method.fullName} is a suspend function under @Transactional"
            }

        private fun <T : Any> condition(description: String, violation: (T) -> Pair<Boolean, String>) =
            object : ArchCondition<T>(description) {
                override fun check(item: T, events: ConditionEvents) {
                    val (violated, message) = violation(item)
                    events.add(SimpleConditionEvent(item, !violated, message))
                }
            }
    }
}
