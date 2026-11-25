package com.example.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;

/**
 * DDD 패키지 구조와 의존 규칙 (ARCHITECTURE.md 3절, AGENTS.md 6절). Checkstyle의 import 검사와 달리 바이트코드의 실제 의존성을 본다.
 *
 * <p>Bounded Context는 {@code com.example.app} 바로 아래 패키지 하나이고, {@code common}은 컨텍스트가 아니다.
 */
@AnalyzeClasses(packages = ArchitectureTest.BASE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String BASE = "com.example.app";

    private static final String DOMAIN = BASE + ".*.domain..";
    private static final String APPLICATION = BASE + ".*.application..";
    private static final String INFRASTRUCTURE = BASE + ".*.infrastructure..";
    private static final String PRESENTATION = BASE + ".*.presentation..";
    private static final String COMMON = BASE + ".common..";

    private static final String ENTITY = "jakarta.persistence.Entity";
    private static final String TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional";

    // ===== 계층 =====

    @ArchTest
    static final ArchRule LAYER_DEPENDENCIES = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Presentation")
            .definedBy(PRESENTATION)
            .layer("Application")
            .definedBy(APPLICATION)
            .layer("Domain")
            .definedBy(DOMAIN)
            .layer("Infrastructure")
            .definedBy(INFRASTRUCTURE)
            .whereLayer("Presentation")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Presentation")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Presentation", "Application", "Infrastructure")
            .whereLayer("Infrastructure")
            .mayNotBeAccessedByAnyLayer()
            .as("계층 의존 방향: presentation → application → domain ← infrastructure");

    @ArchTest
    static final ArchRule DOMAIN_IS_FRAMEWORK_FREE = classes()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "java..",
                    "jakarta.persistence..",
                    "org.hibernate.annotations..",
                    "lombok..",
                    DOMAIN,
                    BASE + ".common.error..",
                    BASE + ".common.audit..")
            .as("domain은 Java 표준, JPA·Hibernate 매핑, common.error·common.audit, domain에만 의존한다");

    @ArchTest
    static final ArchRule COMMON_ERROR_IS_FRAMEWORK_FREE = classes()
            .that()
            .resideInAPackage(BASE + ".common.error..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("java..", BASE + ".common.error..")
            .as("common.error는 모든 계층이 쓰므로 Java 표준에만 의존한다");

    @ArchTest
    static final ArchRule COMMON_DOES_NOT_DEPEND_ON_CONTEXTS = noClasses()
            .that()
            .resideInAPackage(COMMON)
            .should()
            .dependOnClassesThat(isInBoundedContext())
            .as("common은 특정 Bounded Context에 의존하지 않는다");

    // ===== Bounded Context =====

    @ArchTest
    static final ArchRule CONTEXTS_TALK_THROUGH_APPLICATION = classes()
            .should(dependOnOtherContextsOnlyThroughApplication())
            .as("다른 Bounded Context는 그 컨텍스트의 application 계층으로만 호출한다");

    // ===== 위치 =====

    @ArchTest
    static final ArchRule ENTITIES_IN_DOMAIN = classes()
            .that()
            .areAnnotatedWith(ENTITY)
            .should()
            .resideInAPackage(DOMAIN)
            .as("@Entity는 domain에 둔다");

    @ArchTest
    static final ArchRule CONTROLLERS_IN_PRESENTATION = classes()
            .that()
            .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .or()
            .areAnnotatedWith("org.springframework.stereotype.Controller")
            .should()
            .resideInAPackage(PRESENTATION)
            .as("Controller는 presentation에 둔다");

    @ArchTest
    static final ArchRule SERVICES_IN_APPLICATION = classes()
            .that()
            .areAnnotatedWith("org.springframework.stereotype.Service")
            .should()
            .resideInAPackage(APPLICATION)
            .as("@Service는 application에 둔다");

    @ArchTest
    static final ArchRule SPRING_DATA_REPOSITORIES_IN_INFRASTRUCTURE = classes()
            .that()
            .areAssignableTo("org.springframework.data.repository.Repository")
            .and()
            .areInterfaces()
            .should()
            .resideInAPackage(INFRASTRUCTURE)
            .as("Spring Data Repository 인터페이스는 infrastructure에 둔다 (domain에는 순수 Repository 인터페이스)");

    // ===== 계층별 책임 =====

    @ArchTest
    static final ArchRule TRANSACTIONAL_CLASSES_ONLY_IN_APPLICATION = noClasses()
            .that()
            .resideOutsideOfPackage(APPLICATION)
            .should()
            .beAnnotatedWith(TRANSACTIONAL)
            .as("@Transactional은 application에만 붙인다 (클래스)");

    @ArchTest
    static final ArchRule TRANSACTIONAL_METHODS_ONLY_IN_APPLICATION = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .resideOutsideOfPackage(APPLICATION)
            .should()
            .beAnnotatedWith(TRANSACTIONAL)
            .as("@Transactional은 application에만 붙인다 (메서드)");

    @ArchTest
    static final ArchRule DOMAIN_HAS_NO_PUBLIC_SETTERS = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage(DOMAIN)
            .and()
            .arePublic()
            .should()
            .haveNameMatching("set[A-Z].*")
            .as("domain에는 public setter를 두지 않는다. 상태 변경은 의도가 드러나는 도메인 메서드(cancel() 등)로 한다");

    @ArchTest
    static final ArchRule CONTROLLERS_DO_NOT_RETURN_ENTITIES = methods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage(PRESENTATION)
            .and()
            .arePublic()
            .should(notExposeEntities())
            .as("presentation은 Entity를 반환하지 않고 Response DTO로 바꾼다");

    // ===== 일반 코딩 규칙 =====

    @ArchTest
    static final ArchRule NO_FIELD_INJECTION = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    @ArchTest
    static final ArchRule NO_STANDARD_STREAMS = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule NO_GENERIC_EXCEPTIONS = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    @ArchTest
    static final ArchRule NO_JAVA_UTIL_LOGGING = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    // ===== 사용자 정의 조건 =====

    /** 패키지가 속한 Bounded Context 이름. 컨텍스트 밖(루트, common, 외부 라이브러리)이면 null. */
    private static String contextOf(String packageName) {
        if (!packageName.startsWith(BASE + ".")) {
            return null;
        }
        String context = packageName.substring(BASE.length() + 1).split("\\.")[0];
        return "common".equals(context) ? null : context;
    }

    private static DescribedPredicate<JavaClass> isInBoundedContext() {
        return DescribedPredicate.describe(
                "Bounded Context에 속한 클래스", javaClass -> contextOf(javaClass.getPackageName()) != null);
    }

    private static ArchCondition<JavaClass> dependOnOtherContextsOnlyThroughApplication() {
        return new ArchCondition<>("다른 컨텍스트에는 application 계층으로만 의존한다") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                String source = contextOf(javaClass.getPackageName());
                if (source == null) {
                    return;
                }
                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass().getBaseComponentType();
                    String targetContext = contextOf(target.getPackageName());
                    boolean otherContext = targetContext != null && !targetContext.equals(source);
                    boolean viaApplication =
                            target.getPackageName().startsWith(BASE + "." + targetContext + ".application");
                    if (otherContext && !viaApplication) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> notExposeEntities() {
        return new ArchCondition<>("Entity를 반환하지 않는다") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                method.getReturnType().getAllInvolvedRawTypes().stream()
                        .filter(type -> type.isAnnotatedWith(ENTITY))
                        .forEach(type -> events.add(SimpleConditionEvent.violated(
                                method, method.getFullName() + " 가 Entity " + type.getName() + " 를 반환한다")));
            }
        };
    }
}
