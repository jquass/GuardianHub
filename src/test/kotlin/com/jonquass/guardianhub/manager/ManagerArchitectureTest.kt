package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.core.ExcludeManagerCheck
import com.jonquass.guardianhub.core.Result
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition

@AnalyzeClasses(packages = ["com.jonquass.guardianhub.manager"])
object ManagerArchitectureTest {
  @ArchTest
  val managers_must_return_result: ArchRule? =
      ArchRuleDefinition.methods()
          .that()
          .areDeclaredInClassesThat()
          .haveSimpleNameEndingWith("Manager")
          .and()
          .arePublic()
          .and()
          .haveNameNotMatching(".*\\$.*")
          .and()
          .doNotHaveRawReturnType(Void.TYPE)
          .and()
          .areNotAnnotatedWith(ExcludeManagerCheck::class.java)
          .and(
              object : DescribedPredicate<JavaMethod>("not inherited from an excluded interface") {
                override fun test(method: JavaMethod): Boolean {
                  return method.owner.interfaces.none { iface ->
                    iface.toErasure().methods.any { m ->
                      m.name == method.name && m.isAnnotatedWith(ExcludeManagerCheck::class.java)
                    }
                  }
                }
              })
          .should()
          .haveRawReturnType(Result::class.java)
          .because(
              "Manager public methods must return Result objects for consistent error handling")
}
