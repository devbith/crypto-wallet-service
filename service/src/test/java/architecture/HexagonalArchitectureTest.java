package architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.crypto.wallet", importOptions = ImportOption.DoNotIncludeTests.class)
public class HexagonalArchitectureTest {

  @ArchTest
  static final ArchRule application_should_not_depend_on_infrastructure = noClasses().that()
      .resideInAPackage("..application..")
      .should()
      .dependOnClassesThat()
      .resideInAPackage("..infrastructure..");

}
